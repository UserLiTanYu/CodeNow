package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.AuthorProfile;
import com.codenow.mapper.AuthorProfileMapper;
import com.codenow.service.AuthorProfileService;
import org.springframework.stereotype.Service;

@Service
public class AuthorProfileServiceImpl extends ServiceImpl<AuthorProfileMapper, AuthorProfile> implements AuthorProfileService {
    @Override
    public AuthorProfile getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<AuthorProfile>().eq(AuthorProfile::getUserId, userId));
    }
}
