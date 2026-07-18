package com.codenow.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.codenow.config.StorageProperties;
import com.codenow.exception.BusinessException;
import com.codenow.service.StorageService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "oss", matchIfMissing = true)
public class OssStorageServiceImpl implements StorageService {

    private final StorageProperties storageProperties;
    private OSS ossClient;

    @PostConstruct
    public void init() {
        try {
            ossClient = new OSSClientBuilder().build(
                    "https://" + storageProperties.getEndpoint(),
                    storageProperties.getAccessKeyId(),
                    storageProperties.getAccessKeySecret()
            );
            log.info("阿里云 OSS 客户端初始化成功，Bucket: {}", storageProperties.getBucketName());
        } catch (Exception e) {
            log.warn("阿里云 OSS 客户端初始化失败: {}，文件上传功能将不可用", e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    @Override
    public String upload(MultipartFile file) {
        try {
            return upload(file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("文件读取失败");
        }
    }

    @Override
    public String upload(String originalFilename, byte[] content) {
        if (ossClient == null) {
            throw new BusinessException("OSS 服务未初始化，请检查配置");
        }

        try {
            String extension = getExtension(originalFilename);

            // 按日期组织目录
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectName = "codenow/" + datePath + "/" + UUID.randomUUID() + "." + extension;

            try (InputStream inputStream = new ByteArrayInputStream(content)) {
                ossClient.putObject(
                        storageProperties.getBucketName(),
                        objectName,
                        inputStream
                );
            }

            // 返回可访问的 URL
            String url = "https://" + storageProperties.getBucketName() + "." + storageProperties.getEndpoint() + "/" + objectName;
            log.info("文件上传成功: {}", url);
            return url;
        } catch (RuntimeException | IOException e) {
            log.error("文件上传失败: {}", e.getMessage());
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isManagedUrl(String url) {
        if (url == null) {
            return false;
        }
        String prefix = "https://" + storageProperties.getBucketName() + "."
                + storageProperties.getEndpoint() + "/codenow/";
        return url.startsWith(prefix) && url.length() > prefix.length();
    }

    @Override
    public void delete(String url) {
        if (ossClient == null || url == null) {
            return;
        }
        try {
            // 从 URL 提取 objectName
            String endpoint = storageProperties.getBucketName() + "." + storageProperties.getEndpoint() + "/";
            int idx = url.indexOf(endpoint);
            if (idx < 0) return;
            String objectName = url.substring(idx + endpoint.length());
            ossClient.deleteObject(storageProperties.getBucketName(), objectName);
            log.info("文件删除成功: {}", objectName);
        } catch (Exception e) {
            log.warn("文件删除失败: {}", e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
