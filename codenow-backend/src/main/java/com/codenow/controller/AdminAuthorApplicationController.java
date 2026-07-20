package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.annotation.OperationLog;
import com.codenow.common.R;
import com.codenow.dto.AuthorApplicationReviewDTO;
import com.codenow.dto.AuthorApplicationVO;
import com.codenow.dto.AuthorRoleRevokeDTO;
import com.codenow.service.AuthorApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
/**
 * 管理员作者申请管理控制器。
 * 提供作者申请的审核、驳回及作者资格撤销等管理功能。
 */
public class AdminAuthorApplicationController {
    private final AuthorApplicationService applicationService;

    /**
     * 分页查询作者申请列表。
     * 支持按状态和关键词筛选，按创建时间倒序排列。
     */
    @GetMapping("/author-applications")
    public R<Page<AuthorApplicationVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return R.ok(applicationService.pageAdmin(pageNum, pageSize, status, keyword));
    }

    /**
     * 查询作者申请详情。
     */
    @GetMapping("/author-applications/{id}")
    public R<AuthorApplicationVO> detail(@PathVariable Long id) {
        return R.ok(applicationService.detail(id));
    }

    /**
     * 通过作者申请。
     */
    @OperationLog("通过作者申请")
    @PutMapping("/author-applications/{id}/approve")
    public R<Void> approve(@PathVariable Long id,
                           @Valid @RequestBody(required = false) AuthorApplicationReviewDTO dto) {
        applicationService.approve(id, StpUtil.getLoginIdAsLong(), dto == null ? null : dto.getReviewRemark());
        return R.ok();
    }

    /**
     * 驳回作者申请。
     */
    @OperationLog("驳回作者申请")
    @PutMapping("/author-applications/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @Valid @RequestBody AuthorApplicationReviewDTO dto) {
        applicationService.reject(id, StpUtil.getLoginIdAsLong(), dto.getReviewRemark());
        return R.ok();
    }

    /**
     * 撤销用户作者资格。
     */
    @OperationLog("撤销作者资格")
    @PutMapping("/users/{id}/author-role/revoke")
    public R<Void> revoke(@PathVariable Long id, @Valid @RequestBody AuthorRoleRevokeDTO dto) {
        applicationService.revokeAuthor(id, StpUtil.getLoginIdAsLong(), dto.getReason());
        return R.ok();
    }
}
