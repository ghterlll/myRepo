#!/bin/bash

# ============================================
# Database Reset Script for Aura
# ============================================
# This script drops and recreates the database,
# then runs the initialization script.
#
# Usage: ./reset_db.sh [options]
# Options:
#   -h, --help              Show this help message
#   -y, --yes               Skip confirmation prompt
#   --db-name <name>        Database name (default: app)
#   --db-user <user>        Database user (default: root)
#   --db-pass <password>    Database password (default: root)
#   --db-host <host>        Database host (default: localhost)
#   --db-port <port>        Database port (default: 3306)
#
# Example:
#   ./reset_db.sh --yes --db-name my_app --db-pass secret
# ============================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
DB_NAME="${DB_NAME:-app}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
SKIP_CONFIRM=false

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INIT_SQL="${SCRIPT_DIR}/init.sql"

# ============================================
# Functions
# ============================================

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_help() {
    cat << EOF
Database Reset Script for Aura

Usage: $0 [options]

Options:
  -h, --help              Show this help message
  -y, --yes               Skip confirmation prompt
  --db-name <name>        Database name (default: app)
  --db-user <user>        Database user (default: root)
  --db-pass <password>    Database password (default: root)
  --db-host <host>        Database host (default: localhost)
  --db-port <port>        Database port (default: 3306)

Environment Variables:
  You can also set these environment variables:
  - DB_NAME
  - DB_USER
  - DB_PASS
  - DB_HOST
  - DB_PORT

Example:
  $0 --yes --db-name my_app --db-pass secret

  DB_NAME=my_app DB_PASS=secret $0 --yes

EOF
}

check_mysql_command() {
    if ! command -v mysql &> /dev/null; then
        print_error "mysql command not found. Please install MySQL client."
        exit 1
    fi
}

check_init_sql() {
    if [ ! -f "$INIT_SQL" ]; then
        print_error "init.sql not found at: $INIT_SQL"
        exit 1
    fi
}

test_connection() {
    print_info "Testing database connection..."
    if [ -z "$DB_PASS" ]; then
        if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -e "SELECT 1;" &> /dev/null; then
            print_success "Database connection successful"
            return 0
        else
            print_error "Cannot connect to database. Please check your credentials."
            print_info "Host: $DB_HOST:$DB_PORT"
            print_info "User: $DB_USER"
            return 1
        fi
    else
        if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" -e "SELECT 1;" &> /dev/null; then
            print_success "Database connection successful"
            return 0
        else
            print_error "Cannot connect to database. Please check your credentials."
            print_info "Host: $DB_HOST:$DB_PORT"
            print_info "User: $DB_USER"
            return 1
        fi
    fi
}

confirm_reset() {
    if [ "$SKIP_CONFIRM" = true ]; then
        return 0
    fi

    echo ""
    print_warning "This will DROP and RECREATE the database: $DB_NAME"
    print_warning "ALL DATA WILL BE LOST!"
    echo ""
    read -p "Are you sure you want to continue? (yes/no): " response

    if [ "$response" != "yes" ]; then
        print_info "Operation cancelled."
        exit 0
    fi
}

drop_database() {
    print_info "Dropping database if exists: $DB_NAME"
    if [ -z "$DB_PASS" ]; then
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -e "DROP DATABASE IF EXISTS \`$DB_NAME\`;"
    else
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" -e "DROP DATABASE IF EXISTS \`$DB_NAME\`;"
    fi
    print_success "Database dropped"
}

create_database() {
    print_info "Creating database: $DB_NAME"
    if [ -z "$DB_PASS" ]; then
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -e "CREATE DATABASE \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    else
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" -e "CREATE DATABASE \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    fi
    print_success "Database created"
}

run_init_script() {
    print_info "Running initialization script: $INIT_SQL"
    if [ -z "$DB_PASS" ]; then
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" "$DB_NAME" < "$INIT_SQL"
    else
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$INIT_SQL"
    fi
    print_success "Initialization script completed"
}

show_summary() {
    echo ""
    echo "============================================"
    print_success "Database reset completed successfully!"
    echo "============================================"
    echo ""
    print_info "Database Configuration:"
    echo "  Host:     $DB_HOST:$DB_PORT"
    echo "  Database: $DB_NAME"
    echo "  User:     $DB_USER"
    echo ""
    print_info "You can now start your application."
    echo ""
}

# ============================================
# Parse command line arguments
# ============================================

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            print_help
            exit 0
            ;;
        -y|--yes)
            SKIP_CONFIRM=true
            shift
            ;;
        --db-name)
            DB_NAME="$2"
            shift 2
            ;;
        --db-user)
            DB_USER="$2"
            shift 2
            ;;
        --db-pass)
            DB_PASS="$2"
            shift 2
            ;;
        --db-host)
            DB_HOST="$2"
            shift 2
            ;;
        --db-port)
            DB_PORT="$2"
            shift 2
            ;;
        *)
            print_error "Unknown option: $1"
            print_help
            exit 1
            ;;
    esac
done

# ============================================
# Main execution
# ============================================

echo ""
echo "============================================"
echo "  Aura Database Reset Script"
echo "============================================"
echo ""

# Pre-flight checks
check_mysql_command
check_init_sql
test_connection || exit 1

# Confirm operation
confirm_reset

echo ""
print_info "Starting database reset..."
echo ""

# Execute reset steps
drop_database
create_database
run_init_script

# Show summary
show_summary

exit 0
