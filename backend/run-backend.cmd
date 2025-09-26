@echo off
cd /d "%~dp0"

:: Credenciales Oracle
set ORACLE_USER=AEROLINEA
set ORACLE_PASSWORD=Aero123
set ORACLE_CONNECT_STRING=localhost:1521/XEPDB1
set DB_POOL=10

:: Configuración de JWT
set JWT_SECRET=supercalifragilisticoespialidoso
set JWT_EXP_MIN=120
set BCRYPT_COST=10

set RECAPTCHA_SECRET=6LdcFMArAAAAAGa1QLVlFnxkuldScup3xuGfaJil

:: ===== SMTP - GMAIL =====
set "MAIL_HOST=smtp.gmail.com"
set "MAIL_PORT=587"
set "MAIL_USER=kds2games@gmail.com"
set "MAIL_PASS=ncsa noau lkcd gics"
set "MAIL_FROM=Aerolíneas <kds2games@gmail.com>"
set "MAIL_TLS=true"
set "MAIL_AUTH=true"

:: Ejecutar backend
java -jar target\aerolineas-backend-1.0.0.jar
