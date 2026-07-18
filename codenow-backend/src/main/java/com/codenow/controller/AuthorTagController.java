package com.codenow.controller;

import com.codenow.common.R;
import com.codenow.entity.BlogTag;
import com.codenow.service.BlogTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
