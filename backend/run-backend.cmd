@echo off
cd /d "%~dp0"

:: ============================================
:: CONFIGURACIÓN DINÁMICA
:: ============================================
:: IMPORTANTE: En Oracle, cada aerolínea debe tener su propio usuario
:: (un usuario = un schema en Oracle)
::
:: Puedes pasar los parámetros como:
::   1. Argumentos: --user=AEROLINEA_2 --password=Aero123 --schema=AEROLINEA_2 --port=8081
::   2. Variables de entorno: set ORACLE_USER=AEROLINEA_2, etc.
::   3. System properties: -Doracle.user=AEROLINEA_2, etc.
::
:: Si solo especificas --schema=, se usará como usuario también
:: ============================================

:: Credenciales Oracle (valores por defecto, pueden sobrescribirse con argumentos)
set ORACLE_USER=AEROLINEA
set ORACLE_PASSWORD=Aero123
set ORACLE_CONNECT_STRING=localhost:1521/XEPDB1
:: Schema: Si no se especifica, usa ORACLE_USER como fallback
:: set ORACLE_SCHEMA=AEROLINEA
set DB_POOL=10

:: Configuración de JWT
set JWT_SECRET=supercalifragilisticoespialidoso
set JWT_EXP_MIN=120
set BCRYPT_COST=10

set RECAPTCHA_SECRET=6LdcFMArAAAAAGa1QLVlFnxkuldScup3xuGfaJil

:: SMTP - GMAIL
set "MAIL_HOST=smtp.gmail.com"
set "MAIL_PORT=587"
set "MAIL_USER=kds2games@gmail.com"
set "MAIL_PASS=ncsanoaulkcdgics"
set "MAIL_FROM=Aerolíneas <kds2games@gmail.com>"
set "MAIL_TLS=true"
set "MAIL_AUTH=true"
set "MAIL_DEBUG=true"

:: Ejecutar backend
:: Ejemplos:
::   Solo schema (usará schema como usuario): --schema=AEROLINEA_2 --password=Aero123 --port=8081
::   Usuario y schema explícitos: --user=AEROLINEA_2 --password=Aero123 --schema=AEROLINEA_2 --port=8081
::   Con variables de entorno: set ORACLE_USER=AEROLINEA_2 y luego ejecutar sin argumentos
java -jar target\aerolineas-backend-1.0.0.jar %*
