package com.volunteerintegration.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class SignupForm {

    @NotBlank(message = "이름을 입력하세요.")
    @Size(max = 50, message = "이름은 50자 이하로 입력하세요.")
    private String name;

    @NotBlank(message = "이메일을 입력하세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 100, message = "이메일은 100자 이하로 입력하세요.")
    private String email;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력하세요.")
    private String password;

    @NotBlank(message = "전화번호를 입력하세요.")
    @Size(max = 20, message = "전화번호는 20자 이하로 입력하세요.")
    @Pattern(regexp = "^[0-9\\-]{8,20}$", message = "전화번호는 숫자와 하이픈만 입력하세요.")
    private String phone;

    @NotBlank(message = "혈액형을 선택하세요.")
    @Pattern(regexp = "A|B|O|AB", message = "혈액형을 선택하세요.")
    private String bloodType;

    @NotBlank(message = "지역을 선택하세요.")
    private String sido;

    @NotEmpty(message = "관심 분야를 하나 이상 선택하세요.")
    private List<String> interests = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
