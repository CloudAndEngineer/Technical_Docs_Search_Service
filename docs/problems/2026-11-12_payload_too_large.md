# Web Frontend에서 파일 업로드 기능을 구현할 때 발생한 문제

* Nginx의 기본 Payload 크기는 1MB이다. 이를 Nginx 설정 파일에서 21MB(ingestion-worker의 제한인 20MB, 전체 요청 21MB에 맞춘 값)으로 변경했다.