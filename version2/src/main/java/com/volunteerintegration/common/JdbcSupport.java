package com.volunteerintegration.common;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** ResultSet 값 변환 도우미. Oracle NUMBER/DATE의 NULL을 안전하게 꺼낸다. */
public final class JdbcSupport {

    private JdbcSupport() {
    }

    public static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate();
    }

    public static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime();
    }

    public static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        BigDecimal v = rs.getBigDecimal(column);
        return v == null ? null : v.doubleValue();
    }

    public static Long nullableLong(ResultSet rs, String column) throws SQLException {
        BigDecimal v = rs.getBigDecimal(column);
        return v == null ? null : v.longValue();
    }

    public static Date sqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    /** LIKE 검색어의 %, _ 를 이스케이프한다. SQL에는 ESCAPE '\' 를 붙인다. */
    public static String likeContains(String keyword) {
        String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
