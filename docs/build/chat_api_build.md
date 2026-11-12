# Chat API 빌드 및 배포 

## Prerequisites

* DB를 미리 빌드, 배포하고 Gemini API Key 설정을 완료해야 한다.

* 자세한 내용은 `docs/build/prerequisites_for_build.md` 문서 참조

## 주요 기능

### POST

* `/api/v1/notebooks`: 공용 노트북을 생성한다. 요청 본문은 `{"title":"노트북 제목"}`이며, 생성된 `notebookId`, 제목, 생성 시각을 반환한다. 성공 응답은 `201 Created`이다.
* `/api/v1/notebooks/{notebookId}/sessions`: 해당 노트북에 대화 세션을 생성한다. 요청 본문은 없으며, `sessionId`와 `notebookId`를 반환한다. 성공 응답은 `201 Created`이다.
* `/api/v1/sessions/{sessionId}/chat`: 질문을 받아 해당 세션의 노트북에 등록된 문서 청크에서 관련 내용을 검색하고, Gemini 답변과 참고 자료 목록을 반환한다. 요청 본문은 `{"question":"질문 내용"}`이다. 관련 문서가 없으면 안내 답변과 빈 `sources` 목록을 반환한다.

### GET

* `/api/v1/notebooks`: 공용 노트북 목록을 생성 시각의 내림차순으로 조회한다.
* `/api/v1/notebooks/{notebookId}/sessions`: 노트북에 속한 대화 세션 목록을 생성 시각의 내림차순으로 조회한다.
* `/api/v1/sessions/{sessionId}`: 세션 ID에 해당하는 세션 정보와 연결된 `notebookId`를 조회한다.

### DELETE

* `/api/v1/notebooks/{notebookId}`: 노트북을 삭제한다. 연결된 세션과 문서 청크도 함께 삭제되며, 성공 응답은 `204 No Content`이다.

* 요청 값 검증, 존재하지 않는 리소스, 데이터베이스 또는 Gemini 처리 오류는 공통 JSON 오류 형식과 HTTP `400 Bad Request`로 반환한다.

## Prerequisites

* DB를 미리 빌드, 배포하고 Gemini API Key 설정을 완료해야 한다.

* 자세한 내용은 `docs/build/prerequisites_for_build.md` 문서 참조

## 빌드 & 배포

* chat-api 디렉토리로 이동한다.

```bash
cd apps/chat-api
```

* chat-api 모듈을 jar 파일로 빌드한다.

Windows
```powershell
.\mvnw.cmd -o "-Dmaven.repo.local=$($env:USERPROFILE)\.m2\repository" "-Dmaven.test.skip=true" package
```

Linux / Mac
```bash
./mvnw -o -Dmaven.repo.local="$HOME/.m2/repository" -Dmaven.test.skip=true package
```

* jar 파일을 이용해 docker 이미지를 빌드한다.

```bash
docker build -t finalproject/chat-api:local -f Dockerfile .
```

* apps/chat-api에서 진행해야 한다. 다른 디렉토리일 경우 POM이 없다는 오류가 발생한다.

* Kubernetes 클러스터에 배포한다.

```bash
cd <path-to-finalproject>
kubectl apply -f deployments/k8s/deployments/chat-api.yaml; kubectl rollout status deployment/chat-api --timeout=180s
```

## 테스트

* Kubernetes Cluster는 호스트와 독립된 별도의 내부망을 사용하기 때문에 호스트 컴퓨터에서는 Private IP로 클러스터에 접근할 수 없다.

* 따라서 포트포워딩을 진행해야 한다.

```bash
kubectl port-forward service/chat-api 8081:8081
```

* 호스트 컴퓨터의 8081 포트와 클러스터의 8081 포트를 Mapping한다는 뜻이다. 일종의 임시 터널. 딘일 모듈 테스트 할 때는 앞의 포트 번호는 아무거나 입력해도 된다.

* 포트포워딩이 활성화되어있을 때는 localhost:8081로 접근 가능하다. 포트포워딩 끊고 싶으면 터미널 창에서 ^C 입력

* `service` 대신 alias인 `svc`를 입력해도 된다.

* 브라우저를 열고 `localhost:8081`로 접속한다.

* 개발자 도구를 열어 Console 탭을 클릭한 다음 `allow pasting`을 입력한다.

* 아래 코드를 붙여넣는다.

```javascript
const notebookResponse = await fetch("/api/v1/notebooks", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ title: "Gemini 테스트" })
});
const notebook = await notebookResponse.json();
console.log("노트북:", notebook);

const sessionResponse = await fetch(
  `/api/v1/notebooks/${notebook.notebookId}/sessions`,
  { method: "POST" }
);
const session = await sessionResponse.json();
console.log("세션:", session);

const chatResponse = await fetch(
  `/api/v1/sessions/${session.sessionId}/chat`,
  {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question: "등록된 문서의 핵심 내용을 알려줘" })
  }
);
console.log("채팅 응답:", await chatResponse.json());
```

* 정상적인 응답 결과

```json
노트북: {notebookId: 'cc32a26f-392f-45d2-bb6e-47447b59d142', title: 'Gemini 테스트', createdAt: '2026-11-11T02:06:18.623523964Z'}
세션: {sessionId: '23b80bf3-f2c0-4b31-b847-5ddbe7c5cd70', notebookId: 'cc32a26f-392f-45d2-bb6e-47447b59d142', createdAt: '2026-11-11T02:06:18.928736173Z'}
채팅 응답: {answer: '등록된 문서 중 연관된 기술 정보를 찾을 수 없습니다.', sources: Array(0)}
```

* 세부 정보는 표시하지 않았다.