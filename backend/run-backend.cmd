@echo off
cd /d "%~dp0"

set ORACLE_USER=AEROLINEA
set ORACLE_PASSWORD=Aero123
set ORACLE_CONNECT_STRING=localhost:1521/XEPDB1
set DB_POOL=10

java -jar target\aerolineas-backend-1.0.0.jar
