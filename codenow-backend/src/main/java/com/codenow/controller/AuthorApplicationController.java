package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.annotation.OperationLog;
import com.codenow.common.R;
import com.codenow.dto.AuthorApplicationDTO;
import com.codenow.dto.AuthorApplicationVO;
import com.codenow.service.AuthorApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/author-applications")
@RequiredArgsConstructor
/**
 * 会员作者申请控制器。
 * 提供作者申请的提交、查询和撤回等功能。
 */
public class AuthorApplicationController {
    private final AuthorApplicationService applicationService;

    /**
     * 提交作者申请。
     */
    @OperationLog("提交作者申请")
    @PostMapping
    public R<Void> submit(@Valid @RequestBody AuthorApplicationDTO dto) {
        applicationService.submit(StpUtil.getLoginIdAsLong(), dto);
        return R.ok();
    }

    /**
     * 查询最近一次作者申请。
     */
    @GetMapping("/latest")
    public R<AuthorApplicationVO> latest() {
        return R.ok(applicationService.latest(StpUtil.getLoginIdAsLong()));
    }

    /**
     * 分页查询作者申请历史记录。
     */
    @GetMapping
    public R<Page<AuthorApplicationVO>> history(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(applicationService.pageMine(StpUtil.getLoginIdAsLong(), pageNum, pageSize));
    }

    /**
     * 撤回作者申请。
     */
    @OperationLog("撤回作者申请")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        applicationService.cancel(StpUtil.getLoginIdAsLong(), id);
        return R.ok();
    }
}
