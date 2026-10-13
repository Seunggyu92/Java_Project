package com.volunteerintegration.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegionTest {

    @Test
    void 여러_표기를_짧은_이름으로_맞춘다() {
        assertThat(Region.normalize("서울특별시")).isEqualTo("서울");
        assertThat(Region.normalize("서울")).isEqualTo("서울");
        assertThat(Region.normalize("강원도")).isEqualTo("강원");
        assertThat(Region.normalize("전북특별자치도")).isEqualTo("전북");
        assertThat(Region.normalize("밀양시")).isNull();
    }

    @Test
    void 주소에서_시도와_시군구를_꺼낸다() {
        assertThat(Region.fromAddress("경기 용인시 기흥구 서천동로 60")).isEqualTo("경기");
        assertThat(Region.sigunguFromAddress("경기 용인시 기흥구 서천동로 60")).isEqualTo("용인시");
        // 2026 전남·광주 통합 표기: 구는 광주, 시·군은 전남
        assertThat(Region.fromAddress("전남광주통합특별시 광산구 비아안길 15")).isEqualTo("광주");
        assertThat(Region.fromAddress("전남광주통합특별시 광양시 중마청룡1길 7")).isEqualTo("전남");
    }
}
