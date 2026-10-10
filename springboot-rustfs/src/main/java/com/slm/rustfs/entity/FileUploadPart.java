package com.slm.rustfs.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 分片上传明细
 * <p>
 * 子表随任务一起物理删除,不参与逻辑删除,避免同一 uploadId 重复上传同一分片时唯一键冲突。
 */
@Data
@TableName("file_upload_part")
public class FileUploadPart {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分片上传ID
     */
    @TableField("upload_id")
    private String uploadId;

    /**
     * 分片序号,从1开始
     */
    @TableField("part_number")
    private Integer partNumber;

    /**
     * 对象存储返回的分片ETag(合并时回传)
     */
    @TableField("e_tag")
    private String eTag;

    /**
     * 分片大小(字节)
     */
    @TableField("size_bytes")
    private Long sizeBytes;

    /**
     * 上传时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

}