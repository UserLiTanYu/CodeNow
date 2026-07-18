package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.common.R;
import com.codenow.entity.BlogComment;
import com.codenow.service.CommentService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorCommentControllerTest {

    @Test
    void listUsesCurrentIdentityAndOwnershipScopedService() {
        CommentService service = mock(CommentService.class);
        Page<BlogComment> page = new Page<>(1, 10);
        when(service.pageAuthorComments(1, 10, 9L, 7L, false)).thenReturn(page);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            stp.when(() -> StpUtil.hasRole("ADMIN")).thenReturn(false);
            AuthorCommentController controller = new AuthorCommentController(service);

            R<Page<BlogComment>> result = controller.list(1, 10, 9L);

            assertEquals(page, result.getData());
            verify(service).pageAuthorComments(1, 10, 9L, 7L, false);
        }
    }

    @Test
    void deleteUsesOwnershipScopedService() {
        CommentService service = mock(CommentService.class);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            stp.when(() -> StpUtil.hasRole("ADMIN")).thenReturn(false);
            AuthorCommentController controller = new AuthorCommentController(service);

            controller.delete(5L);

            verify(service).deleteAuthorCommentWithChildren(5L, 7L, false);
        }
    }
}
