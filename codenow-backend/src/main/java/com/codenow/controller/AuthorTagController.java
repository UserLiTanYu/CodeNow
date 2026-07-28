package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.codenow.annotation.OperationLog;
import com.codenow.common.R;
import com.codenow.entity.BlogTag;
import com.codenow.service.BlogTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "作者标签管理")
@RestController
@RequestMapping("/api/author/tags")
@RequiredArgsConstructor
/**
 * 作者标签管理控制器。
 * 提供作者对自身标签的增删改查功能，仅允许操作自己创建的标签。
 */
public class AuthorTagController {

    private final BlogTagService tagService;

    /**
     * 查询当前作者创建的标签列表。
     */
    @Operation(summary = "查询我的标签列表")
    @GetMapping
    public R<List<BlogTag>> list() {
        //获取当前登录用户ID，查询该作者创建的所有标签
        long userId = StpUtil.getLoginIdAsLong();
        return R.ok(tagService.listByCreator(userId));
    }

    /**
     * 创建标签。
     * 标签名称在同一作者下唯一。
     */
    @Operation(summary = "创建标签")
    @OperationLog("作者创建标签")
    @PostMapping
    public R<BlogTag> create(@Valid @RequestBody TagDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        return R.ok(tagService.createAuthorTag(dto.getName(), userId));
    }

    /**
     * 修改标签。
     * 仅允许修改自己创建的标签。
     */
    @Operation(summary = "修改标签")
    @OperationLog("作者修改标签")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TagDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        tagService.updateAuthorTag(id, dto.getName(), userId);
        return R.ok();
    }

    /**
     * 删除标签。
     * 仅允许删除自己创建的标签。
     */
    @Operation(summary = "删除标签")
    @OperationLog("作者删除标签")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        tagService.deleteAuthorTag(id, userId);
        return R.ok();
    }

    @Data
    public static class TagDTO {
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 50, message = "标签名称不能超过 50 字")
        private String name;
    }
}
