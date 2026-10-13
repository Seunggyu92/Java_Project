package com.volunteerintegration.volunteer;

import com.volunteerintegration.common.JdbcSupport;
import com.volunteerintegration.common.PageResult;
import com.volunteerintegration.domain.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class VolunteerRepository {

    public static final String COLUMNS = "v.volunteer_id, v.title, v.org_name, v.org_region, v.sido, v.sigungu, "
            + "v.activity_date, v.start_time, v.end_time, v.hours, v.content, v.recognized_date, "
            + "v.category_code, v.facility_id, v.participant_count";

    public static final RowMapper<Volunteer> MAPPER = (rs, rowNum) -> new Volunteer(
            rs.getString("volunteer_id"),
            rs.getString("title"),
            rs.getString("org_name"),
            rs.getString("org_region"),
            rs.getString("sido"),
            rs.getString("sigungu"),
            JdbcSupport.localDate(rs, "activity_date"),
            rs.getString("start_time"),
            rs.getString("end_time"),
            rs.getInt("hours"),
            rs.getString("content"),
            JdbcSupport.localDate(rs, "recognized_date"),
            Category.fromCode(rs.getString("category_code")),
            rs.getString("facility_id"),
            rs.getInt("participant_count"));

    private final JdbcTemplate jdbc;

    public VolunteerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 조건 검색 + 페이징. 값이 있는 조건만 WHERE에 붙이고 모든 값은 바인드 변수(?)로 넘긴다.
     * Oracle 12c 이상의 OFFSET ... FETCH NEXT 문법을 쓴다.
     */
    public PageResult<Volunteer> search(VolunteerSearchCondition.Resolved c, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();

        if (c.sido() != null) {
            where.append(" AND v.sido = ?");
            args.add(c.sido());
        }
        if (c.category() != null) {
            where.append(" AND v.category_code = ?");
            args.add(c.category().getCode());
        }
        if (c.from() != null) {
            where.append(" AND v.activity_date >= ?");
            args.add(JdbcSupport.sqlDate(c.from()));
        }
        if (c.to() != null) {
            where.append(" AND v.activity_date < ?");
            args.add(JdbcSupport.sqlDate(c.to().plusDays(1)));
        }
        for (String keyword : c.keywords()) {
            where.append(" AND (v.title LIKE ? ESCAPE '\\' OR v.org_name LIKE ? ESCAPE '\\'"
                    + " OR v.content LIKE ? ESCAPE '\\' OR v.org_region LIKE ? ESCAPE '\\')");
            String like = JdbcSupport.likeContains(keyword);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM volunteer v" + where, Long.class, args.toArray());

        String orderBy = switch (c.sort()) {
            case "oldest" -> " ORDER BY v.activity_date ASC, v.volunteer_id ASC";
            case "hours" -> " ORDER BY v.hours DESC, v.activity_date DESC, v.volunteer_id DESC";
            case "people" -> " ORDER BY v.participant_count DESC, v.activity_date DESC, v.volunteer_id DESC";
            default -> " ORDER BY v.activity_date DESC, v.volunteer_id DESC";
        };
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(PageResult.offset(c.page(), size));
        pageArgs.add(size);
        List<Volunteer> content = jdbc.query(
                "SELECT " + COLUMNS + " FROM volunteer v" + where + orderBy
                        + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                MAPPER, pageArgs.toArray());

        return new PageResult<>(content, c.page(), size, total == null ? 0 : total);
    }

    public Optional<Volunteer> findById(String volunteerId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM volunteer v WHERE v.volunteer_id = ?", MAPPER, volunteerId)
                .stream().findFirst();
    }

    /** 같은 기관의 다른 활동 (상세 화면 하단). */
    public List<Volunteer> findSameOrg(String orgName, String exceptId, int limit) {
        return jdbc.query("SELECT " + COLUMNS + " FROM volunteer v"
                        + " WHERE v.org_name = ? AND v.volunteer_id <> ?"
                        + " ORDER BY v.activity_date DESC, v.volunteer_id DESC FETCH FIRST ? ROWS ONLY",
                MAPPER, orgName, exceptId, limit);
    }

    public List<Volunteer> findByFacility(String facilityId, int limit) {
        return jdbc.query("SELECT " + COLUMNS + " FROM volunteer v WHERE v.facility_id = ?"
                        + " ORDER BY v.activity_date DESC, v.volunteer_id DESC FETCH FIRST ? ROWS ONLY",
                MAPPER, facilityId, limit);
    }

    public List<Volunteer> findLatest(int limit) {
        return jdbc.query("SELECT " + COLUMNS + " FROM volunteer v"
                + " ORDER BY v.activity_date DESC, v.participant_count DESC, v.volunteer_id DESC"
                + " FETCH FIRST ? ROWS ONLY", MAPPER, limit);
    }

    /**
     * 맞춤 추천. AI 모델 없이 SQL 점수로 순위를 매긴다.
     * <pre>
     *   관심 분야 일치            +3
     *   내 지역(시·도) 일치        +2
     *   내가 기록한 봉사와 같은 분야 +1
     * </pre>
     * 이미 찜한 활동은 뺀다.
     */
    public List<Volunteer> recommend(long memberId, String memberSido, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder score = new StringBuilder("(CASE WHEN v.category_code IN "
                + "(SELECT mi.category_code FROM member_interest mi WHERE mi.member_id = ?) THEN 3 ELSE 0 END");
        args.add(memberId);
        if (memberSido != null) {
            score.append(" + CASE WHEN v.sido = ? THEN 2 ELSE 0 END");
            args.add(memberSido);
        }
        score.append(" + CASE WHEN v.category_code IN (SELECT v2.category_code FROM my_record r"
                + " JOIN volunteer v2 ON v2.volunteer_id = r.volunteer_id WHERE r.member_id = ?) THEN 1 ELSE 0 END)");
        args.add(memberId);
        args.add(memberId);
        args.add(limit);

        return jdbc.query("SELECT " + COLUMNS + ", " + score + " AS score FROM volunteer v"
                        + " WHERE NOT EXISTS (SELECT 1 FROM favorite f"
                        + "                   WHERE f.volunteer_id = v.volunteer_id AND f.member_id = ?)"
                        + " ORDER BY score DESC, v.activity_date DESC, v.participant_count DESC, v.volunteer_id DESC"
                        + " FETCH FIRST ? ROWS ONLY",
                MAPPER, args.toArray());
    }

    public Map<Category, Long> countByCategory() {
        Map<Category, Long> result = new EnumMap<>(Category.class);
        jdbc.query("SELECT category_code, COUNT(*) AS cnt FROM volunteer GROUP BY category_code", rs -> {
            Category c = Category.fromCode(rs.getString("category_code"));
            if (c != null) {
                result.put(c, rs.getLong("cnt"));
            }
        });
        return result;
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM volunteer", Long.class);
        return n == null ? 0 : n;
    }
}
