package com.slm.rustfs.service;

import com.slm.rustfs.model.AccessUrlResponse;
import com.slm.rustfs.model.FileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件响应
     */
    FileResponse upload(MultipartFile file);

    /**
     * 批量获取带过期时间的文件访问地址
     * <p>
     * 仅返回 file_metadata 中存在的 key,不存在的 key 跳过不报错(调用方回退为不展示)。
     */
    List<AccessUrlResponse> getAccessUrls(List<String> objectKeys, Integer expiresSeconds);

}
