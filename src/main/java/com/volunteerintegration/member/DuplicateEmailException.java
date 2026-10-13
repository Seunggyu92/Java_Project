package com.volunteerintegration.member;

public class DuplicateEmailException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateEmailException(String email) {
        super("이미 가입된 이메일입니다: " + email);
    }
}
