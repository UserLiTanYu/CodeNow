package com.codenow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.common.R;
import com.codenow.dto.ArticleVO;
import com.codenow.dto.PublicAuthorVO;
import com.codenow.service.PublicAuthorService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PublicAuthorControllerTest {

    @Test
    void listDelegatesNormalizedPublicQueryToService() {
        PublicAuthorService service = mock(PublicAuthorService.class);
        Page<PublicAuthorVO> page = new Page<>(1, 12, 0);
        when(service.pagePublicAuthors(1, 12, "Java", "popular")).thenReturn(page);
        PublicAuthorController controller = new PublicAuthorController(service);

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
        PublicAuthorController controller = new PublicAuthorController(service);

        R<PublicAuthorVO> result = controller.detail(7L);

        assertEquals(200, result.getCode());
        assertEquals(author, result.getData());
    }

    @Test
    void articlesDelegatesAuthorScopeToService() {
        PublicAuthorService service = mock(PublicAuthorService.class);
        Page<ArticleVO> page = new Page<>(1, 10, 0);
        when(service.pagePublicAuthorArticles(7L, 1, 10, "latest")).thenReturn(page);
        PublicAuthorController controller = new PublicAuthorController(service);

        R<Page<ArticleVO>> result = controller.articles(7L, 1, 10, "latest");

        assertEquals(200, result.getCode());
        assertEquals(page, result.getData());
        verify(service).pagePublicAuthorArticles(7L, 1, 10, "latest");
    }
}
