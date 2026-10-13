package com.volunteerintegration.mypage;

import com.volunteerintegration.volunteer.Volunteer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.volunteerintegration.volunteer.VolunteerRepository.COLUMNS;
import static com.volunteerintegration.volunteer.VolunteerRepository.MAPPER;

@Repository
public class FavoriteRepository {

    private final JdbcTemplate jdbc;

    public FavoriteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean exists(long memberId, String volunteerId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM favorite WHERE member_id = ? AND volunteer_id = ?",
                Integer.class, memberId, volunteerId);
        return n != null && n > 0;
    }

    public void add(long memberId, String volunteerId) {
        jdbc.update("INSERT INTO favorite (member_id, volunteer_id) VALUES (?, ?)", memberId, volunteerId);
    }

    public void remove(long memberId, String volunteerId) {
        jdbc.update("DELETE FROM favorite WHERE member_id = ? AND volunteer_id = ?", memberId, volunteerId);
    }

    public List<Volunteer> findVolunteers(long memberId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM favorite f JOIN volunteer v ON v.volunteer_id = f.volunteer_id"
                        + " WHERE f.member_id = ? ORDER BY f.created_at DESC, v.volunteer_id",
                MAPPER, memberId);
    }
}
