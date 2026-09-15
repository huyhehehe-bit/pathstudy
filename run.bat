@echo off
REM ================================================================
REM  PathStudy - chay ung dung tren localhost:8080
REM  Dung JDK 21 + Maven da dong goi san trong thu muc .tools
REM ================================================================
setlocal
cd /d "%~dp0"

set "JAVA_HOME=%~dp0.tools\jdk\jdk-21.0.12.1+1"
set "MVN=%~dp0.tools\maven\apache-maven-3.9.9\bin\mvn.cmd"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [LOI] Khong tim thay JDK trong .tools. Hay cai JDK 21 va chay: mvn spring-boot:run
  pause
  exit /b 1
)

echo ================================================================
echo  Dang khoi dong PathStudy...
echo  Mo trinh duyet: http://localhost:8080
echo  Tai khoan demo: demo@pathstudy.vn / 123456
echo  Nhan Ctrl+C de dung.
echo ================================================================

call "%MVN%" -ntp spring-boot:run

endlocal
