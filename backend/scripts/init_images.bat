@echo off
setlocal enabledelayedexpansion

REM ============================================
REM MinIO Image Initialization Script
REM ============================================
REM This script uploads seed images from Android assets to MinIO
REM 
REM Prerequisites:
REM   - MinIO Client (mc) installed
REM   - MinIO server running (or will be started)
REM   - Images in: Aura\app\src\main\assets\images\
REM
REM Usage:
REM   init_images.bat

REM Configuration
set MINIO_ALIAS=local
set MINIO_ENDPOINT=http://localhost:9000
set MINIO_ACCESS_KEY=minioadmin
set MINIO_SECRET_KEY=minioadmin
set BUCKET_NAME=aura
set UPLOAD_PREFIX=posts/seed

REM Get script directory and repository root
set SCRIPT_DIR=%~dp0
set BACKEND_DIR=%SCRIPT_DIR%..
set REPO_ROOT=%BACKEND_DIR%\..
set IMAGES_DIR=%REPO_ROOT%\Aura\app\src\main\assets\images

echo ============================================
echo MinIO Image Upload Tool
echo ============================================
echo.
echo Configuration:
echo   MinIO: %MINIO_ENDPOINT%
echo   Bucket: %BUCKET_NAME%
echo   Upload Path: %UPLOAD_PREFIX%/
echo   Source: %IMAGES_DIR%
echo.

REM [1/6] Check MinIO Client
echo [1/6] Checking MinIO client...
where mc >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] MinIO client 'mc' not found
    echo.
    echo Please install MinIO client:
    echo   Download from: https://dl.min.io/client/mc/release/windows-amd64/mc.exe
    echo   Place mc.exe in your PATH or in this directory
    echo.
    pause
    exit /b 1
)
echo [OK] MinIO client found

REM [2/6] Check source images directory
echo.
echo [2/6] Checking source images...
if not exist "%IMAGES_DIR%" (
    echo [ERROR] Images directory not found: %IMAGES_DIR%
    pause
    exit /b 1
)

set IMAGE_COUNT=0
for %%f in ("%IMAGES_DIR%\*.png" "%IMAGES_DIR%\*.jpg" "%IMAGES_DIR%\*.jpeg") do (
    set /a IMAGE_COUNT+=1
)

if %IMAGE_COUNT% equ 0 (
    echo [ERROR] No images found in: %IMAGES_DIR%
    pause
    exit /b 1
)

echo [OK] Found %IMAGE_COUNT% image(s)

REM [3/6] Check/Start MinIO container
echo.
echo [3/6] Checking MinIO server...
docker ps -q -f name=minio >nul 2>nul
if %errorlevel% equ 0 (
    echo [OK] MinIO container is running
) else (
    docker ps -aq -f name=minio >nul 2>nul
    if %errorlevel% equ 0 (
        echo [INFO] MinIO container exists but is not running
        echo Starting container...
        docker start minio >nul
        timeout /t 3 /nobreak >nul
        echo [OK] MinIO container started
    ) else (
        echo [INFO] MinIO container does not exist
        echo Creating MinIO container...
        docker run -d --name minio ^
            -p 9000:9000 -p 9001:9001 ^
            -e MINIO_ROOT_USER=minioadmin ^
            -e MINIO_ROOT_PASSWORD=minioadmin ^
            -v %TEMP%\minio\data:/data ^
            minio/minio server /data --console-address ":9001"
        timeout /t 5 /nobreak >nul
        echo [OK] MinIO container created
    )
)

REM Wait for MinIO to be ready
echo Waiting for MinIO...
set retries=10
:wait_loop
curl -s --connect-timeout 2 %MINIO_ENDPOINT% >nul 2>nul
if %errorlevel% equ 0 (
    echo [OK] MinIO is ready
    goto minio_ready
)
echo | set /p=.
timeout /t 1 /nobreak >nul
set /a retries-=1
if %retries% gtr 0 goto wait_loop

echo.
echo [ERROR] MinIO did not start in time
pause
exit /b 1

:minio_ready
echo.

REM [4/6] Configure MinIO client
echo [4/6] Configuring MinIO client...
mc alias set %MINIO_ALIAS% %MINIO_ENDPOINT% %MINIO_ACCESS_KEY% %MINIO_SECRET_KEY% --api S3v4 >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Failed to configure MinIO client
    pause
    exit /b 1
)
echo [OK] Connected to MinIO

REM [5/6] Ensure bucket exists
echo.
echo [5/6] Checking bucket...
mc ls %MINIO_ALIAS%/%BUCKET_NAME% >nul 2>nul
if %errorlevel% neq 0 (
    echo [INFO] Creating bucket '%BUCKET_NAME%'...
    mc mb %MINIO_ALIAS%/%BUCKET_NAME% >nul
    mc anonymous set download %MINIO_ALIAS%/%BUCKET_NAME% >nul
    echo [OK] Bucket created and set to public read
) else (
    echo [OK] Bucket '%BUCKET_NAME%' exists
)

REM [6/6] Upload images
echo.
echo [6/6] Uploading images...
echo.

set UPLOADED=0
set FAILED=0

for %%f in ("%IMAGES_DIR%\*.png" "%IMAGES_DIR%\*.jpg" "%IMAGES_DIR%\*.jpeg") do (
    set FILENAME=%%~nxf
    echo   Uploading: !FILENAME!
    mc cp "%%f" %MINIO_ALIAS%/%BUCKET_NAME%/%UPLOAD_PREFIX%/!FILENAME! >nul 2>nul
    if !errorlevel! equ 0 (
        set /a UPLOADED+=1
        echo   [OK] %MINIO_ENDPOINT%/%BUCKET_NAME%/%UPLOAD_PREFIX%/!FILENAME!
    ) else (
        set /a FAILED+=1
        echo   [FAILED] Could not upload !FILENAME!
    )
)

echo.
echo ============================================
echo Upload Complete
echo ============================================
echo   Total images: %IMAGE_COUNT%
echo   Uploaded: %UPLOADED%
echo   Failed: %FAILED%
echo.
echo MinIO Console: http://localhost:9001
echo   Username: minioadmin
echo   Password: minioadmin
echo.
echo Browse uploaded images:
echo   http://localhost:9001/browser/aura/%UPLOAD_PREFIX%
echo.

REM List uploaded files
echo Uploaded files:
mc ls %MINIO_ALIAS%/%BUCKET_NAME%/%UPLOAD_PREFIX%/
echo.

echo [OK] Image initialization complete!
echo.
pause
endlocal
