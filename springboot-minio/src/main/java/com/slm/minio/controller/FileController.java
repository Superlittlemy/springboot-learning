package com.slm.minio.controller;

import com.slm.minio.model.ApiResponse;
import com.slm.minio.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public ApiResponse<?> upload(@RequestParam MultipartFile file) {
        fileStorageService.upload(file, file.getOriginalFilename());
        return ApiResponse.ok();
    }

    @GetMapping("{filename}")
    public ApiResponse<?> getUrl(@PathVariable String filename) {
        return ApiResponse.ok(fileStorageService.getUrl(filename));
    }

}
