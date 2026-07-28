package com.codenow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.common.R;
import com.codenow.dto.ArticleVO;
import com.codenow.dto.PublicAuthorVO;
import com.codenow.exception.BusinessException;
import com.codenow.service.BlogCategoryService;
import com.codenow.service.BlogTagService;
import com.codenow.service.PublicAuthorService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PublicAuthorControllerTest {

    @Test
    void listDelegatesNormalizedPublicQueryToService() {
        PublicAuthorService service = mock(PublicAuthorService.class);
        Page<PublicAuthorVO> page = new Page<>(1, 12, 0);
        when(service.pagePublicAuthors(1, 12, "Java", "popular")).thenReturn(page);
        PublicAuthorController controller = new PublicAuthorController(service, mock(BlogCategoryService.class), mock(BlogTagService.class));

        R<Page<PublicAuthorVO>> result = controller.list(1, 12, "Java", "popular");

        assertEquals(200, result.getCode());
        assertEquals(page, result.getData());
    }

    @Test
    void detailReturnsPublicProjection() {
        PublicAuthorService service = mock(PublicAuthorService.class);
        PublicAuthorVO author = new PublicAuthorVO();
        author.setUserId(7L);
        when(service.getPublicAuthor(7L)).thenReturn(author);
        PublicAuthorController controller = new PublicAuthorController(service, mock(BlogCategoryService.class), mock(BlogTagService.class));

        R<PublicAuthorVO> result = controller.detail(7L);

        assertEquals(200, result.getCode());
        assertEquals(author, result.getData());
    }

    @Test
    void articlesDelegatesAuthorScopeToService() {
        PublicAuthorService service = mock(PublicAuthorService.class);
        Page<ArticleVO> page = new Page<>(1, 10, 0);
        when(service.pagePublicAuthorArticles(7L, 1, 10, "latest", null, null, "Redis")).thenReturn(page);
        PublicAuthorController controller = new PublicAuthorController(service, mock(BlogCategoryService.class), mock(BlogTagService.class));

        R<Page<ArticleVO>> result = controller.articles(7L, 1, 10, "latest", null, null, "Redis");

        assertEquals(200, result.getCode());
        assertEquals(page, result.getData());
        verify(service).pagePublicAuthorArticles(7L, 1, 10, "latest", null, null, "Redis");
    }

    @Test
    void categoriesRejectHiddenAuthorBeforeReadingTaxonomy() {
        PublicAuthorService service = mock(PublicAuthorService.class);
        BlogCategoryService categoryService = mock(BlogCategoryService.class);
        when(service.getPublicAuthor(8L)).thenThrow(new BusinessException(404, "作者不存在"));
        PublicAuthorController controller = new PublicAuthorController(service, categoryService, mock(BlogTagService.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.authorCategories(8L));

        assertEquals(404, exception.getCode());
        verifyNoInteractions(categoryService);
    }

    @Test
    void tagsRejectHiddenAuthorBeforeReadingTaxonomy() {
        PublicAuthorService service = mock(PublicAuthorService.class);
        BlogTagService tagService = mock(BlogTagService.class);
        when(service.getPublicAuthor(8L)).thenThrow(new BusinessException(404, "作者不存在"));
        PublicAuthorController controller = new PublicAuthorController(service, mock(BlogCategoryService.class), tagService);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.authorTags(8L));

        assertEquals(404, exception.getCode());
        verifyNoInteractions(tagService);
    }
}
