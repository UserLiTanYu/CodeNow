package com.codenow.service.impl;

import com.codenow.exception.BusinessException;
import com.codenow.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalStorageServiceImpl implements StorageService {

    private final Path storageRoot;

    public LocalStorageServiceImpl(@Value("${storage.local-path:uploads}") String storagePath) {
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Override
    public String upload(MultipartFile file) {
        try {
            return upload(file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("本地文件保存失败");
        }
    }

    @Override
    public String upload(String originalFilename, byte[] content) {
        String extension = extension(originalFilename);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String filename = UUID.randomUUID() + "." + extension;
        Path destination = storageRoot.resolve(datePath).resolve(filename).normalize();
        ensureWithinStorage(destination);
        try {
            Files.createDirectories(destination.getParent());
            try (var inputStream = new java.io.ByteArrayInputStream(content)) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/api/blog/files/" + datePath + "/" + filename;
        } catch (IOException e) {
            throw new BusinessException("本地文件保存失败");
        }
    }

    @Override
    public boolean isManagedUrl(String url) {
        return url != null && url.startsWith("/api/blog/files/")
                && url.length() > "/api/blog/files/".length();
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith("/api/blog/files/")) {
            return;
        }
        Path target = resolve(url.substring("/api/blog/files/".length()));
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new BusinessException("本地文件删除失败");
        }
    }

    public Path resolve(String relativePath) {
        Path target = storageRoot.resolve(relativePath).normalize();
        ensureWithinStorage(target);
        return target;
    }

    private void ensureWithinStorage(Path path) {
        if (!path.startsWith(storageRoot)) {
            throw new BusinessException(400, "非法文件路径");
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException(400, "文件扩展名缺失");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
