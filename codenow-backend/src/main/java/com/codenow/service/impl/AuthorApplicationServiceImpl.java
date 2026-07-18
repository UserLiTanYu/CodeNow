package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.common.AuthorApplicationStatus;
import com.codenow.common.UserRole;
import com.codenow.common.UserStatus;
import com.codenow.dto.AuthorApplicationDTO;
import com.codenow.dto.AuthorApplicationVO;
import com.codenow.entity.AuthorApplication;
import com.codenow.entity.AuthorProfile;
import com.codenow.entity.SysUser;
import com.codenow.entity.UserNotification;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.AuthorApplicationMapper;
import com.codenow.mapper.SysUserMapper;
import com.codenow.service.AuthorApplicationService;
import com.codenow.service.AuthorProfileService;
import com.codenow.service.SysUserService;
import com.codenow.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorApplicationServiceImpl implements AuthorApplicationService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_NOTIFICATION_CONTENT_CODE_POINTS = 500;

    private final AuthorApplicationMapper applicationMapper;
    private final SysUserMapper userMapper;
    private final SysUserService userService;
    private final AuthorProfileService profileService;
    private final UserNotificationService notificationService;

    @Override
    @Transactional
    public AuthorApplication submit(Long userId, AuthorApplicationDTO dto) {
        SysUser user = requireUser(userId);
        if (!UserStatus.ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(403, "账号不可用");
        }
        if (!UserRole.USER.equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(409, "当前账号无需重复申请作者");
        }
        String normalizedReason = requireTrimmedMinimum(dto.getReason(), 50, "申请理由长度不能少于 50 个字符");
        String normalizedBio = requireTrimmedMinimum(dto.getBio(), 20, "个人简介长度不能少于 20 个字符");
        long pendingCount = applicationMapper.selectCount(new LambdaQueryWrapper<AuthorApplication>()
                .eq(AuthorApplication::getUserId, userId)
                .eq(AuthorApplication::getStatus, AuthorApplicationStatus.PENDING));
        if (pendingCount > 0) {
            throw new BusinessException(409, "已有待审核的作者申请");
        }

        AuthorApplication application = new AuthorApplication();
        application.setUserId(userId);
        application.setReason(normalizedReason);
        application.setExpertise(normalizeExpertise(dto.getExpertise()));
        application.setBio(normalizedBio);
        application.setPortfolioUrl(trimToNull(dto.getPortfolioUrl()));
        application.setWebsiteUrl(trimToNull(dto.getWebsiteUrl()));
        application.setStatus(AuthorApplicationStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        application.setSubmittedAt(now);
        application.setUpdateTime(now);
        try {
            if (applicationMapper.insert(application) != 1) {
                throw new BusinessException(500, "作者申请保存失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "已有待审核的作者申请");
        }
        return application;
    }

    @Override
    public AuthorApplicationVO latest(Long userId) {
        AuthorApplication application = applicationMapper.selectOne(new LambdaQueryWrapper<AuthorApplication>()
                .eq(AuthorApplication::getUserId, userId)
                .orderByDesc(AuthorApplication::getSubmittedAt)
                .orderByDesc(AuthorApplication::getId)
                .last("LIMIT 1"));
        return application == null ? null : toVO(application, userMap(List.of(application)));
    }

    @Override
    public Page<AuthorApplicationVO> pageMine(Long userId, Integer pageNum, Integer pageSize) {
        Page<AuthorApplication> page = applicationMapper.selectPage(page(pageNum, pageSize),
                new LambdaQueryWrapper<AuthorApplication>()
                        .eq(AuthorApplication::getUserId, userId)
                        .orderByDesc(AuthorApplication::getSubmittedAt)
                        .orderByDesc(AuthorApplication::getId));
        return toVOPage(page);
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long applicationId) {
        AuthorApplication application = applicationMapper.selectById(applicationId);
        if (application == null || !Objects.equals(application.getUserId(), userId)) {
            throw new BusinessException(404, "作者申请不存在");
        }
        transition(application, AuthorApplicationStatus.CANCELED, null, null);
    }

    @Override
    public Page<AuthorApplicationVO> pageAdmin(Integer pageNum, Integer pageSize, String status, String keyword) {
        LambdaQueryWrapper<AuthorApplication> wrapper = new LambdaQueryWrapper<>();
        String normalizedStatus = trimToNull(status);
        if (normalizedStatus != null) {
            requireStatus(normalizedStatus);
            wrapper.eq(AuthorApplication::getStatus, normalizedStatus.toUpperCase());
        }
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            List<Long> userIds = userService.list(new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getUsername, normalizedKeyword)
                            .or().like(SysUser::getNickname, normalizedKeyword)
                            .or().like(SysUser::getEmail, normalizedKeyword))
                    .stream().map(SysUser::getId).toList();
            wrapper.and(query -> query
                    .like(AuthorApplication::getExpertise, normalizedKeyword)
                    .or().in(!userIds.isEmpty(), AuthorApplication::getUserId, userIds));
        }
        wrapper.orderByAsc(AuthorApplication::getStatus)
                .orderByDesc(AuthorApplication::getSubmittedAt)
                .orderByDesc(AuthorApplication::getId);
        return toVOPage(applicationMapper.selectPage(page(pageNum, pageSize), wrapper));
    }

    @Override
    public AuthorApplicationVO detail(Long applicationId) {
        AuthorApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(404, "作者申请不存在");
        }
        return toVO(application, userMap(List.of(application)));
    }

    @Override
    @Transactional
    public void approve(Long applicationId, Long reviewerId, String remark) {
        AuthorApplication application = requirePending(applicationId);
        SysUser applicant = requireUser(application.getUserId());
        if (!UserRole.USER.equalsIgnoreCase(applicant.getRole())) {
            throw new BusinessException(409, "申请人角色已发生变化");
        }
        if (!UserStatus.ACTIVE.equalsIgnoreCase(applicant.getStatus())) {
            throw new BusinessException(409, "申请人账号状态已发生变化");
        }
        transition(application, AuthorApplicationStatus.APPROVED, reviewerId, trimToNull(remark));

        int promoted = userMapper.update(null, new UpdateWrapper<SysUser>()
                .eq("id", applicant.getId())
                .eq("role", UserRole.USER)
                .eq("status", UserStatus.ACTIVE)
                .set("role", UserRole.AUTHOR)
                .set("update_time", LocalDateTime.now()));
        if (promoted != 1) {
            throw new BusinessException(409, "申请人角色或账号状态已发生变化");
        }

        AuthorProfile profile = profileService.getByUserId(applicant.getId());
        boolean isNew = profile == null;
        if (isNew) {
            profile = new AuthorProfile();
            profile.setUserId(applicant.getId());
            profile.setCreateTime(LocalDateTime.now());
        }
        profile.setBio(application.getBio());
        profile.setExpertise(application.getExpertise());
        profile.setWebsiteUrl(application.getWebsiteUrl());
        profile.setPortfolioUrl(application.getPortfolioUrl());
        profile.setUpdateTime(LocalDateTime.now());
        boolean profileSaved = isNew ? profileService.save(profile) : profileService.updateById(profile);
        if (!profileSaved) {
            throw new BusinessException(500, "作者资料保存失败");
        }
        notifyUser(applicant.getId(), "AUTHOR_APPLICATION_APPROVED", "作者申请已通过",
                "你的作者申请已通过，作者身份已生效。");
    }

    @Override
    @Transactional
    public void reject(Long applicationId, Long reviewerId, String reason) {
        String normalizedReason = trimToNull(reason);
        if (normalizedReason == null) {
            throw new BusinessException(400, "驳回原因不能为空");
        }
        AuthorApplication application = requirePending(applicationId);
        transition(application, AuthorApplicationStatus.REJECTED, reviewerId, normalizedReason);
        notifyUser(application.getUserId(), "AUTHOR_APPLICATION_REJECTED", "作者申请未通过",
                "你的作者申请未通过：" + normalizedReason);
    }

    @Override
    @Transactional
    public void revokeAuthor(Long userId, Long reviewerId, String reason) {
        String normalizedReason = trimToNull(reason);
        if (normalizedReason == null) {
            throw new BusinessException(400, "撤销原因不能为空");
        }
        SysUser user = requireUser(userId);
        if (!UserRole.AUTHOR.equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(409, "该用户当前不是作者");
        }
        if (Objects.equals(userId, reviewerId)) {
            throw new BusinessException(400, "不能撤销当前登录账号的作者资格");
        }
        int revoked = userMapper.update(null, new UpdateWrapper<SysUser>()
                .eq("id", userId)
                .eq("role", UserRole.AUTHOR)
                .set("role", UserRole.USER)
                .set("update_time", LocalDateTime.now()));
        if (revoked != 1) {
            throw new BusinessException(409, "用户角色已发生变化");
        }
        notifyUser(userId, "AUTHOR_ROLE_REVOKED", "作者资格已撤销",
                "你的作者资格已被撤销：" + normalizedReason);
    }

    private AuthorApplication requirePending(Long applicationId) {
        AuthorApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(404, "作者申请不存在");
        }
        if (!AuthorApplicationStatus.PENDING.equals(application.getStatus())) {
            throw new BusinessException(409, "该作者申请已处理");
        }
        return application;
    }

    private void transition(AuthorApplication application, String targetStatus, Long reviewerId, String remark) {
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<AuthorApplication> update = new UpdateWrapper<AuthorApplication>()
                .eq("id", application.getId())
                .eq("status", AuthorApplicationStatus.PENDING)
                .set("status", targetStatus)
                .set("update_time", now);
        if (reviewerId != null) {
            update.set("reviewer_id", reviewerId)
                    .set("reviewed_at", now)
                    .set("review_remark", remark);
        }
        if (applicationMapper.update(null, update) != 1) {
            throw new BusinessException(409, "该作者申请已处理");
        }
        application.setStatus(targetStatus);
        application.setReviewerId(reviewerId);
        application.setReviewRemark(remark);
        application.setReviewedAt(reviewerId == null ? null : now);
        application.setUpdateTime(now);
    }

    private void notifyUser(Long userId, String type, String title, String content) {
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(truncateCodePoints(content, MAX_NOTIFICATION_CONTENT_CODE_POINTS));
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        if (!notificationService.save(notification)) {
            throw new BusinessException(500, "站内通知保存失败");
        }
    }

    private String truncateCodePoints(String value, int maximumCodePoints) {
        if (value.codePointCount(0, value.length()) <= maximumCodePoints) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maximumCodePoints));
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private String normalizeExpertise(List<String> expertise) {
        if (expertise == null) {
            throw new BusinessException(400, "请至少填写一个擅长领域");
        }
        Set<String> normalized = expertise.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            throw new BusinessException(400, "请至少填写一个擅长领域");
        }
        return String.join(",", normalized);
    }

    private void requireStatus(String status) {
        if (!List.of(AuthorApplicationStatus.PENDING, AuthorApplicationStatus.APPROVED,
                AuthorApplicationStatus.REJECTED, AuthorApplicationStatus.CANCELED)
                .contains(status.toUpperCase())) {
            throw new BusinessException(400, "不支持的申请状态");
        }
    }

    private Page<AuthorApplication> page(Integer pageNum, Integer pageSize) {
        int current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        return new Page<>(current, size);
    }

    private Page<AuthorApplicationVO> toVOPage(Page<AuthorApplication> source) {
        Map<Long, SysUser> users = userMap(source.getRecords());
        Page<AuthorApplicationVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(item -> toVO(item, users)).toList());
        return result;
    }

    private Map<Long, SysUser> userMap(Collection<AuthorApplication> applications) {
        Set<Long> ids = new LinkedHashSet<>();
        applications.forEach(item -> {
            if (item.getUserId() != null) ids.add(item.getUserId());
            if (item.getReviewerId() != null) ids.add(item.getReviewerId());
        });
        if (ids.isEmpty()) return Collections.emptyMap();
        return userService.listByIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
    }

    private AuthorApplicationVO toVO(AuthorApplication source, Map<Long, SysUser> users) {
        AuthorApplicationVO vo = new AuthorApplicationVO();
        vo.setId(source.getId());
        vo.setUserId(source.getUserId());
        SysUser applicant = users.get(source.getUserId());
        if (applicant != null) {
            vo.setUsername(applicant.getUsername());
            vo.setNickname(applicant.getNickname());
            vo.setEmail(applicant.getEmail());
        }
        vo.setReason(source.getReason());
        vo.setExpertise(splitExpertise(source.getExpertise()));
        vo.setBio(source.getBio());
        vo.setPortfolioUrl(source.getPortfolioUrl());
        vo.setWebsiteUrl(source.getWebsiteUrl());
        vo.setStatus(source.getStatus());
        vo.setReviewerId(source.getReviewerId());
        SysUser reviewer = users.get(source.getReviewerId());
        if (reviewer != null) vo.setReviewerName(reviewer.getNickname() == null ? reviewer.getUsername() : reviewer.getNickname());
        vo.setReviewRemark(source.getReviewRemark());
        vo.setSubmittedAt(source.getSubmittedAt());
        vo.setReviewedAt(source.getReviewedAt());
        vo.setUpdateTime(source.getUpdateTime());
        return vo;
    }

    private List<String> splitExpertise(String expertise) {
        if (expertise == null || expertise.isBlank()) return Collections.emptyList();
        return Arrays.stream(expertise.split(",")).map(String::trim).filter(item -> !item.isEmpty()).toList();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireTrimmedMinimum(String value, int minimum, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null || trimmed.length() < minimum) {
            throw new BusinessException(400, message);
        }
        return trimmed;
    }
}
