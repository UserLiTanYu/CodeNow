package com.codenow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alioss")
public class StorageProperties {

    private String endpoint;
    private String region;
    private String bucketName;
    private String accessKeyId;
    private String accessKeySecret;
}
