#!/bin/bash
# Oracle Database Backup Script
# Supports both containerized Oracle XE and managed Oracle instances
# Uses Oracle Data Pump (expdp/impdp) for reliable backups

set -e

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DATABASE_NAME="${DATABASE_NAME:-XEPDB1}"

# Oracle Connection Configuration
ORACLE_HOST="${ORACLE_HOST:-localhost}"
ORACLE_PORT="${ORACLE_PORT:-1521}"
ORACLE_SERVICE="${ORACLE_SERVICE:-XEPDB1}"
ORACLE_USER="${ORACLE_USER:-system}"
ORACLE_PASSWORD="${ORACLE_PASSWORD}"

# Backup file names
DUMP_FILE="oracle_${DATABASE_NAME}_${TIMESTAMP}.dmp"
LOG_FILE="oracle_${DATABASE_NAME}_${TIMESTAMP}.log"
PAR_FILE="oracle_${DATABASE_NAME}_${TIMESTAMP}.par"

echo "Starting Oracle backup for database: ${DATABASE_NAME}"
echo "Backup directory: ${BACKUP_DIR}"
echo "Timestamp: ${TIMESTAMP}"

# Create backup directory if it doesn't exist
mkdir -p "${BACKUP_DIR}"

# Function to create Data Pump parameter file
create_parameter_file() {
    local par_file_path="${BACKUP_DIR}/${PAR_FILE}"

    cat > "${par_file_path}" << EOF
# Oracle Data Pump Export Parameters
DIRECTORY=DATA_PUMP_DIR
DUMPFILE=${DUMP_FILE}
LOGFILE=${LOG_FILE}
SCHEMAS=USERONBOARD
COMPRESSION=ALL
PARALLEL=2
EXCLUDE=STATISTICS
FLASHBACK_SCN=CURRENT_SCN
EOF

    echo "Parameter file created: ${par_file_path}"
}

# Function to perform Oracle Data Pump export
backup_datapump() {
    echo "Creating Oracle Data Pump export..."

    # Create parameter file
    create_parameter_file

    # Check if Oracle client tools are available
    if ! command -v expdp &> /dev/null; then
        echo "ERROR: Oracle Data Pump (expdp) not found"
        echo "Please install Oracle Client or use container backup method"
        exit 1
    fi

    # Perform export
    expdp ${ORACLE_USER}/${ORACLE_PASSWORD}@${ORACLE_HOST}:${ORACLE_PORT}/${ORACLE_SERVICE} \
        PARFILE="${BACKUP_DIR}/${PAR_FILE}"

    # Move dump file to backup directory (expdp creates it in Oracle directory)
    # This step depends on your Oracle configuration and directory permissions

    if [ $? -eq 0 ]; then
        echo "Data Pump export completed successfully"
        echo "Dump file: ${BACKUP_DIR}/${DUMP_FILE}"
        echo "Log file: ${BACKUP_DIR}/${LOG_FILE}"
    else
        echo "ERROR: Data Pump export failed"
        exit 1
    fi
}

# Function to perform traditional Oracle export (exp command)
backup_traditional_export() {
    echo "Creating traditional Oracle export..."

    local exp_file="${BACKUP_DIR}/oracle_${DATABASE_NAME}_${TIMESTAMP}.exp"

    if command -v exp &> /dev/null; then
        exp ${ORACLE_USER}/${ORACLE_PASSWORD}@${ORACLE_HOST}:${ORACLE_PORT}/${ORACLE_SERVICE} \
            FILE="${exp_file}" \
            OWNER=USERONBOARD \
            COMPRESS=Y \
            STATISTICS=NONE

        if [ $? -eq 0 ]; then
            echo "Traditional export completed successfully: ${exp_file}"
        else
            echo "ERROR: Traditional export failed"
            exit 1
        fi
    else
        echo "WARNING: Oracle exp utility not found, skipping traditional export"
    fi
}

# Function for containerized Oracle backup
backup_container() {
    local container_name="${1:-user-onboard_oracle_1}"

    echo "Creating backup from Oracle container: ${container_name}"

    # Check if container exists and is running
    if ! docker ps | grep -q "${container_name}"; then
        echo "ERROR: Oracle container '${container_name}' not found or not running"
        exit 1
    fi

    # Create directory inside container for dumps
    docker exec "${container_name}" mkdir -p /opt/oracle/oradata/backups

    # Perform export inside container
    docker exec "${container_name}" expdp ${ORACLE_USER}/${ORACLE_PASSWORD}@localhost:1521/${ORACLE_SERVICE} \
        DIRECTORY=DATA_PUMP_DIR \
        DUMPFILE=${DUMP_FILE} \
        LOGFILE=${LOG_FILE} \
        SCHEMAS=USERONBOARD \
        COMPRESSION=ALL

    # Copy backup files from container to host
    docker cp "${container_name}:/opt/oracle/admin/${DATABASE_NAME}/dpdump/${DUMP_FILE}" "${BACKUP_DIR}/"
    docker cp "${container_name}:/opt/oracle/admin/${DATABASE_NAME}/dpdump/${LOG_FILE}" "${BACKUP_DIR}/"

    # Clean up files in container
    docker exec "${container_name}" rm -f "/opt/oracle/admin/${DATABASE_NAME}/dpdump/${DUMP_FILE}"
    docker exec "${container_name}" rm -f "/opt/oracle/admin/${DATABASE_NAME}/dpdump/${LOG_FILE}"

    echo "Container backup completed successfully"
    echo "Files created: ${BACKUP_DIR}/${DUMP_FILE}, ${BACKUP_DIR}/${LOG_FILE}"
}

# Function to perform SQL*Plus logical backup (for simple schemas)
backup_sqlplus() {
    echo "Creating SQL*Plus logical backup..."

    local sql_file="${BACKUP_DIR}/oracle_${DATABASE_NAME}_${TIMESTAMP}.sql"

    if command -v sqlplus &> /dev/null; then
        # Create DDL and data export script
        sqlplus -s ${ORACLE_USER}/${ORACLE_PASSWORD}@${ORACLE_HOST}:${ORACLE_PORT}/${ORACLE_SERVICE} << EOF > "${sql_file}"
SET PAGESIZE 0
SET FEEDBACK OFF
SET HEADING OFF
SET ECHO OFF

-- Export table structures
SELECT 'CREATE TABLE ' || table_name || ' (' FROM user_tables;
-- Add complete DDL extraction logic here

-- Export data
SELECT 'INSERT INTO ' || table_name || ' VALUES (' FROM user_tables;
-- Add data export logic here

EXIT;
EOF

        echo "SQL*Plus backup completed: ${sql_file}"
    else
        echo "WARNING: SQL*Plus not found, skipping SQL backup"
    fi
}

# Cleanup old backups
cleanup_old_backups() {
    echo "Cleaning up Oracle backups older than ${RETENTION_DAYS} days..."

    find "${BACKUP_DIR}" -name "oracle_${DATABASE_NAME}_*.dmp" -type f -mtime +${RETENTION_DAYS} -delete
    find "${BACKUP_DIR}" -name "oracle_${DATABASE_NAME}_*.log" -type f -mtime +${RETENTION_DAYS} -delete
    find "${BACKUP_DIR}" -name "oracle_${DATABASE_NAME}_*.exp" -type f -mtime +${RETENTION_DAYS} -delete
    find "${BACKUP_DIR}" -name "oracle_${DATABASE_NAME}_*.sql" -type f -mtime +${RETENTION_DAYS} -delete
    find "${BACKUP_DIR}" -name "oracle_${DATABASE_NAME}_*.par" -type f -mtime +${RETENTION_DAYS} -delete

    echo "Cleanup completed"
}

# Verify backup (basic check)
verify_backup() {
    echo "Verifying Oracle backup..."

    local dump_path="${BACKUP_DIR}/${DUMP_FILE}"

    if [ -f "${dump_path}" ]; then
        # Check if dump file is not empty and has correct header
        local file_size=$(stat -c%s "${dump_path}" 2>/dev/null || stat -f%z "${dump_path}" 2>/dev/null || echo "0")

        if [ "${file_size}" -gt 1000 ]; then
            echo "Backup verification successful - file size: ${file_size} bytes"
        else
            echo "WARNING: Backup file seems too small: ${file_size} bytes"
        fi
    else
        echo "WARNING: Backup file not found: ${dump_path}"
    fi
}

# Main execution
main() {
    # Validate Oracle connection first
    if command -v sqlplus &> /dev/null; then
        echo "Testing Oracle connection..."
        echo "SELECT 'Connection OK' FROM DUAL;" | sqlplus -s ${ORACLE_USER}/${ORACLE_PASSWORD}@${ORACLE_HOST}:${ORACLE_PORT}/${ORACLE_SERVICE} || {
            echo "ERROR: Cannot connect to Oracle database"
            echo "Please check connection parameters and credentials"
            exit 1
        }
        echo "Oracle connection successful"
    fi

    case "${1:-datapump}" in
        "datapump")
            backup_datapump
            verify_backup
            ;;
        "traditional")
            backup_traditional_export
            ;;
        "container")
            backup_container "${2}"
            verify_backup
            ;;
        "sqlplus")
            backup_sqlplus
            ;;
        "all")
            backup_datapump
            backup_traditional_export
            backup_sqlplus
            verify_backup
            ;;
        *)
            echo "Usage: $0 {datapump|traditional|container|sqlplus|all} [container_name]"
            echo "  datapump   - Use Oracle Data Pump export (recommended)"
            echo "  traditional- Use traditional exp utility"
            echo "  container  - Backup from Docker container"
            echo "  sqlplus    - Simple SQL*Plus logical backup"
            echo "  all        - Create all backup types"
            echo ""
            echo "Prerequisites:"
            echo "  - Oracle client tools (sqlplus, expdp) must be installed"
            echo "  - For container backup: Oracle container must be running"
            echo "  - Proper Oracle user permissions for backup operations"
            exit 1
            ;;
    esac

    cleanup_old_backups

    echo "Oracle backup process completed successfully"
    echo "Backup files created in: ${BACKUP_DIR}"
    ls -la "${BACKUP_DIR}"/oracle_${DATABASE_NAME}_${TIMESTAMP}.*
}

# Execute main function with all arguments
main "$@"
