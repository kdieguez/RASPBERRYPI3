@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

:: Parámetros soportados:
::   --api=http://localhost:8001   
::   --port=5174                   

::   .\run-frontend.cmd
::   .\run-frontend.cmd --api=http://localhost:8001 --port=5174
::   .\run-frontend.cmd --api=http://localhost:8002 --port=5175


set "FRONTEND_PORT=5174"
set "BACKEND_API=http://localhost:8001"

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

if /i "!arg_name!"=="--api" (
    if not "!arg_value!"=="" set "BACKEND_API=!arg_value!"
    if "!arg!"=="!arg_name!" shift
    shift
    goto parse_args
)

if /i "!arg_name!"=="--port" (
    if not "!arg_value!"=="" set "FRONTEND_PORT=!arg_value!"
    if "!arg!"=="!arg_name!" shift
    shift
    goto parse_args
)

shift
goto parse_args

:end_parse

set "PORT=%FRONTEND_PORT%"
set "VITE_API_URL=%BACKEND_API%"

echo ============================================
echo Agencias FRONTEND
echo Puerto frontend : %FRONTEND_PORT%
echo Backend API     : %BACKEND_API%
echo ============================================
echo.

npm run dev




