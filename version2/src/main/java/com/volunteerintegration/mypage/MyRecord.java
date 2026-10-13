package com.volunteerintegration.mypage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** 내 활동 기록 한 건. volunteerTitle은 봉사활동과 연결된 경우에만 있다. */
public record MyRecord(
        long id,
        RecordType type,
        LocalDate activityDate,
        String place,
        BigDecimal hours,
        Long amount,
        String memo,
        String volunteerId,
        String volunteerTitle) {

    public String getDateLabel() {
        return activityDate == null ? "" : activityDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }

    /** 3시간 / 1회 / 30,000원 */
    public String getValueLabel() {
        return switch (type) {
            case VOLUNTEER -> hours == null ? "-" : hours.stripTrailingZeros().toPlainString() + "시간";
            case BLOOD -> "1회";
            case DONATION -> amount == null ? "-" : String.format("%,d원", amount);
        };
    }
}
