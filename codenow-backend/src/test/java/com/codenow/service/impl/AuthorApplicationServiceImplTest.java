package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.codenow.common.AuthorApplicationStatus;
import com.codenow.common.UserRole;
import com.codenow.dto.AuthorApplicationDTO;
import com.codenow.entity.AuthorApplication;
import com.codenow.entity.AuthorProfile;
import com.codenow.entity.SysUser;
import com.codenow.entity.UserNotification;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.AuthorApplicationMapper;
import com.codenow.mapper.SysUserMapper;
import com.codenow.service.AuthorProfileService;
import com.codenow.service.SysUserService;
import com.codenow.service.UserNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorApplicationServiceImplTest {
    @Mock private AuthorApplicationMapper applicationMapper;
    @Mock private SysUserMapper userMapper;
    @Mock private SysUserService userService;
    @Mock private AuthorProfileService profileService;
    @Mock private UserNotificationService notificationService;

    private AuthorApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthorApplicationServiceImpl(applicationMapper, userMapper, userService, profileService, notificationService);
    }

    @Test
    void submit_shouldCreatePendingApplicationForOrdinaryUser() {
        SysUser user = user(7L, UserRole.USER);
        when(userService.getById(7L)).thenReturn(user);
        when(applicationMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(applicationMapper.insert(any(AuthorApplication.class))).thenAnswer(invocation -> {
            invocation.<AuthorApplication>getArgument(0).setId(21L);
            return 1;
        });

        AuthorApplication result = service.submit(7L, applicationDto());

        assertEquals(21L, result.getId());
        assertEquals(7L, result.getUserId());
        assertEquals(AuthorApplicationStatus.PENDING, result.getStatus());
        assertEquals("Java,Spring Boot", result.getExpertise());
        verify(applicationMapper).insert(result);
    }

    @Test
    void submit_shouldRejectDuplicatePendingApplication() {
        when(userService.getById(7L)).thenReturn(user(7L, UserRole.USER));
        when(applicationMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.submit(7L, applicationDto()));

        assertEquals(409, error.getCode());
        verify(applicationMapper, never()).insert(any(AuthorApplication.class));
    }

    @Test
    void submit_shouldRejectAuthorOrAdministrator() {
        when(userService.getById(7L)).thenReturn(user(7L, UserRole.AUTHOR));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.submit(7L, applicationDto()));

        assertEquals(409, error.getCode());
        verify(applicationMapper, never()).insert(any(AuthorApplication.class));
    }

    @Test
    void approve_shouldAtomicallyPromoteUserCreateProfileAndNotify() {
        AuthorApplication application = pendingApplication(21L, 7L);
        SysUser applicant = user(7L, UserRole.USER);
        when(applicationMapper.selectById(21L)).thenReturn(application);
        when(userService.getById(7L)).thenReturn(applicant);
        when(applicationMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(userMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(profileService.getByUserId(7L)).thenReturn(null);
        when(profileService.save(any(AuthorProfile.class))).thenReturn(true);
        when(notificationService.save(any(UserNotification.class))).thenReturn(true);

        service.approve(21L, 1L, "资料完整");

        assertEquals(AuthorApplicationStatus.APPROVED, application.getStatus());
        assertEquals(1L, application.getReviewerId());
        assertEquals(UserRole.USER, applicant.getRole());

        ArgumentCaptor<AuthorProfile> profileCaptor = ArgumentCaptor.forClass(AuthorProfile.class);
        verify(profileService).save(profileCaptor.capture());
        assertEquals(7L, profileCaptor.getValue().getUserId());
        assertEquals(application.getBio(), profileCaptor.getValue().getBio());

        ArgumentCaptor<UserNotification> notificationCaptor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationService).save(notificationCaptor.capture());
        assertEquals(7L, notificationCaptor.getValue().getUserId());
        assertEquals("AUTHOR_APPLICATION_APPROVED", notificationCaptor.getValue().getType());
    }

    @Test
    void reject_shouldKeepUserRoleAndRequireReason() {
        AuthorApplication application = pendingApplication(21L, 7L);
        when(applicationMapper.selectById(21L)).thenReturn(application);
        when(applicationMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(notificationService.save(any(UserNotification.class))).thenReturn(true);

        service.reject(21L, 1L, "作品样例不足");

        assertEquals(AuthorApplicationStatus.REJECTED, application.getStatus());
        assertEquals("作品样例不足", application.getReviewRemark());
        verify(userService, never()).updateById(any());
        verify(profileService, never()).save(any());
        verify(notificationService).save(argThat(item ->
                item.getUserId().equals(7L) && "AUTHOR_APPLICATION_REJECTED".equals(item.getType())));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.reject(21L, 1L, " "));
        assertEquals(400, error.getCode());
    }

    @Test
    void cancel_shouldOnlyAllowOwnerOfPendingApplication() {
        AuthorApplication application = pendingApplication(21L, 7L);
        when(applicationMapper.selectById(21L)).thenReturn(application);
        when(applicationMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        service.cancel(7L, 21L);
        assertEquals(AuthorApplicationStatus.CANCELED, application.getStatus());

        AuthorApplication other = pendingApplication(22L, 8L);
        when(applicationMapper.selectById(22L)).thenReturn(other);
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.cancel(7L, 22L));
        assertEquals(404, error.getCode());
    }

    @Test
    void revokeAuthor_shouldDowngradeRoleAndPreserveContent() {
        SysUser author = user(7L, UserRole.AUTHOR);
        when(userService.getById(7L)).thenReturn(author);
        when(userMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(notificationService.save(any(UserNotification.class))).thenReturn(true);

        service.revokeAuthor(7L, 1L, "长期违反创作规范");

        assertEquals(UserRole.AUTHOR, author.getRole());
        verify(notificationService).save(argThat(item ->
                item.getUserId().equals(7L) && "AUTHOR_ROLE_REVOKED".equals(item.getType())));
    }

    private static AuthorApplicationDTO applicationDto() {
        AuthorApplicationDTO dto = new AuthorApplicationDTO();
        dto.setReason("r".repeat(50));
        dto.setExpertise(java.util.List.of("Java", "Spring Boot"));
        dto.setBio("b".repeat(20));
        dto.setPortfolioUrl("https://example.com/works");
        dto.setWebsiteUrl("https://example.com");
        dto.setAgreementAccepted(true);
        return dto;
    }

    private static AuthorApplication pendingApplication(Long id, Long userId) {
        AuthorApplication application = new AuthorApplication();
        application.setId(id);
        application.setUserId(userId);
        application.setReason("希望持续分享后端开发经验和项目实践");
        application.setExpertise("Java,Spring Boot");
        application.setBio("专注于 Java 后端开发，并持续记录完整项目实践过程。");
        application.setWebsiteUrl("https://example.com");
        application.setPortfolioUrl("https://example.com/works");
        application.setStatus(AuthorApplicationStatus.PENDING);
        return application;
    }

    private static SysUser user(Long id, String role) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRole(role);
        user.setStatus("ACTIVE");
        return user;
    }
}
