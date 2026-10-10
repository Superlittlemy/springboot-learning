package com.slm.rustfs.controller;

import com.slm.rustfs.model.ApiResponse;
import com.slm.rustfs.model.AccessUrlRequest;
import com.slm.rustfs.model.AccessUrlResponse;
import com.slm.rustfs.model.ChunkCompleteResponse;
import com.slm.rustfs.model.ChunkInitRequest;
import com.slm.rustfs.model.ChunkInitResponse;
import com.slm.rustfs.model.ChunkStatusResponse;
import com.slm.rustfs.model.ChunkUploadResponse;
import com.slm.rustfs.model.FileResponse;
import com.slm.rustfs.model.InstantUploadRequest;
import com.slm.rustfs.model.InstantUploadResponse;
import com.slm.rustfs.service.ChunkUploadService;
import com.slm.rustfs.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import javax.validation.Valid;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private ChunkUploadService chunkUploadService;

    /**
     * 上传文件(按内容去重)
     *
     * @param md5 可选的客户端 MD5(请求头 X-File-Md5);提供且命中时直接返回历史记录,不落库也不写对象存储
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileResponse> upload(@RequestPart("file") MultipartFile file,
                                            @RequestHeader(value = "X-File-Md5", required = false) String md5) {
        return ApiResponse.success(fileService.upload(file, md5));
    }

    /**
     * 闪传探测:按客户端计算出的 MD5 判断服务端是否已存在相同内容
     * <p>
     * 命中即返回历史文件信息,客户端无需传输任何字节;未命中返回 instant=false,客户端走正常上传流程。
     */
    @PostMapping("/instant-upload")
    public ApiResponse<InstantUploadResponse> instantUpload(@Valid @RequestBody InstantUploadRequest request) {
        return ApiResponse.success(fileService.instantUpload(request));
    }

    /**
     * 批量获取文件访问地址(带过期时间)
     */
    @PostMapping("/access-url")
    public ApiResponse<List<AccessUrlResponse>> accessUrl(@Valid @RequestBody AccessUrlRequest request) {
        return ApiResponse.success(fileService.getAccessUrls(request.getObjectKeys(), request.getExpiresSeconds()));
    }

    /**
     * 分片上传-初始化:创建 multipart upload,返回 uploadId 与分片规划
     */
    @PostMapping("/chunk/init")
    public ApiResponse<ChunkInitResponse> initChunk(@Valid @RequestBody ChunkInitRequest request) {
        return ApiResponse.success(chunkUploadService.init(request));
    }

    /**
     * 分片上传-上传分片:partNumber 从1开始,同序号重复上传视为重传
     */
    @PostMapping(value = "/chunk/part", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ChunkUploadResponse> uploadChunk(@RequestParam String uploadId,
                                                        @RequestParam int partNumber,
                                                        @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(chunkUploadService.uploadPart(uploadId, partNumber, file));
    }

    /**
     * 分片上传-查询进度:断点续传时凭 uploadId 拉取已上传的分片序号
     */
    @GetMapping("/chunk/{uploadId}")
    public ApiResponse<ChunkStatusResponse> chunkStatus(@PathVariable String uploadId) {
        return ApiResponse.success(chunkUploadService.status(uploadId));
    }

    /**
     * 分片上传-合并:分片齐全后合并对象并登记元数据(命中内容去重时复用历史记录)
     */
    @PostMapping("/chunk/complete")
    public ApiResponse<ChunkCompleteResponse> completeChunk(@RequestParam String uploadId) {
        return ApiResponse.success(chunkUploadService.complete(uploadId));
    }

    /**
     * 分片上传-取消:终止 multipart upload 并清理任务
     */
    @DeleteMapping("/chunk/{uploadId}")
    public ApiResponse<?> abortChunk(@PathVariable String uploadId) {
        chunkUploadService.abort(uploadId);
        return ApiResponse.success();
    }

}
