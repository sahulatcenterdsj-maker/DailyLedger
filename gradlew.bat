@echo off
setlocal
set DIR=%~dp0
set JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
if not exist "%JAR%" (
  echo Gradle wrapper jar is missing; downloading official Gradle wrapper...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar' -OutFile '%JAR%'"
  if errorlevel 1 exit /b 1
)
if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_EXE=java.exe
)
"%JAVA_EXE%" -Dorg.gradle.appname=gradlew -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
