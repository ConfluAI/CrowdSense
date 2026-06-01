package com.example.crowdsenseserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.crowdsenseserver.entity.InferenceTask;
import com.example.crowdsenseserver.entity.SysUser;
import com.example.crowdsenseserver.security.UserDetailsServiceImpl;
import com.example.crowdsenseserver.service.InferenceTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inference_tasks")
public class InferenceTaskController {

    @Autowired
    private InferenceTaskService inferenceTaskService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser user = userDetailsService.getSysUser(username);
        return user.getId();
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String imageName,
            @RequestParam(required = false) String imagePath,
            @RequestParam(required = false) Integer crowdCount,
            @RequestParam(required = false) String densityPath,
            @RequestParam(required = false) Long inferenceTime,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType) {
        Page<InferenceTask> page = new Page<>(current, size);
        LambdaQueryWrapper<InferenceTask> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(InferenceTask::getUserId, getCurrentUserId());

        if (imageName != null && !imageName.isEmpty()) {
            wrapper.like(InferenceTask::getImageName, imageName);
        }
        if (imagePath != null && !imagePath.isEmpty()) {
            wrapper.like(InferenceTask::getImagePath, imagePath);
        }
        if (crowdCount != null) {
            wrapper.eq(InferenceTask::getCrowdCount, crowdCount);
        }
        if (densityPath != null && !densityPath.isEmpty()) {
            wrapper.like(InferenceTask::getDensityPath, densityPath);
        }
        if (inferenceTime != null) {
            wrapper.eq(InferenceTask::getInferenceTime, inferenceTime);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.like(InferenceTask::getStatus, status);
        }
        if (taskType != null && !taskType.isEmpty()) {
            wrapper.eq(InferenceTask::getTaskType, taskType);
        } else {
            // Default: exclude FRAME child records, show only IMAGE + VIDEO
            wrapper.ne(InferenceTask::getTaskType, "FRAME");
        }

        wrapper.orderByDesc(InferenceTask::getCreateTime);
        Page<InferenceTask> result = inferenceTaskService.page(page, wrapper);
        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        return map;
    }

    @GetMapping("/{id}/frames")
    public Map<String, Object> getVideoFrames(@PathVariable Long id) {
        LambdaQueryWrapper<InferenceTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InferenceTask::getBatchId, id)
               .orderByAsc(InferenceTask::getFrameIndex);
        Map<String, Object> map = new HashMap<>();
        map.put("records", inferenceTaskService.list(wrapper));
        return map;
    }

    @GetMapping("/{id}")
    public InferenceTask getById(@PathVariable Long id) {
        return inferenceTaskService.getById(id);
    }

    @PostMapping
    public boolean save(@RequestBody InferenceTask inferenceTask) {
        return inferenceTaskService.save(inferenceTask);
    }

    @PutMapping("/{id}")
    public boolean update(@PathVariable Long id, @RequestBody InferenceTask inferenceTask) {
        inferenceTask.setId(id);
        return inferenceTaskService.updateById(inferenceTask);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return inferenceTaskService.removeById(id);
    }

    @DeleteMapping("/batch/{batchId}")
    public boolean deleteByBatchId(@PathVariable Long batchId) {
        LambdaQueryWrapper<InferenceTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InferenceTask::getBatchId, batchId);
        return inferenceTaskService.remove(wrapper);
    }

    @DeleteMapping("/batch")
    public boolean deleteBatch(@RequestBody List<Long> ids) {
        return inferenceTaskService.removeByIds(ids);
    }
}
