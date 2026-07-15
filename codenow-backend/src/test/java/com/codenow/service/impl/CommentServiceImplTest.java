package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.entity.BlogComment;
import com.codenow.mapper.BlogCommentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private BlogCommentMapper commentMapper;

    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentServiceImpl();
        ReflectionTestUtils.setField(commentService, "baseMapper", commentMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getCommentTree_shouldPageRootsAndAttachDescendants() {
        BlogComment root = comment(1L, 0L);
        BlogComment child = comment(2L, 1L);
        BlogComment grandchild = comment(3L, 2L);

        when(commentMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<BlogComment> page = invocation.getArgument(0);
            page.setTotal(1);
            page.setRecords(List.of(root));
            return page;
        });
        when(commentMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(child))
                .thenReturn(List.of(grandchild))
                .thenReturn(Collections.emptyList());

        Page<BlogComment> result = commentService.getCommentTree(10L, 1, 20);

        assertEquals(1, result.getTotal());
        assertEquals(2L, result.getRecords().getFirst().getChildren().getFirst().getId());
        assertEquals(3L, result.getRecords().getFirst().getChildren().getFirst().getChildren().getFirst().getId());
        verify(commentMapper, times(3)).selectList(any(Wrapper.class));
    }

    @Test
    void countApproved_shouldReturnMapperCount() {
        when(commentMapper.selectCount(any(Wrapper.class))).thenReturn(7L);

        assertEquals(7, commentService.countApproved(10L));
    }

    @Test
    void deleteWithChildren_shouldDeleteChildrenBeforeParent() {
        BlogComment child = comment(2L, 1L);
        when(commentMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(child))
                .thenReturn(Collections.emptyList());
        when(commentMapper.deleteById(any(Serializable.class))).thenReturn(1);

        commentService.deleteWithChildren(1L);

        var inOrder = inOrder(commentMapper);
        inOrder.verify(commentMapper).deleteById(2L);
        inOrder.verify(commentMapper).deleteById(1L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getCommentTree_withNoRoots_shouldAvoidDescendantQuery() {
        when(commentMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<BlogComment> page = invocation.getArgument(0);
            page.setRecords(Collections.emptyList());
            return page;
        });

        Page<BlogComment> result = commentService.getCommentTree(10L, 1, 20);

        assertTrue(result.getRecords().isEmpty());
        verify(commentMapper, never()).selectList(any(Wrapper.class));
    }

    private BlogComment comment(Long id, Long parentId) {
        BlogComment comment = new BlogComment();
        comment.setId(id);
        comment.setArticleId(10L);
        comment.setParentId(parentId);
        comment.setStatus(1);
        return comment;
    }
}
