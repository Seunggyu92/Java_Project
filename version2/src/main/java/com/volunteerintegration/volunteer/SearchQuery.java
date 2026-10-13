package com.volunteerintegration.volunteer;

import com.volunteerintegration.domain.Category;
import com.volunteerintegration.domain.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * 통합 검색창 한 줄 입력("강남구 환경정화", "부산 교육")을 조건으로 나눈다.
 * <ul>
 *   <li>시·도 이름 → 지역 조건</li>
 *   <li>카테고리 이름 → 분야 조건</li>
 *   <li>나머지 → 키워드 (활동명·기관명·내용·기관지역에 LIKE)</li>
 * </ul>
 */
public record SearchQuery(String sido, Category category, List<String> keywords) {

    private static final int MAX_KEYWORDS = 5;

    public static SearchQuery parse(String q) {
        String sido = null;
        Category category = null;
        List<String> keywords = new ArrayList<>();
        if (q != null) {
            for (String token : q.trim().split("\\s+")) {
                if (token.isEmpty()) {
                    continue;
                }
                String region = Region.normalize(token);
                Category c = Category.fromKeyword(token);
                if (region != null && sido == null) {
                    sido = region;
                } else if (c != null && category == null) {
                    category = c;
                } else if (keywords.size() < MAX_KEYWORDS) {
                    keywords.add(token);
                }
            }
        }
        return new SearchQuery(sido, category, List.copyOf(keywords));
    }
}
