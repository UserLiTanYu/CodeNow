package com.codenow.controller;

import com.codenow.common.R;
import com.codenow.entity.BlogTag;
import com.codenow.service.BlogTagService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorTagControllerTest {

    @Test
    void listReturnsAllAvailableSharedTags() {
        BlogTagService tagService = mock(BlogTagService.class);
        BlogTag java = new BlogTag();
        java.setId(1L);
        java.setName("Java");
        when(tagService.list()).thenReturn(List.of(java));

        AuthorTagController controller = new AuthorTagController(tagService);
        R<List<BlogTag>> result = controller.list();

        assertEquals(200, result.getCode());
        assertEquals(List.of(java), result.getData());
        verify(tagService).list();
    }
}
