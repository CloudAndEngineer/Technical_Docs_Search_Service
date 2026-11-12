# Ingestion Worker 빌드 및 배포

## Prerequisites

* DB를 미리 빌드, 배포하고 Gemini API Key 설정을 완료해야 한다.

* 자세한 내용은 `docs/build/prerequisites_for_build.md` 문서 참조

## 주요 기능

* `POST /api/v1/notebooks/{notebookId}/documents`: PDF, MD, TXT 파일과 선택 문서 제목을 접수하고 작업 ID와 `QUEUED` 상태를 반환한다. 제목을 생략하면 파일명이 제목으로 사용된다.
* `GET /api/v1/ingestion-jobs/{jobId}`: `QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED` 상태와 실패 코드를 조회한다.

* 업로드와 작업 상태 조회 API는 Gateway `30080` 포트에서 호출한다. Worker가 업로드 파일을 파싱하고 Gemini 임베딩을 생성한 뒤 PostgreSQL `document_chunks`에 노트북 ID, 문서 텍스트, 파일 메타데이터와 벡터를 저장한다. 작업 상태는 `ingestion_jobs` 테이블에 기록된다.

* 지원되는 업로드 형식: UTF-8 인코딩 PDF, Markdown, TXT

* 파일 크기 기본 제한: 20 MiB `MAX_FILE_SIZE` 및 `MAX_FILE_SIZE_BYTES` 환경 변수로 조정 가능

* PDF는 텍스트 추출을 지원하며 이미지 전용 PDF의 OCR 처리는 포함하지 않는다.

* 웹 검색 및 웹 페이지 수집은 ingestion-worker에서 수행하지 않는다. 추후 crawler-worker가 수집한 결과를 ingestion-worker에 전달하는 연동을 별도로 추가한다.

## 패키징 및 배포

* ingestion-worker 디렉토리로 이동한다.

```bash
cd apps/ingestion-worker
```

Windows
```powershell
.\mvnw.cmd -o "-Dmaven.repo.local=$($env:USERPROFILE)\.m2\repository" "-Dmaven.test.skip=true" package
```

Linux / Mac
```bash
./mvnw -o -Dmaven.repo.local="$HOME/.m2/repository" -Dmaven.test.skip=true package
```

* Docker 이미지를 빌드한다.

```bash
docker build -t finalproject/ingestion-worker:local -f Dockerfile .
```

```bash
cd <path-to-finalproject>
kubectl apply -f deployments/k8s/deployments/ingestion-worker.yaml
kubectl rollout status deployment/ingestion-worker --timeout=180s
```

API Gateway가 변경된 라우팅 설정을 사용하도록 Gateway 이미지도 다시 빌드하고 배포한다.

```bash
docker build -t finalproject/api-gateway:local -f apps/api-gateway/Dockerfile apps/api-gateway
kubectl apply -f deployments/k8s/deployments/api-gateway.yaml
kubectl rollout status deployment/api-gateway --timeout=180s
```

클러스터에는 `postgres-credentials`와 `gemini-credentials` Secret이 필요하다. Worker는 `postgres:5432` Service와 공유 데이터베이스를 사용한다.

## 테스트 및 API 예시

* 파일 업로드 테스트

* 단일 모듈을 테스트할 경우 포트포워딩을 진행한다.

```bash
kubectl port-forward svc/ingestion-worker 8082:8082
```

* UUID는 api-gateway에 접속해서 직접 확인해야 한다.

* `docs/build/api_gateway_build.md` 참조

Windows
```powershell
$notebookId = "<노트북 UUID>"
$job = curl.exe -s `
  -F "title=Kubernetes 운영 가이드" `
-F "file=@C:\docs\guide.pdf" `
"http://localhost:8082/api/v1/notebooks/$notebookId/documents" | ConvertFrom-Json

Invoke-RestMethod "http://localhost:8082/api/v1/ingestion-jobs/$($job.jobId)"
```

* `jq`가 설치되어 있는 경우

Linux / Mac
```bash
NOTEBOOK_ID="<노트북 UUID>"
JOB_ID=$(curl -s \
  -F "title=Kubernetes 운영 가이드" \
  -F "file=@/path/to/guide.pdf" \
  "http://localhost:8082/api/v1/notebooks/$NOTEBOOK_ID/documents" | jq -r '.jobId')

curl -s "http://localhost:8082/api/v1/ingestion-jobs/$JOB_ID"
```

* 별도의 JSON Parser(jq, python 등)이 없는 경우 Text Parsing으로 대체

```bash
NOTEBOOK_ID="<노트북 UUID>"
JOB_ID=$(curl -s \
  -F "title=Kubernetes 운영 가이드" \
  -F "file=@/path/to/guide.pdf" \
  "http://localhost:8082/api/v1/notebooks/$NOTEBOOK_ID/documents" | grep -o '"jobId":"[^"]*"' | cut -d'"' -f4)

curl -s "http://localhost:8082/api/v1/ingestion-jobs/$JOB_ID"
```