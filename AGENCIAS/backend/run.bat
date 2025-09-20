@echo off
set PYTHONUTF8=1
call .venv\Scripts\activate
uvicorn app.main:app --reload --port 8001