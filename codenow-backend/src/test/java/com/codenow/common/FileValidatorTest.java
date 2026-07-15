package com.codenow.common;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileValidatorTest {

    @Test
    void validateImage_shouldAcceptMatchingPngSignature() throws Exception {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0};

        assertEquals("png", FileValidator.validateImage(new ByteArrayInputStream(png), "cover.png"));
    }

    @Test
    void validateImage_shouldRejectSpoofedExtension() {
        byte[] html = "<html>bad</html>".getBytes();

        assertThrows(IllegalArgumentException.class,
                () -> FileValidator.validateImage(new ByteArrayInputStream(html), "cover.png"));
    }

    @Test
    void validateImage_shouldRejectUnsupportedExtension() {
        byte[] svg = "<svg></svg>".getBytes();

        assertThrows(IllegalArgumentException.class,
                () -> FileValidator.validateImage(new ByteArrayInputStream(svg), "cover.svg"));
    }
}
