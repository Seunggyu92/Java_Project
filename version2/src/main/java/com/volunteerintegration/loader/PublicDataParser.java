package com.volunteerintegration.loader;

import com.volunteerintegration.domain.Category;
import com.volunteerintegration.domain.Region;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 공공데이터 CSV 4종을 DB에 넣을 행(Row)으로 바꾼다. 스프링에 의존하지 않는 순수 자바 코드라
 * 단위 테스트로 검증할 수 있다.
 *
 * <pre>
 * facility.csv        통합 시설 (사회복지시설, VMS 봉사활동처, 청소년활동·지원시설, 헌혈의집)
 * facility_link.csv   출처가 다른 같은 기관 연계
 * care_service.csv    돌봄 서비스 프로그램 → 시설
 * volunteer_1365.csv  1365 봉사 실적
 * </pre>
 */
public final class PublicDataParser {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    private PublicDataParser() {
    }

    // ------------------------------------------------------------------ rows

    public record FacilityRow(
            String facilityId, String source, String sourceKey, String name,
            String facilityType, String facilityDetail, String sido, String sigungu,
            String regionCode, String address, String addressDetail, String zipcode,
            Double latitude, Double longitude, String phone, String fax,
            LocalDate establishedDate, String operatorName, String homepage, Category category) {
    }

    public record FacilityLinkRow(String linkId, String facilityId, String linkedFacilityId, String matchType) {
    }

    public record CareServiceRow(
            String serviceId, String facilityId, String matchType, String serviceCode,
            String serviceName, String facilityName, String sido, String sigungu,
            String regionCode, String jurisdiction) {
    }

    public record VolunteerRow(
            String volunteerId, String title, String orgName, String orgRegion,
            String sido, String sigungu, LocalDate activityDate, String startTime, String endTime,
            int hours, String content, LocalDate recognizedDate, String status,
            Category category, String facilityId, String matchType, int participantCount) {
    }

    // --------------------------------------------------------------- parsers

    public static List<FacilityRow> parseFacilities(Path file) throws IOException {
        List<FacilityRow> rows = new ArrayList<>();
        CsvReader.forEach(file, r -> {
            String source = r.get("출처");
            String address = r.get("주소");
            String sido = Region.normalize(r.get("시도"));
            if (sido == null) {
                sido = Region.fromAddress(address);
            }
            String sigungu = r.get("시군구");
            if (sigungu == null) {
                sigungu = Region.sigunguFromAddress(address);
            }
            rows.add(new FacilityRow(
                    r.get("시설ID"), source, r.get("출처키"), cut(r.get("시설명"), 100),
                    r.get("시설분류"), r.get("시설상세"), sido, sigungu,
                    r.get("지역코드"), cut(address, 300), cut(r.get("상세주소"), 300), r.get("우편번호"),
                    toDouble(r.get("위도")), toDouble(r.get("경도")),
                    r.get("전화번호"), r.get("팩스번호"), toDate(r.get("설립일")),
                    cut(r.get("운영기관"), 100), cut(r.get("홈페이지"), 500),
                    facilityCategory(source)));
        });
        return rows;
    }

    public static List<FacilityLinkRow> parseFacilityLinks(Path file) throws IOException {
        List<FacilityLinkRow> rows = new ArrayList<>();
        CsvReader.forEach(file, r -> rows.add(new FacilityLinkRow(
                r.get("연계ID"), r.get("시설ID"), r.get("연결시설ID"), r.get("매칭"))));
        return rows;
    }

    public static List<CareServiceRow> parseCareServices(Path file) throws IOException {
        List<CareServiceRow> rows = new ArrayList<>();
        CsvReader.forEach(file, r -> rows.add(new CareServiceRow(
                r.get("자원ID"), r.get("시설ID"), r.get("매칭"), r.get("서비스코드"),
                cut(r.get("서비스명"), 100), cut(r.get("시설명"), 100),
                Region.normalize(r.get("시도")), r.get("시군구"), r.get("지역코드"), r.get("소관지역"))));
        return rows;
    }

    /**
     * 1365 실적 데이터는 봉사자 한 명당 한 행이다. 활동명·기관·날짜·시간이 모두 같은 행을
     * 하나의 봉사활동으로 묶고, 묶인 행 수를 참여 인원(participantCount)으로 둔다.
     */
    public static List<VolunteerRow> parseVolunteers(Path file) throws IOException {
        Map<String, VolunteerRow> grouped = new LinkedHashMap<>();
        CsvReader.forEach(file, r -> {
            String title = r.get("봉사활동명");
            String org = r.get("수요기관명");
            String date = r.get("봉사일자");
            String start = time(r.get("시작시"), r.get("시작분"));
            String end = time(r.get("종료시"), r.get("종료분"));
            String key = String.join("|", nz(title), nz(org), nz(date), nz(start), nz(end));

            VolunteerRow prev = grouped.get(key);
            if (prev != null) {
                // 시설 매칭이 뒤 행에만 있을 수 있으니 비어 있으면 채운다.
                String facilityId = prev.facilityId() != null ? prev.facilityId() : r.get("시설ID");
                String matchType = prev.facilityId() != null ? prev.matchType() : r.get("매칭");
                grouped.put(key, new VolunteerRow(prev.volunteerId(), prev.title(), prev.orgName(),
                        prev.orgRegion(), prev.sido(), prev.sigungu(), prev.activityDate(),
                        prev.startTime(), prev.endTime(), prev.hours(), prev.content(),
                        prev.recognizedDate(), prev.status(), prev.category(),
                        facilityId, matchType, prev.participantCount() + 1));
                return;
            }

            String orgRegion = r.get("수요기관지역");
            String sido = Region.normalize(r.get("소속지역"));
            if (sido == null) {
                sido = Region.normalize(orgRegion);
            }
            String sigungu = Region.isSigungu(orgRegion) ? orgRegion : null;
            String content = r.get("활동내용");
            Category category = Category.classify(nz(title) + " " + nz(content) + " " + nz(org));

            grouped.put(key, new VolunteerRow(
                    r.get("봉사ID"), cut(title, 200), cut(org, 100), orgRegion, sido, sigungu,
                    toDate(date), start, end, toInt(r.get("봉사시간")), cut(content, 500),
                    toDate(r.get("실적인정일")), r.get("작업상태"), category,
                    r.get("시설ID"), r.get("매칭"), 1));
        });
        return new ArrayList<>(grouped.values());
    }

    // --------------------------------------------------------------- helpers

    static Category facilityCategory(String source) {
        if (source == null) {
            return Category.CARE;
        }
        if (source.contains("헌혈")) {
            return Category.BLOOD;
        }
        if (source.contains("청소년")) {
            return Category.EDU;
        }
        return Category.CARE;
    }

    /** "20211231" 또는 "2016-11-14" → LocalDate. 형식이 틀리면 null. */
    static LocalDate toDate(String s) {
        if (s == null) {
            return null;
        }
        String digits = s.replace("-", "").replace(".", "").trim();
        if (digits.length() != 8) {
            return null;
        }
        try {
            return LocalDate.parse(digits, YYYYMMDD);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static String time(String hour, String minute) {
        Integer h = toIntOrNull(hour);
        if (h == null) {
            return null;
        }
        Integer m = toIntOrNull(minute);
        return String.format("%02d:%02d", h, m == null ? 0 : m);
    }

    private static Double toDouble(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Double.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int toInt(String s) {
        Integer v = toIntOrNull(s);
        return v == null ? 0 : v;
    }

    private static Integer toIntOrNull(String s) {
        if (s == null) {
            return null;
        }
        try {
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String cut(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
