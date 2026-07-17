package com.codenow.service;

import com.codenow.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArticlePackageImportServiceTest {
    private StorageService storageService;
    private ArticlePackageImportService service;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        service = new ArticlePackageImportService(storageService);
    }

    @Test
    void importsImagesAndRewritesLocalMarkdownReferences() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("article.md", "# ZIP 导入测试\n![本地](images/demo.png)\n![远程](https://example.com/a.png)"
                .getBytes(StandardCharsets.UTF_8));
        entries.put("images/demo.png", pngHeader());
        when(storageService.upload(eq("demo.png"), any(byte[].class))).thenReturn("https://cdn.example/demo.png");

        var result = service.importPackage(zip(entries));

        assertEquals("ZIP 导入测试", result.getTitle());
        assertEquals(1, result.getImageCount());
        assertTrue(result.getContent().contains("![本地](https://cdn.example/demo.png)"));
        assertTrue(result.getContent().contains("![远程](https://example.com/a.png)"));
        verify(storageService).upload(eq("demo.png"), any(byte[].class));
    }

    @Test
    void rejectsMissingReferencedImage() throws Exception {
        var file = zip(Map.of("article.md", "![缺失](images/missing.png)".getBytes(StandardCharsets.UTF_8)));

        BusinessException error = assertThrows(BusinessException.class, () -> service.importPackage(file));

        assertTrue(error.getMessage().contains("图片不存在"));
        verifyNoInteractions(storageService);
    }

    @Test
    void rejectsPathTraversalEntries() throws Exception {
        var file = zip(Map.of("../article.md", "# unsafe".getBytes(StandardCharsets.UTF_8)));

        BusinessException error = assertThrows(BusinessException.class, () -> service.importPackage(file));

        assertEquals(400, error.getCode());
        verifyNoInteractions(storageService);
    }

    @Test
    void deletesUploadedImagesWhenLaterReferenceFails() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("article.md", "![成功](images/ok.png)\n![缺失](images/missing.png)"
                .getBytes(StandardCharsets.UTF_8));
        entries.put("images/ok.png", pngHeader());
        when(storageService.upload(eq("ok.png"), any(byte[].class))).thenReturn("https://cdn.example/ok.png");

        assertThrows(BusinessException.class, () -> service.importPackage(zip(entries)));

        verify(storageService).delete("https://cdn.example/ok.png");
    }

    @Test
    void retriesWithGbkForLegacyChineseEntryNames() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("article.md", "# 中文文件名\n![安装](img/安装步骤.png)".getBytes(StandardCharsets.UTF_8));
        entries.put("img/安装步骤.png", pngHeader());
        when(storageService.upload(eq("安装步骤.png"), any(byte[].class)))
                .thenReturn("https://cdn.example/install.png");

        var result = service.importPackage(zip(entries, Charset.forName("GBK")));

        assertEquals(1, result.getImageCount());
        assertTrue(result.getContent().contains("![安装](https://cdn.example/install.png)"));
        verify(storageService).upload(eq("安装步骤.png"), any(byte[].class));
    }

    private MockMultipartFile zip(Map<String, byte[]> entries) throws Exception {
        return zip(entries, StandardCharsets.UTF_8);
    }

    private MockMultipartFile zip(Map<String, byte[]> entries, Charset filenameCharset) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, filenameCharset)) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return new MockMultipartFile("file", "article.zip", "application/zip", bytes.toByteArray());
    }

    private byte[] pngHeader() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    }
}
