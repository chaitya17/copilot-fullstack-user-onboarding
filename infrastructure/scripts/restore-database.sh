#!/bin/bash
# Database Restore Script - Universal for MSSQL and Oracle
# Usage: ./restore-database.sh {mssql|oracle} backup_file [options]

set -e

DB_TYPE="${1}"
BACKUP_FILE="${2}"
RESTORE_OPTIONS="${3:-}"

if [ -z "$DB_TYPE" ] || [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 {mssql|oracle} backup_file [restore_options]"
    echo ""
    echo "Examples:"
    echo "  $0 mssql /backups/mssql_useronboard_20231201.bak"
    echo "  $0 oracle /backups/oracle_useronboard_20231201.dmp"
    exit 1
fi

# Configuration from environment
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESTORE_LOG_DIR="${RESTORE_LOG_DIR:-/var/log/database-restore}"
mkdir -p "$RESTORE_LOG_DIR"

# MSSQL Restore Function
restore_mssql() {
    local backup_file="$1"
    local db_name="${DATABASE_NAME:-useronboard}"
    local restore_db_name="${RESTORE_DB_NAME:-${db_name}_restored_${TIMESTAMP}}"

    echo "Starting MSSQL restore from: $backup_file"
    echo "Target database: $restore_db_name"

    # Check if backup file exists
    if [ ! -f "$backup_file" ]; then
        echo "ERROR: Backup file not found: $backup_file"
        exit 1
    fi

    # Get backup file information
    sqlcmd -S "${MSSQL_HOST:-localhost},${MSSQL_PORT:-1433}" \
           -U "${MSSQL_USER:-sa}" -P "${MSSQL_PASSWORD}" -Q \
        "RESTORE FILELISTONLY FROM DISK = '$backup_file'"

    # Perform restore
    sqlcmd -S "${MSSQL_HOST:-localhost},${MSSQL_PORT:-1433}" \
           -U "${MSSQL_USER:-sa}" -P "${MSSQL_PASSWORD}" -Q \
        "RESTORE DATABASE [$restore_db_name] FROM DISK = '$backup_file'
         WITH MOVE 'useronboard' TO '/var/opt/mssql/data/${restore_db_name}.mdf',
         MOVE 'useronboard_Log' TO '/var/opt/mssql/data/${restore_db_name}_Log.ldf',
         REPLACE, STATS = 10" \
    > "$RESTORE_LOG_DIR/mssql_restore_${TIMESTAMP}.log" 2>&1

    if [ $? -eq 0 ]; then
        echo "MSSQL restore completed successfully"
        echo "Restored database: $restore_db_name"
        echo "Log file: $RESTORE_LOG_DIR/mssql_restore_${TIMESTAMP}.log"
    else
        echo "ERROR: MSSQL restore failed. Check log: $RESTORE_LOG_DIR/mssql_restore_${TIMESTAMP}.log"
        exit 1
    fi
}

# Oracle Restore Function
restore_oracle() {
    local dump_file="$1"
    local schema_name="${ORACLE_SCHEMA:-USERONBOARD}"
    local restore_schema="${RESTORE_SCHEMA:-${schema_name}_RESTORED_${TIMESTAMP}}"

    echo "Starting Oracle restore from: $dump_file"
    echo "Target schema: $restore_schema"

    # Check if dump file exists
    if [ ! -f "$dump_file" ]; then
        echo "ERROR: Dump file not found: $dump_file"
        exit 1
    fi

    # Create restore user/schema if it doesn't exist
    sqlplus -s ${ORACLE_USER}/${ORACLE_PASSWORD}@${ORACLE_HOST:-localhost}:${ORACLE_PORT:-1521}/${ORACLE_SERVICE:-XEPDB1} << EOF
CREATE USER $restore_schema IDENTIFIED BY "TempPassword123!";
GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO $restore_schema;
EXIT;
EOF

    # Perform Data Pump import
    impdp ${ORACLE_USER}/${ORACLE_PASSWORD}@${ORACLE_HOST:-localhost}:${ORACLE_PORT:-1521}/${ORACLE_SERVICE:-XEPDB1} \
        DIRECTORY=DATA_PUMP_DIR \
        DUMPFILE=$(basename "$dump_file") \
        LOGFILE=restore_${TIMESTAMP}.log \
        SCHEMAS=$schema_name \
        REMAP_SCHEMA=${schema_name}:${restore_schema} \
        TABLE_EXISTS_ACTION=REPLACE

    if [ $? -eq 0 ]; then
        echo "Oracle restore completed successfully"
        echo "Restored schema: $restore_schema"
        echo "Log file available in Oracle Data Pump directory"
    else
        echo "ERROR: Oracle restore failed. Check Oracle Data Pump logs"
        exit 1
    fi
}

# Main execution
case "$DB_TYPE" in
    "mssql")
        restore_mssql "$BACKUP_FILE"
        ;;
    "oracle")
        restore_oracle "$BACKUP_FILE"
        ;;
    *)
        echo "ERROR: Invalid database type. Use 'mssql' or 'oracle'"
        exit 1
        ;;
esac

echo "Database restore process completed successfully"
