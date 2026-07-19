package com.codenow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.dto.ArticleVO;
import com.codenow.dto.PublicAuthorVO;

public interface PublicAuthorService {
    Page<PublicAuthorVO> pagePublicAuthors(Integer pageNum, Integer pageSize, String keyword, String sort);

    PublicAuthorVO getPublicAuthor(Long userId);

    Page<ArticleVO> pagePublicAuthorArticles(Long userId, Integer pageNum, Integer pageSize, String sort);
}
