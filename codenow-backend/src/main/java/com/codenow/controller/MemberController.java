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

/**
 * 会员中心接口。所有数据操作都以当前 Sa-Token 登录用户为边界，并在响应前移除敏感字段。
 */
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

    /**
     * 获取当前用户个人信息。
     */
    @GetMapping("/profile")
    public R<SysUser> profile() {
        //获取当前登录用户信息
        SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
        //清除密码字段，避免敏感信息泄露
        user.setPassword(null);
        //返回用户个人信息
        return R.ok(user);
    }

    /**
     * 修改用户昵称。
     */
    @PutMapping("/profile")
    public R<SysUser> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        //获取当前登录用户信息
        SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
        //昵称按纯文本保存，避免个人资料字段成为持久化XSS入口
        String nickname = Jsoup.clean(dto.getNickname().trim(), Safelist.none());
        //校验昵称不能为空
        if (nickname.isBlank()) {
            return R.error(400, "昵称不能为空");
        }
        //更新用户昵称
        user.setNickname(nickname);
        userService.updateById(user);
        //清除密码字段，避免敏感信息泄露
        user.setPassword(null);
        //返回更新后的用户信息
        return R.ok(user);
    }

    /**
     * 修改密码。
     * 验证当前密码后修改为新密码，并踢出已有会话。
     */
    @PutMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        //获取当前登录用户信息
        SysUser user = currentUser();
        //验证当前密码是否正确
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            return R.error(400, "当前密码不正确");
        }
        //校验新密码不能与当前密码相同
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            return R.error(400, "新密码不能与当前密码相同");
        }
        //加密新密码并更新到数据库
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);
        //踢出该用户的所有已有会话，强制重新登录
        StpUtil.kickout(user.getId());
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 发送邮箱变更验证码。
     * 向新邮箱发送验证码，限制发送频率。
     */
    @RateLimit(maxCount = 10, timeWindow = 3600, message = "验证码发送次数过多，请稍后再试")
    @PostMapping("/email/code")
    public R<Void> sendChangeEmailCode(@Valid @RequestBody EmailCodeDTO dto) {
        //标准化邮箱格式（去除空格、转小写）
        String email = dto.getEmail().trim().toLowerCase();
        //检查新邮箱是否已被其他用户使用
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            return R.error(409, "该邮箱已被使用");
        }
        //调用邮件服务发送邮箱变更验证码
        emailCodeService.sendChangeEmailCode(email);
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 变更邮箱。
     * 校验验证码后将邮箱变更为新地址。
     */
    @PutMapping("/email")
    public R<SysUser> changeEmail(@Valid @RequestBody ChangeEmailDTO dto) {
        //标准化邮箱格式（去除空格、转小写）
        String email = dto.getEmail().trim().toLowerCase();
        //检查新邮箱是否已被其他用户使用
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            return R.error(409, "该邮箱已被使用");
        }
        //校验邮箱变更验证码是否正确
        emailCodeService.verifyChangeEmailCode(email, dto.getVerificationCode());
        //获取当前登录用户信息
        SysUser user = currentUser();
        //更新用户邮箱和验证状态
        user.setEmail(email);
        user.setEmailVerified(1);
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);
        //清除密码字段，避免敏感信息泄露
        user.setPassword(null);
        //返回更新后的用户信息
        return R.ok(user);
    }

    /**
     * 上传用户头像。
     */
    @RateLimit(maxCount = 10, timeWindow = 60, message = "上传过于频繁，请稍后再试")
    @PostMapping("/avatar")
    public R<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        //校验文件是否为空
        if (file.isEmpty()) return R.error(400, "请选择头像文件");
        //校验文件大小是否超过限制
        if (file.getSize() > uploadProperties.getMaxSize()) return R.error(400, "头像不能超过 5MB");
        try {
            //校验文件是否为合法的图片格式
            FileValidator.validateImage(file.getInputStream(), file.getOriginalFilename());
        } catch (Exception e) {
            return R.error(400, e instanceof IllegalArgumentException ? e.getMessage() : "头像读取失败");
        }
        //调用存储服务上传头像文件，返回可访问的URL
        String url = storageService.upload(file);
        //获取当前登录用户信息，更新头像URL
        SysUser user = currentUser();
        user.setAvatar(url);
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);
        //返回头像访问URL
        return R.ok(Map.of("url", url));
    }

    /**
     * 分页查询当前用户的评论列表。
     */
    @GetMapping("/comments")
    public R<Page<BlogComment>> comments(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        //分页查询当前用户的评论列表，按创建时间倒序排列
        Page<BlogComment> page = commentService.page(new Page<>(pageNum, Math.min(pageSize, 50)),
                new LambdaQueryWrapper<BlogComment>()
                        .eq(BlogComment::getUserId, StpUtil.getLoginIdAsLong())
                        .orderByDesc(BlogComment::getCreateTime));
        //批量查询评论关联的文章信息，避免N+1查询
        List<Long> articleIds = page.getRecords().stream().map(BlogComment::getArticleId).distinct().toList();
        Map<Long, BlogArticle> articles = articleIds.isEmpty() ? Collections.emptyMap()
                : articleService.listByIds(articleIds).stream().collect(Collectors.toMap(BlogArticle::getId, Function.identity()));
        //为每条评论补充文章标题，并清除敏感字段
        page.getRecords().forEach(comment -> {
            BlogArticle article = articles.get(comment.getArticleId());
            comment.setArticleTitle(article == null ? "文章已删除" : article.getTitle());
            comment.setEmail(null);
            comment.setIp(null);
            comment.setOwnedByCurrentUser(true);
        });
        //返回评论分页数据
        return R.ok(page);
    }

    /**
     * 删除自己的评论。
     * 仅删除评论内容，保留占位记录。
     */
    @DeleteMapping("/comments/{id}")
    public R<Void> deleteOwnComment(@PathVariable Long id) {
        //根据评论ID查询评论信息
        BlogComment comment = commentService.getById(id);
        //评论不存在时返回404错误
        if (comment == null) return R.error(404, "评论不存在");
        //校验评论是否属于当前用户，仅允许删除自己的评论
        if (comment.getUserId() == null || StpUtil.getLoginIdAsLong() != comment.getUserId()) {
            return R.error(403, "无权删除该评论");
        }
        //逻辑删除：保留占位记录，清除评论内容和用户信息
        comment.setContent("该评论已由用户删除");
        comment.setNickname("已删除用户");
        comment.setEmail(null);
        comment.setIp(null);
        comment.setUserId(null);
        commentService.updateById(comment);
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 收藏文章。
     */
    @PostMapping("/favorites/{articleId}")
    public R<Void> favorite(@PathVariable Long articleId) {
        //校验文章是否存在且为已发布状态
        BlogArticle article = articleService.getById(articleId);
        if (article == null || !Objects.equals(ArticleStatus.PUBLISHED, article.getStatus())) {
            return R.error(404, "文章不存在");
        }
        //创建收藏记录，设置用户ID、文章ID和收藏时间
        ArticleFavorite favorite = new ArticleFavorite();
        favorite.setUserId(StpUtil.getLoginIdAsLong());
        favorite.setArticleId(articleId);
        favorite.setCreateTime(LocalDateTime.now());
        try {
            //保存收藏记录，重复收藏按幂等成功处理
            favoriteService.save(favorite);
        } catch (DuplicateKeyException ignored) {
            //重复收藏按幂等成功处理
        }
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 取消收藏文章。
     */
    @DeleteMapping("/favorites/{articleId}")
    public R<Void> unfavorite(@PathVariable Long articleId) {
        //删除当前用户对该文章的收藏记录
        favoriteService.remove(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getUserId, StpUtil.getLoginIdAsLong())
                .eq(ArticleFavorite::getArticleId, articleId));
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 查询文章收藏状态。
     */
    @GetMapping("/favorites/{articleId}/status")
    public R<Map<String, Boolean>> favoriteStatus(@PathVariable Long articleId) {
        //查询当前用户是否收藏过该文章
        boolean favorited = favoriteService.count(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getUserId, StpUtil.getLoginIdAsLong())
                .eq(ArticleFavorite::getArticleId, articleId)) > 0;
        //返回收藏状态
        return R.ok(Map.of("favorited", favorited));
    }

    /**
     * 分页查询收藏文章列表。
     */
    @GetMapping("/favorites")
    public R<Page<FavoriteArticleVO>> favorites(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        //分页查询当前用户的收藏记录，按收藏时间倒序排列
        Page<ArticleFavorite> page = favoriteService.page(new Page<>(pageNum, Math.min(pageSize, 50)),
                new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getUserId, StpUtil.getLoginIdAsLong())
                        .orderByDesc(ArticleFavorite::getCreateTime));
        //批量查询收藏关联的文章信息，避免N+1查询
        List<Long> articleIds = page.getRecords().stream().map(ArticleFavorite::getArticleId).toList();
        Map<Long, BlogArticle> articleMap = articleIds.isEmpty() ? Collections.emptyMap()
                : articleService.listByIds(articleIds).stream()
                .collect(Collectors.toMap(BlogArticle::getId, Function.identity()));
        //将收藏记录转换为VO，过滤掉已删除的文章
        List<FavoriteArticleVO> records = page.getRecords().stream()
                .filter(item -> articleMap.containsKey(item.getArticleId()))
                .map(item -> toFavoriteVO(item, articleMap.get(item.getArticleId())))
                .toList();
        //构建分页结果
        Page<FavoriteArticleVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        //返回收藏文章分页数据
        return R.ok(result);
    }

    private FavoriteArticleVO toFavoriteVO(ArticleFavorite favorite, BlogArticle article) {
        //创建收藏文章VO对象
        FavoriteArticleVO vo = new FavoriteArticleVO();
        //设置文章基本信息
        vo.setArticleId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setArticleCreateTime(article.getCreateTime());
        //设置收藏时间
        vo.setFavoriteTime(favorite.getCreateTime());
        //返回收藏文章VO
        return vo;
    }

    private SysUser currentUser() {
        return userService.getById(StpUtil.getLoginIdAsLong());
    }
}
