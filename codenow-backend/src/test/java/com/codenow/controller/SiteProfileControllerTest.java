package com.codenow.controller;

import com.codenow.dto.SiteProfileUpdateDTO;
import com.codenow.entity.SiteProfile;
import com.codenow.service.SiteProfileService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SiteProfileControllerTest {
    @Test
    void adminAndPublicControllersUseTheSameSiteProfile() {
        SiteProfileService service = mock(SiteProfileService.class);
        SiteProfile profile = new SiteProfile();
        profile.setBio("一个支持 Markdown 写作的个人技术博客，帮助开发者记录和分享学习笔记。");
        when(service.getSiteProfile()).thenReturn(profile);
        when(service.updateBio("新的管理员个人简介内容。")).thenReturn(profile);

        SiteProfileController adminController = new SiteProfileController(service);
        PublicSiteProfileController publicController = new PublicSiteProfileController(service);
        SiteProfileUpdateDTO dto = new SiteProfileUpdateDTO();
        dto.setBio("新的管理员个人简介内容。");

        assertSame(profile, adminController.get().getData());
        assertSame(profile, publicController.get().getData());
        assertSame(profile, adminController.update(dto).getData());
        verify(service).updateBio("新的管理员个人简介内容。");
    }
}
