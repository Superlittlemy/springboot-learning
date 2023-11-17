package com.slm.minio.controller;

import com.slm.minio.model.ApiResponse;
import com.slm.minio.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public ApiResponse<String> upload(@RequestParam MultipartFile file) {
        return ApiResponse.ok(fileStorageService.upload(file, file.getOriginalFilename()));
    }

    @GetMapping("/url/{id}")
    public ApiResponse<String> getUrl(@PathVariable String id) {
        return ApiResponse.ok(fileStorageService.getUrl(id));
    }

    @GetMapping("/download/{filename}")
    public ApiResponse<?> download(@PathVariable String filename, HttpServletResponse response) throws IOException {
        // 清空response
        response.reset();
        // 设置response的Header
        response.setCharacterEncoding("UTF-8");
        // Content-Disposition的作用：告知浏览器以何种方式显示响应返回的文件，用浏览器打开还是以附件的形式下载到本地保存
        // attachment表示以附件方式下载   inline表示在线打开   "Content-Disposition: inline; filename=文件名.mp3"
        // filename表示文件的默认名称，因为网络传输只支持URL编码的相关支付，因此需要将文件名URL编码后进行传输,前端收到后需要反编码才能获取到真正的名称
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
        // 告知浏览器文件的大小
        InputStream inputStream = fileStorageService.getInputStream(filename);
        byte[] bytes = inputStream.readAllBytes();
        response.addHeader("Content-Length", "" + bytes.length);
        response.setContentType("application/octet-stream");
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
        return ApiResponse.ok();
    }

}
