package com.codenow.service.impl;

import com.codenow.common.FileValidator;
import com.codenow.config.UploadProperties;
import com.codenow.exception.BusinessException;
import com.codenow.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 图片上传服务。
 * 负责接收上传的图片文件，进行文件大小和格式校验后，委托底层存储服务保存文件。
 */
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final StorageService storageService;
    private final UploadProperties uploadProperties;

    /**
     * 上传图片文件
     *
     * @param file 待上传的图片文件
     * @return 上传成功后的文件访问 URL
     * @throws BusinessException 当文件为空、大小超限或格式不合法时抛出
     */
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
