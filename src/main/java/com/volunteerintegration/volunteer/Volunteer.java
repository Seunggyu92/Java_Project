package com.volunteerintegration.volunteer;

import com.volunteerintegration.domain.Category;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** 1365 봉사활동 한 건 (화면용 읽기 모델). */
public record Volunteer(
        String id,
        String title,
        String orgName,
        String orgRegion,
        String sido,
        String sigungu,
        LocalDate activityDate,
        String startTime,
        String endTime,
        int hours,
        String content,
        LocalDate recognizedDate,
        Category category,
        String facilityId,
        int participantCount) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /** 2021.12.31 (금) */
    public String getDateLabel() {
        if (activityDate == null) {
            return "";
        }
        return activityDate.format(DATE) + " ("
                + activityDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN) + ")";
    }

    /** 14:00–17:00 */
    public String getTimeLabel() {
        if (startTime == null) {
            return "";
        }
        return endTime == null ? startTime : startTime + "–" + endTime;
    }

    /** 경남 밀양시 */
    public String getRegionLabel() {
        if (sido == null) {
            return orgRegion == null ? "" : orgRegion;
        }
        return sigungu == null ? sido : sido + " " + sigungu;
    }
}
