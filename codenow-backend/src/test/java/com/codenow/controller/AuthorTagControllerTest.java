package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.codenow.entity.BlogTag;
import com.codenow.service.BlogTagService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AuthorTagControllerTest {

    @Test
    void writeEndpointsDelegateAuthorScopedOperationsToService() {
        BlogTagService tagService = mock(BlogTagService.class);
        AuthorTagController controller = new AuthorTagController(tagService);
        AuthorTagController.TagDTO dto = new AuthorTagController.TagDTO();
        dto.setName("  Java  ");
        BlogTag created = new BlogTag();
        created.setId(9L);
        when(tagService.createAuthorTag("  Java  ", 7L)).thenReturn(created);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);

            assertEquals(created, controller.create(dto).getData());
            assertEquals(200, controller.update(9L, dto).getCode());
            assertEquals(200, controller.delete(9L).getCode());
        }

        verify(tagService).createAuthorTag("  Java  ", 7L);
        verify(tagService).updateAuthorTag(9L, "  Java  ", 7L);
        verify(tagService).deleteAuthorTag(9L, 7L);
    }
}
