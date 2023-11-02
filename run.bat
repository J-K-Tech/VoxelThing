@echo off
echo 1) build only
echo 2) run only
echo 3) build and run
CHOICE /C 123 /M "choice:"

IF ERRORLEVEL 3 GOTO br
IF ERRORLEVEL 2 GOTO rr
IF ERRORLEVEL 1 GOTO bb

:bb
call gradlew :client:shadowJar
goto lv

:rr
set JAVA_HOME ="C:\Program Files\java\jdk-17"
java17 -jar client\build\libs\voxelthing-all.jar
goto lv

:br
call gradlew :client:shadowJar
goto rr

:lv
@echo on