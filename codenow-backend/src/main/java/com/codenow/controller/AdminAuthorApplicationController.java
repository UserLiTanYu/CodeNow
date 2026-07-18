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
public class AdminAuthorApplicationController {
    private final AuthorApplicationService applicationService;

    @GetMapping("/author-applications")
    public R<Page<AuthorApplicationVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return R.ok(applicationService.pageAdmin(pageNum, pageSize, status, keyword));
    }

    @GetMapping("/author-applications/{id}")
    public R<AuthorApplicationVO> detail(@PathVariable Long id) {
        return R.ok(applicationService.detail(id));
    }

    @OperationLog("通过作者申请")
    @PutMapping("/author-applications/{id}/approve")
    public R<Void> approve(@PathVariable Long id,
                           @Valid @RequestBody(required = false) AuthorApplicationReviewDTO dto) {
        applicationService.approve(id, StpUtil.getLoginIdAsLong(), dto == null ? null : dto.getReviewRemark());
        return R.ok();
    }

    @OperationLog("驳回作者申请")
    @PutMapping("/author-applications/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @Valid @RequestBody AuthorApplicationReviewDTO dto) {
        applicationService.reject(id, StpUtil.getLoginIdAsLong(), dto.getReviewRemark());
        return R.ok();
    }

    @OperationLog("撤销作者资格")
    @PutMapping("/users/{id}/author-role/revoke")
    public R<Void> revoke(@PathVariable Long id, @Valid @RequestBody AuthorRoleRevokeDTO dto) {
        applicationService.revokeAuthor(id, StpUtil.getLoginIdAsLong(), dto.getReason());
        return R.ok();
    }
}
