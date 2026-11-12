# chat-api jar 파일 빌드 중 발생한 문제

* IntelliJ 의 Maven 빌드 기능을 사용할 때 오류 발생

* 직접 입력한 명령어

Windows
```powershell
.\mvnw.cmd -o "-Dmaven.repo.local=$($env:USERPROFILE)\.m2\repository" "-Dmaven.test.skip=true" package
```

Linux / Mac
```bash
./mvnw -o -Dmaven.repo.local="$HOME/.m2/repository" -Dmaven.test.skip=true package
```

* 명령어 설명

    * `-o`: offline. 인터넷에서 의존성을 다운받지 않고 로컬에 존재하는 라이브러리만 사용

    * `-Dmaven.repo.local`: 로컬 저장소의 경로를 강제로 지정. 위 명령어에서는 Apache Maven이 기본적으로 사용하는 로컬 설정 저장소 폴더를 가리킴

    * `-Dmaven.test.skip=true`: 테스트 케이스를 실행하지 않고, 컴파일하지 않는다.

    * `package`: jar package로 빌드한다.

* 오류가 발생했던 이유

    * 다른 이유가 있을 수도 있지만, 로그 메시지의 Classpath 경로에 공백이 있었는데, Quotation 처리를 하지 않아 명령어 처리에 실패한 듯

# app-ingestion jar 파일 빌드 중 발생한 문제

* 상술한 이유로 Maven 빌드에 문제가 발생하자 codex가 임의로 빌드 스크립트인 `mvnw.cmd`를 수정하였다.

```powershell
 $MAVEN_WRAPPER_DISTS = $null
-if ((Get-Item $MAVEN_M2_PATH).Target[0] -eq $null) {
+$MAVEN_M2_ITEM = Get-Item $MAVEN_M2_PATH
+if ($null -eq $MAVEN_M2_ITEM.Target) {
   $MAVEN_WRAPPER_DISTS = "$MAVEN_M2_PATH/wrapper/dists"
 } else {
-  $MAVEN_WRAPPER_DISTS = (Get-Item $MAVEN_M2_PATH).Target[0] + "/wrapper/dists"
+  $MAVEN_WRAPPER_DISTS = $MAVEN_M2_ITEM.Target[0] + "/wrapper/dists"
 }
```

* 앞에 `-`가 붙은 행은 codex가 삭제한 행, `+`가 붙은 행은 codex가 추가한 행이다.

* 스크립트는 근본 문제가 아니었으며, chat-api를 빌드할 때 사용했던 명령어 인수를 사용하니 문제가 해결되었다.