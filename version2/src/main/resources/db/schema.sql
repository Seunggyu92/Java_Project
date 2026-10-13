-- =====================================================================
-- VolunteerIntegration : 테이블 생성
-- 서버가 처음 실행될 때 SchemaInitializer가 이 파일을 자동 실행한다 (테이블이 없을 때만).
-- 직접 실행해도 된다: SQL Developer F5, 또는 sqlplus에서 @schema.sql
-- Oracle 12c 이상 (IDENTITY 컬럼, OFFSET/FETCH 사용)
--
--  코드 테이블   region, category
--  공공데이터    facility, facility_link, care_service, volunteer
--  회원 기능     member, member_interest, favorite, my_record
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 코드 테이블
-- ---------------------------------------------------------------------
CREATE TABLE region (
    sido_code   VARCHAR2(10 CHAR)  NOT NULL,
    full_name   VARCHAR2(20 CHAR)  NOT NULL,
    sort_order  NUMBER(2)          NOT NULL,
    CONSTRAINT pk_region PRIMARY KEY (sido_code)
);

CREATE TABLE category (
    category_code  VARCHAR2(10)       NOT NULL,
    category_name  VARCHAR2(20 CHAR)  NOT NULL,
    sort_order     NUMBER(2)          NOT NULL,
    CONSTRAINT pk_category PRIMARY KEY (category_code)
);

-- ---------------------------------------------------------------------
-- 2. 공공데이터 (서버가 CSV에서 적재, 화면은 읽기만 한다)
-- ---------------------------------------------------------------------

-- 통합 시설: 사회복지시설 · VMS 봉사활동처 · 청소년활동/지원시설 · 헌혈의집
-- facility_id = FAC-출처코드-원본키 (예: FAC-VMS-26000502, FAC-BLD-0001)
CREATE TABLE facility (
    facility_id       VARCHAR2(30)        NOT NULL,
    source            VARCHAR2(20 CHAR)   NOT NULL,
    source_key        VARCHAR2(20)        NOT NULL,
    name              VARCHAR2(100 CHAR)  NOT NULL,
    facility_type     VARCHAR2(30 CHAR),
    facility_detail   VARCHAR2(30 CHAR),
    sido              VARCHAR2(10 CHAR),
    sigungu           VARCHAR2(20 CHAR),
    region_code       VARCHAR2(10),
    address           VARCHAR2(300 CHAR),
    address_detail    VARCHAR2(300 CHAR),
    zipcode           VARCHAR2(6),
    latitude          NUMBER(10, 7),
    longitude         NUMBER(10, 7),
    phone             VARCHAR2(20),
    fax               VARCHAR2(20),
    established_date  DATE,
    operator_name     VARCHAR2(100 CHAR),
    homepage          VARCHAR2(500 CHAR),
    category_code     VARCHAR2(10)        NOT NULL,
    CONSTRAINT pk_facility PRIMARY KEY (facility_id),
    CONSTRAINT fk_facility_region   FOREIGN KEY (sido) REFERENCES region (sido_code),
    CONSTRAINT fk_facility_category FOREIGN KEY (category_code) REFERENCES category (category_code)
);
CREATE INDEX ix_facility_sido     ON facility (sido, category_code);
CREATE INDEX ix_facility_category ON facility (category_code);
CREATE INDEX ix_facility_name     ON facility (name);

-- 출처가 다른 같은 기관 (VMS 봉사활동처 ↔ 사회복지·청소년시설)
CREATE TABLE facility_link (
    link_id             VARCHAR2(20)       NOT NULL,
    facility_id         VARCHAR2(30)       NOT NULL,
    linked_facility_id  VARCHAR2(30)       NOT NULL,
    match_type          VARCHAR2(20 CHAR),
    CONSTRAINT pk_facility_link PRIMARY KEY (link_id),
    CONSTRAINT fk_link_facility FOREIGN KEY (facility_id)        REFERENCES facility (facility_id),
    CONSTRAINT fk_link_linked   FOREIGN KEY (linked_facility_id) REFERENCES facility (facility_id)
);
CREATE INDEX ix_link_facility ON facility_link (facility_id);
CREATE INDEX ix_link_linked   ON facility_link (linked_facility_id);

-- 돌봄 서비스 프로그램 (우리동네키움센터 등)
CREATE TABLE care_service (
    service_id     VARCHAR2(30)        NOT NULL,
    facility_id    VARCHAR2(30)        NOT NULL,
    match_type     VARCHAR2(20 CHAR),
    service_code   VARCHAR2(20),
    service_name   VARCHAR2(100 CHAR)  NOT NULL,
    facility_name  VARCHAR2(100 CHAR),
    sido           VARCHAR2(10 CHAR),
    sigungu        VARCHAR2(20 CHAR),
    region_code    VARCHAR2(10),
    jurisdiction   VARCHAR2(50 CHAR),
    CONSTRAINT pk_care_service PRIMARY KEY (service_id),
    CONSTRAINT fk_care_facility FOREIGN KEY (facility_id) REFERENCES facility (facility_id),
    CONSTRAINT fk_care_region   FOREIGN KEY (sido) REFERENCES region (sido_code)
);
CREATE INDEX ix_care_facility ON care_service (facility_id);

-- 1365 봉사활동 (같은 활동명·기관·일시의 실적 행을 하나로 묶은 것)
CREATE TABLE volunteer (
    volunteer_id       VARCHAR2(20)        NOT NULL,
    title              VARCHAR2(200 CHAR)  NOT NULL,
    org_name           VARCHAR2(100 CHAR)  NOT NULL,
    org_region         VARCHAR2(20 CHAR),
    sido               VARCHAR2(10 CHAR),
    sigungu            VARCHAR2(20 CHAR),
    activity_date      DATE                NOT NULL,
    start_time         VARCHAR2(5),
    end_time           VARCHAR2(5),
    hours              NUMBER(3)           DEFAULT 0 NOT NULL,
    content            VARCHAR2(500 CHAR),
    recognized_date    DATE,
    status             CHAR(1),
    category_code      VARCHAR2(10)        NOT NULL,
    facility_id        VARCHAR2(30),
    match_type         VARCHAR2(20 CHAR),
    participant_count  NUMBER(5)           DEFAULT 1 NOT NULL,
    CONSTRAINT pk_volunteer PRIMARY KEY (volunteer_id),
    CONSTRAINT fk_volunteer_region   FOREIGN KEY (sido) REFERENCES region (sido_code),
    CONSTRAINT fk_volunteer_category FOREIGN KEY (category_code) REFERENCES category (category_code),
    CONSTRAINT fk_volunteer_facility FOREIGN KEY (facility_id) REFERENCES facility (facility_id)
);
CREATE INDEX ix_volunteer_search   ON volunteer (sido, category_code, activity_date);
CREATE INDEX ix_volunteer_date     ON volunteer (activity_date);
CREATE INDEX ix_volunteer_facility ON volunteer (facility_id);
CREATE INDEX ix_volunteer_org      ON volunteer (org_name);

-- ---------------------------------------------------------------------
-- 3. 회원 기능
-- ---------------------------------------------------------------------
CREATE TABLE member (
    member_id   NUMBER GENERATED BY DEFAULT ON NULL AS IDENTITY,
    email       VARCHAR2(100)       NOT NULL,
    pw_hash     VARCHAR2(100)       NOT NULL,   -- BCrypt 해시 (평문 저장 금지)
    name        VARCHAR2(50 CHAR)   NOT NULL,
    phone       VARCHAR2(20),
    blood_type  VARCHAR2(2),
    sido        VARCHAR2(10 CHAR),
    created_at  DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_member PRIMARY KEY (member_id),
    CONSTRAINT uq_member_email UNIQUE (email),
    CONSTRAINT ck_member_blood CHECK (blood_type IN ('A', 'B', 'O', 'AB')),
    CONSTRAINT fk_member_region FOREIGN KEY (sido) REFERENCES region (sido_code)
);

-- 관심 분야 (회원 N : 카테고리 M)
CREATE TABLE member_interest (
    member_id      NUMBER        NOT NULL,
    category_code  VARCHAR2(10)  NOT NULL,
    CONSTRAINT pk_member_interest PRIMARY KEY (member_id, category_code),
    CONSTRAINT fk_interest_member   FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE CASCADE,
    CONSTRAINT fk_interest_category FOREIGN KEY (category_code) REFERENCES category (category_code)
);

-- 관심 봉사 (찜)
CREATE TABLE favorite (
    member_id     NUMBER        NOT NULL,
    volunteer_id  VARCHAR2(20)  NOT NULL,
    created_at    DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_favorite PRIMARY KEY (member_id, volunteer_id),
    CONSTRAINT fk_favorite_member    FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_volunteer FOREIGN KEY (volunteer_id) REFERENCES volunteer (volunteer_id) ON DELETE CASCADE
);

-- 내 활동 기록 (봉사 · 헌혈 · 기부를 사용자가 직접 남김)
CREATE TABLE my_record (
    record_id      NUMBER GENERATED BY DEFAULT ON NULL AS IDENTITY,
    member_id      NUMBER              NOT NULL,
    record_type    VARCHAR2(10)        NOT NULL,
    activity_date  DATE                NOT NULL,
    place          VARCHAR2(100 CHAR),
    hours          NUMBER(4, 1),
    amount         NUMBER(10),
    memo           VARCHAR2(500 CHAR),
    volunteer_id   VARCHAR2(20),
    created_at     DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT pk_my_record PRIMARY KEY (record_id),
    CONSTRAINT ck_record_type CHECK (record_type IN ('VOLUNTEER', 'BLOOD', 'DONATION')),
    CONSTRAINT fk_record_member    FOREIGN KEY (member_id) REFERENCES member (member_id) ON DELETE CASCADE,
    CONSTRAINT fk_record_volunteer FOREIGN KEY (volunteer_id) REFERENCES volunteer (volunteer_id) ON DELETE SET NULL
);
CREATE INDEX ix_record_member ON my_record (member_id, activity_date);

-- ---------------------------------------------------------------------
-- 4. 코드 데이터
-- ---------------------------------------------------------------------
INSERT INTO region VALUES ('서울', '서울특별시', 1);
INSERT INTO region VALUES ('부산', '부산광역시', 2);
INSERT INTO region VALUES ('대구', '대구광역시', 3);
INSERT INTO region VALUES ('인천', '인천광역시', 4);
INSERT INTO region VALUES ('광주', '광주광역시', 5);
INSERT INTO region VALUES ('대전', '대전광역시', 6);
INSERT INTO region VALUES ('울산', '울산광역시', 7);
INSERT INTO region VALUES ('세종', '세종특별자치시', 8);
INSERT INTO region VALUES ('경기', '경기도', 9);
INSERT INTO region VALUES ('강원', '강원특별자치도', 10);
INSERT INTO region VALUES ('충북', '충청북도', 11);
INSERT INTO region VALUES ('충남', '충청남도', 12);
INSERT INTO region VALUES ('전북', '전북특별자치도', 13);
INSERT INTO region VALUES ('전남', '전라남도', 14);
INSERT INTO region VALUES ('경북', '경상북도', 15);
INSERT INTO region VALUES ('경남', '경상남도', 16);
INSERT INTO region VALUES ('제주', '제주특별자치도', 17);

INSERT INTO category VALUES ('ENV',    '환경정화',      1);
INSERT INTO category VALUES ('EDU',    '교육/멘토링',   2);
INSERT INTO category VALUES ('CARE',   '시설/돌봄',     3);
INSERT INTO category VALUES ('TALENT', '재능기부',      4);
INSERT INTO category VALUES ('BLOOD',  '헌혈/헌혈센터', 5);
INSERT INTO category VALUES ('ETC',    '안내/기타',     6);

COMMIT;
