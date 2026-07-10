package com.codenow.controller;

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
public class TagController {

    private final BlogTagService tagService;

    @Operation(summary = "查询标签列表", description = "查询全部标签")
    @GetMapping
    public R<List<BlogTag>> list() {
        return R.ok(tagService.list());
    }

    @Operation(summary = "新增标签", description = "创建一个新标签")
    @PostMapping
    public R<Void> save(@Valid @RequestBody TagDTO dto) {
        BlogTag tag = new BlogTag();
        BeanUtils.copyProperties(dto, tag);
        tagService.save(tag);
        return R.ok();
    }

    @Operation(summary = "删除标签", description = "根据 ID 逻辑删除标签")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "标签 ID", example = "1") @PathVariable Long id) {
        tagService.removeById(id);
        return R.ok();
    }
}
