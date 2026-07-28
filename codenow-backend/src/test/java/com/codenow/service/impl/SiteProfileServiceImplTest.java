package com.codenow.service.impl;

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
    void returnsDefaultBioWhenTheRowHasNotBeenCreated() {
        when(mapper.selectById(SiteProfileServiceImpl.PROFILE_ID)).thenReturn(null);

        SiteProfile profile = service.getSiteProfile();

        assertEquals(SiteProfileServiceImpl.PROFILE_ID, profile.getId());
        assertEquals(SiteProfileServiceImpl.DEFAULT_BIO, profile.getBio());
    }

    @Test
    void updatesTheSingletonProfileBio() {
        SiteProfile existing = new SiteProfile();
        existing.setId(SiteProfileServiceImpl.PROFILE_ID);
        when(mapper.selectById(SiteProfileServiceImpl.PROFILE_ID)).thenReturn(existing);
        when(mapper.updateById(existing)).thenReturn(1);

        SiteProfile result = service.updateBio("  专注技术内容创作与学习经验分享。  ");

        assertEquals("专注技术内容创作与学习经验分享。", result.getBio());
        assertNotNull(result.getUpdateTime());
        verify(mapper).updateById(existing);
    }

    @Test
    void rejectsBioThatIsTooShortAfterTrimming() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateBio("         简介"));

        assertEquals(400, exception.getCode());
    }
}
