# Web Frontend에서 파일 업로드 기능을 구현할 때 발생한 문제

## Trial 1

* Nginx의 기본 Payload 크기는 1MB이다. 이를 Nginx 설정 파일에서 21MB(ingestion-worker의 제한인 20MB, 전체 요청 21MB에 맞춘 값)으로 변경했다.

* 413 오류는 사라졌지만, 계속해서 요청 처리에 실패하였다.

* 백엔드에서 직접 명령어로 파일을 업로드했을 때는 성공했지만, 웹 페이지에서만 오류가 발생했기 때문에 프론트엔드의 문제라고 판단하였다.

## Trial 2

* INTERNAL_ERROR 코드는 chat-api에만 정의되어있다. 따라서 api-gateway의 라우팅 규칙이 변경되지 않았을 가능성이 높았고, 실제로도 그랬다.

* Docker Image를 새로 빌드하지 않아서 생긴 문제인 줄 알았지만, 새로 컴파일하고 이미지를 빌드, 배포한 다음에도 같은 문제가 발생하였다.

* api-gateway에 새로운 라우팅 규칙을 추가하자 502 오류가 반환되었다.

## Trial 3

* 502 로그에서 Nginx가 Gateway의 이전 ClusterIP 10.96.159.132로 연결을 시도했지만 거부됐고, 현재 Gateway Service 주소는 10.96.203.50

* Gateway Service가 재생성되면서 Nginx가 시작 시 캐시한 주소가 낡은 상태였다.

* 전체 이미지를 다시 빌드하고 배포한 다음에는 502 문제는 해결되었다.

## Trial 4

* 이후에도 같은 400 문제가 발생하자 codex는 api-gateway의 라우팅 우선순위를 변경하였다.

* 이후에는 문서 업로드 시 chat-api로 라우팅되지 않고 ingestion-worker로 정상적으로 이동한 것을 확인하였다.