* Docker 이미지를 빌드한다.

```bash
cd apps/web-frontend
docker build -t finalproject/web-frontend:local -f Dockerfile .
```

```bash
cd <path-to-finalproject>
kubectl apply -f deployments/k8s/deployments/web-frontend.yaml
kubectl rollout status deployment/web-frontend --timeout=180s
```

* 클러스터에 접속하기 위해 포트포워딩을 진행한다.

* 단일 모듈 테스트를 진행할 때는 앞의 포트 번호는 아무거나 입력해도 된다. 단, 브라우저로 접속할 때는 앞의 포트 번호를 정확히 입력해야 한다.

```bash
kubectl port-forward svc/web-frontend 8080:8080 
```

* `localhost:8080` 로 접속했을 때 생성형 AI 메인화면과 유사한 화면이 나타난다.