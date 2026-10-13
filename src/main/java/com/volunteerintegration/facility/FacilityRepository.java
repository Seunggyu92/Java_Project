package com.volunteerintegration.facility;

import com.volunteerintegration.common.JdbcSupport;
import com.volunteerintegration.common.PageResult;
import com.volunteerintegration.domain.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class FacilityRepository {

    private static final String COLUMNS = "f.facility_id, f.source, f.name, f.facility_type, f.facility_detail, "
            + "f.sido, f.sigungu, f.address, f.address_detail, f.zipcode, f.latitude, f.longitude, "
            + "f.phone, f.fax, f.established_date, f.operator_name, f.homepage, f.category_code";

    private static final RowMapper<Facility> MAPPER = (rs, rowNum) -> new Facility(
            rs.getString("facility_id"),
            rs.getString("source"),
            rs.getString("name"),
            rs.getString("facility_type"),
            rs.getString("facility_detail"),
            rs.getString("sido"),
            rs.getString("sigungu"),
            rs.getString("address"),
            rs.getString("address_detail"),
            rs.getString("zipcode"),
            JdbcSupport.nullableDouble(rs, "latitude"),
            JdbcSupport.nullableDouble(rs, "longitude"),
            rs.getString("phone"),
            rs.getString("fax"),
            JdbcSupport.localDate(rs, "established_date"),
            rs.getString("operator_name"),
            rs.getString("homepage"),
            Category.fromCode(rs.getString("category_code")));

    private final JdbcTemplate jdbc;

    public FacilityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PageResult<Facility> search(FacilitySearchCondition c, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (c.resolvedSido() != null) {
            where.append(" AND f.sido = ?");
            args.add(c.resolvedSido());
        }
        if (c.resolvedCategory() != null) {
            where.append(" AND f.category_code = ?");
            args.add(c.resolvedCategory().getCode());
        }
        if (c.resolvedSource() != null) {
            where.append(" AND f.source = ?");
            args.add(c.resolvedSource());
        }
        for (String keyword : c.keywords()) {
            where.append(" AND (f.name LIKE ? ESCAPE '\\' OR f.address LIKE ? ESCAPE '\\'"
                    + " OR f.facility_type LIKE ? ESCAPE '\\' OR f.sigungu LIKE ? ESCAPE '\\')");
            String like = JdbcSupport.likeContains(keyword);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM facility f" + where, Long.class, args.toArray());

        int page = c.resolvedPage();
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(PageResult.offset(page, size));
        pageArgs.add(size);
        List<Facility> content = jdbc.query("SELECT " + COLUMNS + " FROM facility f" + where
                        + " ORDER BY f.name, f.facility_id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                MAPPER, pageArgs.toArray());
        return new PageResult<>(content, page, size, total == null ? 0 : total);
    }

    public Optional<Facility> findById(String facilityId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM facility f WHERE f.facility_id = ?", MAPPER, facilityId)
                .stream().findFirst();
    }

    /** 연계 테이블로 이어진 같은 기관 (양방향). */
    public List<Facility> findLinked(String facilityId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM facility f WHERE f.facility_id IN ("
                        + "  SELECT l.linked_facility_id FROM facility_link l WHERE l.facility_id = ?"
                        + "  UNION"
                        + "  SELECT l.facility_id FROM facility_link l WHERE l.linked_facility_id = ?"
                        + ") ORDER BY f.source, f.name",
                MAPPER, facilityId, facilityId);
    }

    public List<CareService> findCareServices(String facilityId) {
        return jdbc.query("SELECT service_id, service_name, facility_name, jurisdiction"
                        + " FROM care_service WHERE facility_id = ? ORDER BY service_name",
                (rs, n) -> new CareService(rs.getString("service_id"), rs.getString("service_name"),
                        rs.getString("facility_name"), rs.getString("jurisdiction")),
                facilityId);
    }

    /** 헌혈의 집 목록 (시·도 순 → 이름 순). */
    public List<Facility> findBloodCenters(String sido) {
        String sql = "SELECT " + COLUMNS + " FROM facility f LEFT JOIN region r ON r.sido_code = f.sido"
                + " WHERE f.category_code = 'BLOOD'";
        if (sido != null) {
            return jdbc.query(sql + " AND f.sido = ? ORDER BY r.sort_order, f.name", MAPPER, sido);
        }
        return jdbc.query(sql + " ORDER BY r.sort_order, f.name", MAPPER);
    }

    /** 출처별 시설 수 (필터 드롭다운용). */
    public List<String> findSources() {
        return jdbc.queryForList("SELECT source FROM facility GROUP BY source ORDER BY COUNT(*) DESC", String.class);
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM facility", Long.class);
        return n == null ? 0 : n;
    }

    public long countByCategory(Category category) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM facility WHERE category_code = ?",
                Long.class, category.getCode());
        return n == null ? 0 : n;
    }

    public long countCareServices() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM care_service", Long.class);
        return n == null ? 0 : n;
    }
}
