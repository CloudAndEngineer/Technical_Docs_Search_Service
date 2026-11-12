# Web Frontend 빌드 및 배포

## 주요 기능

* 사용자가 접속할 수 있는 웹 페이지. 사용자는 프론트엔드 UI를 이용해 쉽게 요청을 전달할 수 있다.

* 새로운 워크스페이스 생성: 새로운 문서를 학습할 수 있는 독립적인 대화를 생성한다.

* 새 대화 생성: 각 워크스페이스(노트북)마다 여러 개의 대화를 생성할 수 있다.

## Prerequisites

* 테스트할 백엔드 Service와 DB를 미리 빌드, 배포해야 한다.

* 자세한 내용은 `docs/build/` 디렉토리의 다른 문서 참조

## 빌드 & 배포

* Docker 이미지를 빌드한다.

```bash
cd apps/web-frontend
docker build -t finalproject/web-frontend:local -f Dockerfile .
```

* 빌드한 이미지를 Kubernetes 환경에 배포한다.

```bash
cd <path-to-finalproject>
kubectl apply -f deployments/k8s/deployments/web-frontend.yaml
kubectl rollout status deployment/web-frontend --timeout=180s
```

## 테스트

* 클러스터에 접속하기 위해 포트포워딩을 진행한다.

* 단일 모듈 테스트를 진행할 때는 앞의 포트 번호는 아무거나 입력해도 된다. 단, 브라우저로 접속할 때는 앞의 포트 번호를 정확히 입력해야 한다.

```bash
kubectl port-forward svc/web-frontend 8080:8080 
```

* `localhost:8080` 로 접속했을 때 생성형 AI 메인화면과 유사한 화면이 나타난다.