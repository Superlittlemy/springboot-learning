package com.slm.rustfs.model;

import lombok.Data;

/**
 * 分片上传初始化响应
 */
@Data
public class ChunkInitResponse {

    /**
     * 是否命中闪传
     * <p>
     * true 表示服务端已存在内容相同的历史记录,客户端无需再上传任何分片,直接使用 file 即可;
     * 此时 uploadId、objectKey、chunkSize、totalChunks 均为 null
     */
    private Boolean instant;

    /**
     * 命中的历史文件信息(未命中时为 null)
     */
    private FileResponse file;

    /**
     * 分片上传ID,后续上传分片/查询进度/合并/取消均以此为凭据
     */
    private String uploadId;

    /**
     * 目标对象Key
     */
    private String objectKey;

    /**
     * 生效的分片大小(字节)
     */
    private Integer chunkSize;

    /**
     * 分片总数
     */
    private Integer totalChunks;

    /**
     * 已上传分片数
     */
    private Integer uploadedChunks;

}