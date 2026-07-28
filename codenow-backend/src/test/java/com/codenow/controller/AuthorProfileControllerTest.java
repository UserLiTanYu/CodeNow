package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.codenow.dto.AuthorProfileUpdateDTO;
import com.codenow.entity.AuthorProfile;
import com.codenow.service.AuthorProfileService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class AuthorProfileControllerTest {

    @Test
    void readsAndUpdatesOnlyTheCurrentAuthorProfile() {
        AuthorProfileService service = mock(AuthorProfileService.class);
        AuthorProfileController controller = new AuthorProfileController(service);
        AuthorProfile profile = new AuthorProfile();
        profile.setUserId(7L);
        AuthorProfileUpdateDTO dto = new AuthorProfileUpdateDTO();
        dto.setBio("专注 Java 后端工程实践与性能优化。");
        dto.setExpertise(List.of("Java"));
        when(service.getByUserId(7L)).thenReturn(profile);
        when(service.updateAuthorProfile(7L, dto)).thenReturn(profile);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);

            assertSame(profile, controller.get().getData());
            assertSame(profile, controller.update(dto).getData());
        }

        verify(service).getByUserId(7L);
        verify(service).updateAuthorProfile(7L, dto);
    }
}
