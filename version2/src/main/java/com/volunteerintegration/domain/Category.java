package com.volunteerintegration.domain;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 봉사 분야(카테고리).
 * <p>
 * 1365 공공데이터에는 분야 컬럼이 없어서 활동명·활동내용·기관명에 들어간 키워드로 분류한다.
 * 분류는 선언 순서(헌혈 → 환경 → 교육 → 재능 → 돌봄)대로 처음 맞는 것을 쓰고,
 * 아무 것도 맞지 않으면 {@link #ETC}가 된다.
 */
public enum Category {

    BLOOD("헌혈/헌혈센터", "🩸", List.of("헌혈", "헌혈센터", "헌혈의집", "혈액"),
            "헌혈"),
    ENV("환경정화", "🌱", List.of("환경", "환경정화", "정화", "플로깅"),
            "환경|정화|쓰레기|플로깅|줍깅|하천|해양|해변|바다|공원|산림|숲|꽃|나무|식재|재활용|분리수거|탄소|생태|벽화"),
    EDU("교육/멘토링", "📚", List.of("교육", "멘토링", "멘토", "학습"),
            "학습|교육|멘토|공부|튜터|과외|수업|강의|독서|도서|지역아동센터|공부방|숙제|코딩|영어|수학|진로|교실"),
    TALENT("재능기부", "🎨", List.of("재능", "재능기부"),
            "재능|공연|연주|악기|미용|사진|촬영|통역|번역|디자인|영상|공예|만들기|뜨개|그림|음악|댄스|마술|노래"),
    CARE("시설/돌봄", "🤝", List.of("시설", "돌봄", "복지"),
            "요양|노인|어르신|돌봄|장애|복지관|복지센터|급식|배식|목욕|말벗|식사|도시락|반찬|보육|어린이집|아동|주간보호|재가|양로|병원|간병|이동지원|활동보조|김장|푸드"),
    ETC("안내/기타", "📋", List.of("기타", "안내"), null);

    private final String label;
    private final String icon;
    private final List<String> aliases;
    private final Pattern pattern;

    Category(String label, String icon, List<String> aliases, String regex) {
        this.label = label;
        this.icon = icon;
        this.aliases = aliases;
        this.pattern = regex == null ? null : Pattern.compile(regex);
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    /** 홈 화면 카테고리 버튼 5종 (기타 제외). */
    public static List<Category> mainCategories() {
        return List.of(ENV, EDU, CARE, TALENT, BLOOD);
    }

    /** 활동명/내용 텍스트를 키워드 규칙으로 분류한다. */
    public static Category classify(String text) {
        if (text == null || text.isBlank()) {
            return ETC;
        }
        for (Category c : values()) {
            if (c.pattern != null && c.pattern.matcher(text).find()) {
                return c;
            }
        }
        return ETC;
    }

    /** "ENV" 같은 코드를 enum으로. 잘못된 값이면 null. */
    public static Category fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 검색어 토큰("환경정화", "교육" 등)이 카테고리 이름이면 해당 카테고리. */
    public static Category fromKeyword(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String t = token.trim();
        for (Category c : values()) {
            if (c.label.equals(t) || c.aliases.contains(t)) {
                return c;
            }
        }
        return null;
    }
}
