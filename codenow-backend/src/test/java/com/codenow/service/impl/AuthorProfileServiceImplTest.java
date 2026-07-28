package com.codenow.service.impl;

import com.codenow.dto.AuthorProfileUpdateDTO;
import com.codenow.entity.AuthorProfile;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.AuthorProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorProfileServiceImplTest {

    @InjectMocks
    private AuthorProfileServiceImpl service;

    @Mock
    private AuthorProfileMapper profileMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", profileMapper);
    }

    @Test
    void updateAuthorProfileNormalizesEditableFields() {
        AuthorProfile existing = new AuthorProfile();
        existing.setId(3L);
        existing.setUserId(7L);
        when(profileMapper.selectOne(any(), eq(true))).thenReturn(existing);
        when(profileMapper.updateById(existing)).thenReturn(1);
        AuthorProfileUpdateDTO dto = new AuthorProfileUpdateDTO();
        dto.setBio("  专注 Java 后端工程实践与性能优化。  ");
        dto.setExpertise(List.of(" Java ", "Spring Boot", "Java"));
        dto.setWebsiteUrl("  https://example.com  ");
        dto.setPortfolioUrl("   ");

        AuthorProfile result = service.updateAuthorProfile(7L, dto);

        assertSame(existing, result);
        assertEquals("专注 Java 后端工程实践与性能优化。", result.getBio());
        assertEquals("Java,Spring Boot", result.getExpertise());
        assertEquals("https://example.com", result.getWebsiteUrl());
        assertNull(result.getPortfolioUrl());
        assertNotNull(result.getUpdateTime());
        verify(profileMapper).updateById(existing);
    }

    @Test
    void updateAuthorProfileRejectsMissingProfile() {
        when(profileMapper.selectOne(any(), eq(true))).thenReturn(null);
        AuthorProfileUpdateDTO dto = new AuthorProfileUpdateDTO();
        dto.setBio("专注 Java 后端工程实践与性能优化。");
        dto.setExpertise(List.of("Java"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateAuthorProfile(7L, dto));

        assertEquals(404, exception.getCode());
        verify(profileMapper, never()).updateById(any(AuthorProfile.class));
    }

    @Test
    void updateAuthorProfileRejectsBioThatIsTooShortAfterTrimming() {
        AuthorProfile existing = new AuthorProfile();
        existing.setId(3L);
        existing.setUserId(7L);
        when(profileMapper.selectOne(any(), eq(true))).thenReturn(existing);
        AuthorProfileUpdateDTO dto = new AuthorProfileUpdateDTO();
        dto.setBio("                   简介");
        dto.setExpertise(List.of("Java"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateAuthorProfile(7L, dto));

        assertEquals(400, exception.getCode());
        verify(profileMapper, never()).updateById(any(AuthorProfile.class));
    }
}
