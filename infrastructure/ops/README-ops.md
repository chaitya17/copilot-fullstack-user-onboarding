# User Onboard Operations Runbook

This runbook provides operational procedures for the User Onboard application supporting both MSSQL and Oracle databases.

## 🗄️ Database Operations

### MSSQL Operations

#### Connection and Monitoring
```bash
# Test MSSQL connection
sqlcmd -S your-mssql-server,1433 -U username -P password -Q "SELECT @@VERSION"

# Check database size
sqlcmd -S server -U user -P password -Q "
SELECT 
    DB_NAME(database_id) AS DatabaseName,
    (size * 8.0) / 1024 AS SizeMB
FROM sys.master_files 
WHERE DB_NAME(database_id) = 'useronboard'"

# Monitor active connections
sqlcmd -S server -U user -P password -Q "
SELECT 
    login_name, 
    COUNT(*) as connection_count 
FROM sys.dm_exec_sessions 
WHERE database_id = DB_ID('useronboard') 
GROUP BY login_name"
```

#### Backup and Restore Operations
```bash
# Create backup using provided script
./infrastructure/scripts/backup-mssql.sh native

# Restore from backup
sqlcmd -S server -U user -P password -Q "
RESTORE DATABASE useronboard 
FROM DISK = '/backups/mssql_useronboard_20231201_120000.bak' 
WITH REPLACE, STATS = 10"

# Restore from BACPAC (portable format)
SqlPackage /Action:Import \
    /SourceFile:"/backups/mssql_useronboard_20231201_120000.bacpac" \
    /TargetServerName:"your-server,1433" \
    /TargetDatabaseName:"useronboard_restored" \
    /TargetUser:"username" \
    /TargetPassword:"password"
```

#### Database Maintenance
```bash
# Update statistics
sqlcmd -S server -U user -P password -Q "
USE useronboard;
EXEC sp_updatestats;"

# Rebuild indexes
sqlcmd -S server -U user -P password -Q "
USE useronboard;
ALTER INDEX ALL ON users REBUILD;
ALTER INDEX ALL ON refresh_tokens REBUILD;"

# Check database integrity
sqlcmd -S server -U user -P password -Q "
USE useronboard;
DBCC CHECKDB('useronboard') WITH NO_INFOMSGS;"
```

### Oracle Operations

#### Connection and Monitoring
```bash
# Test Oracle connection
echo "SELECT * FROM v\$version;" | sqlplus -s username/password@host:1521/servicename

# Check database size
sqlplus -s username/password@host:1521/servicename << EOF
SELECT 
    tablespace_name,
    ROUND(bytes/1024/1024, 2) as size_mb,
    ROUND(maxbytes/1024/1024, 2) as max_size_mb
FROM dba_data_files 
WHERE tablespace_name = 'USERS';
EOF

# Monitor active sessions
sqlplus -s username/password@host:1521/servicename << EOF
SELECT 
    username, 
    COUNT(*) as session_count 
FROM v\$session 
WHERE username IS NOT NULL 
GROUP BY username;
EOF
```

#### Backup and Restore Operations
```bash
# Create backup using provided script
./infrastructure/scripts/backup-oracle.sh datapump

# Import from Data Pump dump
impdp username/password@host:1521/servicename \
    DIRECTORY=DATA_PUMP_DIR \
    DUMPFILE=oracle_useronboard_20231201_120000.dmp \
    LOGFILE=restore_20231201_120000.log \
    SCHEMAS=USERONBOARD \
    REMAP_SCHEMA=USERONBOARD:USERONBOARD_RESTORED

# Container backup and restore
docker exec oracle_container expdp system/password@localhost:1521/XEPDB1 \
    DIRECTORY=DATA_PUMP_DIR \
    DUMPFILE=backup.dmp \
    SCHEMAS=USERONBOARD
```

#### Database Maintenance
```bash
# Gather statistics
sqlplus -s username/password@host:1521/servicename << EOF
BEGIN
    DBMS_STATS.GATHER_SCHEMA_STATS(
        ownname => 'USERONBOARD',
        estimate_percent => DBMS_STATS.AUTO_SAMPLE_SIZE,
        cascade => TRUE
    );
END;
/
EOF

# Rebuild indexes
sqlplus -s username/password@host:1521/servicename << EOF
ALTER INDEX USERONBOARD.IX_USERS_EMAIL REBUILD;
ALTER INDEX USERONBOARD.IX_REFRESH_TOKENS_USER_ID REBUILD;
EOF
```

## 🔒 Security Operations

### Credential Rotation

#### Database Password Rotation (MSSQL)
```bash
# 1. Update password in MSSQL
sqlcmd -S server -U sa -P current_password -Q "
ALTER LOGIN [useronboard_user] WITH PASSWORD = 'new_secure_password'"

# 2. Update Kubernetes secret
kubectl create secret generic user-onboard-db-secret \
    --from-literal=username=useronboard_user \
    --from-literal=password=new_secure_password \
    --dry-run=client -o yaml | kubectl apply -f -

# 3. Restart application pods
kubectl rollout restart deployment/user-onboard-backend -n user-onboard-prod
```

#### Database Password Rotation (Oracle)
```bash
# 1. Update password in Oracle
sqlplus -s system/admin_password@host:1521/servicename << EOF
ALTER USER useronboard_user IDENTIFIED BY "new_secure_password";
EOF

# 2. Update Kubernetes secret (same as MSSQL above)
# 3. Restart application pods (same as above)
```

#### JWT Key Rotation
```bash
# 1. Generate new RSA key pair
openssl genrsa -out new_private_key.pem 2048
openssl rsa -in new_private_key.pem -pubout -out new_public_key.pem

# 2. Create new Kubernetes secret
kubectl create secret generic user-onboard-jwt-secret-new \
    --from-file=private_key.pem=new_private_key.pem \
    --from-file=public_key.pem=new_public_key.pem \
    -n user-onboard-prod

# 3. Update deployment to use new secret
kubectl patch deployment user-onboard-backend -n user-onboard-prod \
    -p '{"spec":{"template":{"spec":{"volumes":[{"name":"jwt-keys","secret":{"secretName":"user-onboard-jwt-secret-new"}}]}}}}'

# 4. Remove old secret after verification
kubectl delete secret user-onboard-jwt-secret -n user-onboard-prod
```

### Monitoring User Permissions

#### Create Database Monitoring User (MSSQL)
```sql
-- Create monitoring user with minimal permissions
CREATE LOGIN [monitoring_user] WITH PASSWORD = 'secure_monitoring_password';
USE useronboard;
CREATE USER [monitoring_user] FOR LOGIN [monitoring_user];
GRANT VIEW DATABASE STATE TO [monitoring_user];
GRANT VIEW SERVER STATE TO [monitoring_user];
```

#### Create Database Monitoring User (Oracle)
```sql
-- Create monitoring user with minimal permissions
CREATE USER monitoring_user IDENTIFIED BY "secure_monitoring_password";
GRANT CREATE SESSION TO monitoring_user;
GRANT SELECT ON v$database TO monitoring_user;
GRANT SELECT ON v$session TO monitoring_user;
GRANT SELECT ON dba_data_files TO monitoring_user;
```

## 📊 Monitoring and Alerting

### Database-Specific Monitoring Setup

#### MSSQL Monitoring with sql_exporter
```yaml
# sql_exporter configuration for MSSQL
collectors:
  - collector_name: "mssql_standard"
    metrics:
      - metric_name: "mssql_connections"
        type: "gauge"
        help: "Number of active connections"
        query: "SELECT COUNT(*) as connections FROM sys.dm_exec_sessions WHERE database_id = DB_ID('useronboard')"
      - metric_name: "mssql_database_size_mb"
        type: "gauge"
        help: "Database size in MB"
        query: "SELECT (SUM(size) * 8.0) / 1024 as size_mb FROM sys.master_files WHERE database_id = DB_ID('useronboard')"
```

#### Oracle Monitoring with oracle_exporter
```yaml
# oracle_exporter configuration
metrics:
  - name: oracle_sessions
    help: "Number of active sessions"
    query: "SELECT COUNT(*) as sessions FROM v$session WHERE username IS NOT NULL"
  - name: oracle_tablespace_usage
    help: "Tablespace usage percentage"
    query: "SELECT tablespace_name, ROUND((used_space/total_space)*100, 2) as usage_percent FROM (SELECT tablespace_name, SUM(bytes) as total_space FROM dba_data_files GROUP BY tablespace_name) t1 JOIN (SELECT tablespace_name, SUM(bytes) as used_space FROM dba_segments GROUP BY tablespace_name) t2 USING (tablespace_name)"
```

### Application Monitoring

#### Key Metrics to Monitor
```yaml
# Prometheus alerting rules
groups:
  - name: user-onboard-alerts
    rules:
      - alert: HighDatabaseConnections
        expr: (mssql_connections > 80) or (oracle_sessions > 100)
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High database connection count"
          
      - alert: DatabaseDown
        expr: up{job="mssql-exporter"} == 0 or up{job="oracle-exporter"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Database monitoring is down"
          
      - alert: ApplicationDown
        expr: up{job="user-onboard-backend"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "User Onboard application is down"
```

## 🚀 Deployment Operations

### Cross-Database Migration

#### Migrating from MSSQL to Oracle
```bash
# 1. Create full backup of MSSQL database
./infrastructure/scripts/backup-mssql.sh both

# 2. Export schema and data
SqlPackage /Action:Export \
    /SourceServerName:"mssql-server" \
    /SourceDatabaseName:"useronboard" \
    /TargetFile:"useronboard_export.bacpac"

# 3. Set up Oracle database with proper schema
# Note: Schema conversion may require manual adjustments

# 4. Update application configuration
kubectl patch configmap user-onboard-config -n production \
    -p '{"data":{"DB_TYPE":"oracle","HIBERNATE_DIALECT":"org.hibernate.dialect.Oracle12cDialect"}}'

# 5. Update database connection secrets
kubectl create secret generic user-onboard-db-secret \
    --from-literal=username=oracle_user \
    --from-literal=password=oracle_password \
    --from-literal=jdbc-url="jdbc:oracle:thin:@oracle-server:1521/servicename" \
    --dry-run=client -o yaml | kubectl apply -f -

# 6. Rolling update deployment
kubectl rollout restart deployment/user-onboard-backend -n production
```

#### Migrating from Oracle to MSSQL
```bash
# 1. Create Oracle Data Pump export
./infrastructure/scripts/backup-oracle.sh datapump

# 2. Convert schema (manual process or use migration tools)
# Oracle → MSSQL schema conversion tools:
# - Oracle SQL Developer Migration Workbench
# - AWS Database Migration Service
# - Manual SQL script conversion

# 3. Update application configuration (reverse of above process)
```

### Scaling Operations

#### Horizontal Scaling
```bash
# Scale backend pods
kubectl scale deployment user-onboard-backend --replicas=5 -n production

# Enable horizontal pod autoscaler
kubectl autoscale deployment user-onboard-backend \
    --cpu-percent=70 \
    --min=2 \
    --max=10 \
    -n production
```

#### Database Connection Pool Tuning
```yaml
# Spring Boot application.yml for database-specific tuning
spring:
  datasource:
    hikari:
      # MSSQL optimizations
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1800000
      connection-timeout: 30000
      
      # Oracle optimizations  
      # maximum-pool-size: 25
      # minimum-idle: 10
      # idle-timeout: 600000
```

## 🔧 Troubleshooting

### Common Database Issues

#### MSSQL Connection Problems
```bash
# Check if MSSQL is accessible
telnet mssql-server 1433

# Check MSSQL logs
docker logs mssql-container
# or for managed instance:
# Check Azure SQL Database query store and logs

# Verify authentication
sqlcmd -S server -U username -P password -Q "SELECT SYSTEM_USER, ORIGINAL_LOGIN()"
```

#### Oracle Connection Problems
```bash
# Check Oracle listener status
lsnrctl status

# Test TNS connectivity
tnsping your-oracle-service

# Check Oracle alert log
tail -f $ORACLE_BASE/diag/rdbms/xe/XE/trace/alert_XE.log
```

### Application Performance Issues

#### Database Query Optimization
```sql
-- MSSQL: Check expensive queries
SELECT TOP 10 
    total_elapsed_time/execution_count AS avg_elapsed_time,
    text 
FROM sys.dm_exec_query_stats 
CROSS APPLY sys.dm_exec_sql_text(sql_handle) 
ORDER BY avg_elapsed_time DESC;

-- Oracle: Check expensive queries  
SELECT sql_id, elapsed_time/executions AS avg_elapsed_time, sql_text 
FROM v$sql 
WHERE executions > 0 
ORDER BY avg_elapsed_time DESC 
FETCH FIRST 10 ROWS ONLY;
```

## 📞 Emergency Procedures

### Database Corruption Recovery

#### MSSQL Recovery
```bash
# Check database integrity
sqlcmd -S server -U user -P password -Q "DBCC CHECKDB('useronboard')"

# Restore from latest backup if corruption found
sqlcmd -S server -U user -P password -Q "
RESTORE DATABASE useronboard 
FROM DISK = '/backups/latest_backup.bak' 
WITH REPLACE"
```

#### Oracle Recovery
```bash
# Check database integrity
sqlplus -s user/password@server << EOF
SELECT COUNT(*) FROM dba_objects WHERE status = 'INVALID';
EOF

# Restore from Data Pump if needed
impdp user/password@server DIRECTORY=DATA_PUMP_DIR DUMPFILE=latest_backup.dmp
```

### Disaster Recovery Procedures

1. **Assess the situation** - Determine scope of outage
2. **Activate backup database** - Switch to standby/replica if available
3. **Update DNS/Load Balancer** - Point traffic to backup systems
4. **Restore from backup** - Use appropriate backup script
5. **Verify data integrity** - Run consistency checks
6. **Resume normal operations** - Switch back when primary is restored

## 📋 Maintenance Schedules

### Daily Operations
- Monitor application logs and metrics
- Check database connection pools
- Verify backup completion

### Weekly Operations  
- Review security logs
- Update statistics on databases
- Clean up old backup files

### Monthly Operations
- Rotate database credentials
- Review and update monitoring rules
- Performance baseline review
- Security patch assessment

This runbook should be updated as the system evolves and new operational procedures are developed.
