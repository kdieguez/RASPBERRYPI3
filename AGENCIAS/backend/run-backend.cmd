@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

:: Parámetros soportados 
::   --port=8001 
::   --mongo-db=agencia_viajes 
::   --mongo-uri=<URI>        
::   --aerolineas-api=http://localhost:8080  
::   .\run-backend.cmd
::   .\run-backend.cmd --port=8002 --mongo-db=agencia2
::   .\run-backend.cmd --port=8003 --mongo-db=agencia3 --aerolineas-api=http://localhost:8081
::   .\run-backend.cmd --mongo-uri="mongodb+srv://user:pass@cluster/?..." --mongo-db=agencia4


:: Valores por defecto
set "BACKEND_PORT=8001"
:: Si no se especifica MONGO_DB, el código usa "agencia_viajes"
set "MONGO_DB_NAME="
set "MONGO_URI_ARG="
set "AEROLINEAS_API_URL_ARG="

:: Parsear argumentos - soporta --arg=value y --arg value
:parse_args
if "%~1"=="" goto end_parse

set "arg=%~1"
set "arg=!arg:"=!"

echo !arg! | findstr /C:"=" >nul
if !errorlevel! equ 0 (
    for /f "tokens=1,* delims==" %%a in ("!arg!") do (
        set "arg_name=%%a"
        set "arg_value=%%b"
    )
) else (
    set "arg_name=!arg!"
    set "arg_value=%~2"
)

if /i "!arg_name!"=="--port" (
    if not "!arg_value!"=="" set "BACKEND_PORT=!arg_value!"
    if "!arg!"=="!arg_name!" shift
    shift
    goto parse_args
)

if /i "!arg_name!"=="--mongo-db" (
    if not "!arg_value!"=="" set "MONGO_DB_NAME=!arg_value!"
    if "!arg!"=="!arg_name!" shift
    shift
    goto parse_args
)

if /i "!arg_name!"=="--mongo-uri" (
    if not "!arg_value!"=="" set "MONGO_URI_ARG=!arg_value!"
    if "!arg!"=="!arg_name!" shift
    shift
    goto parse_args
)

if /i "!arg_name!"=="--aerolineas-api" (
    if not "!arg_value!"=="" set "AEROLINEAS_API_URL_ARG=!arg_value!"
    if "!arg!"=="!arg_name!" shift
    shift
    goto parse_args
)

shift
goto parse_args

:end_parse

:: Exportar variables de entorno para FastAPI
set "PORT=%BACKEND_PORT%"
if defined MONGO_DB_NAME set "MONGO_DB=%MONGO_DB_NAME%"
if defined MONGO_URI_ARG set "MONGO_URI=%MONGO_URI_ARG%"
if defined AEROLINEAS_API_URL_ARG set "AEROLINEAS_API_URL=%AEROLINEAS_API_URL_ARG%"

echo ============================================
echo Agencias BACKEND
echo Puerto FastAPI : %BACKEND_PORT%
if defined MONGO_DB_NAME echo Mongo DB        : %MONGO_DB_NAME%
if defined MONGO_URI_ARG echo Mongo URI       : %MONGO_URI_ARG%
if defined AEROLINEAS_API_URL_ARG echo Aerolineas API : %AEROLINEAS_API_URL_ARG%
echo ============================================
echo.

set PYTHONUTF8=1
call .venv\Scripts\activate
uvicorn app.main:app --reload --port %BACKEND_PORT%




