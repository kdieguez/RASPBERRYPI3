@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "FRONTEND_PORT=5173"
set "BACKEND_API=http://localhost:8080"

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
    if /i "!arg_name!"=="--api" (
        set "BACKEND_API=!arg_value!"
        shift
        goto parse_args
    )
    if /i "!arg_name!"=="--port" (
        set "FRONTEND_PORT=!arg_value!"
        shift
        goto parse_args
    )
) else (
    if /i "!arg!"=="--api" (
        set "BACKEND_API=%~2"
        shift
        shift
        goto parse_args
    )
    if /i "!arg!"=="--port" (
        set "FRONTEND_PORT=%~2"
        shift
        shift
        goto parse_args
    )
)

shift
goto parse_args

:end_parse

set "PORT=!FRONTEND_PORT!"
set "VITE_API_BASE=!BACKEND_API!"

echo ============================================
echo Frontend: Puerto !FRONTEND_PORT!
echo Backend:  !BACKEND_API!
echo ============================================
echo.

npm run dev

