package com.volunteerintegration.facility;

import com.volunteerintegration.domain.Category;
import com.volunteerintegration.domain.Region;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

/** 시설 검색 조건 (/facilities). */
public class FacilitySearchCondition {

    private static final int MAX_KEYWORDS = 5;

    private String q;
    private String sido;
    private String category;
    private String source;
    private int page = 1;

    public String resolvedSido() {
        return Region.normalize(sido);
    }

    public Category resolvedCategory() {
        return Category.fromCode(category);
    }

    public String resolvedSource() {
        return source == null || source.isBlank() ? null : source.trim();
    }

    public List<String> keywords() {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return Arrays.stream(q.trim().split("\\s+")).limit(MAX_KEYWORDS).toList();
    }

    public int resolvedPage() {
        return Math.max(page, 1);
    }

    public String pageUrl(int targetPage) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/facilities");
        if (q != null && !q.isBlank()) {
            b.queryParam("q", q.trim());
        }
        if (resolvedSido() != null) {
            b.queryParam("sido", resolvedSido());
        }
        if (resolvedCategory() != null) {
            b.queryParam("category", resolvedCategory().getCode());
        }
        if (resolvedSource() != null) {
            b.queryParam("source", resolvedSource());
        }
        if (targetPage > 1) {
            b.queryParam("page", targetPage);
        }
        return b.encode().build().toUriString();
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getSido() {
        return sido;
    }

    public void setSido(String sido) {
        this.sido = sido;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
