package com.slm.rustfs.controller;

import com.slm.rustfs.model.ApiResponse;
import com.slm.rustfs.model.AccessUrlRequest;
import com.slm.rustfs.model.AccessUrlResponse;
import com.slm.rustfs.model.FileResponse;
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

    /**
     * 上传文件(按内容去重)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileResponse> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(fileService.upload(file));
    }

    /**
     * 批量获取文件访问地址(带过期时间)
     */
    @PostMapping("/access-url")
    public ApiResponse<List<AccessUrlResponse>> accessUrl(@Valid @RequestBody AccessUrlRequest request) {
        return ApiResponse.success(fileService.getAccessUrls(request.getObjectKeys(), request.getExpiresSeconds()));
    }

}
