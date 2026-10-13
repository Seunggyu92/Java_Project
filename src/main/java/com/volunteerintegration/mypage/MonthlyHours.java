package com.volunteerintegration.mypage;

import java.math.BigDecimal;

/** 월별 봉사시간 막대그래프 한 칸. percent는 가장 큰 달을 100으로 둔 막대 높이. */
public record MonthlyHours(String month, String label, BigDecimal hours, int percent) {

    public String getHoursLabel() {
        return hours.stripTrailingZeros().toPlainString();
    }
}
