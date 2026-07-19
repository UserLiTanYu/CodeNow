package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.dto.ArticleVO;
import com.codenow.dto.ArticleAuthorVO;
import com.codenow.dto.PublicAuthorRow;
import com.codenow.common.ArticleStatus;
import com.codenow.exception.BusinessException;
import com.codenow.entity.BlogArticle;
import com.codenow.entity.BlogArticleTag;
import com.codenow.entity.BlogCategory;
import com.codenow.entity.BlogTag;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.mapper.BlogArticleTagMapper;
import com.codenow.mapper.BlogCategoryMapper;
import com.codenow.mapper.BlogTagMapper;
import com.codenow.mapper.PublicAuthorMapper;
import com.codenow.service.BlogArticleService;
import com.codenow.service.HotArticleService;
import com.codenow.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogArticleServiceImpl extends ServiceImpl<BlogArticleMapper, BlogArticle> implements BlogArticleService {

    private final BlogArticleTagMapper articleTagMapper;
    private final BlogCategoryMapper categoryMapper;
    private final BlogTagMapper tagMapper;
    private final HotArticleService hotArticleService;
    private final StorageService storageService;
    private final PublicAuthorMapper publicAuthorMapper;

    @Override
    @Transactional
    public void saveArticleWithTags(BlogArticle article, List<Long> tagIds) {
        // 1. 保存文章
        save(article);
        // 2. 保存文章-标签关联
        saveArticleTagRelations(article.getId(), tagIds);
    }

    @Override
    @Transactional
    public void updateArticleWithTags(BlogArticle article, List<Long> tagIds) {
        // 1. 更新文章
        updateById(article);
        // 2. 删除旧的标签关联
        articleTagMapper.delete(
                new LambdaQueryWrapper<BlogArticleTag>().eq(BlogArticleTag::getArticleId, article.getId()));
        // 3. 保存新的标签关联
        saveArticleTagRelations(article.getId(), tagIds);
    }

    @Override
    @Transactional
    public void deleteArticleWithTags(Long id) {
        // 仅逻辑删除文章，保留标签关联，后续恢复文章时不会丢失标签。
        removeById(id);
    }

    @Override
    public boolean toggleStatus(Long id) {
        return baseMapper.toggleStatus(id) == 1;
    }

    @Override
    public boolean toggleTop(Long id) {
        return baseMapper.toggleTop(id) == 1;
    }

    @Override
    public ArticleVO getArticleVOById(Long id) {
        BlogArticle article = getById(id);
        if (article == null) {
            return null;
        }
        List<ArticleVO> voList = buildArticleVOBatch(List.of(article));
        return voList.isEmpty() ? null : voList.get(0);
    }

    @Override
    public Page<ArticleVO> pageArticleVO(Integer pageNum, Integer pageSize, Long categoryId, Long tagId) {
        // 如果按标签筛选，先查出该标签下的文章 ID 列表
        List<Long> articleIds = null;
        if (tagId != null) {
            List<BlogArticleTag> relations = articleTagMapper.selectList(
                    new LambdaQueryWrapper<BlogArticleTag>().eq(BlogArticleTag::getTagId, tagId));
            articleIds = relations.stream().map(BlogArticleTag::getArticleId).collect(Collectors.toList());
            if (articleIds.isEmpty()) {
                // 该标签下没有文章，直接返回空页
                return new Page<>(pageNum, pageSize);
            }
        }

        // 构建查询条件
        List<Long> categoryIds = categoryId == null ? Collections.emptyList() : selfAndDescendantCategoryIds(categoryId);
        LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<BlogArticle>()
                .in(!categoryIds.isEmpty(), BlogArticle::getCategoryId, categoryIds)
                .in(articleIds != null && !articleIds.isEmpty(), BlogArticle::getId, articleIds)
                .orderByDesc(BlogArticle::getIsTop)
                .orderByAsc(BlogArticle::getSort)
                .orderByDesc(BlogArticle::getCreateTime);

        // 分页查询文章
        Page<BlogArticle> articlePage = page(new Page<>(pageNum, pageSize), wrapper);

        // 批量转换为 VO（避免 N+1 查询）
        Page<ArticleVO> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        voPage.setRecords(buildArticleVOBatch(articlePage.getRecords()));
        return voPage;
    }

    @Override
    public Page<ArticleVO> pageAuthorArticleVO(Integer pageNum, Integer pageSize, Long categoryId, Long tagId,
                                               Long currentUserId, boolean admin) {
        validatePage(pageNum, pageSize);
        List<Long> categoryIds = categoryId == null ? Collections.emptyList() : selfAndDescendantCategoryIds(categoryId);
        Page<BlogArticle> articlePage = baseMapper.selectAuthorArticlePage(
                new Page<>(pageNum, pageSize), categoryIds, tagId, currentUserId, admin);
        Page<ArticleVO> result = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        result.setRecords(buildArticleVOBatch(articlePage.getRecords()));
        return result;
    }

    @Override
    public ArticleVO getAuthorArticleVOById(Long id, Long currentUserId, boolean admin) {
        BlogArticle article = requireAccessibleArticle(id, currentUserId, admin);
        return buildArticleVOBatch(List.of(article)).get(0);
    }

    @Override
    @Transactional
    public void saveAuthorArticleWithTags(BlogArticle article, List<Long> tagIds, Long currentUserId) {
        normalizeAuthorArticle(article);
        validateCategoryAndTagOwnership(article.getCategoryId(), tagIds, currentUserId);
        article.setAuthorId(currentUserId);
        article.setIsTop(0);
        saveArticleWithTags(article, tagIds);
    }

    @Override
    @Transactional
    public void updateAuthorArticleWithTags(BlogArticle article, List<Long> tagIds,
                                            Long currentUserId, boolean admin) {
        normalizeAuthorArticle(article);
        validateCategoryAndTagOwnership(article.getCategoryId(), tagIds, currentUserId);
        article.setAuthorId(null);
        article.setIsTop(null);
        if (baseMapper.updateAuthorArticle(article, currentUserId, admin) != 1) {
            throwMutationFailure(article.getId(), currentUserId, admin);
        }
        articleTagMapper.delete(new LambdaQueryWrapper<BlogArticleTag>()
                .eq(BlogArticleTag::getArticleId, article.getId()));
        saveArticleTagRelations(article.getId(), tagIds);
    }

    @Override
    @Transactional
    public void deleteAuthorArticleWithTags(Long id, Long currentUserId, boolean admin) {
        if (baseMapper.deleteAuthorArticle(id, currentUserId, admin) != 1) {
            throwMutationFailure(id, currentUserId, admin);
        }
    }

    @Override
    public void toggleAuthorStatus(Long id, Long currentUserId, boolean admin) {
        if (baseMapper.toggleAuthorStatus(id, currentUserId, admin) != 1) {
            throwMutationFailure(id, currentUserId, admin);
        }
    }

    private BlogArticle requireAccessibleArticle(Long id, Long currentUserId, boolean admin) {
        BlogArticle article = getById(id);
        if (article == null) throw new BusinessException(404, "文章不存在");
        if (!admin && !Objects.equals(article.getAuthorId(), currentUserId)) {
            throw new BusinessException(403, "无权操作该文章");
        }
        return article;
    }

    private void throwMutationFailure(Long id, Long currentUserId, boolean admin) {
        BlogArticle article = getById(id);
        if (article == null) throw new BusinessException(404, "文章不存在");
        if (!admin && !Objects.equals(article.getAuthorId(), currentUserId)) {
            throw new BusinessException(403, "无权操作该文章");
        }
        throw new BusinessException(409, "文章状态已发生变化，请刷新后重试");
    }

    private void validatePage(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1 || pageSize == null || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(400, "分页参数不合法");
        }
    }

    private void normalizeAuthorArticle(BlogArticle article) {
        String coverImage = article.getCoverImage();
        if (coverImage != null) {
            coverImage = coverImage.trim();
            if (coverImage.isEmpty()) {
                coverImage = null;
            } else if (coverImage.length() > 255 || !storageService.isManagedUrl(coverImage)) {
                throw new BusinessException(400, "封面图片地址不合法");
            }
        }
        article.setCoverImage(coverImage);
        if (article.getStatus() == null) article.setStatus(ArticleStatus.DRAFT);
        if (!Objects.equals(article.getStatus(), ArticleStatus.DRAFT)
                && !Objects.equals(article.getStatus(), ArticleStatus.PUBLISHED)) {
            throw new BusinessException(400, "文章状态不合法");
        }
        if (article.getSort() == null) article.setSort(0);
        if (article.getSort() < 0 || article.getSort() > 9999) {
            throw new BusinessException(400, "学习顺序必须在 0 到 9999 之间");
        }
    }

    /**
     * 验证分类和标签是否属于当前作者
     */
    private void validateCategoryAndTagOwnership(Long categoryId, List<Long> tagIds, Long currentUserId) {
        if (categoryId != null) {
            BlogCategory category = categoryMapper.selectById(categoryId);
            if (category == null) {
                throw new BusinessException(400, "分类不存在");
            }
            if (!Objects.equals(category.getAuthorId(), currentUserId)) {
                throw new BusinessException(400, "只能选择自己创建的分类");
            }
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            List<Long> distinctTagIds = tagIds.stream().filter(Objects::nonNull).distinct().toList();
            if (!distinctTagIds.isEmpty()) {
                List<BlogTag> tags = tagMapper.selectBatchIds(distinctTagIds);
                if (tags.size() != distinctTagIds.size()) {
                    throw new BusinessException(400, "包含不存在的标签 ID");
                }
                for (BlogTag tag : tags) {
                    if (!Objects.equals(tag.getCreatedBy(), currentUserId)) {
                        throw new BusinessException(400, "只能选择自己创建的标签");
                    }
                }
            }
        }
    }

    @Override
    public Page<ArticleVO> pagePublishedArticles(Integer pageNum, Integer pageSize, Long categoryId, Long tagId,
                                                 String keyword, String sort) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        if (normalizedKeyword != null && normalizedKeyword.length() > 100) {
            throw new BusinessException(400, "搜索关键词不能超过 100 个字符");
        }
        if (normalizedKeyword != null && normalizedKeyword.isEmpty()) {
            normalizedKeyword = null;
        }
        String normalizedSort = sort == null || sort.isBlank() ? "learning" : sort.trim();
        if (!"learning".equals(normalizedSort) && !"latest".equals(normalizedSort)
                && !"mostViewed".equals(normalizedSort)) {
            throw new BusinessException(400, "不支持的文章排序方式");
        }

        List<Long> categoryIds = categoryId == null ? Collections.emptyList() : selfAndDescendantCategoryIds(categoryId);

        Page<BlogArticle> articlePage = baseMapper.selectPublishedArticlePage(
                new Page<>(pageNum, pageSize), categoryIds, tagId, normalizedKeyword, normalizedSort);

        Page<ArticleVO> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        voPage.setRecords(buildArticleVOBatch(articlePage.getRecords()));
        return voPage;
    }

    @Override
    @Transactional
    public ArticleVO getPublishedArticleById(Long id) {
        BlogArticle article = getById(id);
        if (article == null || !Objects.equals(article.getStatus(), ArticleStatus.PUBLISHED)) {
            return null;
        }
        // 浏览量 +1（原子更新后重新读取真实值，避免并发竞态）
        lambdaUpdate().eq(BlogArticle::getId, id)
                .setSql("view_count = view_count + 1")
                .update();
        // 重新读取数据库中的真实浏览量，推送到 Redis
        int actualViewCount = getById(id).getViewCount();
        article.setViewCount(actualViewCount);
        hotArticleService.incrementViewCount(id, actualViewCount);
        List<ArticleVO> voList = buildArticleVOBatch(List.of(article));
        return voList.isEmpty() ? null : voList.get(0);
    }

    /**
     * 保存文章-标签关联记录
     */
    private void saveArticleTagRelations(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<Long> distinctTagIds = tagIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctTagIds.isEmpty()) {
            return;
        }
        List<BlogTag> existingTags = tagMapper.selectBatchIds(distinctTagIds);
        if (existingTags.size() != distinctTagIds.size()) {
            throw new BusinessException(400, "包含不存在的标签 ID");
        }
        for (Long tagId : distinctTagIds) {
            BlogArticleTag relation = new BlogArticleTag();
            relation.setArticleId(articleId);
            relation.setTagId(tagId);
            articleTagMapper.insert(relation);
        }
    }

    /**
     * 批量构建 ArticleVO（避免 N+1 查询：分类和标签一次性批量加载）
     */
    @Override
    public List<ArticleVO> buildArticleVOBatch(List<BlogArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> articleIds = articles.stream().map(BlogArticle::getId).collect(Collectors.toList());

        // 1. 批量查询分类
        Set<Long> categoryIds = articles.stream()
                .map(BlogArticle::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = Collections.emptyMap();
        if (!categoryIds.isEmpty()) {
            List<BlogCategory> categories = categoryMapper.selectList(null);
            Map<Long, BlogCategory> byId = categories.stream()
                    .collect(Collectors.toMap(BlogCategory::getId, item -> item, (a, b) -> a));
            Map<Long, String> paths = new HashMap<>();
            for (Long id : categoryIds) {
                String path = categoryPath(byId.get(id), byId);
                if (path != null) paths.put(id, path);
            }
            categoryNameMap = paths;
        }

        // 2. 批量查询文章-标签关联
        List<BlogArticleTag> allRelations = articleTagMapper.selectList(
                new LambdaQueryWrapper<BlogArticleTag>().in(BlogArticleTag::getArticleId, articleIds));

        // 3. 批量查询标签
        Set<Long> tagIds = allRelations.stream().map(BlogArticleTag::getTagId).collect(Collectors.toSet());
        Map<Long, BlogTag> tagMap = Collections.emptyMap();
        if (!tagIds.isEmpty()) {
            List<BlogTag> tags = tagMapper.selectBatchIds(tagIds);
            tagMap = tags.stream().collect(Collectors.toMap(BlogTag::getId, t -> t, (a, b) -> a));
        }

        // 4. 批量查询仍可公开发现的作者，避免返回 SysUser 私有字段或产生 N+1。
        Set<Long> authorIds = articles.stream()
                .map(BlogArticle::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, PublicAuthorRow> authorMap = Collections.emptyMap();
        if (!authorIds.isEmpty()) {
            authorMap = publicAuthorMapper.selectPublicAuthorSummariesByUserIds(authorIds).stream()
                    .collect(Collectors.toMap(PublicAuthorRow::getUserId, item -> item, (a, b) -> a));
        }

        // 5. 按 articleId 分组标签
        Map<Long, List<Long>> articleTagMap = allRelations.stream()
                .collect(Collectors.groupingBy(BlogArticleTag::getArticleId,
                        Collectors.mapping(BlogArticleTag::getTagId, Collectors.toList())));

        // 6. 组装 VO
        Map<Long, String> finalCategoryNameMap = categoryNameMap;
        Map<Long, BlogTag> finalTagMap = tagMap;
        Map<Long, PublicAuthorRow> finalAuthorMap = authorMap;
        return articles.stream().map(article -> {
            ArticleVO vo = new ArticleVO();
            vo.setArticle(article);
            vo.setCategoryName(finalCategoryNameMap.get(article.getCategoryId()));
            List<Long> articleTagIds = articleTagMap.getOrDefault(article.getId(), Collections.emptyList());
            vo.setTags(articleTagIds.stream()
                    .map(finalTagMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
            PublicAuthorRow author = finalAuthorMap.get(article.getAuthorId());
            if (author != null) {
                ArticleAuthorVO summary = new ArticleAuthorVO();
                summary.setUserId(author.getUserId());
                summary.setDisplayName(author.getDisplayName());
                summary.setAvatar(author.getAvatar());
                vo.setAuthor(summary);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private List<Long> selfAndDescendantCategoryIds(Long categoryId) {
        List<BlogCategory> categories = categoryMapper.selectList(null);
        Map<Long, List<Long>> children = new HashMap<>();
        for (BlogCategory category : categories) {
            Long parentId = category.getParentId() == null ? 0L : category.getParentId();
            children.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(category.getId());
        }
        List<Long> result = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(categoryId);
        while (!queue.isEmpty()) {
            Long current = queue.removeFirst();
            if (result.contains(current)) continue;
            result.add(current);
            queue.addAll(children.getOrDefault(current, Collections.emptyList()));
        }
        return result;
    }

    private String categoryPath(BlogCategory category, Map<Long, BlogCategory> byId) {
        if (category == null) return null;
        List<String> names = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        BlogCategory current = category;
        while (current != null && visited.add(current.getId())) {
            names.add(current.getName());
            Long parentId = current.getParentId() == null ? 0L : current.getParentId();
            current = parentId == 0L ? null : byId.get(parentId);
        }
        Collections.reverse(names);
        return String.join(" / ", names);
    }
}
