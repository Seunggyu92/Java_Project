-- =====================================================================
-- VolunteerIntegration : Oracle 계정 생성 (SYSTEM 또는 SYS 계정으로 1회 실행)
-- Oracle XE 18c / 21c 기준. 계정은 플러그인 DB(XEPDB1) 안에 만든다.
-- =====================================================================
ALTER SESSION SET CONTAINER = XEPDB1;

CREATE USER volunteer IDENTIFIED BY volunteer
    DEFAULT TABLESPACE users
    QUOTA UNLIMITED ON users;

GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE TO volunteer;
