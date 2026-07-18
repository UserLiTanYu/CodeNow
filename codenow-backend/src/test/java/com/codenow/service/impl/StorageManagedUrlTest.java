package com.codenow.service.impl;

import com.codenow.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageManagedUrlTest {

    @TempDir
    Path tempDir;

    @Test
    void localStorageOnlyTrustsPublicLocalFilePrefix() {
        LocalStorageServiceImpl service = new LocalStorageServiceImpl(tempDir.toString());

        assertTrue(service.isManagedUrl("/api/blog/files/2026/07/19/cover.png"));
        assertFalse(service.isManagedUrl("/api/blog/files/"));
        assertFalse(service.isManagedUrl("https://tracker.example.com/pixel.png"));
    }

    @Test
    void ossStorageOnlyTrustsConfiguredBucketAndCodenowPrefix() {
        StorageProperties properties = new StorageProperties();
        properties.setBucketName("codenow-bucket");
        properties.setEndpoint("oss-cn-beijing.aliyuncs.com");
        OssStorageServiceImpl service = new OssStorageServiceImpl(properties);

        assertTrue(service.isManagedUrl(
                "https://codenow-bucket.oss-cn-beijing.aliyuncs.com/codenow/2026/07/19/cover.png"));
        assertFalse(service.isManagedUrl(
                "https://other-bucket.oss-cn-beijing.aliyuncs.com/codenow/2026/07/19/cover.png"));
        assertFalse(service.isManagedUrl(
                "https://codenow-bucket.oss-cn-beijing.aliyuncs.com/other/cover.png"));
        assertFalse(service.isManagedUrl(
                "https://codenow-bucket.oss-cn-beijing.aliyuncs.com.evil.test/codenow/cover.png"));
    }
}
