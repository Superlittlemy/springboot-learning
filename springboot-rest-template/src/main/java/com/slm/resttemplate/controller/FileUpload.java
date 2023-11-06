package com.slm.resttemplate.controller;

import com.slm.resttemplate.utils.HttpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("file-upload")
@RequiredArgsConstructor
public class FileUpload {

    private final HttpUtil httpUtil;

    @PostMapping
    public void upload(MultipartFile file) throws IOException {
        httpUtil.uploadFile(HttpMethod.POST, "http://localhost:9100/files", Map.of(), file.getInputStream(), "新文件名2.s");
    }

}
