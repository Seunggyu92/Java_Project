package com.volunteerintegration.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 광역 시·도. DB에는 짧은 이름("서울", "경기")을 저장한다.
 * <p>
 * 출처마다 "서울특별시 / 서울 / 서울시", "강원도 / 강원특별자치도"처럼 표기가 달라서
 * {@link #normalize(String)}로 하나의 코드로 맞춘다.
 */
public enum Region {

    SEOUL("서울", "서울특별시", "서울시"),
    BUSAN("부산", "부산광역시", "부산시"),
    DAEGU("대구", "대구광역시", "대구시"),
    INCHEON("인천", "인천광역시", "인천시"),
    GWANGJU("광주", "광주광역시", "광주시"),
    DAEJEON("대전", "대전광역시", "대전시"),
    ULSAN("울산", "울산광역시", "울산시"),
    SEJONG("세종", "세종특별자치시", "세종시"),
    GYEONGGI("경기", "경기도"),
    GANGWON("강원", "강원특별자치도", "강원도"),
    CHUNGBUK("충북", "충청북도"),
    CHUNGNAM("충남", "충청남도"),
    JEONBUK("전북", "전북특별자치도", "전라북도"),
    JEONNAM("전남", "전라남도"),
    GYEONGBUK("경북", "경상북도"),
    GYEONGNAM("경남", "경상남도"),
    JEJU("제주", "제주특별자치도", "제주도");

    /** 2026년 전남·광주 통합으로 생긴 주소 표기. 구(區)면 광주, 시·군이면 전남으로 나눈다. */
    private static final String MERGED_JEONNAM_GWANGJU = "전남광주";

    private static final Map<String, Region> LOOKUP = new HashMap<>();

    static {
        for (Region r : values()) {
            LOOKUP.put(r.shortName, r);
            for (String alias : r.aliases) {
                LOOKUP.put(alias, r);
            }
        }
    }

    private final String shortName;
    private final String fullName;
    private final List<String> aliases;

    Region(String shortName, String fullName, String... extraAliases) {
        this.shortName = shortName;
        this.fullName = fullName;
        this.aliases = new ArrayList<>();
        this.aliases.add(fullName);
        this.aliases.addAll(List.of(extraAliases));
    }

    public String getShortName() {
        return shortName;
    }

    public String getFullName() {
        return fullName;
    }

    /** 표기가 제각각인 시·도 이름을 짧은 이름으로. 모르는 값이면 null. */
    public static String normalize(String raw) {
        Region r = find(raw);
        return r == null ? null : r.shortName;
    }

    public static Region find(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        return LOOKUP.get(t);
    }

    /** "경기 용인시 기흥구 ..." 같은 주소의 첫 토큰으로 시·도를 찾는다. */
    public static String fromAddress(String address) {
        String[] tokens = tokens(address);
        if (tokens.length == 0) {
            return null;
        }
        if (tokens[0].startsWith(MERGED_JEONNAM_GWANGJU)) {
            if (tokens.length > 1 && tokens[1].endsWith("구")) {
                return GWANGJU.shortName;
            }
            return JEONNAM.shortName;
        }
        return normalize(tokens[0]);
    }

    /** 주소의 두 번째 토큰이 시·군·구면 그 값을 돌려준다. */
    public static String sigunguFromAddress(String address) {
        String[] tokens = tokens(address);
        if (tokens.length < 2) {
            return null;
        }
        return isSigungu(tokens[1]) ? tokens[1] : null;
    }

    /** "강남구", "수원시", "양평군"처럼 시·군·구 이름인지 (시·도 이름은 제외). */
    public static boolean isSigungu(String token) {
        if (token == null || token.length() < 2 || find(token) != null) {
            return false;
        }
        char last = token.charAt(token.length() - 1);
        return last == '시' || last == '군' || last == '구';
    }

    private static String[] tokens(String address) {
        if (address == null || address.isBlank()) {
            return new String[0];
        }
        return address.trim().split("\\s+");
    }
}
