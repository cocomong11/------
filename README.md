# AI 간편장부 세무 준비 서비스

소규모 개인사업자가 매출/비용 자료를 정리하고, 간편장부와 신고 준비 리포트를 확인할 수 있도록 돕는 웹 서비스 초기 모노레포입니다.

> 사용자 입력 및 업로드 자료 기준의 참고용 결과이며, 최종 신고 책임은 사용자에게 있음

## 프로젝트 구조

```text
.
├── backend   # Spring Boot 3 + Java 21 + Gradle
├── frontend  # Next.js 15 + TypeScript + Tailwind CSS + shadcn/ui 스타일 컴포넌트
├── docs      # 요구사항 및 아키텍처 문서
└── infra     # PostgreSQL Docker Compose
```

## 실행 준비

### PostgreSQL

```bash
cd infra
docker compose up -d
```

기본 접속 정보는 `infra/.env.example`과 `infra/docker-compose.yml`에 정의되어 있습니다.

### Backend

```bash
cd backend
./gradlew test
./gradlew bootRun
```

Windows PowerShell에서는 다음도 사용할 수 있습니다.

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat bootRun
```

백엔드 기본 주소는 `http://localhost:8080`이며, 헬스체크는 `GET /api/health`입니다.

### Frontend

```bash
cd frontend
npm install
npm run lint
npm run build
npm run dev
```

프론트엔드 기본 주소는 `http://localhost:3000`입니다.

## 현재 구현 범위

- 회원가입, 로그인, `GET /api/auth/me`
- 사업자 정보 등록/조회/수정과 간편장부 대상 예상 판별
- CSV/XLSX 업로드, 거래 파싱, 실패 행 저장
- 키워드 기반 거래 자동 분류와 사용자 수정 기반 개인화 분류 규칙 저장
- 거래 목록 조회와 인라인 카테고리 수정
- 간편장부 생성, 월별/연간 조회, 엑셀 다운로드
- 월별/연간 신고 준비 리포트
- 신고 준비 체크리스트 자동 생성
- `/dashboard`, `/reports`, `/checklist`, `/files`, `/transactions`, `/ledger` API 연동 화면

모든 리포트/장부/체크리스트 결과에는 다음 안내 문구를 포함합니다.

> 사용자 입력 및 업로드 자료 기준의 참고용 결과이며, 최종 신고 책임은 사용자에게 있음

## 테스트 방법

### 전체 백엔드 테스트

```bash
cd backend
./gradlew test
```

Windows PowerShell:

```powershell
cd backend
.\gradlew.bat test
```

핵심 사용자 흐름은 `UserFlowIntegrationTest`에서 다음 순서로 검증합니다.

1. 회원가입
2. 로그인
3. 내 정보 조회
4. 사업자 정보 등록
5. 간편장부 대상 예상 판별 확인
6. CSV 업로드 및 거래 파싱
7. XLSX 업로드 및 거래 파싱
8. 거래 자동 분류
9. 거래 카테고리 수정
10. 간편장부 생성
11. 월별/연간 리포트 생성
12. 체크리스트 생성
13. 간편장부 엑셀 다운로드 및 파일 내용 확인

### 프론트엔드 검증

```bash
cd frontend
npm install
npm run lint
npm run build
```

개발 서버 실행:

```bash
cd frontend
npm run dev
```

브라우저에서 `http://localhost:3000`에 접속한 뒤 회원가입 → 사업자 등록 → 파일 업로드 → 거래 분류 → 장부/리포트/체크리스트 순서로 확인합니다.

## 샘플 파일

더미 거래 파일은 `docs/sample-files`에 있습니다.

- `docs/sample-files/sample-transactions.csv`
- `docs/sample-files/sample-transactions.xlsx`

두 파일 모두 거래일자, 거래처, 내용, 입금액, 출금액, 부가세 컬럼을 포함합니다. `/files` 화면에서 업로드해 파싱과 자동 분류 흐름을 확인할 수 있습니다.
