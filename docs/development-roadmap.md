# FactoryFlow 개발 로드맵 (Development Roadmap)

전체 개발은 **작은 단위로 동작을 검증하며 점진적으로 확장**하는 방식으로 진행합니다.

---

## Phase 1: 개발 환경 및 MySQL 연결 구축 (완료)
- [x] Spring Boot 4.2.0 / Java 25 기반 프로젝트 구조 확인
- [x] `.gitignore` 보안 설정 및 민감 정보 보호
- [x] `application.properties` / `application-local.properties` 프로필 분리
- [x] `AGENTS.md` 및 `docs/` 기본 문서 작성
- [x] MySQL `factoryflow` 데이터베이스 및 전용 계정 생성 (사용자 승인 후 실행)
- [x] 환경변수(`DB_USERNAME`, `DB_PASSWORD`) 등록 및 서버/테스트 정상 기동 확인

---

## Phase 2: 백엔드 기반 구조 및 첫 번째 API 검증 (완료)
- **목표**: React 연동 전, Spring Boot의 공통 기반과 첫 CRUD 흐름 완성
- **주요 작업**:
  - [x] 공통 예외 처리(`@RestControllerAdvice`) 및 공통 응답 구조 설계
  - [x] CORS 설정 (`http://localhost:5173` 및 환경변수 기반 허용)
  - [x] `/api/health` 헬스체크 API로 기동 확인
  - [x] **기준정보(품목 Item)** Entity, Repository, Service, Controller, DTO 구현
  - [x] 품목 CRUD API 단위/통합 테스트 작성

---

## Phase 3: React 프론트엔드 구축 및 백엔드 연동 (완료)
- **목표**: `frontend/`에 Vite + React 프로젝트를 만들고 실제 백엔드 데이터 화면 출력
- **주요 작업**:
  - [x] `frontend/`에 React(Vite) 프로젝트 생성 및 폴더 구조 정리
  - [x] API 클라이언트 모듈 구성 (`fetch` 기반 `client.js`, `endpoints.js`)
  - [x] 기본 레이아웃(사이드바/상단 헤더/페이지 라우팅/현장 터치 모드) 구성
  - [x] 품목 목록 조회·등록·수정 화면 구현 및 Spring Boot API 연동 테스트

---

## Phase 4: Mini MES 핵심 기능(생산 · 재고) 구현 (완료)
- **목표**: 공장 업무의 핵심인 작업지시 → 생산실적 → 재고 증감 연결
- **주요 작업**:
  - [x] 거래처(Partner) 및 BOM(소요량) 관리 기능
  - [x] 작업지시(WorkOrder) 생성, 상태 변경(대기/작업중/일시정지/완료/취소) API 및 화면
  - [x] 생산실적(ProductionResult) 등록, 양품/불량 수량 집계 및 날짜별 분류 이력
  - [x] 재고(Inventory) 및 재고 이력(InventoryHistory) 구현
  - [x] 생산실적 등록 시 원재료 자동 차감·완제품 자동 증가 / 음수 재고 방지 검증
  - [x] 핵심 재고/트랜잭션 비즈니스 로직 테스트 작성

---

## Phase 5: 대시보드, 품질 개선 및 안정화 (완료)
- **목표**: 실제 현장에서 보기 편한 요약 화면과 안정성 확보
- **주요 작업**:
  - [x] 안전재고 미만·일시정지 작업 경고 표시 및 금일 생산 요약 대시보드 구현
  - [x] 검색·필터링·정렬·날짜별 그룹화 개선
  - [x] 입력 검증, 확인 모달, Toast 알림 및 사용자 친화적 오류 메시지 개선

---

## Phase 6: 배포 준비 및 인증/보안 (진행 중)
- **목표**: 실사용 및 포트폴리오 배포를 위한 마무리
- **주요 작업**:
  - [x] 운영용 프로필(`application-prod.properties`) 및 환경변수 기반 CORS 분리
  - [x] 클라우드 배포 패키징 (`Dockerfile`, `.dockerignore`, `frontend/vercel.json`, `frontend/.env.example`) 및 배포 가이드(`docs/deployment-guide.md`) 정리
  - [ ] 클라우드 실배포 및 동작 확인 (GitHub 푸시 → MySQL / Render·Railway / Vercel)
  - [ ] Spring Security 기반 사용자 인증 및 권한 분리(관리자/작업자)
