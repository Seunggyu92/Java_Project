package com.volunteerintegration.member;

import com.volunteerintegration.common.JdbcSupport;
import com.volunteerintegration.domain.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class MemberRepository {

    private static final String COLUMNS =
            "member_id, email, pw_hash, name, phone, blood_type, sido, created_at";

    private static final RowMapper<Member> MAPPER = (rs, rowNum) -> new Member(
            rs.getLong("member_id"),
            rs.getString("email"),
            rs.getString("pw_hash"),
            rs.getString("name"),
            rs.getString("phone"),
            rs.getString("blood_type"),
            rs.getString("sido"),
            JdbcSupport.localDateTime(rs, "created_at"));

    private final JdbcTemplate jdbc;

    public MemberRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Member> findByEmail(String email) {
        return jdbc.query("SELECT " + COLUMNS + " FROM member WHERE email = ?", MAPPER, email)
                .stream().findFirst();
    }

    public Optional<Member> findById(long memberId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM member WHERE member_id = ?", MAPPER, memberId)
                .stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM member WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    /** 회원을 저장하고 IDENTITY 컬럼으로 만들어진 member_id를 돌려준다. */
    public long insert(String email, String pwHash, String name, String phone, String bloodType, String sido) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO member (email, pw_hash, name, phone, blood_type, sido) VALUES (?, ?, ?, ?, ?, ?)",
                    new String[] {"MEMBER_ID"});
            ps.setString(1, email);
            ps.setString(2, pwHash);
            ps.setString(3, name);
            ps.setString(4, phone);
            ps.setString(5, bloodType);
            ps.setString(6, sido);
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey(), "member_id 생성 실패").longValue();
    }

    public void updateProfile(long memberId, String name, String phone, String bloodType, String sido) {
        jdbc.update("UPDATE member SET name = ?, phone = ?, blood_type = ?, sido = ? WHERE member_id = ?",
                new Object[] {name, phone, bloodType, sido, memberId},
                new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.NUMERIC});
    }

    public void delete(long memberId) {
        jdbc.update("DELETE FROM member WHERE member_id = ?", memberId);
    }

    // ---------------------------------------------------------- 관심 분야

    public List<Category> findInterests(long memberId) {
        return jdbc.query(
                "SELECT mi.category_code FROM member_interest mi "
                        + "JOIN category c ON c.category_code = mi.category_code "
                        + "WHERE mi.member_id = ? ORDER BY c.sort_order",
                (rs, n) -> Category.fromCode(rs.getString(1)), memberId);
    }

    public void replaceInterests(long memberId, List<Category> interests) {
        jdbc.update("DELETE FROM member_interest WHERE member_id = ?", memberId);
        if (interests.isEmpty()) {
            return;
        }
        List<Object[]> args = interests.stream()
                .distinct()
                .map(c -> new Object[] {memberId, c.getCode()})
                .toList();
        jdbc.batchUpdate("INSERT INTO member_interest (member_id, category_code) VALUES (?, ?)", args);
    }
}
