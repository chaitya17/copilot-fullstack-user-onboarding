#!/bin/bash
# MSSQL Database Backup Script
# Supports both containerized and managed MSSQL Server instances

set -e

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DATABASE_NAME="${DATABASE_NAME:-useronboard}"

# MSSQL Connection Configuration
MSSQL_HOST="${MSSQL_HOST:-localhost}"
MSSQL_PORT="${MSSQL_PORT:-1433}"
MSSQL_USER="${MSSQL_USER:-sa}"
MSSQL_PASSWORD="${MSSQL_PASSWORD}"

# Backup file names
BACKUP_FILE="${BACKUP_DIR}/mssql_${DATABASE_NAME}_${TIMESTAMP}.bak"
BACPAC_FILE="${BACKUP_DIR}/mssql_${DATABASE_NAME}_${TIMESTAMP}.bacpac"

echo "Starting MSSQL backup for database: ${DATABASE_NAME}"
echo "Backup directory: ${BACKUP_DIR}"
echo "Timestamp: ${TIMESTAMP}"

# Create backup directory if it doesn't exist
mkdir -p "${BACKUP_DIR}"

# Function to perform native SQL Server backup (BAK file)
backup_native() {
    echo "Creating native SQL Server backup..."

    sqlcmd -S "${MSSQL_HOST},${MSSQL_PORT}" -U "${MSSQL_USER}" -P "${MSSQL_PASSWORD}" -Q \
        "BACKUP DATABASE [${DATABASE_NAME}] TO DISK = '${BACKUP_FILE}'
         WITH FORMAT, INIT, SKIP, NOREWIND, NOUNLOAD, STATS = 10"

    if [ $? -eq 0 ]; then
        echo "Native backup completed successfully: ${BACKUP_FILE}"
    else
        echo "ERROR: Native backup failed"
        exit 1
    fi
}

# Function to perform BACPAC export (portable format)
backup_bacpac() {
    echo "Creating BACPAC export..."

    # Check if SqlPackage is available
    if command -v SqlPackage &> /dev/null; then
        SqlPackage /Action:Export \
            /SourceServerName:"${MSSQL_HOST},${MSSQL_PORT}" \
            /SourceDatabaseName:"${DATABASE_NAME}" \
            /SourceUser:"${MSSQL_USER}" \
            /SourcePassword:"${MSSQL_PASSWORD}" \
            /TargetFile:"${BACPAC_FILE}" \
            /OverwriteFiles:True

        if [ $? -eq 0 ]; then
            echo "BACPAC export completed successfully: ${BACPAC_FILE}"
        else
            echo "ERROR: BACPAC export failed"
            exit 1
        fi
    else
        echo "WARNING: SqlPackage not found, skipping BACPAC export"
        echo "Install SqlPackage for cross-platform backup compatibility"
    fi
}

# Function for containerized MSSQL backup
backup_container() {
    local container_name="${1:-user-onboard_mssql_1}"

    echo "Creating backup from container: ${container_name}"

    docker exec "${container_name}" /opt/mssql-tools/bin/sqlcmd \
        -S localhost -U "${MSSQL_USER}" -P "${MSSQL_PASSWORD}" -Q \
        "BACKUP DATABASE [${DATABASE_NAME}] TO DISK = '/var/opt/mssql/data/${DATABASE_NAME}_${TIMESTAMP}.bak'
         WITH FORMAT, INIT, SKIP, NOREWIND, NOUNLOAD, STATS = 10"

    # Copy backup from container to host
    docker cp "${container_name}:/var/opt/mssql/data/${DATABASE_NAME}_${TIMESTAMP}.bak" "${BACKUP_FILE}"

    # Clean up backup file in container
    docker exec "${container_name}" rm "/var/opt/mssql/data/${DATABASE_NAME}_${TIMESTAMP}.bak"

    echo "Container backup completed successfully: ${BACKUP_FILE}"
}

# Cleanup old backups
cleanup_old_backups() {
    echo "Cleaning up backups older than ${RETENTION_DAYS} days..."

    find "${BACKUP_DIR}" -name "mssql_${DATABASE_NAME}_*.bak" -type f -mtime +${RETENTION_DAYS} -delete
    find "${BACKUP_DIR}" -name "mssql_${DATABASE_NAME}_*.bacpac" -type f -mtime +${RETENTION_DAYS} -delete

    echo "Cleanup completed"
}

# Verify backup integrity
verify_backup() {
    echo "Verifying backup integrity..."

    sqlcmd -S "${MSSQL_HOST},${MSSQL_PORT}" -U "${MSSQL_USER}" -P "${MSSQL_PASSWORD}" -Q \
        "RESTORE VERIFYONLY FROM DISK = '${BACKUP_FILE}'"

    if [ $? -eq 0 ]; then
        echo "Backup verification successful"
    else
        echo "WARNING: Backup verification failed"
    fi
}

# Main execution
main() {
    case "${1:-native}" in
        "native")
            backup_native
            verify_backup
            ;;
        "bacpac")
            backup_bacpac
            ;;
        "container")
            backup_container "${2}"
            ;;
        "both")
            backup_native
            verify_backup
            backup_bacpac
            ;;
        *)
            echo "Usage: $0 {native|bacpac|container|both} [container_name]"
            echo "  native   - Create native .BAK backup (default)"
            echo "  bacpac   - Create portable .BACPAC export"
            echo "  container- Backup from Docker container"
            echo "  both     - Create both native and BACPAC backups"
            exit 1
            ;;
    esac

    cleanup_old_backups

    echo "MSSQL backup process completed successfully"
    echo "Backup files created:"
    ls -la "${BACKUP_DIR}"/mssql_${DATABASE_NAME}_${TIMESTAMP}.*
}

# Execute main function with all arguments
main "$@"
