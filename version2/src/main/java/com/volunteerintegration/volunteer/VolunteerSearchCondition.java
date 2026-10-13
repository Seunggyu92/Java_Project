package com.volunteerintegration.volunteer;

import com.volunteerintegration.domain.Category;
import com.volunteerintegration.domain.Region;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

/**
 * 봉사 검색 조건. /search 의 쿼리 파라미터가 그대로 바인딩된다.
 * 검색창 한 줄 입력(q)에 들어 있는 지역·분야는 {@link #resolve()}에서 조건으로 옮긴다.
 */
public class VolunteerSearchCondition {

    private String q;
    private String sido;
    private String category;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;
    private String sort = "latest";
    private int page = 1;

    /** 검색창 입력과 드롭다운 선택을 합친 최종 조건. 드롭다운 선택이 우선한다. */
    public Resolved resolve() {
        SearchQuery parsed = SearchQuery.parse(q);
        String finalSido = Region.normalize(sido) != null ? Region.normalize(sido) : parsed.sido();
        Category finalCategory = Category.fromCode(category) != null ? Category.fromCode(category) : parsed.category();
        LocalDate f = from;
        LocalDate t = to;
        if (f != null && t != null && f.isAfter(t)) {
            LocalDate tmp = f;
            f = t;
            t = tmp;
        }
        return new Resolved(finalSido, finalCategory, parsed.keywords(), f, t, sortOrDefault(), Math.max(page, 1));
    }

    public record Resolved(String sido, Category category, java.util.List<String> keywords,
                           LocalDate from, LocalDate to, String sort, int page) {
    }

    private String sortOrDefault() {
        return switch (sort == null ? "" : sort) {
            case "oldest", "hours", "people" -> sort;
            default -> "latest";
        };
    }

    /** 현재 조건을 유지한 채 page만 바꾼 URL (빈 값은 빼고 만든다). */
    public String pageUrl(int targetPage) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/search");
        addIfPresent(b, "q", q);
        addIfPresent(b, "sido", sido);
        addIfPresent(b, "category", category);
        if (from != null) {
            b.queryParam("from", from);
        }
        if (to != null) {
            b.queryParam("to", to);
        }
        if (!"latest".equals(sortOrDefault())) {
            b.queryParam("sort", sort);
        }
        if (targetPage > 1) {
            b.queryParam("page", targetPage);
        }
        return b.encode().build().toUriString();
    }

    private static void addIfPresent(UriComponentsBuilder b, String name, String value) {
        if (value != null && !value.isBlank()) {
            b.queryParam(name, value.trim());
        }
    }

    // ---------------------------------------------------- getters / setters

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

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }

    public String getSort() {
        return sortOrDefault();
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
