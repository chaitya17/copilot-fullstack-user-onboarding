# Infrastructure Overview

This directory contains production-ready infrastructure components supporting both MSSQL and Oracle databases for the User Onboard application.

## 📁 Directory Structure

```
infrastructure/
├── helm-chart/user-onboard/     # Helm chart with dual DB support
├── k8s/                         # Plain Kubernetes manifests
├── monitoring/                  # Prometheus, Grafana, Loki configs
├── scripts/                     # Database backup/restore scripts
├── security/                    # Security scanning configuration
└── ops/                         # Operational runbooks
```

## 🚀 Quick Deployment Guide

### Local Development (Docker Compose)
```bash
# Default MSSQL setup
docker-compose up -d

# Switch to Oracle (requires manual setup)
# 1. Edit docker-compose.yml: uncomment oracle service
# 2. Add Oracle JDBC driver to backend/libs/
# 3. Set environment variables
docker-compose down
# Edit .env.local: DB_TYPE=oracle, ORACLE_SYS_PASSWORD=...
docker-compose up -d
```

### Production Kubernetes Deployment
```bash
# Using Helm (recommended)
helm install user-onboard ./helm-chart/user-onboard \
  --set database.type=mssql \
  --set database.external.jdbcUrl="your-jdbc-url" \
  --set global.registry=ghcr.io/your-org

# Using plain K8s manifests
kubectl apply -f k8s/
```

## 🗄️ Database Support Matrix

| Database | Local Dev | Production | Backup Method | Restore Method |
|----------|-----------|------------|---------------|----------------|
| **MSSQL** | ✅ Docker | ✅ Managed | BAK + BACPAC | SQL Server Restore |
| **Oracle** | ⚠️ Manual | ✅ Managed | Data Pump | impdp/expdp |

### MSSQL Configuration
```yaml
# Helm values for MSSQL
database:
  type: mssql
  hibernate:
    dialect: org.hibernate.dialect.SQLServerDialect
  external:
    jdbcUrl: jdbc:sqlserver://server:1433;database=useronboard
```

### Oracle Configuration
```yaml
# Helm values for Oracle
database:
  type: oracle
  hibernate:
    dialect: org.hibernate.dialect.Oracle12cDialect
  external:
    jdbcUrl: jdbc:oracle:thin:@server:1521/servicename
```

## 📊 Monitoring Stack

### Components Included
- **Prometheus** - Metrics collection with DB-specific exporters
- **Grafana** - Unified dashboard for both database types
- **Loki** - Log aggregation
- **AlertManager** - Alert routing and notifications

### Database-Specific Monitoring
```bash
# MSSQL monitoring requires sql_exporter
# Oracle monitoring requires oracle_exporter
# Configuration included in monitoring/prometheus.yml
```

## 🔒 Security & Compliance

### Security Scanning
- **Trivy** integration for vulnerability scanning
- **License compliance** checking (Oracle license awareness)
- **Container security** best practices

### Secret Management
```bash
# Database credentials
kubectl create secret generic user-onboard-db-secret \
  --from-literal=username=dbuser \
  --from-literal=password=dbpass \
  --from-literal=jdbc-url=jdbc:...

# JWT keys
kubectl create secret generic user-onboard-jwt-secret \
  --from-file=private_key.pem=./keys/private.pem \
  --from-file=public_key.pem=./keys/public.pem
```

## 🔄 CI/CD Pipeline Features

### Multi-Database Testing
- Parallel testing against both MSSQL and Oracle
- Database-agnostic integration tests
- Container security scanning

### Deployment Automation
- Registry-agnostic (GHCR, ACR, ECR compatible)
- Environment-specific configurations
- Database migration validation

## 📋 Operational Procedures

### Backup & Restore
```bash
# MSSQL backup
./scripts/backup-mssql.sh both

# Oracle backup  
./scripts/backup-oracle.sh datapump

# Universal restore
./scripts/restore-database.sh mssql /backups/file.bak
./scripts/restore-database.sh oracle /backups/file.dmp
```

### Database Migration (MSSQL ↔ Oracle)
1. **Backup current database**
2. **Schema conversion** (manual or automated tools)
3. **Update configuration** (DB_TYPE, HIBERNATE_DIALECT, JDBC_URL)
4. **Deploy with new database settings**
5. **Verify functionality**

## ⚠️ Important Notes

### Oracle Considerations
- **License Compliance**: Oracle JDBC driver requires appropriate licensing
- **Driver Installation**: Manual addition of ojdbc8.jar required
- **Production Recommendation**: Use managed Oracle services (Oracle Cloud, AWS RDS Oracle)
- **Container Limitations**: Oracle containers not recommended for production K8s clusters

### MSSQL Considerations
- **Default Choice**: Fully supported out-of-the-box
- **Managed Services**: Azure SQL Database, AWS RDS SQL Server
- **Backup Portability**: BACPAC format for cross-platform compatibility

## 🔗 Integration Points

### Frontend Integration
- Database type is transparent to frontend
- API endpoints remain consistent regardless of backend database
- CORS configuration supports both local and production deployments

### External Services
- **Email Services** - SMTP configuration for notifications
- **Message Queues** - RabbitMQ for async processing
- **Monitoring** - Prometheus metrics endpoints
- **Logging** - Structured JSON logging for aggregation

## 📞 Support & Troubleshooting

### Common Issues
1. **Oracle JDBC Driver Missing** - Check backend/libs/ directory
2. **Database Connection Timeouts** - Verify network connectivity and credentials
3. **Schema Migration Errors** - Review Flyway migration compatibility
4. **Performance Issues** - Check database-specific monitoring dashboards

### Getting Help
- Review operational runbook: `ops/README-ops.md`
- Check monitoring dashboards for system health
- Examine application logs for detailed error messages
- Verify database-specific configuration in Helm values

This infrastructure setup provides a production-ready, database-agnostic foundation that scales from local development to enterprise production deployments.
