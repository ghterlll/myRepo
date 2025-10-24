@echo off
REM ============================================
REM Database Reset Script for Aura (Windows)
REM ============================================
REM This script drops and recreates the database,
REM then runs the initialization script.
REM
REM Requirements:
REM   - MySQL installed and mysql.exe in PATH
REM   - Docker Desktop running (if using Docker MySQL)
REM ============================================

setlocal enabledelayedexpansion

REM Default configuration
set DB_NAME=app
set DB_USER=root
set DB_PASS=
set DB_HOST=localhost
set DB_PORT=3306

REM Get script directory
set SCRIPT_DIR=%~dp0
set INIT_SQL=%SCRIPT_DIR%init.sql

echo ============================================
echo Database Reset Tool for Aura
echo ============================================
echo.

REM Check if mysql is available
where mysql >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] MySQL client not found in PATH
    echo Please install MySQL or add it to your PATH
    echo.
    echo For Docker users, you can run:
    echo   docker exec -i mysql_container_name mysql -uroot -p%DB_NAME% ^< "%INIT_SQL%"
    exit /b 1
)

REM Display configuration
echo Configuration:
echo   Database Name: %DB_NAME%
echo   Database User: %DB_USER%
echo   Database Host: %DB_HOST%
echo   Database Port: %DB_PORT%
echo   Init SQL File: %INIT_SQL%
echo.

REM Confirmation
set /p CONFIRM="WARNING: This will DELETE ALL DATA in database '%DB_NAME%'. Continue? (yes/no): "
if /i not "%CONFIRM%"=="yes" (
    echo Operation cancelled.
    exit /b 0
)

echo.
echo [1/3] Dropping database '%DB_NAME%'...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -e "DROP DATABASE IF EXISTS %DB_NAME%;"
if %errorlevel% neq 0 (
    echo [ERROR] Failed to drop database
    exit /b 1
)
echo [OK] Database dropped

echo.
echo [2/3] Creating database '%DB_NAME%'...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -e "CREATE DATABASE %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if %errorlevel% neq 0 (
    echo [ERROR] Failed to create database
    exit /b 1
)
echo [OK] Database created

echo.
echo [3/3] Running initialization script...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% %DB_NAME% < "%INIT_SQL%"
if %errorlevel% neq 0 (
    echo [ERROR] Failed to run initialization script
    exit /b 1
)
echo [OK] Initialization script completed

echo.
echo ============================================
echo Database Reset Complete!
echo ============================================
echo   Database '%DB_NAME%' is ready to use
echo.

endlocal
