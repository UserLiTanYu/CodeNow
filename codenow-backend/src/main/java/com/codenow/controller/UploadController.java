package com.codenow.controller;

import com.codenow.annotation.OperationLog;
import com.codenow.annotation.RateLimit;
import com.codenow.common.FileValidator;
import com.codenow.common.R;
import com.codenow.config.UploadProperties;
import com.codenow.exception.BusinessException;
import com.codenow.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;
    private final UploadProperties uploadProperties;

    @RateLimit(maxCount = 10, timeWindow = 60, message = "上传过于频繁，请稍后再试")
    @OperationLog("上传图片")
    @Operation(summary = "上传图片", description = "上传图片到已配置的存储服务，返回可访问的 URL")
    @PostMapping("/image")
    public R<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        // 校验文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        // 校验文件大小
        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw new BusinessException("文件大小不能超过 5MB");
        }

        // 通过 Magic Bytes 校验文件真实类型（防止 Content-Type 伪造）
        try {
            FileValidator.validateImage(file.getInputStream(), file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        } catch (java.io.IOException e) {
            throw new BusinessException("文件读取失败");
        }

        // 上传到当前环境配置的存储服务
        String url = storageService.upload(file);

        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return R.ok(result);
    }
}
