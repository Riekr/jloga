@echo off
if not exist "build\libs\jloga-all.jar" ( cmd /C gradlew -q :assemble )
start "" javaw -jar build\libs\jloga-all.jar %*
