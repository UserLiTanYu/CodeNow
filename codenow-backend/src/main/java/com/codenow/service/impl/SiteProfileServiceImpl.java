package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.dto.SiteProfileUpdateDTO;
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
    public static final String DEFAULT_SITE_NAME = "码上记";
    public static final String DEFAULT_SLOGAN = "记录实践，分享知识";
    public static final String DEFAULT_DESCRIPTION =
            "码上记是一个面向开发者的技术学习与知识分享平台，专注于系统化教程、开发实践和项目经验沉淀。";
    public static final String DEFAULT_BIO =
            "一个支持 Markdown 写作的个人技术博客，帮助开发者记录和分享学习笔记。";
    public static final String DEFAULT_ABOUT_CONTENT =
            "我们整理可靠的技术知识、真实的开发经验和可复用的解决方案，帮助学习过程更清晰、更连贯。";

    @Override
    public SiteProfile getSiteProfile() {
        SiteProfile profile = getById(PROFILE_ID);
        if (profile != null) return profile;
        SiteProfile fallback = new SiteProfile();
        fallback.setId(PROFILE_ID);
        fallback.setSiteName(DEFAULT_SITE_NAME);
        fallback.setSlogan(DEFAULT_SLOGAN);
        fallback.setDescription(DEFAULT_DESCRIPTION);
        fallback.setBio(DEFAULT_BIO);
        fallback.setAboutContent(DEFAULT_ABOUT_CONTENT);
        return fallback;
    }

    @Override
    @Transactional
    public SiteProfile updateProfile(SiteProfileUpdateDTO dto) {
        if (dto == null) throw new BusinessException(400, "站点资料不能为空");
        SiteProfile profile = getById(PROFILE_ID);
        boolean creating = profile == null;
        if (creating) {
            profile = new SiteProfile();
            profile.setId(PROFILE_ID);
        }
        profile.setSiteName(normalizeRequired(dto.getSiteName(), 2, 50, "站点名称"));
        profile.setSlogan(normalizeRequired(dto.getSlogan(), 2, 100, "站点标语"));
        profile.setDescription(normalizeRequired(dto.getDescription(), 10, 500, "关于页简介"));
        profile.setBio(normalizeRequired(dto.getBio(), 10, 500, "个人简介"));
        profile.setAboutContent(normalizeRequired(dto.getAboutContent(), 20, 5000, "关于页正文"));
        profile.setContactEmail(normalizeOptional(dto.getContactEmail()));
        profile.setGithubUrl(normalizeOptional(dto.getGithubUrl()));
        profile.setFoundedAt(dto.getFoundedAt());
        profile.setUpdateTime(LocalDateTime.now());
        boolean saved = creating ? save(profile) : updateById(profile);
        if (!saved) throw new BusinessException(409, "站点资料保存失败，请刷新后重试");
        return profile;
    }

    private String normalizeRequired(String value, int min, int max, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() < min || normalized.length() > max) {
            throw new BusinessException(400, fieldName + "长度应为 " + min + "-" + max + " 个字符");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
