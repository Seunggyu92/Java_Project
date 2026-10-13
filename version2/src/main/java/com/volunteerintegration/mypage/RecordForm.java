package com.volunteerintegration.mypage;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 활동 기록 추가 폼. 종류별 필수 값(봉사=시간, 기부=금액)은 컨트롤러에서 한 번 더 확인한다. */
public class RecordForm {

    @NotBlank(message = "종류를 고르세요.")
    private String type = "VOLUNTEER";

    @NotNull(message = "날짜를 입력하세요.")
    @PastOrPresent(message = "미래 날짜는 기록할 수 없습니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate activityDate;

    @Size(max = 100, message = "장소는 100자 이하로 입력하세요.")
    private String place;

    @DecimalMin(value = "0.5", message = "봉사시간은 0.5시간 이상이어야 합니다.")
    @DecimalMax(value = "24", message = "하루 봉사시간은 24시간을 넘을 수 없습니다.")
    private BigDecimal hours;

    @Min(value = 1, message = "기부 금액은 1원 이상이어야 합니다.")
    @Max(value = 1_000_000_000L, message = "기부 금액이 너무 큽니다.")
    private Long amount;

    @Size(max = 500, message = "메모는 500자 이하로 입력하세요.")
    private String memo;

    private String volunteerId;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(LocalDate activityDate) {
        this.activityDate = activityDate;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(String volunteerId) {
        this.volunteerId = volunteerId;
    }
}
