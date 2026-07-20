package com.codenow.controller;

import com.codenow.service.impl.LocalStorageServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
/**
 * 本地文件访问控制器。
 * 仅在 storage.type=local 时启用，提供本地存储文件的 HTTP 访问功能。
 */
public class LocalFileController {

    private final LocalStorageServiceImpl storageService;

    /**
     * 获取本地存储的文件。
     * 根据日期路径和文件名返回文件资源，支持浏览器缓存。
     */
    @GetMapping("/api/blog/files/{year}/{month}/{day}/{filename:.+}")
    public ResponseEntity<Resource> getFile(
            @PathVariable String year,
            @PathVariable String month,
            @PathVariable String day,
            @PathVariable String filename) throws Exception {
        Path path = storageService.resolve(String.join("/", year, month, day, filename));
        if (!Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        String contentType = Files.probeContentType(path);
        MediaType mediaType = contentType == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .contentType(mediaType)
                .body(new FileSystemResource(path));
    }
}
