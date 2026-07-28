package com.codenow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.dto.ArticleVO;
import com.codenow.dto.PublicAuthorVO;

/**
 * 公开作者服务接口，提供用户端作者信息查询
 */
public interface PublicAuthorService {

    /**
     * 分页查询公开作者列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param keyword  搜索关键词（可选）
     * @param sort     排序方式（可选）
     * @return 分页结果
     */
    Page<PublicAuthorVO> pagePublicAuthors(Integer pageNum, Integer pageSize, String keyword, String sort);

    /**
     * 查询公开作者详情
     *
     * @param userId 用户ID
     * @return 作者视图对象
     */
    PublicAuthorVO getPublicAuthor(Long userId);

    /**
     * 分页查询公开作者的已发布文章
     *
     * @param userId     用户ID
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @param sort       排序方式（可选）
     * @param categoryId 分类ID（可选）
     * @param tagId      标签ID（可选）
     * @param keyword    搜索关键词（可选）
     * @return 分页结果
     */
    Page<ArticleVO> pagePublicAuthorArticles(
            Long userId, Integer pageNum, Integer pageSize, String sort,
            Long categoryId, Long tagId, String keyword);
}
