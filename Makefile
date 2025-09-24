# User Onboard Project Makefile
# Common commands for development workflow

.PHONY: help build up down test clean docker-build logs restart

# Default target
help:
	@echo "Available commands:"
	@echo "  make build        - Build all services"
	@echo "  make up           - Start all services in background"
	@echo "  make down         - Stop all services"
	@echo "  make test         - Run all tests"
	@echo "  make clean        - Clean up containers, volumes, and build artifacts"
	@echo "  make docker-build - Build Docker images without cache"
	@echo "  make logs         - View logs from all services"
	@echo "  make restart      - Restart all services"
	@echo "  make backend-test - Run only backend tests"
	@echo "  make frontend-test- Run only frontend tests"

# Build all services
build:
	docker-compose build

# Start services
up:
	docker-compose up -d
	@echo "Services started. Access:"
	@echo "  Frontend: http://localhost:3000"
	@echo "  Backend:  http://localhost:8080"
	@echo "  RabbitMQ: http://localhost:15672 (guest/guest)"

# Stop services
down:
	docker-compose down

# Run all tests
test: backend-test frontend-test

# Run backend tests
backend-test:
	@echo "Running backend tests..."
	cd backend && ./mvnw test -DskipTests=false

# Run frontend tests
frontend-test:
	@echo "Running frontend tests..."
	cd frontend && npm test -- --watchAll=false

# Clean up everything
clean:
	docker-compose down -v --remove-orphans
	docker system prune -f
	@echo "Cleaning backend artifacts..."
	@if exist backend\target rmdir /s /q backend\target
	@echo "Cleaning frontend artifacts..."
	@if exist frontend\node_modules rmdir /s /q frontend\node_modules
	@if exist frontend\build rmdir /s /q frontend\build

# Build Docker images without cache
docker-build:
	docker-compose build --no-cache

# View logs
logs:
	docker-compose logs -f

# Restart services
restart: down up

# Initialize development environment
init:
	@echo "Initializing development environment..."
	@if not exist .env.local copy .env.example .env.local
	@echo "Environment file created. Please edit .env.local with your settings."
	@echo "Don't forget to generate JWT keys: make generate-keys"

# Generate JWT keys (requires OpenSSL)
generate-keys:
	@if not exist keys mkdir keys
	openssl genrsa -out keys/private_key.pem 2048
	openssl rsa -in keys/private_key.pem -pubout -out keys/public_key.pem
	@echo "JWT keys generated in ./keys/"

# Database operations
db-reset:
	docker-compose down mssql
	docker volume rm copilot-fullstack-user-onboarding_mssql_data
	docker-compose up -d mssql
	@echo "Database reset complete. Waiting for initialization..."
	timeout /t 30 /nobreak

# Switch to Oracle (helper command)
switch-to-oracle:
	@echo "To switch to Oracle:"
	@echo "1. Edit docker-compose.yml: uncomment oracle service, comment mssql"
	@echo "2. Edit .env.local: set DB_TYPE=oracle, update JDBC_URL to ORACLE_JDBC_URL"
	@echo "3. Run: make down && make up"
