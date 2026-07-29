package com.codenow.controller;

import com.codenow.dto.SiteProfileUpdateDTO;
import com.codenow.entity.SiteProfile;
import com.codenow.service.SiteProfileService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SiteProfileControllerTest {
    @Test
    void adminAndPublicControllersUseTheSameSiteProfile() {
        SiteProfileService service = mock(SiteProfileService.class);
        SiteProfile profile = new SiteProfile();
        profile.setSiteName("码上记");
        profile.setSlogan("记录实践，分享知识");
        profile.setDescription("面向开发者的技术学习与知识分享平台。");
        profile.setBio("一个支持 Markdown 写作的个人技术博客，帮助开发者记录和分享学习笔记。");
        profile.setAboutContent("详细介绍本站的定位、内容方向与长期维护原则。");
        when(service.getSiteProfile()).thenReturn(profile);

        SiteProfileController adminController = new SiteProfileController(service);
        PublicSiteProfileController publicController = new PublicSiteProfileController(service);
        SiteProfileUpdateDTO dto = validDto();
        when(service.updateProfile(dto)).thenReturn(profile);

        assertEquals("码上记", adminController.get().getData().getSiteName());
        assertEquals("码上记", publicController.get().getData().getSiteName());
        assertEquals("码上记", adminController.update(dto).getData().getSiteName());
        verify(service).updateProfile(dto);
    }

    private SiteProfileUpdateDTO validDto() {
        SiteProfileUpdateDTO dto = new SiteProfileUpdateDTO();
        dto.setSiteName("码上记");
        dto.setSlogan("记录实践，分享知识");
        dto.setDescription("面向开发者的技术学习与知识分享平台。");
        dto.setBio("新的管理员个人简介内容。");
        dto.setAboutContent("详细介绍本站的定位、内容方向与长期维护原则。");
        return dto;
    }
}
