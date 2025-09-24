# User Onboarding Application

A full-stack user onboarding application built with Spring Boot, React, and supporting both MSSQL and Oracle databases.

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose
- Git
- VS Code/JetBrains with GitHub Copilot extensions (recommended)

### Setup Steps

1. **Clone and Navigate**
   ```bash
   git clone <your-repo-url>
   cd user-onboard
   ```

2. **Environment Configuration**
   ```bash
   # Copy the example environment file
   copy .env.example .env.local
   
   # Edit .env.local with your configuration
   # At minimum, set MSSQL_SA_PASSWORD=YourStrong!Passw0rd
   ```

3. **Generate JWT Keys**
   ```bash
   make generate-keys
   ```

4. **Start Development Environment**
   ```bash
   make up
   ```

5. **Access Applications**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080
   - RabbitMQ Management: http://localhost:15672 (guest/guest)

## 🏗️ Project Structure

```
user-onboard/
├── backend/                 # Spring Boot application
├── frontend/               # React application
├── infrastructure/         # K8s, Helm, Terraform configs
├── docs/                   # Documentation
├── scripts/                # Helper scripts and DB migrations
├── .github/workflows/      # CI/CD pipelines
├── .devcontainer/         # Development container config
├── .vscode/               # VS Code settings
├── docker-compose.yml     # Local development environment
├── Makefile              # Common development commands
└── README.md             # This file
```

## 🛠️ Development with GitHub Copilot

This workspace is optimized for GitHub Copilot development. Follow these steps:

### 1. Open in Development Container
- Open VS Code
- Press `Ctrl+Shift+P` (or `Cmd+Shift+P`)
- Select "Dev Containers: Reopen in Container"
- Wait for container setup to complete

### 2. Generate Application Code
Use the provided Copilot prompts to generate the complete application:

1. **Backend Development**: 
   - Navigate to `backend/` directory
   - Open Copilot Chat
   - Paste content from `BACKEND_SETUP_COPILOT_PROMPT.md`

2. **Frontend Development**:
   - Navigate to `frontend/` directory  
   - Open Copilot Chat
   - Paste content from `FRONTEND_SETUP_COPILOT_PROMPT.md`

3. **Infrastructure Setup**:
   - Navigate to `infrastructure/` directory
   - Open Copilot Chat
   - Paste content from `DEVOPS_INFRA_SETUP_COPILOT_PROMPT.md`

📖 **Detailed Instructions**: See `docs/copilot-guides.md` for complete step-by-step guidance.

## 🗄️ Database Options

### Default: MSSQL (Local Development)
```bash
# Uses SQL Server 2019 in Docker
make up
```

### Alternative: Oracle XE
To switch to Oracle:

1. Edit `docker-compose.yml` - uncomment Oracle service, comment MSSQL
2. Update `.env.local`:
   ```bash
   DB_TYPE=oracle
   JDBC_URL=${ORACLE_JDBC_URL}
   ORACLE_SYS_PASSWORD=YourOraclePassword123
   ```
3. Restart: `make down && make up`

📖 **Database Guide**: See `infrastructure/README.md` for detailed database configuration.

## 🧪 Testing

```bash
# Run all tests
make test

# Run backend tests only  
make backend-test

# Run frontend tests only
make frontend-test
```

## 🐳 Docker Commands

```bash
# Start all services
make up

# Stop all services  
make down

# View logs
make logs

# Rebuild containers
make docker-build

# Clean up everything
make clean
```

## 🔧 Available Make Commands

| Command | Description |
|---------|-------------|
| `make help` | Show all available commands |
| `make build` | Build all services |
| `make up` | Start all services in background |
| `make down` | Stop all services |
| `make test` | Run all tests |
| `make clean` | Clean up containers and build artifacts |
| `make logs` | View service logs |
| `make restart` | Restart all services |
| `make init` | Initialize development environment |
| `make generate-keys` | Generate JWT RSA key pair |

## 🔑 Security Setup

### JWT Keys (Required)
```bash
# Generate RSA key pair for JWT signing
make generate-keys

# Keys will be created in ./keys/ directory
# Never commit these keys to version control!
```

### Environment Variables
Create `.env.local` from `.env.example` and configure:
- Database passwords
- JWT key paths  
- API URLs
- Any cloud service credentials

## 📦 Service Architecture

### Backend (Port 8080)
- **Framework**: Spring Boot 3.x with Java 17
- **Database**: MSSQL or Oracle with JPA/Hibernate
- **Security**: JWT-based authentication
- **Messaging**: RabbitMQ for async processing
- **API**: RESTful endpoints with OpenAPI documentation

### Frontend (Port 3000)  
- **Framework**: React 18+ with TypeScript
- **Styling**: Tailwind CSS (responsive design)
- **State**: React Query for server state
- **Forms**: React Hook Form with validation
- **Routing**: React Router for navigation

### Infrastructure
- **Containerization**: Docker with multi-stage builds
- **Orchestration**: Docker Compose (local), Kubernetes (production)
- **CI/CD**: GitHub Actions with automated testing
- **Monitoring**: Health checks and logging

## 🚀 Deployment

### Local Development
Uses Docker Compose with local database and message queue.

### Production Options
- **Kubernetes**: Use Helm charts in `infrastructure/helm/`
- **Azure**: ARM templates in `infrastructure/azure/`
- **AWS**: Terraform configs in `infrastructure/terraform/`

### Required Secrets (Production)
- Database connection strings
- JWT signing keys
- Cloud service credentials
- Container registry access

## 🔍 Monitoring & Debugging

### Health Checks
- Backend: http://localhost:8080/actuator/health
- Frontend: http://localhost:3000 (should load app)
- Database: Container health checks configured

### Logs
```bash
# All services
make logs

# Specific service
docker-compose logs backend
docker-compose logs frontend
docker-compose logs mssql
```

### Database Access
```bash
# MSSQL
docker exec -it user-onboard_mssql_1 /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P YourPassword

# Oracle (when enabled)
docker exec -it user-onboard_oracle_1 sqlplus system/password@//localhost:1521/XEPDB1
```

## 🤝 Contributing

1. Create feature branch from `main`
2. Use GitHub Copilot to implement features
3. Ensure all tests pass: `make test`
4. Create pull request with clear description
5. Automated CI will run tests and build validation

## 📚 Documentation

- `docs/copilot-guides.md` - Complete Copilot development workflow
- `infrastructure/README.md` - Database and deployment options
- `scripts/db-init/README.md` - Database schema and migration info

## ⚠️ Important Notes

### Security
- **Never commit** `.env.local` or files in `keys/` directory
- Use strong passwords for database accounts
- Rotate JWT keys regularly in production
- Review all Copilot-generated security code

### Database Switching
- Test thoroughly when switching between MSSQL and Oracle
- Some SQL syntax differences may require manual adjustments
- Ensure proper JDBC drivers are included

### Development
- Always run `make test` before committing
- Use provided VS Code settings for consistent formatting
- Follow the established project structure when adding new features

## 🆘 Troubleshooting

### Common Issues

**Services won't start:**
```bash
make clean
make up
```

**Database connection errors:**
```bash
# Check if DB is running
docker ps | grep mssql
# Reset database
make db-reset
```

**Frontend not loading:**
```bash
# Check frontend logs
docker-compose logs frontend
# Rebuild if needed
make docker-build
```

**Port conflicts:**
```bash
# Check what's using the ports
netstat -tulpn | grep :3000
netstat -tulpn | grep :8080
```

For more help, check the detailed documentation in the `docs/` directory or create an issue in the project repository.
