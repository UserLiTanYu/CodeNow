package com.codenow.service.impl;

import com.codenow.dto.SiteProfileUpdateDTO;
import com.codenow.entity.SiteProfile;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.SiteProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteProfileServiceImplTest {
    @InjectMocks
    private SiteProfileServiceImpl service;

    @Mock
    private SiteProfileMapper mapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void returnsDefaultProfileWhenTheRowHasNotBeenCreated() {
        when(mapper.selectById(SiteProfileServiceImpl.PROFILE_ID)).thenReturn(null);

        SiteProfile profile = service.getSiteProfile();

        assertEquals(SiteProfileServiceImpl.PROFILE_ID, profile.getId());
        assertEquals(SiteProfileServiceImpl.DEFAULT_SITE_NAME, profile.getSiteName());
        assertEquals(SiteProfileServiceImpl.DEFAULT_SLOGAN, profile.getSlogan());
        assertEquals(SiteProfileServiceImpl.DEFAULT_DESCRIPTION, profile.getDescription());
        assertEquals(SiteProfileServiceImpl.DEFAULT_BIO, profile.getBio());
        assertEquals(SiteProfileServiceImpl.DEFAULT_ABOUT_CONTENT, profile.getAboutContent());
    }

    @Test
    void updatesTheSingletonProfile() {
        SiteProfile existing = new SiteProfile();
        existing.setId(SiteProfileServiceImpl.PROFILE_ID);
        when(mapper.selectById(SiteProfileServiceImpl.PROFILE_ID)).thenReturn(existing);
        when(mapper.updateById(existing)).thenReturn(1);

        SiteProfileUpdateDTO dto = validDto();
        dto.setSiteName("  码上记  ");
        dto.setContactEmail("  contact@example.com  ");
        SiteProfile result = service.updateProfile(dto);

        assertEquals("码上记", result.getSiteName());
        assertEquals("专注技术内容创作与学习经验分享。", result.getBio());
        assertEquals("contact@example.com", result.getContactEmail());
        assertNotNull(result.getUpdateTime());
        verify(mapper).updateById(existing);
    }

    @Test
    void rejectsBioThatIsTooShortAfterTrimming() {
        SiteProfileUpdateDTO dto = validDto();
        dto.setBio("         简介");
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateProfile(dto));

        assertEquals(400, exception.getCode());
    }

    private SiteProfileUpdateDTO validDto() {
        SiteProfileUpdateDTO dto = new SiteProfileUpdateDTO();
        dto.setSiteName("码上记");
        dto.setSlogan("记录实践，分享知识");
        dto.setDescription("面向开发者的技术学习与知识分享平台。");
        dto.setBio("专注技术内容创作与学习经验分享。");
        dto.setAboutContent("详细介绍本站的定位、内容方向与长期维护原则。");
        return dto;
    }
}
