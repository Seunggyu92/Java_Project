package com.volunteerintegration.mypage;

import com.volunteerintegration.common.JdbcSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class RecordRepository {

    private static final RowMapper<MyRecord> MAPPER = (rs, rowNum) -> new MyRecord(
            rs.getLong("record_id"),
            RecordType.fromCode(rs.getString("record_type")),
            JdbcSupport.localDate(rs, "activity_date"),
            rs.getString("place"),
            rs.getBigDecimal("hours"),
            JdbcSupport.nullableLong(rs, "amount"),
            rs.getString("memo"),
            rs.getString("volunteer_id"),
            rs.getString("volunteer_title"));

    private final JdbcTemplate jdbc;

    public RecordRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(long memberId, RecordType type, LocalDate date, String place,
                       BigDecimal hours, Long amount, String memo, String volunteerId) {
        jdbc.update("INSERT INTO my_record (member_id, record_type, activity_date, place, hours, amount, memo, volunteer_id)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[] {memberId, type.getCode(), JdbcSupport.sqlDate(date), place, hours, amount, memo, volunteerId},
                new int[] {Types.NUMERIC, Types.VARCHAR, Types.DATE, Types.VARCHAR,
                        Types.NUMERIC, Types.NUMERIC, Types.VARCHAR, Types.VARCHAR});
    }

    /** 본인 기록만 지운다. 지운 행 수를 돌려준다. */
    public int delete(long memberId, long recordId) {
        return jdbc.update("DELETE FROM my_record WHERE record_id = ? AND member_id = ?", recordId, memberId);
    }

    public List<MyRecord> findByMember(long memberId, int limit) {
        return jdbc.query("SELECT r.record_id, r.record_type, r.activity_date, r.place, r.hours, r.amount,"
                        + " r.memo, r.volunteer_id, v.title AS volunteer_title"
                        + " FROM my_record r LEFT JOIN volunteer v ON v.volunteer_id = r.volunteer_id"
                        + " WHERE r.member_id = ?"
                        + " ORDER BY r.activity_date DESC, r.record_id DESC FETCH FIRST ? ROWS ONLY",
                MAPPER, memberId, limit);
    }

    public DashboardSummary summary(long memberId) {
        return jdbc.queryForObject("SELECT"
                        + " NVL(SUM(CASE WHEN record_type = 'VOLUNTEER' THEN hours END), 0) AS volunteer_hours,"
                        + " COUNT(CASE WHEN record_type = 'VOLUNTEER' THEN 1 END)        AS volunteer_count,"
                        + " COUNT(CASE WHEN record_type = 'BLOOD' THEN 1 END)            AS blood_count,"
                        + " NVL(SUM(CASE WHEN record_type = 'DONATION' THEN amount END), 0) AS donation_amount"
                        + " FROM my_record WHERE member_id = ?",
                (rs, n) -> new DashboardSummary(
                        rs.getBigDecimal("volunteer_hours"),
                        rs.getLong("volunteer_count"),
                        rs.getLong("blood_count"),
                        rs.getLong("donation_amount")),
                memberId);
    }

    /** since 이후 월별 봉사시간 합계. 키는 "YYYY-MM". */
    public Map<String, BigDecimal> monthlyVolunteerHours(long memberId, LocalDate since) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbc.query("SELECT TO_CHAR(activity_date, 'YYYY-MM') AS ym, SUM(hours) AS total"
                        + " FROM my_record"
                        + " WHERE member_id = ? AND record_type = 'VOLUNTEER' AND activity_date >= ?"
                        + " GROUP BY TO_CHAR(activity_date, 'YYYY-MM')",
                rs -> {
                    BigDecimal total = rs.getBigDecimal("total");
                    result.put(rs.getString("ym"), total == null ? BigDecimal.ZERO : total);
                },
                memberId, JdbcSupport.sqlDate(since));
        return result;
    }
}
