package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.common.AuthorApplicationStatus;
import com.codenow.common.UserRole;
import com.codenow.common.UserStatus;
import com.codenow.dto.AuthorApplicationDTO;
import com.codenow.entity.AuthorApplication;
import com.codenow.entity.SysUser;
import com.codenow.entity.UserNotification;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.AuthorApplicationMapper;
import com.codenow.mapper.SysUserMapper;
import com.codenow.service.AuthorProfileService;
import com.codenow.service.SysUserService;
import com.codenow.service.UserNotificationService;
import jakarta.validation.Validation;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorApplicationRegressionTest {
    @Mock private AuthorApplicationMapper applicationMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private SysUserService userService;
    @Mock private AuthorProfileService profileService;
    @Mock private UserNotificationService notificationService;

    private AuthorApplicationServiceImpl service;

    @BeforeAll
    static void initializeLambdaMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "author-application-regression"),
                AuthorApplication.class);
    }

    @BeforeEach
    void setUp() {
        service = new AuthorApplicationServiceImpl(
                applicationMapper, userMapper, userService, profileService, notificationService);
    }

    @Test
    void omittedAgreementIsInvalid() {
        AuthorApplicationDTO dto = validDto();
        dto.setAgreementAccepted(null);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(dto).stream()
                    .anyMatch(v -> "agreementAccepted".equals(v.getPropertyPath().toString())));
        }
    }

    @Test
    void submitRejectsWhitespacePaddedReasonAndBioBelowTrimmedMinimum() {
        when(userService.getById(7L)).thenReturn(user(UserRole.USER, UserStatus.ACTIVE));

        AuthorApplicationDTO shortReason = validDto();
        shortReason.setReason(" ".repeat(60) + "short" + " ".repeat(60));
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.submit(7L, shortReason)).getCode());

        AuthorApplicationDTO shortBio = validDto();
        shortBio.setBio(" ".repeat(30) + "short" + " ".repeat(30));
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.submit(7L, shortBio)).getCode());

        verify(applicationMapper, never()).insert(any(AuthorApplication.class));
    }

    @Test
    void rejectAndRevokeBoundNotificationContentToColumnLimit() {
        AuthorApplication application = pendingApplication(21L, 7L);
        String reason = "x".repeat(500);
        when(applicationMapper.selectById(21L)).thenReturn(application);
        when(applicationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(notificationService.save(any(UserNotification.class))).thenReturn(true);

        service.reject(21L, 1L, reason);

        assertEquals(reason, application.getReviewRemark());
        ArgumentCaptor<UserNotification> notification = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationService).save(notification.capture());
        assertTrue(notification.getValue().getContent().length() <= 500);
    }

    @Test
    void rejectTruncatesNotificationAtUnicodeCodePointBoundary() {
        AuthorApplication application = pendingApplication(21L, 7L);
        String reason = "x".repeat(489) + "😀" + "y";
        when(applicationMapper.selectById(21L)).thenReturn(application);
        when(applicationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(notificationService.save(any(UserNotification.class))).thenReturn(true);

        service.reject(21L, 1L, reason);

        ArgumentCaptor<UserNotification> notification = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationService).save(notification.capture());
        String content = notification.getValue().getContent();
        assertTrue(content.codePointCount(0, content.length()) <= 500, content);
        assertFalse(hasUnpairedSurrogate(content), content);
        assertTrue(content.endsWith("😀"), content);
    }

    @Test
    void approveRequiresActiveUserAndAtomicExpectedRoleUpdate() {
        AuthorApplication application = pendingApplication(21L, 7L);
        when(applicationMapper.selectById(21L)).thenReturn(application);
        when(userService.getById(7L)).thenReturn(user(UserRole.USER, UserStatus.BANNED));

        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.approve(21L, 1L, null)).getCode());
        verify(userMapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void approveFailsIfRoleOrStatusChangesBeforeAtomicUpdate() {
        AuthorApplication application = pendingApplication(21L, 7L);
        when(applicationMapper.selectById(21L)).thenReturn(application);
        when(userService.getById(7L)).thenReturn(user(UserRole.USER, UserStatus.ACTIVE));
        when(applicationMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(userMapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.approve(21L, 1L, null)).getCode());
        ArgumentCaptor<Wrapper<SysUser>> update = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).update(isNull(), update.capture());
        String predicate = update.getValue().getSqlSegment().toLowerCase();
        assertTrue(predicate.contains("id") && predicate.contains("role") && predicate.contains("status"), predicate);
        String assignments = ((UpdateWrapper<SysUser>) update.getValue()).getSqlSet().toLowerCase();
        assertTrue(assignments.contains("role=") && assignments.contains("update_time="), assignments);
        assertTrue(!assignments.contains("status=") && !assignments.contains("ban_reason="), assignments);
        verify(profileService, never()).save(any());
        verify(notificationService, never()).save(any());
    }

    @Test
    void revokeUsesConditionalPartialUpdateAndFailsOnConcurrentRoleChange() {
        when(userService.getById(7L)).thenReturn(user(UserRole.AUTHOR, UserStatus.ACTIVE));
        when(userMapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.revokeAuthor(7L, 1L, "x".repeat(500))).getCode());
        ArgumentCaptor<Wrapper<SysUser>> update = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).update(isNull(), update.capture());
        String predicate = update.getValue().getSqlSegment().toLowerCase();
        assertTrue(predicate.contains("id") && predicate.contains("role") && !predicate.contains("status"), predicate);
        String assignments = ((UpdateWrapper<SysUser>) update.getValue()).getSqlSet().toLowerCase();
        assertTrue(assignments.contains("role=") && assignments.contains("update_time="), assignments);
        assertTrue(!assignments.contains("status=") && !assignments.contains("ban_reason="), assignments);
        verify(notificationService, never()).save(any());
    }

    @Test
    void revokeBoundsNotificationContentToColumnLimit() {
        when(userService.getById(7L)).thenReturn(user(UserRole.AUTHOR, UserStatus.ACTIVE));
        when(userMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(notificationService.save(any(UserNotification.class))).thenReturn(true);

        service.revokeAuthor(7L, 1L, "x".repeat(500));

        ArgumentCaptor<UserNotification> notification = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationService).save(notification.capture());
        assertTrue(notification.getValue().getContent().length() <= 500);
    }

    @Test
    void latestAndHistoryUseIdAsDeterministicDescendingTieBreaker() {
        when(applicationMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(applicationMapper.selectPage(any(Page.class), any(Wrapper.class)))
                .thenReturn(new Page<AuthorApplication>(1, 10, 0));

        service.latest(7L);
        service.pageMine(7L, 1, 10);

        ArgumentCaptor<Wrapper<AuthorApplication>> wrappers = ArgumentCaptor.forClass(Wrapper.class);
        verify(applicationMapper).selectOne(wrappers.capture());
        verify(applicationMapper).selectPage(any(Page.class), wrappers.capture());
        for (Wrapper<AuthorApplication> wrapper : wrappers.getAllValues()) {
            String sql = wrapper.getSqlSegment().replace("`", "").toLowerCase();
            assertTrue(sql.matches("(?s).*submitted_at\\s+desc.*id\\s+desc.*"), sql);
        }
    }

    @Test
    void adminPageUsesIdAsDeterministicDescendingTieBreaker() {
        when(applicationMapper.selectPage(any(Page.class), any(Wrapper.class)))
                .thenReturn(new Page<AuthorApplication>(1, 10, 0));

        service.pageAdmin(1, 10, null, null);

        ArgumentCaptor<Wrapper<AuthorApplication>> wrapper = ArgumentCaptor.forClass(Wrapper.class);
        verify(applicationMapper).selectPage(any(Page.class), wrapper.capture());
        String sql = wrapper.getValue().getSqlSegment().replace("`", "").toLowerCase();
        assertTrue(sql.matches("(?s).*status\\s+asc.*submitted_at\\s+desc.*id\\s+desc.*"), sql);
    }

    private static boolean hasUnpairedSurrogate(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    return true;
                }
                index++;
            } else if (Character.isLowSurrogate(current)) {
                return true;
            }
        }
        return false;
    }

    private static AuthorApplicationDTO validDto() {
        AuthorApplicationDTO dto = new AuthorApplicationDTO();
        dto.setReason("r".repeat(50));
        dto.setExpertise(java.util.List.of("Java"));
        dto.setBio("b".repeat(20));
        dto.setAgreementAccepted(true);
        return dto;
    }

    private static AuthorApplication pendingApplication(Long id, Long userId) {
        AuthorApplication application = new AuthorApplication();
        application.setId(id);
        application.setUserId(userId);
        application.setReason("r".repeat(50));
        application.setExpertise("Java");
        application.setBio("b".repeat(20));
        application.setStatus(AuthorApplicationStatus.PENDING);
        return application;
    }

    private static SysUser user(String role, String status) {
        SysUser user = new SysUser();
        user.setId(7L);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
