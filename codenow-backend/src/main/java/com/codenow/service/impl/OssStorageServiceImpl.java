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

/**
 * 阿里云 OSS 存储实现。只认配置 Bucket 下的 codenow/ 前缀为受管资源，删除操作忽略外部 URL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "oss", matchIfMissing = true)
public class OssStorageServiceImpl implements StorageService {

    private final StorageProperties storageProperties;
    private OSS ossClient;

    /**
     * 初始化阿里云 OSS 客户端
     */
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

    /**
     * 销毁时关闭 OSS 客户端连接
     */
    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    /**
     * 上传文件到阿里云 OSS
     *
     * @param file 待上传的文件
     * @return 文件访问 URL
     */
    @Override
    public String upload(MultipartFile file) {
        try {
            return upload(file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("文件读取失败");
        }
    }

    /**
     * 上传文件到阿里云 OSS。
     * 按日期组织目录，使用 UUID 生成文件名，上传后返回可访问的 HTTPS URL。
     *
     * @param originalFilename 原始文件名
     * @param content          文件内容
     * @return 文件访问 URL
     */
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

    /**
     * 判断 URL 是否为本服务管理的 OSS 文件地址。
     * 只认配置 Bucket 下的 codenow/ 前缀为受管资源。
     *
     * @param url 文件 URL
     * @return 是否为受管 URL
     */
    @Override
    public boolean isManagedUrl(String url) {
        if (url == null) {
            return false;
        }
        String prefix = "https://" + storageProperties.getBucketName() + "."
                + storageProperties.getEndpoint() + "/codenow/";
        return url.startsWith(prefix) && url.length() > prefix.length();
    }

    /**
     * 删除 OSS 文件。
     * 从 URL 中提取 objectName 后删除，忽略外部 URL。
     *
     * @param url 文件 URL
     */
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
