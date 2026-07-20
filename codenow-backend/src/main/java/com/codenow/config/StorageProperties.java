package com.codenow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 存储配置属性类，对应 {@code alioss} 前缀的配置项。
 */
@Data
@Component
@ConfigurationProperties(prefix = "alioss")
public class StorageProperties {

    /**
     * OSS 服务的访问端点
     */
    private String endpoint;

    /**
     * OSS 存储区域
     */
    private String region;

    /**
     * OSS 存储桶名称
     */
    private String bucketName;

    /**
     * 访问密钥 ID
     */
    private String accessKeyId;

    /**
     * 访问密钥 Secret
     */
    private String accessKeySecret;
}
