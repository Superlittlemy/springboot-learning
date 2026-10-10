package com.slm.rustfs.model;

import lombok.Data;

/**
 * 分片上传合并响应
 */
@Data
public class ChunkCompleteResponse {

    /**
     * 合并后的文件信息
     */
    private FileResponse file;

    /**
     * 是否命中内容去重
     * <p>
     * true 表示该文件此前已上传过,本次合并后的对象已被清理,直接复用历史记录(等价于"秒传")
     */
    private Boolean deduplicated;

}