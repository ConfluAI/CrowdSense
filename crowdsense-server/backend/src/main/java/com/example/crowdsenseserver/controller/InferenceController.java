package com.example.crowdsenseserver.controller;

import com.example.crowdsenseserver.config.AppProperties;
import com.example.crowdsenseserver.entity.InferenceTask;
import com.example.crowdsenseserver.entity.SysUser;
import com.example.crowdsenseserver.security.UserDetailsServiceImpl;
import com.example.crowdsenseserver.service.InferenceTaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/inference")
public class InferenceController {

    private static final Logger log = LoggerFactory.getLogger(InferenceController.class);

    private final InferenceTaskService inferenceTaskService;
    private final AppProperties appProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserDetailsServiceImpl userDetailsService;
    private String baseDir;

    public InferenceController(InferenceTaskService inferenceTaskService, AppProperties appProperties,
                               UserDetailsServiceImpl userDetailsService) {
        this.inferenceTaskService = inferenceTaskService;
        this.appProperties = appProperties;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.userDetailsService = userDetailsService;
    }

    @PostConstruct
    public void init() {
        this.baseDir = System.getProperty("user.dir");
    }

    @PostMapping("/upload")
    public Map<String, Object> uploadAndInfer(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        if (file.isEmpty()) {
            result.put("code", 400);
            result.put("message", "请选择图片文件");
            return result;
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            result.put("code", 400);
            result.put("message", "仅支持图片文件");
            return result;
        }

        // Get current user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser currentUser = userDetailsService.getSysUser(username);

        // Create task record
        InferenceTask task = new InferenceTask();
        task.setUserId(currentUser.getId());
        task.setImageName(file.getOriginalFilename());
        task.setStatus("PENDING");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        inferenceTaskService.save(task);

        try {
            // Save uploaded image locally
            String uploadDir = appProperties.getUpload().getDir();
            Path uploadPath = resolveUploadPath(uploadDir);
            Files.createDirectories(uploadPath);
            String ext = getExtension(file.getOriginalFilename());
            String savedName = task.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path imagePath = uploadPath.resolve(savedName);
            file.transferTo(imagePath.toFile());
            task.setImagePath(savedName);

            // Call Python GPU inference service
            String inferUrl = "http://127.0.0.1:8000/infer";
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(imagePath.toFile()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(inferUrl, request, String.class);
            JsonNode inferResult = objectMapper.readTree(response.getBody());

            int crowdCount = inferResult.get("crowdCount").asInt();
            long inferTime = inferResult.get("inferenceTime").asLong();
            String heatmapB64 = inferResult.get("heatmapBase64").asText();
            String densityLevel = inferResult.has("densityLevel")
                    ? inferResult.get("densityLevel").asText() : "";
            String levelTag = inferResult.has("levelTag")
                    ? inferResult.get("levelTag").asText() : "info";

            // Save density heatmap
            String densityDir = appProperties.getUpload().getDensityDir();
            Path densityPath = resolveUploadPath(densityDir);
            Files.createDirectories(densityPath);
            String dmapName = "density_" + savedName.replaceAll("\\.[^.]+$", ".png");
            Path dmapFullPath = densityPath.resolve(dmapName);
            byte[] heatmapBytes = Base64.getDecoder().decode(heatmapB64);
            Files.write(dmapFullPath, heatmapBytes);

            // Update task record
            task.setCrowdCount(crowdCount);
            task.setDensityLevel(densityLevel);
            task.setDensityPath(dmapName);
            task.setInferenceTime(inferTime);
            task.setStatus("SUCCESS");
            task.setUpdateTime(LocalDateTime.now());
            inferenceTaskService.updateById(task);

            log.info("Inference task {} completed: count={}, time={}ms", task.getId(), crowdCount, inferTime);

            result.put("code", 200);
            result.put("message", "success");
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", task.getId());
            data.put("crowdCount", crowdCount);
            data.put("densityLevel", densityLevel);
            data.put("levelTag", levelTag);
            data.put("imageUrl", "/api/files/images/" + savedName);
            data.put("densityUrl", "/api/files/density/" + dmapName);
            data.put("inferenceTime", inferTime);
            result.put("data", data);

        } catch (Exception e) {
            log.error("Inference failed for task {}", task.getId(), e);
            task.setStatus("FAILED");
            task.setUpdateTime(LocalDateTime.now());
            inferenceTaskService.updateById(task);

            result.put("code", 500);
            result.put("message", "推理失败: " + e.getMessage());
        }

        return result;
    }

    @PostMapping("/upload-video")
    public Map<String, Object> uploadVideo(@RequestParam("file") MultipartFile file,
                                           @RequestParam(defaultValue = "2") int interval) {
        Map<String, Object> result = new HashMap<>();

        if (file.isEmpty()) {
            result.put("code", 400);
            result.put("message", "请选择视频文件");
            return result;
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            result.put("code", 400);
            result.put("message", "仅支持视频文件");
            return result;
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser currentUser = userDetailsService.getSysUser(username);

        InferenceTask videoTask = new InferenceTask();
        videoTask.setUserId(currentUser.getId());
        videoTask.setTaskType("VIDEO");
        videoTask.setVideoName(file.getOriginalFilename());
        videoTask.setStatus("PENDING");
        videoTask.setCreateTime(LocalDateTime.now());
        videoTask.setUpdateTime(LocalDateTime.now());
        inferenceTaskService.save(videoTask);

        try {
            // Save video to temp file
            String videoDir = appProperties.getUpload().getVideoDir();
            Path videoDirPath = resolveUploadPath(videoDir);
            Files.createDirectories(videoDirPath);
            String ext = getExtension(file.getOriginalFilename());
            String videoSavedName = videoTask.getId() + "_" + ext;
            Path videoPath = videoDirPath.resolve(videoSavedName);
            file.transferTo(videoPath.toFile());
            videoTask.setVideoPath(videoSavedName);

            // Prepare output directories for Python
            String videoAbsPath = videoPath.toAbsolutePath().toString().replace('\\', '/');
            Path framesDirPath = resolveUploadPath(appProperties.getUpload().getFramesDir());
            String framesAbsDir = framesDirPath.toAbsolutePath().toString().replace('\\', '/');
            Path densityDirPath = resolveUploadPath(appProperties.getUpload().getDensityDir());
            String densityAbsDir = densityDirPath.toAbsolutePath().toString().replace('\\', '/');

            // Python does all the work: extract frames, batch inference, save files
            java.net.URI inferUri = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl("http://127.0.0.1:8000/infer-video")
                    .queryParam("interval", interval)
                    .queryParam("path", videoAbsPath)
                    .queryParam("frames_dir", framesAbsDir)
                    .queryParam("density_dir", densityAbsDir)
                    .queryParam("task_id", videoTask.getId())
                    .build(true).toUri();

            log.info("Video inference via Python: {} frames, {}s interval",
                    videoTask.getVideoName(), interval);
            ResponseEntity<String> response = restTemplate.getForEntity(inferUri, String.class);
            JsonNode videoResult = objectMapper.readTree(response.getBody());

            int totalFrames = videoResult.get("totalFrames").asInt();
            JsonNode framesArray = videoResult.get("frames");
            videoTask.setTotalFrames(totalFrames);

            if (totalFrames == 0) {
                videoTask.setStatus("FAILED");
                videoTask.setUpdateTime(LocalDateTime.now());
                inferenceTaskService.updateById(videoTask);
                result.put("code", 500);
                result.put("message", "视频帧提取失败，无有效帧");
                return result;
            }

            // Build response data + DB records from Python's metadata
            List<Map<String, Object>> frameResults = new ArrayList<>();
            List<InferenceTask> frameTasks = new ArrayList<>();
            double sumCount = 0;
            int maxCount = 0, minCount = Integer.MAX_VALUE;
            long totalInferTime = 0;

            for (JsonNode fi : framesArray) {
                int fiIndex = fi.get("frameIndex").asInt();
                double timestamp = fi.get("timestamp").asDouble();
                int crowdCount = fi.get("crowdCount").asInt();
                String densityLevel = fi.get("densityLevel").asText();
                String levelTag = fi.get("levelTag").asText();
                long inferTime = fi.get("inferenceTime").asLong();
                String imagePath = fi.get("imagePath").asText();
                String densityPathVal = fi.get("densityPath").asText();

                InferenceTask frameTask = new InferenceTask();
                frameTask.setUserId(currentUser.getId());
                frameTask.setTaskType("FRAME");
                frameTask.setBatchId(videoTask.getId());
                frameTask.setFrameIndex(fiIndex);
                frameTask.setTimestampSeconds(timestamp);
                frameTask.setImageName("frame_" + String.format("%04d", fiIndex) + ".jpg");
                frameTask.setImagePath(imagePath);
                frameTask.setCrowdCount(crowdCount);
                frameTask.setDensityLevel(densityLevel);
                frameTask.setDensityPath(densityPathVal);
                frameTask.setInferenceTime(inferTime);
                frameTask.setStatus("SUCCESS");
                frameTask.setCreateTime(LocalDateTime.now());
                frameTask.setUpdateTime(LocalDateTime.now());
                frameTasks.add(frameTask);

                sumCount += crowdCount;
                maxCount = Math.max(maxCount, crowdCount);
                minCount = Math.min(minCount, crowdCount);
                totalInferTime += inferTime;

                Map<String, Object> fr = new HashMap<>();
                fr.put("frameIndex", fiIndex);
                fr.put("timestamp", timestamp);
                fr.put("crowdCount", crowdCount);
                fr.put("densityLevel", densityLevel);
                fr.put("levelTag", levelTag);
                fr.put("imageUrl", "/api/files/frames/" + imagePath);
                fr.put("densityUrl", "/api/files/density/" + densityPathVal);
                fr.put("inferenceTime", inferTime);
                frameResults.add(fr);
            }

            double avgCount = totalFrames > 0 ? sumCount / totalFrames : 0;

            // Prepare video task update
            videoTask.setCrowdCount((int) Math.round(avgCount));
            videoTask.setInferenceTime(totalInferTime);
            videoTask.setStatus("SUCCESS");
            videoTask.setUpdateTime(LocalDateTime.now());

            // Save DB records
            inferenceTaskService.updateById(videoTask);
            inferenceTaskService.saveBatch(frameTasks);
            log.info("Video task {} completed: {} frames, avg={}", videoTask.getId(), totalFrames, avgCount);

            // Return result to user
            result.put("code", 200);
            result.put("message", "success");
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", videoTask.getId());
            data.put("videoName", file.getOriginalFilename());
            data.put("totalFrames", totalFrames);
            data.put("interval", interval);
            Map<String, Object> summary = new HashMap<>();
            summary.put("maxCount", maxCount);
            summary.put("minCount", minCount == Integer.MAX_VALUE ? 0 : minCount);
            summary.put("avgCount", Math.round(avgCount * 10) / 10.0);
            data.put("summary", summary);
            data.put("frames", frameResults);
            result.put("data", data);

        } catch (Exception e) {
            log.error("Video inference failed for task {}", videoTask.getId(), e);
            videoTask.setStatus("FAILED");
            videoTask.setUpdateTime(LocalDateTime.now());
            inferenceTaskService.updateById(videoTask);

            result.put("code", 500);
            result.put("message", "视频推理失败: " + e.getMessage());
        }

        return result;
    }

    private Path resolveUploadPath(String relativePath) {
        return Paths.get(baseDir, relativePath);
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot) : ".jpg";
    }
}
