@echo off
echo ===================================================
echo   MediConnect AI - Starting Spring Boot Application
echo ===================================================
echo URL: http://localhost:8080
echo Database: PostgreSQL (mediconnect_db)
echo Flyway Migrations: Enabled
echo ===================================================

.\maven\bin\mvn.cmd spring-boot:run
pause
