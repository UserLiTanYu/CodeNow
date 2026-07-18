package com.codenow.controller;

import com.codenow.annotation.OperationLog;
import com.codenow.annotation.RateLimit;
import com.codenow.common.R;
import com.codenow.service.impl.ImageUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "作者图片上传")
@RestController
@RequestMapping("/api/author/upload")
@RequiredArgsConstructor
public class AuthorUploadController {

    private final ImageUploadService imageUploadService;

    @RateLimit(maxCount = 10, timeWindow = 60, message = "上传过于频繁，请稍后再试")
    @OperationLog("作者上传图片")
    @Operation(summary = "作者上传图片", description = "上传文章封面或正文图片，返回可访问 URL")
    @PostMapping("/image")
    public R<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return R.ok(Map.of("url", imageUploadService.upload(file)));
    }
}
