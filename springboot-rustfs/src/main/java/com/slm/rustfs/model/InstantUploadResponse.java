package com.slm.rustfs.model;

import lombok.Data;

/**
 * 闪传探测响应
 */
@Data
public class InstantUploadResponse {

    /**
     * 是否命中闪传
     * <p>
     * true 表示服务端已存在内容相同的历史记录,客户端无需传输任何字节,直接使用 file 即可;
     * false 表示需要走正常上传流程,此时 file 为 null
     */
    private Boolean instant;

    /**
     * 命中的历史文件信息(未命中时为 null)
     */
    private FileResponse file;

    public static InstantUploadResponse hit(FileResponse file) {
        InstantUploadResponse response = new InstantUploadResponse();
        response.setInstant(true);
        response.setFile(file);
        return response;
    }

    public static InstantUploadResponse miss() {
        InstantUploadResponse response = new InstantUploadResponse();
        response.setInstant(false);
        return response;
    }

}
