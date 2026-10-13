package com.volunteerintegration.facility;

import com.volunteerintegration.domain.Category;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** 통합 시설 한 곳 (화면용 읽기 모델). */
public record Facility(
        String id,
        String source,
        String name,
        String facilityType,
        String facilityDetail,
        String sido,
        String sigungu,
        String address,
        String addressDetail,
        String zipcode,
        Double latitude,
        Double longitude,
        String phone,
        String fax,
        LocalDate establishedDate,
        String operatorName,
        String homepage,
        Category category) {

    public String getFullAddress() {
        if (address == null) {
            return sido == null ? "" : (sigungu == null ? sido : sido + " " + sigungu);
        }
        return addressDetail == null ? address : address + ", " + addressDetail;
    }

    public boolean isHasLocation() {
        return latitude != null && longitude != null;
    }

    /**
     * 카카오맵 링크. 좌표가 있으면 핀을 찍고, 없으면 주소(없으면 이름)로 검색한다.
     * 카카오맵 URL 스킴: /link/map/이름,위도,경도  /link/search/검색어
     */
    public String getMapUrl() {
        if (isHasLocation()) {
            return "https://map.kakao.com/link/map/" + encode(name) + "," + latitude + "," + longitude;
        }
        String query = address != null ? address : name;
        return "https://map.kakao.com/link/search/" + encode(query);
    }

    public String getHomepageUrl() {
        if (homepage == null || homepage.isBlank()) {
            return null;
        }
        String h = homepage.trim();
        return h.startsWith("http://") || h.startsWith("https://") ? h : "http://" + h;
    }

    private static String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
