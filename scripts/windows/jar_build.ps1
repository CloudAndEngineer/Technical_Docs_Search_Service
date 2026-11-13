Set-Location ..\..\apps

Remove-Item -Path ".\api-gateway\target" -Recurse -Force
Remove-Item -Path ".\chat-api\target" -Recurse -Force
Remove-Item -Path ".\ingestion-worker\target" -Recurse -Force

Set-Location .\api-gateway

.\mvnw.cmd -o "-Dmaven.repo.local=$($env:USERPROFILE)\.m2\repository" "-Dmaven.test.skip=true" package

Set-Location ..\chat-api

.\mvnw.cmd -o "-Dmaven.repo.local=$($env:USERPROFILE)\.m2\repository" "-Dmaven.test.skip=true" package

Set-Location ..\ingestion-worker

.\mvnw.cmd -o "-Dmaven.repo.local=$($env:USERPROFILE)\.m2\repository" "-Dmaven.test.skip=true" package

Set-Location ..\..\scripts\windows