package com.volunteerintegration.mypage;

import java.math.BigDecimal;

/** 마이페이지 대시보드 숫자. */
public record DashboardSummary(BigDecimal volunteerHours, long volunteerCount, long bloodCount, long donationAmount) {

    public String getVolunteerHoursLabel() {
        return volunteerHours == null ? "0" : volunteerHours.stripTrailingZeros().toPlainString();
    }

    public String getDonationLabel() {
        if (donationAmount >= 10_000 && donationAmount % 10_000 == 0) {
            return String.format("%,d만", donationAmount / 10_000);
        }
        return String.format("%,d", donationAmount);
    }
}
