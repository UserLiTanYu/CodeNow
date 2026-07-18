package com.codenow.service.impl;

import com.codenow.common.FileValidator;
import com.codenow.config.UploadProperties;
import com.codenow.exception.BusinessException;
import com.codenow.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final StorageService storageService;
    private final UploadProperties uploadProperties;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw new BusinessException("文件大小不能超过 5MB");
        }
        try {
            FileValidator.validateImage(file.getInputStream(), file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        } catch (IOException e) {
            throw new BusinessException("文件读取失败");
        }
        return storageService.upload(file);
    }
}
