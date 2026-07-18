package com.codenow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.entity.AuthorProfile;

public interface AuthorProfileService extends IService<AuthorProfile> {
    AuthorProfile getByUserId(Long userId);
}
