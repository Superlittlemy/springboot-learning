package com.slm.rustfs.service;

import com.slm.rustfs.model.AccessUrlResponse;
import com.slm.rustfs.model.FileResponse;
import com.slm.rustfs.model.InstantUploadRequest;
import com.slm.rustfs.model.InstantUploadResponse;
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
     * @param clientMd5 客户端计算出的 MD5(可选);命中内容去重时直接返回历史记录,不落库也不写对象存储
     * @return 文件响应
     */
    FileResponse upload(MultipartFile file, String clientMd5);

    /**
     * 闪传探测:按客户端上报的 MD5 判断服务端是否已存在相同内容
     *
     * <p>
     * 仅用于"要不要传"的瞬时决策,命中的历史记录不会被本方法写入任何位置;
     * 是否落库始终以服务端回读对象字节算出的 MD5 为准。
     */
    InstantUploadResponse instantUpload(InstantUploadRequest request);

    /**
     * 批量获取带过期时间的文件访问地址
     * <p>
     * 仅返回 file_metadata 中存在的 key,不存在的 key 跳过不报错(调用方回退为不展示)。
     */
    List<AccessUrlResponse> getAccessUrls(List<String> objectKeys, Integer expiresSeconds);

}
