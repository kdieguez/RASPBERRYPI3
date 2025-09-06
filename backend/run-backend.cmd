@echo off
cd /d "%~dp0"

:: Credenciales Oracle
set ORACLE_USER=AEROLINEA
set ORACLE_PASSWORD=Aero123
set ORACLE_CONNECT_STRING=localhost:1521/XEPDB1
set DB_POOL=10

:: Configuración de JWT
set JWT_SECRET=super-secreto-cámbialo-por-uno-más-largo-y-unico
set JWT_EXP_MIN=120
set BCRYPT_COST=10

set RECAPTCHA_SECRET=6LdcFMArAAAAAGa1QLVlFnxkuldScup3xuGfaJil


:: Ejecutar backend
java -jar target\aerolineas-backend-1.0.0.jar
