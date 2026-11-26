-- =============================================================================
-- FactoryFlow MySQL 8.0 초기 데이터베이스 및 전용 계정 설정 스크립트
-- 주의: 이 스크립트는 참조용 SQL입니다.
--       실행 전 반드시 비밀번호 플레이스홀더를 안전한 값으로 변경하세요.
--       기존 데이터베이스를 삭제(DROP)하거나 초기화하지 않습니다.
-- =============================================================================

-- 1. factoryflow 데이터베이스 생성 (이모지 및 한글 완벽 지원 utf8mb4)
CREATE DATABASE IF NOT EXISTS factoryflow
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

-- 2. (권장) root 대신 사용할 factoryflow 전용 로컬 사용자 생성 및 권한 부여
--    실제 실행 시 'REPLACE_WITH_STRONG_PASSWORD' 부분을 본인만 아는 비밀번호로 바꾸어 실행하세요.
--    실행 후 OS 환경변수(DB_USERNAME=factoryflow_user, DB_PASSWORD=설정한비밀번호)를 등록합니다.

CREATE USER IF NOT EXISTS 'factoryflow_user'@'localhost'
IDENTIFIED BY 'REPLACE_WITH_STRONG_PASSWORD';

GRANT ALL PRIVILEGES ON factoryflow.* TO 'factoryflow_user'@'localhost';

FLUSH PRIVILEGES;

-- 3. 정상 생성 확인용 조회 (읽기 전용)
SHOW DATABASES LIKE 'factoryflow';
SELECT User, Host FROM mysql.user WHERE User = 'factoryflow_user';
