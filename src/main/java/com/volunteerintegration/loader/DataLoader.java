package com.volunteerintegration.loader;

import com.volunteerintegration.common.JdbcSupport;
import com.volunteerintegration.loader.PublicDataParser.CareServiceRow;
import com.volunteerintegration.loader.PublicDataParser.FacilityLinkRow;
import com.volunteerintegration.loader.PublicDataParser.FacilityRow;
import com.volunteerintegration.loader.PublicDataParser.VolunteerRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 서버 시작 때 공공데이터 CSV를 오라클에 넣는다.
 * <ul>
 *   <li>app.data.load=true 이고 facility 테이블이 비어 있을 때만 실행 (두 번째 실행부터는 건너뜀)</li>
 *   <li>FK 순서대로 facility → facility_link → care_service → volunteer</li>
 *   <li>1,000행씩 JDBC batch, 전체를 한 트랜잭션으로 묶어 중간에 실패하면 모두 롤백</li>
 * </ul>
 * 다시 넣고 싶으면 sql/99_drop.sql 실행 후 서버를 재시작한다 (테이블은 SchemaInitializer가 다시 만든다).
 */
@Component
@Order(2)
@ConditionalOnProperty(prefix = "app.data", name = "load", havingValue = "true")
public class DataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private static final int BATCH_SIZE = 1000;

    private static final String FACILITY_FILE = "facility.csv";
    private static final String LINK_FILE = "facility_link.csv";
    private static final String CARE_FILE = "care_service.csv";
    private static final String VOLUNTEER_FILE = "volunteer_1365.csv";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Path dataDir;

    public DataLoader(JdbcTemplate jdbc, TransactionTemplate tx, @Value("${app.data.dir:./data}") String dataDir) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.dataDir = Path.of(dataDir).toAbsolutePath().normalize();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Long existing;
        try {
            existing = jdbc.queryForObject("SELECT COUNT(*) FROM facility", Long.class);
        } catch (DataAccessException e) {
            log.error("[DataLoader] facility 테이블을 읽을 수 없습니다. src/main/resources/db/schema.sql 을 확인하세요. ({})",
                    e.getMostSpecificCause().getMessage());
            return;
        }
        if (existing != null && existing > 0) {
            log.info("[DataLoader] 이미 적재됨 (facility {}건) → 건너뜀", existing);
            return;
        }
        for (String f : List.of(FACILITY_FILE, LINK_FILE, CARE_FILE, VOLUNTEER_FILE)) {
            if (!Files.isRegularFile(dataDir.resolve(f))) {
                log.warn("[DataLoader] {} 파일이 없어 적재를 건너뜁니다. (폴더: {})", f, dataDir);
                return;
            }
        }

        long started = System.currentTimeMillis();
        log.info("[DataLoader] 공공데이터 적재 시작: {}", dataDir);

        List<FacilityRow> facilities = PublicDataParser.parseFacilities(dataDir.resolve(FACILITY_FILE));
        List<FacilityLinkRow> links = PublicDataParser.parseFacilityLinks(dataDir.resolve(LINK_FILE));
        List<CareServiceRow> cares = PublicDataParser.parseCareServices(dataDir.resolve(CARE_FILE));
        List<VolunteerRow> volunteers = PublicDataParser.parseVolunteers(dataDir.resolve(VOLUNTEER_FILE));

        tx.executeWithoutResult(status -> {
            insertFacilities(facilities);
            insertLinks(links);
            insertCareServices(cares);
            insertVolunteers(volunteers);
        });

        log.info("[DataLoader] 완료 ({}초): 시설 {} · 연계 {} · 돌봄서비스 {} · 봉사활동 {}",
                (System.currentTimeMillis() - started) / 1000,
                facilities.size(), links.size(), cares.size(), volunteers.size());
    }

    private void insertFacilities(List<FacilityRow> rows) {
        String sql = "INSERT INTO facility (facility_id, source, source_key, name, facility_type, facility_detail,"
                + " sido, sigungu, region_code, address, address_detail, zipcode, latitude, longitude,"
                + " phone, fax, established_date, operator_name, homepage, category_code)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int v = Types.VARCHAR;
        int[] types = {v, v, v, v, v, v, v, v, v, v, v, v, Types.NUMERIC, Types.NUMERIC, v, v, Types.DATE, v, v, v};
        batch("facility", sql, types, rows, r -> new Object[] {
                r.facilityId(), r.source(), r.sourceKey(), r.name(), r.facilityType(), r.facilityDetail(),
                r.sido(), r.sigungu(), r.regionCode(), r.address(), r.addressDetail(), r.zipcode(),
                r.latitude(), r.longitude(), r.phone(), r.fax(), JdbcSupport.sqlDate(r.establishedDate()),
                r.operatorName(), r.homepage(), r.category().getCode()});
    }

    private void insertLinks(List<FacilityLinkRow> rows) {
        String sql = "INSERT INTO facility_link (link_id, facility_id, linked_facility_id, match_type) VALUES (?, ?, ?, ?)";
        int v = Types.VARCHAR;
        batch("facility_link", sql, new int[] {v, v, v, v}, rows,
                r -> new Object[] {r.linkId(), r.facilityId(), r.linkedFacilityId(), r.matchType()});
    }

    private void insertCareServices(List<CareServiceRow> rows) {
        String sql = "INSERT INTO care_service (service_id, facility_id, match_type, service_code, service_name,"
                + " facility_name, sido, sigungu, region_code, jurisdiction) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int v = Types.VARCHAR;
        batch("care_service", sql, new int[] {v, v, v, v, v, v, v, v, v, v}, rows, r -> new Object[] {
                r.serviceId(), r.facilityId(), r.matchType(), r.serviceCode(), r.serviceName(),
                r.facilityName(), r.sido(), r.sigungu(), r.regionCode(), r.jurisdiction()});
    }

    private void insertVolunteers(List<VolunteerRow> rows) {
        String sql = "INSERT INTO volunteer (volunteer_id, title, org_name, org_region, sido, sigungu, activity_date,"
                + " start_time, end_time, hours, content, recognized_date, status, category_code, facility_id,"
                + " match_type, participant_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int v = Types.VARCHAR;
        int[] types = {v, v, v, v, v, v, Types.DATE, v, v, Types.INTEGER, v, Types.DATE, Types.CHAR, v, v, v,
                Types.INTEGER};
        batch("volunteer", sql, types, rows, r -> new Object[] {
                r.volunteerId(), r.title(), r.orgName(), r.orgRegion(), r.sido(), r.sigungu(),
                JdbcSupport.sqlDate(r.activityDate()), r.startTime(), r.endTime(), r.hours(), r.content(),
                JdbcSupport.sqlDate(r.recognizedDate()), r.status(), r.category().getCode(), r.facilityId(),
                r.matchType(), r.participantCount()});
    }

    private <T> void batch(String table, String sql, int[] types, List<T> rows, Function<T, Object[]> toArgs) {
        List<Object[]> chunk = new ArrayList<>(BATCH_SIZE);
        int done = 0;
        for (T row : rows) {
            chunk.add(toArgs.apply(row));
            if (chunk.size() == BATCH_SIZE) {
                jdbc.batchUpdate(sql, chunk, types);
                done += chunk.size();
                chunk.clear();
                if (done % 10_000 == 0) {
                    log.info("[DataLoader]   {} {} / {}", table, done, rows.size());
                }
            }
        }
        if (!chunk.isEmpty()) {
            jdbc.batchUpdate(sql, chunk, types);
        }
        log.info("[DataLoader]   {} {}건 적재", table, rows.size());
    }
}
