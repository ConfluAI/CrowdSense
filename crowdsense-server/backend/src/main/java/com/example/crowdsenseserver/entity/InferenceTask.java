package com.example.crowdsenseserver.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("inference_task")
public class InferenceTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String taskType;  // IMAGE / VIDEO / FRAME
    private Long batchId;  // 视频任务批次ID
    private Integer frameIndex;  // 帧序号
    private Double timestampSeconds;  // 视频时间戳(秒)
    private String videoName;  // 视频文件名
    private String videoPath;  // 视频存储路径
    private Integer totalFrames;  // 总抽帧数
    private String imageName;  // 图片文件名
    private String imagePath;  // 图片存储路径
    private Integer crowdCount;  // 预测人数
    private String densityLevel;  // 密度等级
    private String densityPath;  // 密度图路径
    private Long inferenceTime;  // 推理耗时(ms)
    private String status;  // 状态(PENDING/SUCCESS/FAILED)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
