# 사전 설정

* secrets를 생성한다.

* 로컬 .env 파일에 구체적인 생성 방법이 적혀있으며, 보안을 위해 커밋하지는 않았다.

# 1. chat-api

* chat-api 디렉토리로 이동한다.

```bash
cd apps/chat-api
```

* chat-api 모듈을 jar 파일로 빌드한다.

Windows
```powershell
mvn.cmd -o "-Dmaven.repo.local=$($env:USERPROFILE)\.m2\repository" "-Dmaven.test.skip=true" package
```

Linux / Mac
```bash
mvn -o -Dmaven.repo.local="$HOME/.m2/repository" -Dmaven.test.skip=true package
```

* 명령어 설명

  * `-o`: offline. 인터넷에서 의존성을 다운받지 않고 로컬에 존재하는 라이브러리만 사용

  * `-Dmaven.repo.local`: 로컬 저장소의 경로를 강제로 지정. 위 명령어에서는 Apache Maven이 기본적으로 사용하는 로컬 설정 저장소 폴더를 가리킴

  * `-Dmaven.test.skip=true`: 테스트 케이스를 실행하지 않고, 컴파일하지 않는다.

  * `package`: jar package로 빌드한다.

* IntelliJ의 Maven 빌드 기능을 사용할 때 오류가 발생했던 이유

  * 다른 이유가 있을 수도 있지만, 로그 메시지의 Classpath 경로에 공백이 있었는데, Quotation 처리를 하지 않아 명령어 처리에 실패한 듯

* jar 파일을 이용해 docker 이미지를 빌드한다.

```bash
docker build -t finalproject/chat-api:local -f Dockerfile .
```

* apps/chat-api에서 진행해야 한다. 다른 디렉토리일 경우 `Dockerfile`과 `.`의 경로가 apps/chat-api를 가리키도록 명령어를 수정해야 한다.

* Kubernetes 클러스터에 배포한다.

```bash
cd <path-to-finalproject>
kubectl apply -f deployments/k8s/deployments/chat-api.yaml; kubectl rollout status deployment/chat-api --timeout=180s
```

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

