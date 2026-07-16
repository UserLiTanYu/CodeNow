package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.common.ArticleStatus;
import com.codenow.common.R;
import com.codenow.dto.FavoriteArticleVO;
import com.codenow.dto.ProfileUpdateDTO;
import com.codenow.entity.ArticleFavorite;
import com.codenow.entity.BlogArticle;
import com.codenow.entity.SysUser;
import com.codenow.service.ArticleFavoriteService;
import com.codenow.service.BlogArticleService;
import com.codenow.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    private final SysUserService userService;
    private final ArticleFavoriteService favoriteService;
    private final BlogArticleService articleService;

    @GetMapping("/profile")
    public R<SysUser> profile() {
        SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
        user.setPassword(null);
        return R.ok(user);
    }

    @PutMapping("/profile")
    public R<SysUser> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
        String nickname = Jsoup.clean(dto.getNickname().trim(), Safelist.none());
        if (nickname.isBlank()) {
            return R.error(400, "昵称不能为空");
        }
        user.setNickname(nickname);
        userService.updateById(user);
        user.setPassword(null);
        return R.ok(user);
    }

    @PostMapping("/favorites/{articleId}")
    public R<Void> favorite(@PathVariable Long articleId) {
        BlogArticle article = articleService.getById(articleId);
        if (article == null || !Objects.equals(ArticleStatus.PUBLISHED, article.getStatus())) {
            return R.error(404, "文章不存在");
        }
        ArticleFavorite favorite = new ArticleFavorite();
        favorite.setUserId(StpUtil.getLoginIdAsLong());
        favorite.setArticleId(articleId);
        favorite.setCreateTime(LocalDateTime.now());
        try {
            favoriteService.save(favorite);
        } catch (DuplicateKeyException ignored) {
            // 重复收藏按幂等成功处理。
        }
        return R.ok();
    }

    @DeleteMapping("/favorites/{articleId}")
    public R<Void> unfavorite(@PathVariable Long articleId) {
        favoriteService.remove(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getUserId, StpUtil.getLoginIdAsLong())
                .eq(ArticleFavorite::getArticleId, articleId));
        return R.ok();
    }

    @GetMapping("/favorites/{articleId}/status")
    public R<Map<String, Boolean>> favoriteStatus(@PathVariable Long articleId) {
        boolean favorited = favoriteService.count(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getUserId, StpUtil.getLoginIdAsLong())
                .eq(ArticleFavorite::getArticleId, articleId)) > 0;
        return R.ok(Map.of("favorited", favorited));
    }

    @GetMapping("/favorites")
    public R<Page<FavoriteArticleVO>> favorites(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<ArticleFavorite> page = favoriteService.page(new Page<>(pageNum, Math.min(pageSize, 50)),
                new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getUserId, StpUtil.getLoginIdAsLong())
                        .orderByDesc(ArticleFavorite::getCreateTime));
        List<Long> articleIds = page.getRecords().stream().map(ArticleFavorite::getArticleId).toList();
        Map<Long, BlogArticle> articleMap = articleIds.isEmpty() ? Collections.emptyMap()
                : articleService.listByIds(articleIds).stream()
                .collect(Collectors.toMap(BlogArticle::getId, Function.identity()));
        List<FavoriteArticleVO> records = page.getRecords().stream()
                .filter(item -> articleMap.containsKey(item.getArticleId()))
                .map(item -> toFavoriteVO(item, articleMap.get(item.getArticleId())))
                .toList();
        Page<FavoriteArticleVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        return R.ok(result);
    }

    private FavoriteArticleVO toFavoriteVO(ArticleFavorite favorite, BlogArticle article) {
        FavoriteArticleVO vo = new FavoriteArticleVO();
        vo.setArticleId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setArticleCreateTime(article.getCreateTime());
        vo.setFavoriteTime(favorite.getCreateTime());
        return vo;
    }
}
