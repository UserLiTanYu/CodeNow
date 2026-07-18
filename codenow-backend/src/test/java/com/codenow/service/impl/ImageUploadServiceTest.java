package com.codenow.service.impl;

import com.codenow.config.UploadProperties;
import com.codenow.exception.BusinessException;
import com.codenow.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageUploadServiceTest {

    private StorageService storageService;
    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        UploadProperties properties = new UploadProperties();
        properties.setMaxSize(5 * 1024 * 1024L);
        imageUploadService = new ImageUploadService(storageService, properties);
    }

    @Test
    void uploadAcceptsRealImageAndReturnsStorageUrl() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", png);
        when(storageService.upload(file)).thenReturn("/api/blog/files/2026/07/19/cover.png");

        String url = imageUploadService.upload(file);

        assertEquals("/api/blog/files/2026/07/19/cover.png", url);
        verify(storageService).upload(file);
    }

    @Test
    void uploadRejectsSpoofedImageBeforeStorage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", "<html>bad</html>".getBytes());

        assertThrows(BusinessException.class, () -> imageUploadService.upload(file));
        verify(storageService, never()).upload(file);
    }
}
