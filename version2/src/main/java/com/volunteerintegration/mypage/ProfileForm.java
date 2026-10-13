package com.volunteerintegration.mypage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class ProfileForm {

    @NotBlank(message = "이름을 입력하세요.")
    @Size(max = 50, message = "이름은 50자 이하로 입력하세요.")
    private String name;

    @NotBlank(message = "전화번호를 입력하세요.")
    @Size(max = 20, message = "전화번호는 20자 이하로 입력하세요.")
    @Pattern(regexp = "^[0-9\\-]{8,20}$", message = "전화번호는 숫자와 하이픈만 입력하세요.")
    private String phone;

    private String bloodType;
    @NotBlank(message = "지역을 선택하세요.")
    private String sido;
    private List<String> interests = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getSido() {
        return sido;
    }

    public void setSido(String sido) {
        this.sido = sido;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests == null ? new ArrayList<>() : interests;
    }
}
