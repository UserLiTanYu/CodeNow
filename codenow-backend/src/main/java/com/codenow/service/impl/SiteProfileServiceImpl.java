package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.SiteProfile;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.SiteProfileMapper;
import com.codenow.service.SiteProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SiteProfileServiceImpl extends ServiceImpl<SiteProfileMapper, SiteProfile>
        implements SiteProfileService {

    public static final int PROFILE_ID = 1;
    public static final String DEFAULT_BIO =
            "一个支持 Markdown 写作的个人技术博客，帮助开发者记录和分享学习笔记。";

    @Override
    public SiteProfile getSiteProfile() {
        SiteProfile profile = getById(PROFILE_ID);
        if (profile != null) return profile;
        SiteProfile fallback = new SiteProfile();
        fallback.setId(PROFILE_ID);
        fallback.setBio(DEFAULT_BIO);
        return fallback;
    }

    @Override
    @Transactional
    public SiteProfile updateBio(String bio) {
        String normalizedBio = bio == null ? "" : bio.trim();
        if (normalizedBio.length() < 10 || normalizedBio.length() > 500) {
            throw new BusinessException(400, "个人简介长度应为 10-500 个字符");
        }
        SiteProfile profile = getById(PROFILE_ID);
        boolean creating = profile == null;
        if (creating) {
            profile = new SiteProfile();
            profile.setId(PROFILE_ID);
        }
        profile.setBio(normalizedBio);
        profile.setUpdateTime(LocalDateTime.now());
        boolean saved = creating ? save(profile) : updateById(profile);
        if (!saved) throw new BusinessException(409, "个人简介保存失败，请刷新后重试");
        return profile;
    }
}
