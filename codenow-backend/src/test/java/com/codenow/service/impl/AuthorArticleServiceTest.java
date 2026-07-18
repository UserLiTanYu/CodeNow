package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.dto.ArticleVO;
import com.codenow.entity.BlogArticle;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.mapper.BlogArticleTagMapper;
import com.codenow.mapper.BlogCategoryMapper;
import com.codenow.mapper.BlogTagMapper;
import com.codenow.service.HotArticleService;
import com.codenow.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorArticleServiceTest {

    @InjectMocks
    private BlogArticleServiceImpl service;
    @Mock private BlogArticleMapper articleMapper;
    @Mock private BlogArticleTagMapper articleTagMapper;
    @Mock private BlogCategoryMapper categoryMapper;
    @Mock private BlogTagMapper tagMapper;
    @Mock private HotArticleService hotArticleService;
    @Mock private StorageService storageService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), BlogArticle.class);
        ReflectionTestUtils.setField(service, "baseMapper", articleMapper);
        lenient().when(storageService.isManagedUrl(startsWith("/api/blog/files/"))).thenReturn(true);
    }

    @Test
    void authorListMustBeScopedToCurrentOwner() {
        Page<BlogArticle> empty = new Page<>(1, 10);
        empty.setRecords(List.of());
        when(articleMapper.selectAuthorArticlePage(any(), anyList(), isNull(), eq(7L), eq(false))).thenReturn(empty);

        service.pageAuthorArticleVO(1, 10, null, null, 7L, false);

        verify(articleMapper).selectAuthorArticlePage(any(), eq(List.of()), isNull(), eq(7L), eq(false));
    }

    @Test
    void tagFilteredListMustUseBoundedExistsQueryInsteadOfLoadingAllRelations() {
        Page<BlogArticle> empty = new Page<>(1, 10);
        empty.setRecords(List.of());
        when(articleMapper.selectAuthorArticlePage(any(), anyList(), eq(9L), eq(7L), eq(false))).thenReturn(empty);

        service.pageAuthorArticleVO(1, 10, null, 9L, 7L, false);

        verify(articleTagMapper, never()).selectList(any());
        verify(articleMapper).selectAuthorArticlePage(any(), eq(List.of()), eq(9L), eq(7L), eq(false));
    }

    @Test
    void createDefaultsMissingStatusAndSortToDraftAndZero() {
        BlogArticle article = article(null, 999L);
        when(articleMapper.insert(any(BlogArticle.class))).thenAnswer(invocation -> {
            invocation.<BlogArticle>getArgument(0).setId(3L);
            return 1;
        });

        service.saveAuthorArticleWithTags(article, List.of(), 7L);

        ArgumentCaptor<BlogArticle> saved = ArgumentCaptor.forClass(BlogArticle.class);
        verify(articleMapper).insert(saved.capture());
        assertEquals(0, saved.getValue().getStatus());
        assertEquals(0, saved.getValue().getSort());
    }

    @Test
    void authorMutationRejectsUnsupportedStatus() {
        BlogArticle article = article(1L, 7L);
        article.setStatus(2);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateAuthorArticleWithTags(article, List.of(), 7L, false));

        assertEquals(400, exception.getCode());
        verify(articleMapper, never()).updateAuthorArticle(any(), anyLong(), anyBoolean());
    }

    @Test
    void ownDetailIsReturned() {
        BlogArticle article = article(1L, 7L);
        ArticleVO vo = new ArticleVO();
        vo.setArticle(article);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleTagMapper.selectList(any())).thenReturn(List.of());

        ArticleVO result = service.getAuthorArticleVOById(1L, 7L, false);

        assertSame(article, result.getArticle());
    }

    @Test
    void otherOwnerDetailIsForbiddenButMissingDetailIsNotFound() {
        when(articleMapper.selectById(1L)).thenReturn(article(1L, 8L));
        when(articleMapper.selectById(2L)).thenReturn(null);

        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.getAuthorArticleVOById(1L, 7L, false)).getCode());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.getAuthorArticleVOById(2L, 7L, false)).getCode());
    }

    @Test
    void createOverridesSpoofedOwnerAndTopFlagButKeepsValidatedCover() {
        BlogArticle article = article(null, 999L);
        article.setIsTop(1);
        article.setCoverImage("/api/blog/files/2026/07/19/cover.png");
        when(articleMapper.insert(any(BlogArticle.class))).thenAnswer(invocation -> {
            invocation.<BlogArticle>getArgument(0).setId(3L);
            return 1;
        });

        service.saveAuthorArticleWithTags(article, List.of(), 7L);

        ArgumentCaptor<BlogArticle> saved = ArgumentCaptor.forClass(BlogArticle.class);
        verify(articleMapper).insert(saved.capture());
        assertEquals(7L, saved.getValue().getAuthorId());
        assertEquals(0, saved.getValue().getIsTop());
        assertEquals("/api/blog/files/2026/07/19/cover.png", saved.getValue().getCoverImage());
    }

    @Test
    void authorMutationRejectsSortOutsideSupportedRange() {
        BlogArticle negative = article(1L, 7L);
        negative.setSort(-1);
        BlogArticle excessive = article(2L, 7L);
        excessive.setSort(10000);

        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.updateAuthorArticleWithTags(negative, List.of(), 7L, false)).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.updateAuthorArticleWithTags(excessive, List.of(), 7L, false)).getCode());
        verify(articleMapper, never()).updateAuthorArticle(any(), anyLong(), anyBoolean());
    }

    @Test
    void otherOwnerCannotUpdateDeleteOrToggleStatus() {
        BlogArticle update = article(1L, 999L);
        update.setIsTop(1);
        when(articleMapper.updateAuthorArticle(any(), eq(7L), eq(false))).thenReturn(0);
        when(articleMapper.deleteAuthorArticle(1L, 7L, false)).thenReturn(0);
        when(articleMapper.toggleAuthorStatus(1L, 7L, false)).thenReturn(0);
        when(articleMapper.selectById(1L)).thenReturn(article(1L, 8L));

        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.updateAuthorArticleWithTags(update, List.of(), 7L, false)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.deleteAuthorArticleWithTags(1L, 7L, false)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.toggleAuthorStatus(1L, 7L, false)).getCode());
        verify(articleTagMapper, never()).delete(any(Wrapper.class));
    }

    @Test
    void ownUpdateDeleteAndStatusAreAllowedAndUpdateCannotSetTop() {
        BlogArticle update = article(1L, 7L);
        update.setIsTop(1);
        update.setCoverImage("/api/blog/files/2026/07/19/cover.png");
        when(articleMapper.updateAuthorArticle(any(), eq(7L), eq(false))).thenReturn(1);
        when(articleMapper.deleteAuthorArticle(1L, 7L, false)).thenReturn(1);
        when(articleMapper.toggleAuthorStatus(1L, 7L, false)).thenReturn(1);

        service.updateAuthorArticleWithTags(update, List.of(), 7L, false);
        service.deleteAuthorArticleWithTags(1L, 7L, false);
        service.toggleAuthorStatus(1L, 7L, false);

        ArgumentCaptor<BlogArticle> changed = ArgumentCaptor.forClass(BlogArticle.class);
        verify(articleMapper).updateAuthorArticle(changed.capture(), eq(7L), eq(false));
        assertNull(changed.getValue().getIsTop());
        assertEquals("/api/blog/files/2026/07/19/cover.png", changed.getValue().getCoverImage());
        verify(articleMapper).deleteAuthorArticle(1L, 7L, false);
        verify(articleMapper).toggleAuthorStatus(1L, 7L, false);
    }

    @Test
    void authorCoverRejectsDangerousOrOversizedUrls() {
        BlogArticle dangerous = article(1L, 7L);
        dangerous.setCoverImage("javascript:alert(1)");
        BlogArticle oversized = article(2L, 7L);
        oversized.setCoverImage("https://example.com/" + "a".repeat(240));

        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.updateAuthorArticleWithTags(dangerous, List.of(), 7L, false)).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.updateAuthorArticleWithTags(oversized, List.of(), 7L, false)).getCode());
        verify(articleMapper, never()).updateAuthorArticle(any(), anyLong(), anyBoolean());
    }

    @Test
    void authorCoverRejectsUnmanagedExternalHttpsUrl() {
        BlogArticle external = article(1L, 7L);
        external.setCoverImage("https://tracker.example.com/pixel.png");

        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.updateAuthorArticleWithTags(external, List.of(), 7L, false)).getCode());
        verify(articleMapper, never()).updateAuthorArticle(any(), anyLong(), anyBoolean());
    }

    @Test
    void adminMayManageOtherOwnersThroughAuthorApi() {
        when(articleMapper.selectById(1L)).thenReturn(article(1L, 8L));
        when(articleTagMapper.selectList(any())).thenReturn(List.of());
        when(articleMapper.deleteAuthorArticle(1L, 7L, true)).thenReturn(1);

        assertNotNull(service.getAuthorArticleVOById(1L, 7L, true));
        service.deleteAuthorArticleWithTags(1L, 7L, true);

        verify(articleMapper).deleteAuthorArticle(1L, 7L, true);
    }

    private BlogArticle article(Long id, Long authorId) {
        BlogArticle article = new BlogArticle();
        article.setId(id);
        article.setAuthorId(authorId);
        article.setTitle("title");
        article.setContent("content");
        return article;
    }
}
