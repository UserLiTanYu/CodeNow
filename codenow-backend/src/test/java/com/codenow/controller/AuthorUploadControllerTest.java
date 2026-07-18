package com.codenow.controller;

import com.codenow.common.R;
import com.codenow.service.impl.ImageUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorUploadControllerTest {

    @Test
    void uploadImageReturnsSharedServiceUrl() {
        ImageUploadService service = mock(ImageUploadService.class);
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1});
        when(service.upload(file)).thenReturn("/api/blog/files/2026/07/19/cover.png");
        AuthorUploadController controller = new AuthorUploadController(service);

        R<Map<String, String>> result = controller.uploadImage(file);

        assertEquals("/api/blog/files/2026/07/19/cover.png", result.getData().get("url"));
        verify(service).upload(file);
    }
}
