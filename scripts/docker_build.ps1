docker build -t finalproject/api-gateway:local -f ..\apps\api-gateway\Dockerfile ..\apps\api-gateway
docker build -t finalproject/chat-api:local -f ..\apps\chat-api\Dockerfile ..\apps\chat-api
docker build -t finalproject/ingestion-worker:local -f ..\apps\ingestion-worker\Dockerfile ..\apps\ingestion-worker
docker build -t finalproject/web-frontend:local -f ..\apps\web-frontend\Dockerfile ..\apps\web-frontend