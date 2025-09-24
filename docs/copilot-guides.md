# GitHub Copilot Development Guides

This document provides step-by-step instructions for using GitHub Copilot to generate the complete User Onboarding application.

## Prerequisites

1. **Workspace Setup Complete**: Ensure you've run through the initial setup:
   ```bash
   # Copy environment file and edit with your values
   cp .env.example .env.local
   
   # Generate JWT keys
   make generate-keys
   
   # Start the development environment
   make up
   ```

2. **GitHub Copilot Extensions**: Ensure you have these VS Code extensions installed:
   - GitHub Copilot
   - GitHub Copilot Chat
   - Java Extension Pack
   - Docker Extension

## Step 1: Backend Development with Copilot

**Directory**: Navigate to the `backend/` folder in your terminal and VS Code

**Copilot Prompt to Use**: Copy and paste the content from `BACKEND_SETUP_COPILOT_PROMPT.md`

**Where to Paste**:
1. Open VS Code in the `backend/` directory
2. Open GitHub Copilot Chat (Ctrl+Shift+I or Cmd+Shift+I)
3. Paste the entire backend prompt
4. Follow Copilot's suggestions to generate:
   - Spring Boot project structure
   - JPA entities and repositories
   - REST controllers
   - Security configuration with JWT
   - Database migration scripts
   - Unit and integration tests

**Expected Outputs**:
- `pom.xml` with all required dependencies
- `src/main/java/` with complete application code
- `src/main/resources/` with configuration files
- `src/test/java/` with comprehensive tests
- Dockerfile for containerization

**Validation**:
```bash
# Test the backend
cd backend
./mvnw test
./mvnw spring-boot:run
```

## Step 2: Frontend Development with Copilot

**Directory**: Navigate to the `frontend/` folder

**Copilot Prompt to Use**: Copy and paste the content from `FRONTEND_SETUP_COPILOT_PROMPT.md`

**Where to Paste**:
1. Open VS Code in the `frontend/` directory
2. Open GitHub Copilot Chat
3. Paste the entire frontend prompt
4. Follow Copilot's suggestions to generate:
   - React/Vue.js project setup
   - Component structure for user onboarding
   - API integration with the backend
   - Form validation and state management
   - Responsive UI components
   - Unit tests for components

**Expected Outputs**:
- `package.json` with dependencies
- `src/` directory with React/Vue components
- `src/components/` for UI components
- `src/services/` for API calls
- `src/tests/` for component tests
- Dockerfile for production builds

**Validation**:
```bash
# Test the frontend
cd frontend
npm install
npm test
npm start
```

## Step 3: DevOps and Infrastructure with Copilot

**Directory**: Navigate to the `infrastructure/` folder

**Copilot Prompt to Use**: Copy and paste the content from `DEVOPS_INFRA_SETUP_COPILOT_PROMPT.md`

**Where to Paste**:
1. Open VS Code in the `infrastructure/` directory
2. Open GitHub Copilot Chat
3. Paste the entire DevOps prompt
4. Follow Copilot's suggestions to generate:
   - Kubernetes manifests
   - Helm charts
   - Azure Resource Manager templates
   - Terraform configurations
   - Docker production configurations
   - CI/CD pipeline enhancements

**Expected Outputs**:
- `k8s/` directory with Kubernetes YAML files
- `helm/` directory with Helm chart
- `terraform/` directory with infrastructure as code
- `azure/` directory with ARM templates
- Enhanced GitHub Actions workflows

**Validation**:
```bash
# Validate Kubernetes configs
kubectl --dry-run=client apply -f infrastructure/k8s/

# Test Helm chart
helm template user-onboard infrastructure/helm/user-onboard/

# Validate Terraform
cd infrastructure/terraform
terraform init
terraform plan
```

## Copilot Best Practices

### Getting Better Results

1. **Be Specific**: Include exact technology requirements in your prompts
2. **Context Matters**: Ensure Copilot understands your project structure
3. **Iterative Development**: Use Copilot for incremental improvements
4. **Code Review**: Always review and test Copilot-generated code

### Effective Prompting Techniques

1. **Start with Architecture**: "Create a Spring Boot application with..."
2. **Specify Standards**: "Following REST API best practices..."
3. **Include Security**: "With JWT authentication and RBAC..."
4. **Request Tests**: "Include unit tests and integration tests..."

### Common Copilot Commands

- `Ctrl+I` (Cmd+I): Inline code suggestions
- `Ctrl+Shift+I` (Cmd+Shift+I): Open Copilot Chat
- `/explain`: Explain selected code
- `/fix`: Fix issues in selected code
- `/tests`: Generate tests for selected code
- `/doc`: Generate documentation

## Troubleshooting

### Database Connection Issues
```bash
# Check if database is running
docker ps | grep mssql

# Reset database
make db-reset

# Check logs
docker-compose logs mssql
```

### Build Failures
```bash
# Clean and rebuild
make clean
make build

# Check individual service logs
docker-compose logs backend
docker-compose logs frontend
```

### Copilot Not Working
1. Ensure you're signed in to GitHub in VS Code
2. Check Copilot subscription status
3. Reload VS Code window
4. Clear Copilot cache in VS Code settings

## Development Workflow

### Daily Development
1. `make up` - Start all services
2. Use Copilot to implement features
3. `make test` - Run all tests
4. `make logs` - Check service logs
5. Commit and push changes

### Feature Development
1. Create feature branch
2. Use Copilot Chat to plan implementation
3. Generate code with Copilot suggestions
4. Write tests (ask Copilot to help)
5. Test locally with `make up`
6. Create pull request

### Code Quality
- Let Copilot suggest improvements
- Use `/fix` for error resolution
- Generate documentation with `/doc`
- Review security implications of generated code

## Next Steps After Copilot Generation

1. **Security Review**: Audit generated security code
2. **Performance Testing**: Load test the application
3. **Integration Testing**: Test end-to-end workflows
4. **Documentation**: Generate API docs and user guides
5. **Deployment**: Use generated infrastructure code for production

## Resources

- [GitHub Copilot Documentation](https://docs.github.com/en/copilot)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev/)
- [Docker Documentation](https://docs.docker.com/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
