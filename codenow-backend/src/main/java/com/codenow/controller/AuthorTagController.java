package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.codenow.common.R;
import com.codenow.common.UserRole;
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

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "作者标签")
@RestController
@RequestMapping("/api/author/tags")
@RequiredArgsConstructor
public class AuthorTagController {

    private final BlogTagService tagService;

    @Operation(summary = "查询作者可用标签", description = "返回作者编辑文章时可选的全部标签")
    @GetMapping
    public R<List<BlogTag>> list() {
        return R.ok(tagService.list());
    }

    @Operation(summary = "创建标签")
    @PostMapping
    public R<BlogTag> create(@Valid @RequestBody TagDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        BlogTag existing = tagService.lambdaQuery().eq(BlogTag::getName, dto.getName().trim()).one();
        if (existing != null) {
            return R.error(409, "标签名称已存在");
        }
        BlogTag tag = new BlogTag();
        tag.setName(dto.getName().trim());
        tag.setCreatedBy(userId);
        tag.setCreateTime(LocalDateTime.now());
        tagService.save(tag);
        return R.ok(tag);
    }

    @Operation(summary = "修改标签")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TagDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        BlogTag tag = tagService.getById(id);
        if (tag == null) return R.error(404, "标签不存在");
        if (!StpUtil.getRoleList().contains(UserRole.ADMIN)
                && (tag.getCreatedBy() == null || !tag.getCreatedBy().equals(userId))) {
            return R.error(403, "只能修改自己创建的标签");
        }
        BlogTag duplicate = tagService.lambdaQuery().eq(BlogTag::getName, dto.getName().trim()).ne(BlogTag::getId, id).one();
        if (duplicate != null) return R.error(409, "标签名称已存在");
        tag.setName(dto.getName().trim());
        tagService.updateById(tag);
        return R.ok();
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = StpUtil.getRoleList().contains(UserRole.ADMIN);
        BlogTag tag = tagService.getById(id);
        if (tag == null) return R.error(404, "标签不存在");
        if (!isAdmin && (tag.getCreatedBy() == null || !tag.getCreatedBy().equals(userId))) {
            return R.error(403, "只能删除自己创建的标签");
        }
        tagService.removeById(id);
        return R.ok();
    }

    @Data
    public static class TagDTO {
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 50, message = "标签名称不能超过 50 字")
        private String name;
    }
}
