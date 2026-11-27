# FactoryFlow 클라우드 배포 가이드 (Vercel + Render/Railway + MySQL)

이 문서는 **로컬에 Docker를 설치하지 않아도** GitHub 저장소 연동만으로 FactoryFlow Mini MES를 클라우드에 배포하는 절차를 단계별로 정리합니다.

---

## 1. 전체 배포 아키텍처

```text
[사용자 브라우저 / 현장 태블릿]
        │  HTTPS
        ▼
[Vercel (React + Vite 정적 SPA)]   <-- frontend/ 폴더, vercel.json으로 SPA 라우팅 처리
        │  REST API (/api/**)  (VITE_API_BASE_URL)
        ▼
[Render 또는 Railway (Spring Boot 4.2 / Java 25)]  <-- 루트 Dockerfile 자동 빌드
        │  JDBC (SSL)
        ▼
[외부 MySQL 8.0 (Railway MySQL / Aiven Free MySQL 등)]
```

---

## 2. 사전 준비: GitHub 최신 코드 푸시

배포 플랫폼(Vercel, Render/Railway)은 GitHub 저장소(`origin/main`)의 코드를 가져가 자동 빌드합니다.

1. 민감 정보(`application-local.properties`, `.env`)가 포함되지 않았는지 확인합니다.
   ```powershell
   git status
   ```
2. 원격 저장소로 푸시합니다.
   ```powershell
   git push origin main
   ```

---

## 3. 1단계: 클라우드 MySQL 8.0 생성

Railway MySQL 또는 Aiven Free MySQL 등 MySQL 8.0 호환 서비스를 생성한 뒤 아래 3가지 접속 정보를 확보합니다.

| 항목 | 설명 | 예시 |
| --- | --- | --- |
| `DB_URL` | JDBC 접속 URL (시간대·인코딩 포함) | `jdbc:mysql://<HOST>:<PORT>/<DB_NAME>?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&sslMode=REQUIRED` |
| `DB_USERNAME` | DB 사용자명 | `avnadmin` 또는 `root` 등 서비스 발급 계정 |
| `DB_PASSWORD` | DB 비밀번호 | 서비스 대시보드에서 발급된 비밀번호 |

> **참고**: 클라우드 MySQL 제공자가 SSL을 요구하지 않는 내부망(예: Railway 내부 Private Network)이라면 `sslMode=REQUIRED` 대신 `useSSL=false&allowPublicKeyRetrieval=true`로 조정할 수 있습니다.

---

## 4. 2단계: 백엔드 배포 (Render 또는 Railway)

프로젝트 루트의 `Dockerfile`은 **Java 25 + Gradle 멀티 스테이지 빌드**와 **한국 시간대(`Asia/Seoul`)**를 자동으로 구성합니다.

### 서비스 생성 설정
- **Repository**: `eorha456-bot/factoryflow-mes`
- **Branch**: `main`
- **Root Directory**: `.` (프로젝트 루트 그대로)
- **Runtime / Builder**: `Docker` (루트 `Dockerfile` 자동 감지)
- **Health Check Path**: `/api/health`

### 필수 환경변수 (Environment Variables)

| 변수명 | 필수 | 권장 값 / 예시 | 설명 |
| --- | :---: | --- | --- |
| `SPRING_PROFILES_ACTIVE` | O | `prod` | `application-prod.properties` 활성화 |
| `DB_URL` | O | `jdbc:mysql://...` | 1단계에서 확보한 JDBC URL |
| `DB_USERNAME` | O | `<발급 계정>` | DB 사용자명 |
| `DB_PASSWORD` | O | `<발급 비밀번호>` | DB 비밀번호 |
| `CORS_ALLOWED_ORIGINS` | O | `https://your-app.vercel.app` | 프론트엔드 배포 도메인 (복수 시 쉼표 `,` 구분) |
| `JPA_DDL_AUTO` | 선택 | 최초 `update` → 안정화 후 `validate` | 최초 빈 DB 테이블 자동 생성 후 검증 모드 전환 권장 |
| `DB_POOL_MAX_SIZE` | 선택 | `5` (기본값) | 무료/소규모 DB 동시 접속 제한 대응 |

> **참고**: `PORT` 환경변수는 Render/Railway가 자동 주입하며, `application.properties`의 `server.port=${PORT:8080}`이 이를 자동으로 인식합니다.

### 백엔드 배포 확인
배포가 완료되면 발급된 주소 뒤에 `/api/health`를 붙여 접속합니다.
- 예: `https://<백엔드-도메인>/api/health`
- 기대 응답:
  ```json
  {
    "success": true,
    "message": "FactoryFlow API 서버가 정상 동작 중입니다.",
    "data": { "status": "UP", "service": "factoryflow" }
  }
  ```

---

## 5. 3단계: 프론트엔드 배포 (Vercel)

1. Vercel에서 **Add New Project** → GitHub 저장소 `factoryflow-mes` 선택
2. 빌드 설정 확인:
   - **Root Directory**: `frontend` (반드시 `frontend`로 지정!)
   - **Framework Preset**: `Vite`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
3. **Environment Variables** 추가:
   - `VITE_API_BASE_URL` = `https://<2단계에서 발급된 백엔드 도메인>` (끝에 `/` 없이 입력)
4. **Deploy** 클릭 후 발급된 Vercel URL(`https://xxx.vercel.app`)을 복사합니다.
5. **2단계 백엔드 환경변수 `CORS_ALLOWED_ORIGINS`** 값을 복사한 Vercel URL로 업데이트하고 백엔드를 재배포(Restart)합니다.

> **SPA 새로고침 대응**: `frontend/vercel.json`에 `/(.*) -> /index.html` rewrite 규칙이 포함되어 있어 `/production`, `/inventory` 페이지에서 새로고침(F5)해도 404가 발생하지 않습니다.

---

## 6. 배포 후 최종 점검 체크리스트

- [ ] `https://<백엔드-도메인>/api/health` → HTTP 200 & `"status": "UP"` 확인
- [ ] `https://<프론트-도메인>` 접속 시 대시보드 정상 렌더링 및 콘솔 CORS 오류 없음 확인
- [ ] `/production` 페이지 이동 후 브라우저 새로고침(F5) 시 404 없이 정상 표시 확인
- [ ] 품목 1건 등록 → 작업지시 생성 → 일시정지/재개 → 실적 등록 → 재고 자동 반영 정상 동작 확인
- [ ] (안정화 후) 백엔드 환경변수 `JPA_DDL_AUTO=validate`로 전환 권장

---

## 7. 운영 보안 유의사항

현재 FactoryFlow는 MVP 단계로 **사용자 로그인/권한(Spring Security)** 기능이 아직 적용되어 있지 않습니다.
- 포트폴리오·사내 테스트 용도로는 바로 운영할 수 있습니다.
- 실제 공장 데이터를 외부 인터넷에 공개 운영하기 전에는 **Phase 6(로그인·관리자/작업자 권한 분리)**를 적용하는 것을 권장합니다.
