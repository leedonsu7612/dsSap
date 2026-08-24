@echo off
cd /d "%~dp0"
call "%~dp0mvnw.cmd" spring-boot:run %*
