package com.codenow.controller;

import com.codenow.annotation.OperationLog;
import com.codenow.annotation.RateLimit;
import com.codenow.common.R;
import com.codenow.config.UploadProperties;
import com.codenow.exception.BusinessException;
import com.codenow.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
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
    @Operation(summary = "上传图片", description = "上传图片到 OSS，返回可访问的 URL")
    @PostMapping("/image")
    public R<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        // 校验文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        String allowedTypes = uploadProperties.getAllowedTypes();
        if (contentType == null || !Arrays.asList(allowedTypes.split(",")).contains(contentType)) {
            throw new BusinessException("仅支持 JPG、PNG、GIF、WebP 格式的图片");
        }

        // 校验文件大小
        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw new BusinessException("文件大小不能超过 5MB");
        }

        // 上传到 OSS
        String url = storageService.upload(file);

        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return R.ok(result);
    }
}
