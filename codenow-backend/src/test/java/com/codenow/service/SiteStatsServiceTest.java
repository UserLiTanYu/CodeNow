package com.codenow.service;

import com.codenow.dto.SiteStatsVO;
import com.codenow.mapper.SiteStatsMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SiteStatsServiceTest {
    @Test
    void cachesPublicStatsWithinTheTtl() {
        SiteStatsMapper mapper = mock(SiteStatsMapper.class);
        SiteStatsVO stats = new SiteStatsVO();
        stats.setArticleCount(150L);
        stats.setAuthorCount(8L);
        when(mapper.selectPublicStats()).thenReturn(stats);
        SiteStatsService service = new SiteStatsService(mapper);

        assertSame(stats, service.getPublicStats());
        assertSame(stats, service.getPublicStats());

        verify(mapper).selectPublicStats();
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void returnsZeroStatsWhenTheMapperReturnsNoRow() {
        SiteStatsMapper mapper = mock(SiteStatsMapper.class);
        when(mapper.selectPublicStats()).thenReturn(null);
        SiteStatsService service = new SiteStatsService(mapper);

        SiteStatsVO result = service.getPublicStats();

        assertEquals(0L, result.getArticleCount());
        assertEquals(0L, result.getAuthorCount());
        assertEquals(0L, result.getCategoryCount());
        assertEquals(0L, result.getTotalViews());
    }
}
