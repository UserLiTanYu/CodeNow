package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.dto.AuthorProfileUpdateDTO;
import com.codenow.entity.AuthorProfile;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.AuthorProfileMapper;
import com.codenow.service.AuthorProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;

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

    @Override
    @Transactional
    public AuthorProfile updateAuthorProfile(Long userId, AuthorProfileUpdateDTO dto) {
        AuthorProfile profile = getByUserId(userId);
        if (profile == null) throw new BusinessException(404, "作者资料不存在");

        String bio = dto.getBio() == null ? "" : dto.getBio().trim();
        if (bio.length() < 20 || bio.length() > 500) {
            throw new BusinessException(400, "个人简介长度应为 20-500 个字符");
        }
        profile.setBio(bio);
        profile.setExpertise(normalizeExpertise(dto));
        profile.setWebsiteUrl(trimToNull(dto.getWebsiteUrl()));
        profile.setPortfolioUrl(trimToNull(dto.getPortfolioUrl()));
        profile.setUpdateTime(LocalDateTime.now());
        if (!updateById(profile)) {
            throw new BusinessException(409, "作者资料状态已发生变化，请刷新后重试");
        }
        return profile;
    }

    private String normalizeExpertise(AuthorProfileUpdateDTO dto) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (dto.getExpertise() == null) throw new BusinessException(400, "请至少填写一个擅长领域");
        dto.getExpertise().stream().map(String::trim).filter(value -> !value.isEmpty()).forEach(values::add);
        if (values.isEmpty()) throw new BusinessException(400, "请至少填写一个擅长领域");
        return String.join(",", values);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
