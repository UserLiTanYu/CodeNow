package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.dto.ArticleVO;
import com.codenow.dto.PublicAuthorRow;
import com.codenow.dto.PublicAuthorVO;
import com.codenow.entity.BlogArticle;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.mapper.PublicAuthorMapper;
import com.codenow.service.BlogArticleService;
import com.codenow.service.PublicAuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicAuthorServiceImpl implements PublicAuthorService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final PublicAuthorMapper mapper;
    private final BlogArticleMapper articleMapper;
    private final BlogArticleService articleService;

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

    @Override
    public Page<ArticleVO> pagePublicAuthorArticles(Long userId, Integer pageNum, Integer pageSize, String sort) {
        validatePage(pageNum, pageSize);
        String normalizedSort = normalizeArticleSort(sort);
        // This check deliberately happens before the article query so revoked, banned,
        // deleted or incomplete authors cannot be enumerated through this endpoint.
        getPublicAuthor(userId);
        Page<BlogArticle> source = articleMapper.selectPublishedAuthorArticlePage(
                new Page<>(pageNum, pageSize), userId, normalizedSort);
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
