# User Onboard Service - Backend

A production-ready Spring Boot microservice implementing user registration and approval workflow with dual database support (MSSQL/Oracle).

## 🏗️ Architecture Overview

- **Framework**: Spring Boot 3.1.5 with Java 17
- **Database**: MSSQL Server (default) or Oracle Database
- **Authentication**: JWT with RS256 (RSA key pair)
- **Messaging**: RabbitMQ for async processing
- **Security**: Spring Security with role-based access control
- **Observability**: Micrometer, Prometheus metrics, health checks

## 🗄️ Database Support

### MSSQL Server (Default)
- **Driver**: `com.microsoft.sqlserver.jdbc.SQLServerDriver`
- **Dialect**: `org.hibernate.dialect.SQLServerDialect`
- **JDBC URL Format**: `jdbc:sqlserver://host:port;databaseName=useronboard`

### Oracle Database (Alternative)
- **Driver**: `oracle.jdbc.OracleDriver` ⚠️ **Requires Manual Setup**
- **Dialect**: `org.hibernate.dialect.Oracle12cDialect`
- **JDBC URL Format**: `jdbc:oracle:thin:@host:port/servicename`

#### Oracle Driver Setup (TODO: Required for Oracle)
1. Download Oracle JDBC driver (`ojdbc8.jar`) from [Oracle website](https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html)
2. Place in `libs/` directory
3. Uncomment Oracle dependency in `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.oracle.jdbc</groupId>
       <artifactId>ojdbc8</artifactId>
       <version>21.7.0.0</version>
       <scope>system</scope>
       <systemPath>${project.basedir}/libs/ojdbc8.jar</systemPath>
   </dependency>
   ```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker (for databases)

### Local Development

1. **Start Dependencies**:
   ```bash
   # From project root
   docker-compose up -d mssql rabbitmq
   ```

2. **Generate JWT Keys**:
   ```bash
   mkdir -p keys
   openssl genrsa -out keys/private_key.pem 2048
   openssl rsa -in keys/private_key.pem -pubout -out keys/public_key.pem
   ```

3. **Configure Environment**:
   ```bash
   # Copy and edit environment file
   cp ../.env.example ../.env.local
   
   # Set database password and other configs
   export DB_TYPE=mssql
   export JDBC_URL="jdbc:sqlserver://localhost:1433;databaseName=useronboard;encrypt=false;trustServerCertificate=true"
   export DB_USERNAME=sa
   export DB_PASSWORD=YourStrong!Passw0rd
   ```

4. **Run Application**:
   ```bash
   ./mvnw spring-boot:run -Dspring.profiles.active=local
   ```

### Using Oracle Database

1. **Switch Docker Compose**:
   - Edit `../docker-compose.yml`: uncomment Oracle service, comment MSSQL
   - Set Oracle environment variables in `.env.local`

2. **Update Configuration**:
   ```bash
   export DB_TYPE=oracle
   export JDBC_URL="jdbc:oracle:thin:@localhost:1521/XEPDB1"
   export DB_USERNAME=system
   export DB_PASSWORD=YourOraclePassword123
   export HIBERNATE_DIALECT=org.hibernate.dialect.Oracle12cDialect
   ```

3. **Add Oracle Driver** (see Oracle Driver Setup above)

## 📊 Database Schema

### Portable Design
- Uses `VARCHAR(36)` for UUIDs (compatible with both MSSQL and Oracle)
- Stores enums as `VARCHAR(16)` strings
- JSON data stored as `NVARCHAR(MAX)` (MSSQL) or `CLOB` (Oracle)

### Tables Created
- `users` - User profiles and authentication
- `refresh_tokens` - JWT refresh token storage
- `onboarding_steps` - User onboarding progress tracking
- `user_audit_log` - Audit trail for compliance

### Database Migrations
- **MSSQL**: `src/main/resources/db/migration/mssql/`
- **Oracle**: `src/main/resources/db/migration/oracle/`

#### Running Migrations Manually
```bash
# MSSQL
./mvnw flyway:migrate -Dflyway.url="jdbc:sqlserver://localhost:1433;databaseName=useronboard" -Dflyway.user=sa -Dflyway.password=YourPassword

# Oracle
./mvnw flyway:migrate -Dflyway.url="jdbc:oracle:thin:@localhost:1521/XEPDB1" -Dflyway.user=system -Dflyway.password=YourPassword
```

## 🔐 Security Configuration

### JWT Authentication
- **Algorithm**: RS256 (RSA with SHA-256)
- **Access Token**: 15 minutes (configurable)
- **Refresh Token**: 7 days (configurable)
- **Storage**: Refresh tokens stored in database for revocation support

### Key Management
- **Development**: File-based keys in `./keys/`
- **Production**: Mount secrets via Kubernetes or Docker volumes

### Role-Based Access Control
- `USER`: Standard user role
- `ADMIN`: Administrative privileges

## 🌐 API Endpoints

### Authentication (`/api/v1/auth`)
- `POST /register` - User registration (→ PENDING status)
- `POST /login` - User authentication
- `POST /refresh` - Refresh access token
- `POST /logout` - Revoke refresh token

### User Management (`/api/v1/users`)
- `GET /me` - Get current user profile

### Admin Operations (`/api/v1/admin`) - Requires ADMIN role
- `GET /users/pending` - List pending users
- `GET /users` - List all users (paginated)
- `POST /users/{id}/approve` - Approve user
- `POST /users/{id}/reject` - Reject user
- `GET /statistics` - User statistics

## 📨 Messaging & Notifications

### RabbitMQ Events
- `user.registered` - New user registration
- `user.approved` - User approved by admin (→ welcome email)
- `user.rejected` - User rejected by admin (→ notification email)

### Email Configuration
```yaml
app:
  email:
    enabled: true
    from: noreply@useronboard.com
    smtp:
      host: your-smtp-host
      port: 587
      username: your-smtp-user
      password: your-smtp-password
```

## 🧪 Testing

### Run Tests
```bash
# Unit tests
./mvnw test

# Integration tests (uses H2 database)
./mvnw test -Dspring.profiles.active=test

# With SQL Server integration (requires running database)
./mvnw test -Dspring.profiles.active=local
```

### Oracle Integration Testing
```bash
# Start Oracle container first
docker-compose up -d oracle

# Run tests with Oracle
DB_TYPE=oracle JDBC_URL="jdbc:oracle:thin:@localhost:1521/XEPDB1" ./mvnw test -Dspring.profiles.active=test
```

## 🐳 Production Deployment

### Docker Build
```bash
# Build image
docker build -t user-onboard-service:latest .

# For Oracle support, ensure ojdbc8.jar is in libs/
```

### Kubernetes Deployment
```bash
# Create secrets first
kubectl create secret generic database-secret \
  --from-literal=jdbc-url="jdbc:sqlserver://your-db:1433;database=useronboard" \
  --from-literal=username="your-user" \
  --from-literal=password="your-password"

kubectl create secret generic jwt-keys-secret \
  --from-file=private_key.pem=./keys/private_key.pem \
  --from-file=public_key.pem=./keys/public_key.pem

# Deploy
kubectl apply -f k8s/deployment.yaml
```

## 📊 Monitoring & Observability

### Health Checks
- `/actuator/health` - Application health
- `/actuator/health/liveness` - Kubernetes liveness probe
- `/actuator/health/readiness` - Kubernetes readiness probe

### Metrics
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics endpoint

### Logging
- JSON structured logging (Logstash format)
- Configurable log levels
- Request correlation IDs

## ⚙️ Configuration

### Environment Variables
| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DB_TYPE` | Database type (`mssql` or `oracle`) | `mssql` | ✓ |
| `JDBC_URL` | Database connection URL | - | ✓ |
| `DB_USERNAME` | Database username | - | ✓ |
| `DB_PASSWORD` | Database password | - | ✓ |
| `JWT_PRIVATE_KEY_PATH` | JWT private key file path | - | ✓ |
| `JWT_PUBLIC_KEY_PATH` | JWT public key file path | - | ✓ |
| `RABBITMQ_HOST` | RabbitMQ hostname | `rabbitmq` | ✓ |
| `EMAIL_ENABLED` | Enable email notifications | `false` | - |

### Spring Profiles
- `local` - Local development
- `test` - Unit/integration testing
- `prod` - Production deployment

## 🔧 Troubleshooting

### Database Connection Issues
```bash
# Check database connectivity
telnet your-db-host 1433  # MSSQL
telnet your-db-host 1521  # Oracle

# Verify JDBC URL format
# MSSQL: jdbc:sqlserver://host:port;databaseName=db;encrypt=false
# Oracle: jdbc:oracle:thin:@host:port/servicename
```

### Oracle Driver Issues
1. Ensure `ojdbc8.jar` is in `libs/` directory
2. Uncomment Oracle dependency in `pom.xml`
3. Verify Oracle JDBC URL format
4. Check Oracle service name (not SID)

### JWT Key Issues
```bash
# Regenerate keys
openssl genrsa -out keys/private_key.pem 2048
openssl rsa -in keys/private_key.pem -pubout -out keys/public_key.pem

# Verify key format
openssl rsa -in keys/private_key.pem -text -noout
```

## 🤝 Development Workflow

1. **Database Migration**: Add SQL files to appropriate `db/migration/{mssql|oracle}/` directory
2. **API Changes**: Update controllers and DTOs
3. **Business Logic**: Implement in service layer
4. **Testing**: Add unit tests for services, integration tests for controllers
5. **Documentation**: Update API documentation and README

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Flyway Database Migrations](https://flywaydb.org/documentation/)
- [MSSQL JDBC Driver](https://docs.microsoft.com/en-us/sql/connect/jdbc/)
- [Oracle JDBC Driver](https://docs.oracle.com/en/database/oracle/oracle-database/21/jjdbc/)
