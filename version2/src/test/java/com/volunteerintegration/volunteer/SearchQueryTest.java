package com.volunteerintegration.volunteer;

import com.volunteerintegration.domain.Category;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchQueryTest {

    @Test
    void 검색창_입력을_지역_분야_키워드로_나눈다() {
        SearchQuery q = SearchQuery.parse("부산 강남구 환경정화");
        assertThat(q.sido()).isEqualTo("부산");
        assertThat(q.category()).isEqualTo(Category.ENV);
        assertThat(q.keywords()).containsExactly("강남구");
    }

    @Test
    void 빈_입력은_조건이_없다() {
        SearchQuery q = SearchQuery.parse("   ");
        assertThat(q.sido()).isNull();
        assertThat(q.category()).isNull();
        assertThat(q.keywords()).isEmpty();
    }
}
