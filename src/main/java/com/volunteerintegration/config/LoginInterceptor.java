package com.volunteerintegration.config;

import com.volunteerintegration.common.LoginMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/** 로그인이 필요한 주소에 비로그인으로 들어오면 로그인 화면으로 보낸다. */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(LoginMember.SESSION_KEY) != null) {
            return true;
        }
        // POST(찜하기 등)는 돌아갈 곳을 상세 페이지로, GET은 원래 주소로.
        String back = "GET".equalsIgnoreCase(request.getMethod())
                ? request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString())
                : refererPath(request);
        String target = UriComponentsBuilder.fromPath("/login")
                .queryParam("redirect", back)
                .encode()
                .build()
                .toUriString();
        response.sendRedirect(request.getContextPath() + target);
        return false;
    }

    private static String refererPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null) {
            return "/";
        }
        try {
            java.net.URI uri = java.net.URI.create(referer);
            String path = uri.getRawPath() == null ? "/" : uri.getRawPath();
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
