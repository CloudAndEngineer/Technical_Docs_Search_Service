# Kubernetes 최초 배포 전 완료할 작업

## 사전 설정

* Gemini API와 Postgres ID/PW 정보가 있는 secrets를 생성한다.

* 로컬 .env 파일에 구체적인 `GOOGLE_API_KEY`가 적혀있으며, 보안을 위해 커밋하지는 않았다.

* To Generate gemini-credentials

```bash
kubectl create secret generic gemini-credentials --from-literal=api-key=$GOOGLE_API_KEY --dry-run=client -o yaml | kubectl apply -f -
```

```powershell
kubectl create secret generic gemini-credentials --from-literal=api-key=$env:GOOGLE_API_KEY --dry-run=client -o yaml | kubectl apply -f -
```

* To Generate postgres-credentials

```bash
kubectl create secret generic postgres-credentials --from-literal=username=rag_app --from-literal=password='<사용할-비밀번호>' --dry-run=client -o yaml | kubectl apply -f -
```

## Postgres DB 및 배포

* 프로젝트 루트 디렉토리에서 실행해야 한다.

```bash
kubectl apply -f deployments/k8s/statefulsets/postgres.yaml; kubectl rollout status sts/postgres --timeout=180s
```