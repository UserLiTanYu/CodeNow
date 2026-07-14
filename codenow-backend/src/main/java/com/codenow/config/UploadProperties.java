package com.codenow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "upload")
public class UploadProperties {

    /**
     * 允许的文件类型（逗号分隔）
     */
    private String allowedTypes = "image/jpeg,image/png,image/gif,image/webp";

    /**
     * 最大文件大小（字节）
     */
    private long maxSize = 5242880;
}
