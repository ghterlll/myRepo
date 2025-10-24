#!/bin/bash
# Reset and upload Aura assets to MinIO
# This script requires MinIO client (mc) to be installed

# Configuration
MINIO_ALIAS="local"
MINIO_ENDPOINT="http://localhost:9000"
MINIO_ACCESS_KEY="minioadmin"
MINIO_SECRET_KEY="minioadmin"
BUCKET_NAME="aura"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "Aura OSS Reset Tool"
echo "========================================"

# Check if mc is installed
if ! command -v mc &> /dev/null; then
    echo -e "${RED}Error: MinIO client (mc) not found${NC}"
    echo "Please install it first:"
    echo "  brew install minio-mc"
    echo "  or"
    echo "  wget https://dl.min.io/client/mc/release/darwin-amd64/mc"
    echo "  chmod +x mc"
    echo "  sudo mv mc /usr/local/bin/"
    exit 1
fi

# Check if MinIO container exists and is running
echo -e "\n${YELLOW}Checking MinIO server status...${NC}"
if docker ps -q -f name=minio | grep -q .; then
    echo -e "${GREEN}✓ MinIO container is already running${NC}"
elif docker ps -aq -f name=minio | grep -q .; then
    echo -e "${YELLOW}⚠ MinIO container exists but is not running${NC}"
    echo "Starting existing container..."
    docker start minio
    echo -e "${GREEN}✓ MinIO container started${NC}"
else
    echo -e "${YELLOW}ℹ MinIO container does not exist${NC}"
    echo "Creating and starting MinIO container..."
    docker run -d --name minio \
        -p 9000:9000 -p 9001:9001 \
        -e MINIO_ROOT_USER=minioadmin \
        -e MINIO_ROOT_PASSWORD=minioadmin \
        -v /tmp/minio/data:/data \
        minio/minio server /data --console-address ":9001"
    echo -e "${GREEN}✓ MinIO container created and started${NC}"
fi

# Wait for MinIO to be ready
echo "Waiting for MinIO to be ready..."
retries=30
while [ $retries -gt 0 ]; do
    if curl -s --connect-timeout 2 ${MINIO_ENDPOINT} > /dev/null 2>&1; then
        echo -e "${GREEN}✓ MinIO is ready at ${MINIO_ENDPOINT}${NC}"
        break
    fi
    echo -n "."
    sleep 1
    ((retries--))
done
echo ""

if [ $retries -eq 0 ]; then
    echo -e "${RED}✗ Failed to connect to MinIO after 30 seconds${NC}"
    echo "Checking Docker logs..."
    docker logs minio 2>&1 | tail -20
    exit 1
fi

# Configure MinIO client
echo -e "\n${YELLOW}Configuring MinIO client...${NC}"
mc alias set $MINIO_ALIAS $MINIO_ENDPOINT $MINIO_ACCESS_KEY $MINIO_SECRET_KEY --api S3v4

# Verify connection
if ! mc ls $MINIO_ALIAS &> /dev/null; then
    echo -e "${RED}Error: Cannot connect to MinIO server${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Connected to MinIO server${NC}"

# Create bucket if it doesn't exist
echo -e "\n${YELLOW}Ensuring bucket exists...${NC}"
if ! mc ls $MINIO_ALIAS/$BUCKET_NAME &> /dev/null; then
    mc mb $MINIO_ALIAS/$BUCKET_NAME
    echo -e "${GREEN}✓ Created bucket '$BUCKET_NAME'${NC}"
else
    echo -e "${GREEN}✓ Bucket '$BUCKET_NAME' already exists${NC}"
fi

# Set bucket policy to public read (download)
echo -e "\n${YELLOW}Setting bucket policy to public read...${NC}"
mc anonymous set download $MINIO_ALIAS/$BUCKET_NAME
echo -e "${GREEN}✓ Bucket '$BUCKET_NAME' is now publicly readable${NC}"

# Confirm before clearing
echo -e "\n${RED}WARNING: This will delete ALL content in MinIO bucket '$BUCKET_NAME'${NC}"
read -p "Continue? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
    echo "Operation cancelled"
    exit 0
fi

# Clear existing content
echo -e "\n${YELLOW}Clearing bucket '$BUCKET_NAME'...${NC}"
mc rm -r --force $MINIO_ALIAS/$BUCKET_NAME/ 2>/dev/null || true
echo -e "${GREEN}✓ Bucket cleared${NC}"

# Verify bucket is empty
echo -e "\n${YELLOW}Verifying bucket is empty...${NC}"
total_objects=$(mc ls -r $MINIO_ALIAS/$BUCKET_NAME/ 2>/dev/null | wc -l)
echo -e "${GREEN}✓ Bucket now contains $total_objects objects${NC}"

echo -e "\n${GREEN}================================${NC}"
echo -e "${GREEN}OSS Reset Complete!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo "Summary:"
echo "  - Bucket name: ${BUCKET_NAME}"
echo "  - Bucket status: ${GREEN}Ready${NC}"
echo "  - Total objects: ${total_objects}"
echo ""
echo -e "${GREEN}✓ Bucket '${BUCKET_NAME}' is ready for use${NC}"
echo ""
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}MinIO Management Helper${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "To start MinIO server:"
echo ""
echo -e "${GREEN}docker run -d --name minio \\${NC}"
echo -e "${GREEN}  -p 9000:9000 -p 9001:9001 \\${NC}"
echo -e "${GREEN}  -e MINIO_ROOT_USER=minioadmin \\${NC}"
echo -e "${GREEN}  -e MINIO_ROOT_PASSWORD=minioadmin \\${NC}"
echo -e "${GREEN}  -v /tmp/minio/data:/data \\${NC}"
echo -e "${GREEN}  minio/minio server /data --console-address \":9001\"${NC}"
echo ""
echo "MinIO Console: http://localhost:9001"
echo "MinIO API: http://localhost:9000"
echo ""

# Check if MinIO container is running
if docker ps -q -f name=minio | grep -q .; then
    echo -e "${GREEN}✓ MinIO container is currently running${NC}"
elif docker ps -aq -f name=minio | grep -q .; then
    echo -e "${YELLOW}⚠ MinIO container exists but is not running${NC}"
    echo "  To start: docker start minio"
else
    echo -e "${YELLOW}ℹ MinIO container does not exist${NC}"
    echo "  Use the command above to create it"
fi
