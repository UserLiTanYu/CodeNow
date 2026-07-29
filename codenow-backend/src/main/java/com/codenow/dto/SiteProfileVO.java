package com.codenow.dto;

import com.codenow.entity.SiteProfile;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 站点公开资料，避免公开接口直接暴露持久化实体。 */
@Data
public class SiteProfileVO {
    private String siteName;
    private String slogan;
    private String description;
    private String bio;
    private String aboutContent;
    private String contactEmail;
    private String githubUrl;
    private LocalDate foundedAt;
    private LocalDateTime updateTime;

    public static SiteProfileVO from(SiteProfile profile) {
        SiteProfileVO result = new SiteProfileVO();
        result.setSiteName(profile.getSiteName());
        result.setSlogan(profile.getSlogan());
        result.setDescription(profile.getDescription());
        result.setBio(profile.getBio());
        result.setAboutContent(profile.getAboutContent());
        result.setContactEmail(profile.getContactEmail());
        result.setGithubUrl(profile.getGithubUrl());
        result.setFoundedAt(profile.getFoundedAt());
        result.setUpdateTime(profile.getUpdateTime());
        return result;
    }
}
