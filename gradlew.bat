@echo off
rem Gradle wrapper script for Windows

set DIR=%~dp0
if "%DIR%"=="" set DIR=.

set GRADLE_HOME=%DIR%gradle\wrapper
set PATH=%GRADLE_HOME%;%PATH%

java -classpath "%GRADLE_HOME%\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*