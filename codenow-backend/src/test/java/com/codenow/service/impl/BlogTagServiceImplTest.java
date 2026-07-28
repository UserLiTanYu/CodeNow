package com.codenow.service.impl;

import com.codenow.entity.BlogArticleTag;
import com.codenow.entity.BlogTag;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.mapper.BlogArticleTagMapper;
import com.codenow.mapper.BlogTagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogTagServiceImplTest {

    @InjectMocks
    private BlogTagServiceImpl service;

    @Mock
    private BlogTagMapper tagMapper;

    @Mock
    private BlogArticleMapper articleMapper;

    @Mock
    private BlogArticleTagMapper articleTagMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", tagMapper);
    }

    @Test
    void createAuthorTagNormalizesNameAndSetsOwner() {
        when(tagMapper.selectCount(any())).thenReturn(0L);
        when(tagMapper.insert(any(BlogTag.class))).thenReturn(1);

        BlogTag result = service.createAuthorTag("  Spring Boot  ", 7L);

        assertEquals("Spring Boot", result.getName());
        assertEquals(7L, result.getCreatedBy());
        assertNotNull(result.getCreateTime());
        verify(tagMapper).insert(result);
    }

    @Test
    void createAuthorTagMapsConcurrentDuplicateToConflict() {
        when(tagMapper.selectCount(any())).thenReturn(0L);
        when(tagMapper.insert(any(BlogTag.class))).thenThrow(new DuplicateKeyException("duplicate"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createAuthorTag("Java", 7L));

        assertEquals(409, exception.getCode());
    }

    @Test
    void updateAuthorTagRejectsAnotherAuthorsTag() {
        BlogTag existing = tag(3L, "Java", 8L);
        when(tagMapper.selectById(3L)).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateAuthorTag(3L, "Spring", 7L));

        assertEquals(403, exception.getCode());
        verify(tagMapper, never()).updateById(any(BlogTag.class));
    }

    @Test
    void deleteAuthorTagRejectsTagUsedByArticles() {
        BlogTag existing = tag(3L, "Java", 7L);
        when(tagMapper.selectById(3L)).thenReturn(existing);
        when(articleTagMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.deleteAuthorTag(3L, 7L));

        assertEquals(400, exception.getCode());
        verify(tagMapper, never()).deleteById(3L);
    }

    private BlogTag tag(Long id, String name, Long createdBy) {
        BlogTag tag = new BlogTag();
        tag.setId(id);
        tag.setName(name);
        tag.setCreatedBy(createdBy);
        return tag;
    }
}
