package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
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

import java.time.LocalDateTime;
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
    @PostMapping
    public R<BlogTag> create(@Valid @RequestBody TagDTO dto) {
        //获取当前登录用户ID
        long userId = StpUtil.getLoginIdAsLong();
        //检查当前作者下是否已存在同名标签
        BlogTag existing = tagService.lambdaQuery().eq(BlogTag::getName, dto.getName().trim()).eq(BlogTag::getCreatedBy, userId).one();
        if (existing != null) {
            return R.error(409, "标签名称已存在");
        }
        //创建标签实体，设置名称、创建者和创建时间
        BlogTag tag = new BlogTag();
        tag.setName(dto.getName().trim());
        tag.setCreatedBy(userId);
        tag.setCreateTime(LocalDateTime.now());
        //保存标签到数据库
        tagService.save(tag);
        //返回创建成功的标签信息
        return R.ok(tag);
    }

    /**
     * 修改标签。
     * 仅允许修改自己创建的标签。
     */
    @Operation(summary = "修改标签")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TagDTO dto) {
        //获取当前登录用户ID
        long userId = StpUtil.getLoginIdAsLong();
        //根据标签ID查询标签信息
        BlogTag tag = tagService.getById(id);
        //标签不存在时返回404错误
        if (tag == null) return R.error(404, "标签不存在");
        //校验标签是否属于当前作者，仅允许修改自己创建的标签
        if (tag.getCreatedBy() == null || !tag.getCreatedBy().equals(userId)) {
            return R.error(403, "只能修改自己创建的标签");
        }
        //检查修改后的标签名称是否与当前作者下其他标签重复
        BlogTag duplicate = tagService.lambdaQuery().eq(BlogTag::getName, dto.getName().trim()).eq(BlogTag::getCreatedBy, userId).ne(BlogTag::getId, id).one();
        if (duplicate != null) return R.error(409, "标签名称已存在");
        //更新标签名称
        tag.setName(dto.getName().trim());
        tagService.updateById(tag);
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 删除标签。
     * 仅允许删除自己创建的标签。
     */
    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        //获取当前登录用户ID
        long userId = StpUtil.getLoginIdAsLong();
        //根据标签ID查询标签信息
        BlogTag tag = tagService.getById(id);
        //标签不存在时返回404错误
        if (tag == null) return R.error(404, "标签不存在");
        //校验标签是否属于当前作者，仅允许删除自己创建的标签
        if (tag.getCreatedBy() == null || !tag.getCreatedBy().equals(userId)) {
            return R.error(403, "只能删除自己创建的标签");
        }
        //调用业务层删除标签
        tagService.removeById(id);
        //返回正确的响应结果
        return R.ok();
    }

    @Data
    public static class TagDTO {
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 50, message = "标签名称不能超过 50 字")
        private String name;
    }
}
