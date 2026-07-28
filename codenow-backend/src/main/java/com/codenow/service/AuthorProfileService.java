package com.codenow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.dto.AuthorProfileUpdateDTO;
import com.codenow.entity.AuthorProfile;

/**
 * 作者资料服务接口
 */
public interface AuthorProfileService extends IService<AuthorProfile> {

    /**
     * 根据用户ID获取作者资料
     *
     * @param userId 用户ID
     * @return 作者资料，不存在时返回 null
     */
    AuthorProfile getByUserId(Long userId);

    /** 更新指定作者自己的公开资料。 */
    AuthorProfile updateAuthorProfile(Long userId, AuthorProfileUpdateDTO dto);
}
