package com.volunteerintegration.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 테이블이 없으면 classpath:db/schema.sql 을 실행해 만든다.
 * SQL Developer 같은 도구 없이도 서버 실행만으로 DB를 준비할 수 있게 하기 위함.
 * 이미 테이블이 있으면 아무 것도 하지 않는다 (DataLoader보다 먼저 실행).
 */
@Component
@Order(1)
@ConditionalOnProperty(prefix = "app.db", name = "init-schema", havingValue = "true", matchIfMissing = true)
public class SchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);
    private static final String SCHEMA = "db/schema.sql";

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public SchemaInitializer(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_tables WHERE table_name = 'FACILITY'", Integer.class);
        if (exists != null && exists > 0) {
            ensurePhoneColumn();
            log.info("[SchemaInitializer] 테이블이 이미 있음 → 건너뜀");
            return;
        }
        log.info("[SchemaInitializer] 테이블이 없어 {} 실행", SCHEMA);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(SCHEMA));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
        log.info("[SchemaInitializer] 테이블 10개 생성 완료");
    }

    /** 이미 만들어진 member 테이블에 전화번호 칸이 없으면 추가한다. */
    private void ensurePhoneColumn() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_tab_columns WHERE table_name = 'MEMBER' AND column_name = 'PHONE'",
                Integer.class);
        if (count != null && count == 0) {
            jdbc.execute("ALTER TABLE member ADD phone VARCHAR2(20)");
            log.info("[SchemaInitializer] member.phone 컬럼 추가");
        }
    }
}
