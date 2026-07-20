package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.dto.ArticleVO;
import com.codenow.dto.PublicAuthorRow;
import com.codenow.dto.PublicAuthorVO;
import com.codenow.entity.BlogArticle;
import com.codenow.entity.BlogCategory;
import com.codenow.entity.BlogTag;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.mapper.BlogCategoryMapper;
import com.codenow.mapper.BlogTagMapper;
import com.codenow.mapper.PublicAuthorMapper;
import com.codenow.service.BlogArticleService;
import com.codenow.service.PublicAuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 公开作者查询服务。只暴露符合公开条件的作者，并校验文章筛选条件确实属于目标作者。
 */
@Service
@RequiredArgsConstructor
public class PublicAuthorServiceImpl implements PublicAuthorService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final PublicAuthorMapper mapper;
    private final BlogArticleMapper articleMapper;
    private final BlogCategoryMapper categoryMapper;
    private final BlogTagMapper tagMapper;
    private final BlogArticleService articleService;

    /**
     * 分页查询公开作者列表。
     * 只暴露符合公开条件的作者，支持关键词搜索和多种排序方式。
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词（可选）
     * @param sort     排序方式：popular（默认）、latest、articles
     * @return 分页结果
     */
    @Override
    public Page<PublicAuthorVO> pagePublicAuthors(Integer pageNum, Integer pageSize, String keyword, String sort) {
        validatePage(pageNum, pageSize);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedSort = normalizeSort(sort);
        Page<PublicAuthorRow> source = mapper.selectPublicAuthorPage(
                new Page<>(pageNum, pageSize), normalizedKeyword, normalizedSort);
        Page<PublicAuthorVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    /**
     * 查询公开作者详情
     *
     * @param userId 作者用户 ID
     * @return 公开作者视图对象
     * @throws BusinessException 当作者不存在时抛出
     */
    @Override
    public PublicAuthorVO getPublicAuthor(Long userId) {
        if (userId == null || userId < 1) {
            throw new BusinessException(404, "作者不存在");
        }
        PublicAuthorRow row = mapper.selectPublicAuthorByUserId(userId);
        if (row == null) {
            throw new BusinessException(404, "作者不存在");
        }
        return toVO(row);
    }

    /**
     * 分页查询公开作者的已发布文章。
     * 先校验作者公开身份，再验证分类和标签归属权，最后查询文章列表。
     *
     * @param userId     作者用户 ID
     * @param pageNum    页码
     * @param pageSize   每页大小
     * @param sort       排序方式：latest（默认）、mostViewed
     * @param categoryId 分类 ID（可选，需属于该作者）
     * @param tagId      标签 ID（可选，需属于该作者）
     * @return 分页结果
     */
    @Override
    public Page<ArticleVO> pagePublicAuthorArticles(Long userId, Integer pageNum, Integer pageSize, String sort, Long categoryId, Long tagId) {
        validatePage(pageNum, pageSize);
        String normalizedSort = normalizeArticleSort(sort);
        // This check deliberately happens before the article query so revoked, banned,
        // deleted or incomplete authors cannot be enumerated through this endpoint.
        getPublicAuthor(userId);

        // Validate categoryId belongs to this author
        if (categoryId != null) {
            BlogCategory category = categoryMapper.selectById(categoryId);
            if (category == null || !Objects.equals(category.getAuthorId(), userId)) {
                throw new BusinessException(400, "分类不存在或不属于该作者");
            }
        }

        // Validate tagId belongs to this author
        if (tagId != null) {
            BlogTag tag = tagMapper.selectById(tagId);
            if (tag == null || !Objects.equals(tag.getCreatedBy(), userId)) {
                throw new BusinessException(400, "标签不存在或不属于该作者");
            }
        }

        Page<BlogArticle> source = articleMapper.selectPublishedAuthorArticlePage(
                new Page<>(pageNum, pageSize), userId, normalizedSort, categoryId, tagId);
        Page<ArticleVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(articleService.buildArticleVOBatch(source.getRecords()));
        return result;
    }

    private void validatePage(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1 || pageSize == null || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(400, "分页参数不合法");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String normalized = keyword.trim();
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new BusinessException(400, "搜索关键词不能超过 100 个字符");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeSort(String sort) {
        String normalized = sort == null || sort.isBlank() ? "popular" : sort.trim();
        if (!List.of("popular", "latest", "articles").contains(normalized)) {
            throw new BusinessException(400, "不支持的作者排序方式");
        }
        return normalized;
    }

    private String normalizeArticleSort(String sort) {
        String normalized = sort == null || sort.isBlank() ? "latest" : sort.trim();
        if (!List.of("latest", "mostViewed").contains(normalized)) {
            throw new BusinessException(400, "不支持的作者文章排序方式");
        }
        return normalized;
    }

    private PublicAuthorVO toVO(PublicAuthorRow row) {
        PublicAuthorVO vo = new PublicAuthorVO();
        vo.setUserId(row.getUserId());
        vo.setDisplayName(row.getDisplayName());
        vo.setAvatar(row.getAvatar());
        vo.setBio(row.getBio());
        vo.setExpertise(splitExpertise(row.getExpertise()));
        // 历史资料也可能包含旧脏值，公开投影仅返回可解析的 HTTP/HTTPS 外链。
        vo.setWebsiteUrl(safeExternalUrl(row.getWebsiteUrl()));
        vo.setPortfolioUrl(safeExternalUrl(row.getPortfolioUrl()));
        vo.setArticleCount(row.getArticleCount() == null ? 0L : row.getArticleCount());
        vo.setTotalViews(row.getTotalViews() == null ? 0L : row.getTotalViews());
        vo.setLastPublishedAt(row.getLastPublishedAt());
        return vo;
    }

    private List<String> splitExpertise(String expertise) {
        if (expertise == null || expertise.isBlank()) return List.of();
        return List.copyOf(Arrays.stream(expertise.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
    }

    private String safeExternalUrl(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null) {
                return uri.toASCIIString();
            }
        } catch (IllegalArgumentException ignored) {
            // Historic invalid profile values are omitted from the public projection.
        }
        return null;
    }
}
