package com.codenow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.entity.SiteProfile;

public interface SiteProfileService extends IService<SiteProfile> {
    SiteProfile getSiteProfile();

    SiteProfile updateBio(String bio);
}
