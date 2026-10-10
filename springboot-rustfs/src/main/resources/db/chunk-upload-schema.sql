-- 分片上传相关表结构
-- 分片上传任务:一次分片上传对应对象存储侧的一个 multipart upload
CREATE TABLE IF NOT EXISTS `file_upload_task` (
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `upload_id`     VARCHAR(128) NOT NULL COMMENT '对象存储返回的分片上传ID',
    `object_key`    VARCHAR(512) NOT NULL COMMENT '目标对象Key',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `content_type`  VARCHAR(128)          DEFAULT NULL COMMENT 'MIME类型',
    `total_size`    BIGINT       NOT NULL COMMENT '文件总大小(字节)',
    `chunk_size`    INT          NOT NULL COMMENT '分片大小(字节)',
    `total_chunks`  INT          NOT NULL COMMENT '分片总数',
    `created_by`    BIGINT                DEFAULT NULL COMMENT '创建人',
    `created_time`  DATETIME     NOT NULL COMMENT '创建时间',
    `updated_by`    BIGINT                DEFAULT NULL COMMENT '更新人',
    `updated_time`  DATETIME              DEFAULT NULL COMMENT '更新时间',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除:0未删除、1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_upload_id` (`upload_id`),
    KEY `idx_created_time` (`created_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='分片上传任务';

-- 分片上传明细:随任务一起物理删除,不做逻辑删除
CREATE TABLE IF NOT EXISTS `file_upload_part` (
    `id`           BIGINT       NOT NULL COMMENT '主键',
    `upload_id`    VARCHAR(128) NOT NULL COMMENT '分片上传ID',
    `part_number`  INT          NOT NULL COMMENT '分片序号,从1开始',
    `e_tag`        VARCHAR(128) NOT NULL COMMENT '分片ETag(合并时回传)',
    `size_bytes`   BIGINT       NOT NULL COMMENT '分片大小(字节)',
    `created_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_upload_part` (`upload_id`, `part_number`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='分片上传明细';