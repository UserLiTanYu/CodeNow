package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.common.ArticleStatus;
import com.codenow.common.FileValidator;
import com.codenow.common.R;
import com.codenow.annotation.RateLimit;
import com.codenow.config.UploadProperties;
import com.codenow.dto.ChangeEmailDTO;
import com.codenow.dto.ChangePasswordDTO;
import com.codenow.dto.EmailCodeDTO;
import com.codenow.dto.FavoriteArticleVO;
import com.codenow.dto.ProfileUpdateDTO;
import com.codenow.entity.ArticleFavorite;
import com.codenow.entity.BlogArticle;
import com.codenow.entity.BlogComment;
import com.codenow.entity.SysUser;
import com.codenow.service.ArticleFavoriteService;
import com.codenow.service.BlogArticleService;
import com.codenow.service.CommentService;
import com.codenow.service.EmailCodeService;
import com.codenow.service.StorageService;
import com.codenow.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final CommentService commentService;
    private final EmailCodeService emailCodeService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final UploadProperties uploadProperties;

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

    @PutMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        SysUser user = currentUser();
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            return R.error(400, "当前密码不正确");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            return R.error(400, "新密码不能与当前密码相同");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);
        StpUtil.kickout(user.getId());
        return R.ok();
    }

    @RateLimit(maxCount = 10, timeWindow = 3600, message = "验证码发送次数过多，请稍后再试")
    @PostMapping("/email/code")
    public R<Void> sendChangeEmailCode(@Valid @RequestBody EmailCodeDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            return R.error(409, "该邮箱已被使用");
        }
        emailCodeService.sendChangeEmailCode(email);
        return R.ok();
    }

    @PutMapping("/email")
    public R<SysUser> changeEmail(@Valid @RequestBody ChangeEmailDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            return R.error(409, "该邮箱已被使用");
        }
        emailCodeService.verifyChangeEmailCode(email, dto.getVerificationCode());
        SysUser user = currentUser();
        user.setEmail(email);
        user.setEmailVerified(1);
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);
        user.setPassword(null);
        return R.ok(user);
    }

    @RateLimit(maxCount = 10, timeWindow = 60, message = "上传过于频繁，请稍后再试")
    @PostMapping("/avatar")
    public R<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return R.error(400, "请选择头像文件");
        if (file.getSize() > uploadProperties.getMaxSize()) return R.error(400, "头像不能超过 5MB");
        try {
            FileValidator.validateImage(file.getInputStream(), file.getOriginalFilename());
        } catch (Exception e) {
            return R.error(400, e instanceof IllegalArgumentException ? e.getMessage() : "头像读取失败");
        }
        String url = storageService.upload(file);
        SysUser user = currentUser();
        user.setAvatar(url);
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);
        return R.ok(Map.of("url", url));
    }

    @GetMapping("/comments")
    public R<Page<BlogComment>> comments(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<BlogComment> page = commentService.page(new Page<>(pageNum, Math.min(pageSize, 50)),
                new LambdaQueryWrapper<BlogComment>()
                        .eq(BlogComment::getUserId, StpUtil.getLoginIdAsLong())
                        .orderByDesc(BlogComment::getCreateTime));
        List<Long> articleIds = page.getRecords().stream().map(BlogComment::getArticleId).distinct().toList();
        Map<Long, BlogArticle> articles = articleIds.isEmpty() ? Collections.emptyMap()
                : articleService.listByIds(articleIds).stream().collect(Collectors.toMap(BlogArticle::getId, Function.identity()));
        page.getRecords().forEach(comment -> {
            BlogArticle article = articles.get(comment.getArticleId());
            comment.setArticleTitle(article == null ? "文章已删除" : article.getTitle());
            comment.setEmail(null);
            comment.setIp(null);
            comment.setOwnedByCurrentUser(true);
        });
        return R.ok(page);
    }

    @DeleteMapping("/comments/{id}")
    public R<Void> deleteOwnComment(@PathVariable Long id) {
        BlogComment comment = commentService.getById(id);
        if (comment == null) return R.error(404, "评论不存在");
        if (comment.getUserId() == null || StpUtil.getLoginIdAsLong() != comment.getUserId()) {
            return R.error(403, "无权删除该评论");
        }
        comment.setContent("该评论已由用户删除");
        comment.setNickname("已删除用户");
        comment.setEmail(null);
        comment.setIp(null);
        comment.setUserId(null);
        commentService.updateById(comment);
        return R.ok();
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

    private SysUser currentUser() {
        return userService.getById(StpUtil.getLoginIdAsLong());
    }
}
