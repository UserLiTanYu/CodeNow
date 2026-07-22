package com.codenow.controller;

import com.codenow.annotation.OperationLog;
import com.codenow.common.R;
import com.codenow.dto.TagDTO;
import com.codenow.entity.BlogTag;
import com.codenow.service.BlogTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "标签管理")
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
/**
 * 标签管理控制器。
 * 提供标签的增删改查功能。
 */
public class TagController {

    private final BlogTagService tagService;

    /**
     * 查询全部标签列表。
     */
    @Operation(summary = "查询标签列表", description = "查询全部标签")
    @GetMapping
    public R<List<BlogTag>> list() {
        //查询全部标签列表
        return R.ok(tagService.list());
    }

    /**
     * 新增标签。
     */
    @OperationLog("新增标签")
    @Operation(summary = "新增标签", description = "创建一个新标签")
    @PostMapping
    public R<Void> save(@Valid @RequestBody TagDTO dto) {
        //创建标签实体类
        BlogTag tag = new BlogTag();
        //将DTO参数复制到实体对象中
        BeanUtils.copyProperties(dto, tag);
        //调用业务层保存标签到数据库
        tagService.save(tag);
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 修改标签。
     */
    @OperationLog("修改标签")
    @Operation(summary = "修改标签", description = "根据 ID 修改标签名称")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "标签 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody TagDTO dto) {
        //根据标签ID查询标签信息
        BlogTag tag = tagService.getById(id);
        //标签不存在时返回404错误
        if (tag == null) {
            return R.error(404, "标签不存在");
        }
        //将DTO参数复制到实体对象中
        BeanUtils.copyProperties(dto, tag);
        //设置标签ID，确保更新的是指定标签
        tag.setId(id);
        //调用业务层更新标签
        tagService.updateById(tag);
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 删除标签。
     */
    @OperationLog("删除标签")
    @Operation(summary = "删除标签", description = "根据 ID 逻辑删除标签")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "标签 ID", example = "1") @PathVariable Long id) {
        //调用业务层逻辑删除标签
        tagService.removeById(id);
        //返回正确的响应结果
        return R.ok();
    }
}
