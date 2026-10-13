package com.volunteerintegration.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    @Test
    void 활동명_키워드로_분류한다() {
        assertThat(Category.classify("하천 정화 활동")).isEqualTo(Category.ENV);
        assertThat(Category.classify("지역아동센터 학습지도")).isEqualTo(Category.EDU);
        assertThat(Category.classify("요양원 어르신 말벗")).isEqualTo(Category.CARE);
        assertThat(Category.classify("헌혈의 집 안내 봉사")).isEqualTo(Category.BLOOD);
        assertThat(Category.classify("사무 보조")).isEqualTo(Category.ETC);
        assertThat(Category.classify(null)).isEqualTo(Category.ETC);
    }

    @Test
    void 검색어와_코드로_찾는다() {
        assertThat(Category.fromKeyword("환경정화")).isEqualTo(Category.ENV);
        assertThat(Category.fromKeyword("교육")).isEqualTo(Category.EDU);
        assertThat(Category.fromKeyword("강남구")).isNull();
        assertThat(Category.fromCode("care")).isEqualTo(Category.CARE);
        assertThat(Category.fromCode("NOPE")).isNull();
    }
}
