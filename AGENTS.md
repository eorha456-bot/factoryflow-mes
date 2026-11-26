# FactoryFlow 프로젝트 개발 가이드 (AGENTS.md)

이 문서는 FactoryFlow 프로젝트에 참여하는 개발자와 AI 어시스턴트가 반드시 따라야 할 기준과 안전 규칙을 정의합니다.

---

## 1. FactoryFlow 프로젝트 목적
- **대상 도메인**: 소규모 골판지 상자 제조업체
- **목표**: 수기·엑셀 위주로 관리하던 원자재(원지/원단), 생산 지시, 공정 실적, 완제품 재고 및 입출고 내역을 웹 기반으로 통합 관리하는 **Mini MES(제조실행시스템)** 구축
- **핵심 가치**: 초보 개발자도 구조를 명확히 이해하고 유지보수할 수 있는 단순하고 견고한 풀스택 아키텍처 유지

---

## 2. 기술 스택
- **Backend**: Java 25 (Temurin LTS), Spring Boot 4.2.0, Gradle 9.9.2
- **Web / Validation**: Spring Web (`spring-boot-starter-webmvc`), Spring Validation (`spring-boot-starter-validation`)
- **Data / DB**: Spring Data JPA (`spring-boot-starter-data-jpa`), MySQL 8.0 (`mysql-connector-j`)
- **Developer Tools**: Lombok, Spring Boot DevTools
- **Frontend (예정)**: React, Vite, Node.js 26 (JavaScript 기반)
- **IDE / Collaboration**: IntelliJ IDEA 2026.2.1+, Git / GitHub

---

## 3. 백엔드 및 프론트엔드 구조
현재 단일 Git 저장소(모노레포) 구조를 유지합니다.

```text
factoryflow/
├─ src/main/java/com/lgb/factoryflow/   # Spring Boot 백엔드 루트 패키지
│  ├─ global/                           # 공통 설정(CORS, 예외 처리, 공통 응답 등)
│  └─ domain/                           # 도메인별 패키지 (예: item, order, production, inventory)
│     ├─ entity/                        # JPA Entity
│     ├─ repository/                    # Spring Data JPA Repository
│     ├─ service/                       # 비즈니스 로직 및 트랜잭션 관리
│     ├─ controller/                    # REST API 엔드포인트
│     └─ dto/                           # 요청/응답 DTO
├─ src/main/resources/                  # 설정 파일 (application.properties 등)
├─ frontend/                            # React(Vite) SPA 프론트엔드 (별도 서버 실행)
└─ docs/                                # 설계 문서, 요구사항, 로드맵, DB SQL
```

- **분리 원칙**: 백엔드는 JSON REST API만 제공하며, 화면 렌더링은 `frontend/`의 React가 담당합니다. (Thymeleaf 등 SSR 템플릿 엔진은 사용하지 않습니다.)

---

## 4. 코딩 규칙
- **계층 분리**: `Controller -> Service -> Repository` 책임을 명확히 나눕니다.
- **Entity 직접 반환 금지**: Controller에서 JPA Entity를 직접 요청/응답으로 주고받지 않고 반드시 **DTO(Request/Response)**를 사용합니다.
- **입력값 검증**: 요청 DTO에 `@NotBlank`, `@NotNull`, `@Positive` 등 Bean Validation을 적용하고 Controller에서 `@Valid`로 검증합니다.
- **Lombok 사용 주의**: Entity에 무분별한 `@Setter`, `@Data`, `@ToString`(연관관계 순환 참조 위험) 사용을 지양하고, 필요한 메서드와 생성자/빌더를 명확히 작성합니다.
- **트랜잭션**: 상태를 변경하는 Service 메서드에는 `@Transactional`을 명시하고, 조회 전용 메서드에는 `@Transactional(readOnly = true)`를 사용합니다.
- **명확한 네이밍**: 축약어 대신 도메인 의미가 드러나는 이름(예: `WorkOrder`, `ProductionResult`, `InventoryHistory`)을 사용합니다.

---

## 5. 데이터베이스 관리 규칙
- **민감정보 보호**: DB 사용자명·비밀번호는 코드나 추적 대상 파일에 절대 직접 기록하지 않고 `DB_USERNAME`, `DB_PASSWORD` 환경변수를 통해 주입합니다.
- **로컬 설정 분리**: 로컬 전용 설정은 `application-local.properties`에 작성하며, 반드시 `.gitignore`로 보호합니다.
- **`ddl-auto` 제한**: 개발 중 로컬에서는 `update`까지만 허용하며, `create`, `create-drop`은 데이터 소실 위험이 있으므로 사용하지 않습니다.
- **파괴적 SQL 금지**: `DROP DATABASE`, `DROP TABLE`, `TRUNCATE` 등 데이터/스키마 삭제 명령을 임의로 실행하지 않습니다.
- **OSIV 비활성화**: `spring.jpa.open-in-view=false`를 기본으로 유지하여 DB 커넥션 점유 시간을 최소화합니다.
- **재고 무결성**: 재고 수량은 임의 덮어쓰기보다 입출고 이력(수불)과 함께 트랜잭션 내에서 증감 처리합니다.

---

## 6. Git 커밋 규칙
- **커밋 제외 대상**: `build/`, `.gradle/`, `.idea/`, `node_modules/`, `frontend/dist/`, `.env*`, `application-local.properties`, `*.log`는 절대 커밋하지 않습니다.
- **작은 단위 커밋**: 하나의 커밋에는 한 가지 목적의 변경만 담습니다.
- **커밋 메시지 접두사(권장)**:
  - `[CHORE]` 설정, `.gitignore`, 환경 구성
  - `[DOCS]` `docs/` 및 `AGENTS.md` 문서 작업
  - `[FEAT]` 새로운 기능 구현 (필요 시 `[BE]`, `[FE]` 병기)
  - `[FIX]` 버그 수정
  - `[TEST]` 테스트 코드 추가 및 수정
  - `[REFACTOR]` 동작 변경 없는 코드 구조 개선

---

## 7. 테스트 원칙
- **임의 우회 금지**: 테스트 실패 시 테스트를 삭제하거나 `@Disabled`로 회피하지 않고 원인을 파악해 해결합니다.
- **핵심 로직 우선 테스트**: 특히 생산실적 등록에 따른 자재 차감, 완제품 증가, 재고 부족 예외 처리 등 도메인 핵심 규칙을 우선 테스트합니다.
- **기본 컨텍스트 검증**: 새로운 설정 변경 후에는 최소한 `./gradlew test`로 애플리케이션 컨텍스트와 단위 테스트 통과를 확인합니다.

---

## 8. 파일 변경 시 안전 규칙
- **최소 변경 원칙**: 현재 단계에서 요청되지 않은 파일(예: 미리 만든 Entity, 불필요한 의존성)을 임의로 생성하거나 `build.gradle`을 통째로 교체하지 않습니다.
- **사전 설명 및 승인**: DB 스키마/계정 생성, 의존성 추가, 설정 파일 구조 변경 등 영향이 큰 작업은 이유와 영향을 먼저 설명하고 사용자 승인 후 진행합니다.
- **기존 변경사항 존중**: 사용자가 작업 중이거나 스테이징한 내용을 임의로 삭제하거나 덮어쓰지 않습니다.

---

## 9. 초보자 교육 원칙
- **이유와 원리 설명**: 코드를 수정하거나 명령어를 실행할 때 "무엇을 했는지"뿐 아니라 **"왜 필요한지"와 "어떻게 동작하는지"**를 비전공자 눈높이에서 설명합니다.
- **단계적 학습(Baby Steps)**: 한 번에 수십 개 파일을 만들지 않고, 작은 단위로 구현하고 실행 결과를 눈으로 확인한 뒤 다음 단계로 넘어갑니다.
- **추측 금지**: 확인되지 않은 환경 상태나 에러 원인은 추측하지 않고, 확인된 사실과 확인 불가 사항을 명확히 구분해 안내합니다.
