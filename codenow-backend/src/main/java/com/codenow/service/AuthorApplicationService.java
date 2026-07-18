package com.codenow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.dto.AuthorApplicationDTO;
import com.codenow.dto.AuthorApplicationVO;
import com.codenow.entity.AuthorApplication;

public interface AuthorApplicationService {
    AuthorApplication submit(Long userId, AuthorApplicationDTO dto);
    AuthorApplicationVO latest(Long userId);
    Page<AuthorApplicationVO> pageMine(Long userId, Integer pageNum, Integer pageSize);
    void cancel(Long userId, Long applicationId);
    Page<AuthorApplicationVO> pageAdmin(Integer pageNum, Integer pageSize, String status, String keyword);
    AuthorApplicationVO detail(Long applicationId);
    void approve(Long applicationId, Long reviewerId, String remark);
    void reject(Long applicationId, Long reviewerId, String reason);
    void revokeAuthor(Long userId, Long reviewerId, String reason);
}
