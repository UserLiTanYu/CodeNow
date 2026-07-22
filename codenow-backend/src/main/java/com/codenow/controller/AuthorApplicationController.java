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
        //获取当前登录用户ID，调用业务层提交作者申请
        applicationService.submit(StpUtil.getLoginIdAsLong(), dto);
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 查询最近一次作者申请。
     */
    @GetMapping("/latest")
    public R<AuthorApplicationVO> latest() {
        //获取当前登录用户ID，查询其最近一次作者申请记录
        return R.ok(applicationService.latest(StpUtil.getLoginIdAsLong()));
    }

    /**
     * 分页查询作者申请历史记录。
     */
    @GetMapping
    public R<Page<AuthorApplicationVO>> history(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        //获取当前登录用户ID，分页查询该用户的作者申请历史记录
        return R.ok(applicationService.pageMine(StpUtil.getLoginIdAsLong(), pageNum, pageSize));
    }

    /**
     * 撤回作者申请。
     */
    @OperationLog("撤回作者申请")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        //获取当前登录用户ID，调用业务层撤回指定申请
        applicationService.cancel(StpUtil.getLoginIdAsLong(), id);
        //返回正确的响应结果
        return R.ok();
    }
}
