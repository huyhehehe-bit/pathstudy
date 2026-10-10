@echo off
REM ================================================================
REM  PathStudy - chay ung dung tren localhost:8081
REM ================================================================
setlocal
cd /d "%~dp0"

REM Kiem tra neu co thu muc .tools dong goi san
if exist "%~dp0.tools\jdk\jdk-21.0.12.1+1\bin\java.exe" (
  set "JAVA_HOME=%~dp0.tools\jdk\jdk-21.0.12.1+1"
  set "MVN=%~dp0.tools\maven\apache-maven-3.9.9\bin\mvn.cmd"
) else (
  REM Su dung Maven va Java co san tren he thong
  set "MVN=mvn"
)

REM Port 8080 thuong bi chiem boi service khac, mac dinh dung 8081
set "SERVER_PORT=8081"

echo ================================================================
echo  Dang khoi dong PathStudy tren cong %SERVER_PORT%...
echo  Mo trinh duyet: http://localhost:%SERVER_PORT%
echo  Tai khoan demo: demo@pathstudy.vn / 123456
echo  Nhan Ctrl+C de dung.
echo ================================================================

call %MVN% spring-boot:run -Dspring-boot.run.arguments="--server.port=%SERVER_PORT%"

endlocal
