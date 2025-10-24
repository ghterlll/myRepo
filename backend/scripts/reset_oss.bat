@echo off
REM ============================================
REM MinIO Reset Script for Aura (Windows)
REM ============================================
REM This script manages MinIO container and bucket
REM
REM Requirements:
REM   - Docker Desktop running
REM   - MinIO client (mc.exe) installed
REM     Download from: https://dl.min.io/client/mc/release/windows-amd64/mc.exe
REM ============================================

setlocal enabledelayedexpansion

REM Configuration
set MINIO_ALIAS=local
set MINIO_ENDPOINT=http://localhost:9000
set MINIO_ACCESS_KEY=minioadmin
set MINIO_SECRET_KEY=minioadmin
set BUCKET_NAME=aura

echo ============================================
echo Aura OSS Reset Tool (Windows)
echo ============================================
echo.

REM Check if mc is installed
where mc >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] MinIO client ^(mc^) not found
    echo Please download and install it:
    echo   1. Download from: https://dl.min.io/client/mc/release/windows-amd64/mc.exe
    echo   2. Place mc.exe in a directory in your PATH
    echo   3. Or place it in the current directory
    exit /b 1
)

REM Check if Docker is running
docker info >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running
    echo Please start Docker Desktop first
    exit /b 1
)

echo [1/5] Checking MinIO server status...

REM Check if MinIO container exists and is running
docker ps -q -f name=minio >nul 2>nul
if %errorlevel% equ 0 (
    echo [OK] MinIO container is already running
) else (
    docker ps -aq -f name=minio >nul 2>nul
    if %errorlevel% equ 0 (
        echo [INFO] MinIO container exists but is not running
        echo Starting existing container...
        docker start minio >nul
        echo [OK] MinIO container started
    ) else (
        echo [INFO] MinIO container does not exist
        echo Creating and starting MinIO container...
        docker run -d --name minio ^
            -p 9000:9000 -p 9001:9001 ^
            -e MINIO_ROOT_USER=minioadmin ^
            -e MINIO_ROOT_PASSWORD=minioadmin ^
            -v %TEMP%\minio\data:/data ^
            minio/minio server /data --console-address ":9001"
        echo [OK] MinIO container created and started
    )
)

echo.
echo [2/5] Waiting for MinIO to be ready...
set /a retries=30
:wait_loop
curl -s --connect-timeout 2 %MINIO_ENDPOINT% >nul 2>nul
if %errorlevel% equ 0 (
    echo [OK] MinIO is ready at %MINIO_ENDPOINT%
    goto minio_ready
)
echo | set /p=.
timeout /t 1 /nobreak >nul
set /a retries-=1
if %retries% gtr 0 goto wait_loop

echo.
echo [ERROR] Failed to connect to MinIO after 30 seconds
echo Checking Docker logs...
docker logs minio 2>&1 | more
exit /b 1

:minio_ready
echo.

echo.
echo [3/5] Configuring MinIO client...
mc alias set %MINIO_ALIAS% %MINIO_ENDPOINT% %MINIO_ACCESS_KEY% %MINIO_SECRET_KEY% --api S3v4 >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Failed to configure MinIO client
    exit /b 1
)
echo [OK] Connected to MinIO server

echo.
echo [4/5] Ensuring bucket exists...
mc ls %MINIO_ALIAS%/%BUCKET_NAME% >nul 2>nul
if %errorlevel% neq 0 (
    mc mb %MINIO_ALIAS%/%BUCKET_NAME% >nul
    echo [OK] Created bucket '%BUCKET_NAME%'
) else (
    echo [OK] Bucket '%BUCKET_NAME%' already exists
)

echo.
echo [5/5] Setting bucket policy to public read...
mc anonymous set download %MINIO_ALIAS%/%BUCKET_NAME% >nul
echo [OK] Bucket '%BUCKET_NAME%' is now publicly readable

echo.
REM Confirmation for clearing
set /p CONFIRM="WARNING: Delete ALL content in MinIO bucket '%BUCKET_NAME%'? (yes/no): "
if /i not "%CONFIRM%"=="yes" (
    echo Operation cancelled.
    goto summary
)

echo.
echo Clearing bucket '%BUCKET_NAME%'...
mc rm -r --force %MINIO_ALIAS%/%BUCKET_NAME%/ >nul 2>nul
echo [OK] Bucket cleared

:summary
echo.
echo ============================================
echo OSS Reset Complete!
echo ============================================
echo.
echo Summary:
echo   - Bucket name: %BUCKET_NAME%
echo   - Bucket status: Ready
echo   - MinIO Console: http://localhost:9001
echo   - MinIO API: http://localhost:9000
echo   - Username: minioadmin
echo   - Password: minioadmin
echo.
echo [OK] MinIO container is running
echo.

endlocal
