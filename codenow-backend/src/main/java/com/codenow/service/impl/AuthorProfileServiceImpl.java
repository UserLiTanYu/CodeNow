package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.AuthorProfile;
import com.codenow.mapper.AuthorProfileMapper;
import com.codenow.service.AuthorProfileService;
import org.springframework.stereotype.Service;

/**
 * 作者资料服务实现类。
 * 管理作者的个人资料信息，包括简介、擅长领域、个人网站等。
 */
@Service
public class AuthorProfileServiceImpl extends ServiceImpl<AuthorProfileMapper, AuthorProfile> implements AuthorProfileService {
    /**
     * 根据用户 ID 查询作者资料
     *
     * @param userId 用户 ID
     * @return 作者资料实体，不存在时返回 null
     */
    @Override
    public AuthorProfile getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<AuthorProfile>().eq(AuthorProfile::getUserId, userId));
    }
}
