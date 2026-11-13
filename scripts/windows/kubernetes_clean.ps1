Set-Location ..\..\deployments\k8s\deployments

kubectl delete -f api-gateway.yaml -f chat-api.yaml -f ingestion-worker.yaml -f web-frontend.yaml

Set-Location ..\..\..\scripts\windows