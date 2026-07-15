package com.codenow.service.impl;

import com.codenow.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadAndDeleteKeepFilesInsideConfiguredRoot() throws Exception {
        LocalStorageServiceImpl service = new LocalStorageServiceImpl(tempDir.toString());
        byte[] content = "image-content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "cover.PNG", "image/png", content);

        String url = service.upload(file);
        String relativePath = url.substring("/api/blog/files/".length());
        Path storedFile = service.resolve(relativePath);

        assertTrue(storedFile.startsWith(tempDir.toAbsolutePath().normalize()));
        assertArrayEquals(content, Files.readAllBytes(storedFile));

        service.delete(url);
        assertFalse(Files.exists(storedFile));
    }

    @Test
    void resolveRejectsPathTraversal() {
        LocalStorageServiceImpl service = new LocalStorageServiceImpl(tempDir.toString());

        assertThrows(BusinessException.class, () -> service.resolve("../../outside.txt"));
    }

    @Test
    void uploadRejectsFilenameWithoutExtension() {
        LocalStorageServiceImpl service = new LocalStorageServiceImpl(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "cover", "image/png", new byte[]{1});

        assertThrows(BusinessException.class, () -> service.upload(file));
    }
}
