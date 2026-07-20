package com.codenow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.dto.ArticleVO;
import com.codenow.entity.BlogArticle;

import java.util.List;

/**
 * 博客文章服务接口
 */
public interface BlogArticleService extends IService<BlogArticle> {

    /**
     * 保存文章及其标签关联
     *
     * @param article 文章实体
     * @param tagIds  标签ID列表
     */
    void saveArticleWithTags(BlogArticle article, List<Long> tagIds);

    /**
     * 更新文章及其标签关联
     *
     * @param article 文章实体
     * @param tagIds  标签ID列表
     */
    void updateArticleWithTags(BlogArticle article, List<Long> tagIds);

    /**
     * 逻辑删除文章并保留标签关联，避免恢复文章时丢失标签
     *
     * @param id 文章ID
     */
    void deleteArticleWithTags(Long id);

    /**
     * 原子切换文章发布状态。
     *
     * @param id 文章ID
     * @return 文章存在且切换成功时为 true
     */
    boolean toggleStatus(Long id);

    /**
     * 原子切换文章置顶状态。
     *
     * @param id 文章ID
     * @return 文章存在且切换成功时为 true
     */
    boolean toggleTop(Long id);

    /**
     * 根据 ID 查询文章详情（含分类名称和标签列表）
     *
     * @param id 文章ID
     * @return 文章详情视图对象
     */
    ArticleVO getArticleVOById(Long id);

    /**
     * 分页查询文章列表（含分类名称和标签列表，支持按分类/标签筛选）
     *
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @param categoryId 分类ID（可选）
     * @param tagId      标签ID（可选）
     * @return 分页结果
     */
    Page<ArticleVO> pageArticleVO(Integer pageNum, Integer pageSize, Long categoryId, Long tagId);

    /**
     * 分页查询作者文章列表（仅限当前作者的文章，管理员可查看全部）
     *
     * @param pageNum      页码
     * @param pageSize     每页数量
     * @param categoryId   分类ID（可选）
     * @param tagId        标签ID（可选）
     * @param currentUserId 当前用户ID
     * @param admin        是否为管理员
     * @return 分页结果
     */
    Page<ArticleVO> pageAuthorArticleVO(Integer pageNum, Integer pageSize, Long categoryId, Long tagId,
                                        Long currentUserId, boolean admin);

    /**
     * 查询作者文章详情（仅限当前作者的文章，管理员可查看全部）
     *
     * @param id            文章ID
     * @param currentUserId 当前用户ID
     * @param admin         是否为管理员
     * @return 文章详情视图对象
     */
    ArticleVO getAuthorArticleVOById(Long id, Long currentUserId, boolean admin);

    /**
     * 保存作者文章及其标签关联（作者只能操作自己的文章）
     *
     * @param article       文章实体
     * @param tagIds        标签ID列表
     * @param currentUserId 当前用户ID
     */
    void saveAuthorArticleWithTags(BlogArticle article, List<Long> tagIds, Long currentUserId);

    /**
     * 更新作者文章及其标签关联（作者只能操作自己的文章，管理员可操作全部）
     *
     * @param article       文章实体
     * @param tagIds        标签ID列表
     * @param currentUserId 当前用户ID
     * @param admin         是否为管理员
     */
    void updateAuthorArticleWithTags(BlogArticle article, List<Long> tagIds, Long currentUserId, boolean admin);

    /**
     * 删除作者文章（作者只能删除自己的文章，管理员可删除全部）
     *
     * @param id            文章ID
     * @param currentUserId 当前用户ID
     * @param admin         是否为管理员
     */
    void deleteAuthorArticleWithTags(Long id, Long currentUserId, boolean admin);

    /**
     * 切换作者文章发布状态
     *
     * @param id            文章ID
     * @param currentUserId 当前用户ID
     * @param admin         是否为管理员
     */
    void toggleAuthorStatus(Long id, Long currentUserId, boolean admin);

    /**
     * 分页查询已发布文章（用户端，仅 status=1）
     *
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @param categoryId 分类ID（可选）
     * @param tagId      标签ID（可选）
     * @param keyword    搜索关键词（可选）
     * @param sort       排序方式（可选）
     * @return 分页结果
     */
    Page<ArticleVO> pagePublishedArticles(Integer pageNum, Integer pageSize, Long categoryId, Long tagId,
                                          String keyword, String sort);

    /**
     * 查询已发布文章详情（用户端），同时浏览量 +1
     *
     * @param id 文章ID
     * @return 文章详情视图对象
     */
    ArticleVO getPublishedArticleById(Long id);

    /**
     * 批量构建 ArticleVO（避免 N+1 查询）
     *
     * @param articles 文章实体列表
     * @return 文章详情视图对象列表
     */
    List<ArticleVO> buildArticleVOBatch(List<BlogArticle> articles);
}
