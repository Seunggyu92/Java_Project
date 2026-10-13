package com.volunteerintegration.config;

import com.volunteerintegration.common.LoginMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면에서 쓰는 공통 모델 값.
 * Thymeleaf 3.1부터 템플릿에서 #session, #request 접근이 막혀 있어서 여기서 넣어 준다.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("loginMember")
    public LoginMember loginMember(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (LoginMember) session.getAttribute(LoginMember.SESSION_KEY);
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
