# ingestion-worker 구현 중 발생한 문제

* 내부 문서 파싱 기능만 테스트하고 싶었지만, codex가 작업하는 중 웹 크롤링 기능까지 추가한 것을 발견함

* 웹 크롤링 기능은 아직 구현하지 않은 crawler-worker 모듈에서 구현하고 싶어서 codex에게 ingestion-worker에서 웹 크롤링 기능을 제거하라고 요청