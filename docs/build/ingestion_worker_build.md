# Ingestion Worker 빌드 및 배포

## 주요 기능

* `POST /api/v1/notebooks/{notebookId}/documents`: PDF, MD, TXT 파일과 선택 문서 제목을 접수하고 작업 ID와 `QUEUED` 상태를 반환한다. 제목을 생략하면 파일명이 제목으로 사용된다.
* `POST /api/v1/notebooks/{notebookId}/sources`: `{ "query": "Kubernetes HPA" }`를 받아 Gemini에 `Kubernetes HPA 에 관한 공식 기술 문서 찾아줘` 프롬프트로 공식 문서 검색을 요청한다.
* `GET /api/v1/ingestion-jobs/{jobId}`: `QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED` 상태와 실패 코드를 조회한다.

* 두 접수 API는 Gateway `30080` 포트에서 호출한다. Worker가 파일이나 검색 결과를 처리하고 Gemini 임베딩을 생성한 뒤 PostgreSQL `document_chunks`에 노트북 ID, 문서 텍스트, 출처 메타데이터와 벡터를 저장한다. 작업 상태는 `ingestion_jobs` 테이블에 기록된다.

* 지원되는 업로드 형식: UTF-8 인코딩 PDF, Markdown, TXT

* 파일 크기 기본 제한: 20 MiB `MAX_FILE_SIZE` 및 `MAX_FILE_SIZE_BYTES` 환경 변수로 조정 가능

* PDF는 텍스트 추출을 지원하며 이미지 전용 PDF의 OCR 처리는 포함하지 않는다.

* 외부 검색 결과는 HTTPS이며 공개 IP로 해석되는 URL만 가져오고, HTTP 리디렉션은 허용하지 않는다. 검색 결과 페이지가 이 정책에 맞지 않거나 텍스트를 추출할 수 없으면 해당 결과는 건너뛰며, 색인할 문서가 없으면 작업이 `FAILED`가 된다.

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

API Gateway가 변경된 라우팅 설정을 사용하도록 Gateway 이미지도 다시 빌드하고 배포합니다.

```bash
docker build -t finalproject/api-gateway:local -f apps/api-gateway/Dockerfile apps/api-gateway
kubectl apply -f deployments/k8s/deployments/api-gateway.yaml
kubectl rollout status deployment/api-gateway --timeout=180s
```

클러스터에는 `postgres-credentials`와 `gemini-credentials` Secret이 필요합니다. Worker는 `postgres:5432` Service와 공유 데이터베이스를 사용합니다.

## API 예시

```powershell
$upload = curl.exe -s -F "title=Kubernetes 운영 가이드" -F "file=@C:\docs\guide.pdf" http://localhost:30080/api/v1/notebooks/<notebook-uuid>/documents | ConvertFrom-Json
Invoke-RestMethod "http://localhost:30080/api/v1/ingestion-jobs/$($upload.jobId)"
```

```powershell
$search = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:30080/api/v1/notebooks/<notebook-uuid>/sources" `
  -ContentType "application/json" `
  -Body '{"query":"Kubernetes HPA"}'
Invoke-RestMethod "http://localhost:30080/api/v1/ingestion-jobs/$($search.jobId)"
```
