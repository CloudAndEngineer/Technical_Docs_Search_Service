* api-gateway 디렉토리로 이동한다.

```bash
cd apps/api-gateway
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
docker build -t finalproject/api-gateway:local -f Dockerfile .
```

* apps/api-gateway에서 진행해야 한다. 다른 디렉토리일 경우 POM이 없다는 오류가 발생한다.

* Kubernetes 클러스터에 배포한다.

```bash
cd <path-to-finalproject>
kubectl apply -f deployments/k8s/deployments/api-gateway.yaml; kubectl rollout status deployment/api-gateway --timeout=180s
```

* 포트포워딩을 진행한다.

```bash
kubectl port-forward svc/api-gateway 8080:8080
```

* node port가 30080으로 설정되어 있지만, Docker Desktop 기반 Kubernetes 특성 상 호스트 PC에서 사설 IP로 직접 접근은 불가능하다.

* 테스트 방법

    * 브라우저에 접속해 `localhost:8080/actuator/health` 를 입력한다.

    * 정상적으로 서비스 중이라면, status가 UP으로 표시될 것이다.

    * `localhost:8080/api/v1/notebooks` 를 입력한다.

    * 노트북 목록이 JSON 배열 형태로 표시된다.

    * 아래 명령어를 입력하면 새 노트북을 생성할 수 있다. 정상적으로 처리되면 `localhost:8080/api/v1/notebooks`에 새 노트북 JSON 객체가 표시된다.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/v1/notebooks `
  -ContentType "application/json" `
  -Body '{"title":"Gateway 테스트"}'
```

```bash
curl -X POST http://localhost:8080/api/v1/notebooks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Gateway 테스트"}'
```

