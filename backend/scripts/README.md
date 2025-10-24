# Aura Backend Scripts

This directory contains utility scripts for managing the Aura backend development environment.

## Scripts Overview

| Script | Purpose | Platform |
|--------|---------|----------|
| `reset_db.sh` | Reset MySQL database | macOS/Linux |
| `reset_db.bat` | Reset MySQL database | Windows |
| `reset_oss.sh` | Reset MinIO object storage | macOS/Linux |
| `reset_oss.bat` | Reset MinIO object storage | Windows |
| `init.sql` | Database initialization SQL | All |

## Quick Start

### Database Reset

Drops and recreates the database, then runs the initialization script.

**macOS/Linux:**
```bash
./reset_db.sh
```

**Windows:**
```cmd
reset_db.bat
```

**What it does:**
1. Drops the existing `app` database
2. Creates a new `app` database with UTF-8 encoding
3. Runs `init.sql` to create tables and initial data

### Object Storage Reset

Manages MinIO container and resets the storage bucket.

**macOS/Linux:**
```bash
./reset_oss.sh
```

**Windows:**
```cmd
reset_oss.bat
```

**What it does:**
1. Checks if MinIO container exists, creates if needed
2. Starts MinIO server (ports 9000 for API, 9001 for console)
3. Creates the `aura` bucket
4. Sets bucket to public read access
5. Optionally clears all files in the bucket

## Prerequisites

### For Database Scripts

- **MySQL Client**: `mysql` command must be available
  - macOS: `brew install mysql-client`
  - Windows: Install MySQL or use Docker
  - Linux: `sudo apt-get install mysql-client`

### For Object Storage Scripts

- **Docker**: Docker Desktop must be running
  - Download: https://www.docker.com/products/docker-desktop

- **MinIO Client (mc)**:
  - macOS: `brew install minio-mc`
  - Windows: Download from https://dl.min.io/client/mc/release/windows-amd64/mc.exe
  - Linux: `wget https://dl.min.io/client/mc/release/linux-amd64/mc && chmod +x mc`

## Configuration

### Database Configuration

Default settings (can be customized via environment variables):

```bash
DB_NAME=app
DB_USER=root
DB_PASS=
DB_HOST=localhost
DB_PORT=3306
```

**Example with custom configuration:**
```bash
# macOS/Linux
DB_NAME=myapp DB_PASS=secret ./reset_db.sh

# Windows (set before running)
set DB_NAME=myapp
set DB_PASS=secret
reset_db.bat
```

### MinIO Configuration

Default settings:

```
Endpoint: http://localhost:9000
Console: http://localhost:9001
Username: minioadmin
Password: minioadmin
Bucket: aura
Access: Public read
```

## Accessing Services

### MySQL Database

```bash
# Connect to database
mysql -u root -p app

# Using Docker
docker exec -it mysql_container mysql -u root -p app
```

### MinIO Console

Open in browser: http://localhost:9001

- Username: `minioadmin`
- Password: `minioadmin`

View the `aura` bucket and uploaded files.

### MinIO API

Endpoint: http://localhost:9000

Files are accessible at: `http://localhost:9000/aura/{path}`

Example: `http://localhost:9000/aura/avatars/123/image.jpg`

## Troubleshooting

### Database Scripts

**Error: "mysql: command not found"**
- Install MySQL client or use Docker:
  ```bash
  docker exec -i mysql_container mysql -uroot -p app < init.sql
  ```

**Error: "Access denied"**
- Check your database credentials
- Update DB_USER and DB_PASS environment variables

### Object Storage Scripts

**Error: "mc: command not found"**
- Install MinIO client (see Prerequisites)

**Error: "Cannot connect to MinIO"**
- Ensure Docker Desktop is running
- Check if port 9000/9001 are available
- View logs: `docker logs minio`

**Error: "docker: command not found"**
- Install Docker Desktop
- Ensure Docker is running

### Port Conflicts

If ports 9000/9001 are already in use:
- Stop the conflicting service
- Or modify the ports in the script:
  ```bash
  # Change -p 9000:9000 to -p 9002:9000
  docker run -d --name minio -p 9002:9000 -p 9003:9001 ...
  ```

## Development Workflow

### Initial Setup

1. Reset the database:
   ```bash
   ./reset_db.sh
   ```

2. Setup MinIO:
   ```bash
   ./reset_oss.sh
   ```

3. Start the backend application:
   ```bash
   cd ..
   mvn spring-boot:run
   ```

### Clean Slate Reset

To start fresh with clean data:

```bash
# Reset both database and storage
./reset_db.sh && ./reset_oss.sh
```

### Testing with Permanent Test User

A permanent test user is automatically created when you run `init.sql`:

**Test User Credentials:**
- Email: `test@aura.dev`
- Password: `Test123456`
- User ID: `1000`

**Get Permanent Access Token:**

```bash
curl http://localhost:8080/test/token/permanent
```

**Response:**
```json
{
  "code": 1,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAwIiwiZGV2aWNlSWQiOiJ0ZXN0LWRldmljZSIsImlhdCI6MTc2MTI4MTc5MiwiZXhwIjo0OTE0ODgxNzkyfQ.ggFnlNqCzgN17Q9torL5HVeoudGhbp1_GqSM3vAMXdw",
    "userId": 1000,
    "expiresInYears": 100,
    "note": "This token is valid for 100 years. Use for testing only!"
  }
}
```

**Permanent Test Token (valid for 100 years):**
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAwIiwiZGV2aWNlSWQiOiJ0ZXN0LWRldmljZSIsImlhdCI6MTc2MTI4MTc5MiwiZXhwIjo0OTE0ODgxNzkyfQ.ggFnlNqCzgN17Q9torL5HVeoudGhbp1_GqSM3vAMXdw
```

**Test File Upload:**

```bash
curl -X POST http://localhost:8080/files/avatar \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAwIiwiZGV2aWNlSWQiOiJ0ZXN0LWRldmljZSIsImlhdCI6MTc2MTI4MTc5MiwiZXhwIjo0OTE0ODgxNzkyfQ.ggFnlNqCzgN17Q9torL5HVeoudGhbp1_GqSM3vAMXdw" \
  -F "file=@/path/to/image.jpg"
```

**Verify upload in MinIO Console:** http://localhost:9001

## Files

### init.sql

Database initialization script containing:
- Table schemas (users, posts, comments, etc.)
- Indexes and foreign keys
- Initial seed data (if any)

**Manual execution:**
```bash
mysql -u root -p app < init.sql
```

## Notes

- Scripts are safe to run multiple times
- Database reset will **DELETE ALL DATA**
- OSS reset asks for confirmation before clearing files
- MinIO container data persists in `/tmp/minio/data` (macOS/Linux) or `%TEMP%\minio\data` (Windows)
- All scripts include error handling and status messages
- MinIO bucket is set to public read access for easy file serving

## Additional Resources

- [MySQL Documentation](https://dev.mysql.com/doc/)
- [MinIO Documentation](https://min.io/docs/minio/linux/index.html)
- [Docker Documentation](https://docs.docker.com/)
