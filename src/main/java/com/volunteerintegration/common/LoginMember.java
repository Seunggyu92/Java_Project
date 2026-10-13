package com.volunteerintegration.common;

import java.io.Serializable;

/** 세션에 저장하는 로그인 정보. 비밀번호 해시는 넣지 않는다. */
public record LoginMember(long id, String name, String email) implements Serializable {

    public static final String SESSION_KEY = "loginMember";
}
