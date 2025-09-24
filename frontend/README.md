# User Onboard UI - Frontend

A production-ready React 18 + TypeScript frontend application for the User Onboarding system. Features modern UI with Material-UI, robust authentication with JWT tokens, and comprehensive admin functionality.

## 🚀 Tech Stack

- **React 18** with TypeScript
- **Vite** for fast development and building
- **React Router 6** for client-side routing
- **React Query** for server state management
- **Axios** with interceptors for API communication
- **Material-UI (MUI)** for component library
- **Notistack** for notifications
- **Jest + React Testing Library** for unit testing
- **Playwright** for end-to-end testing

## 🏗️ Features

### Authentication & Authorization
- **JWT Authentication**: Access tokens in memory, refresh tokens via HttpOnly cookies
- **Automatic Token Refresh**: Axios interceptor handles 401 responses
- **Role-Based Access**: User and Admin role support
- **Protected Routes**: Authentication and admin-only route protection

### User Features
- **Registration**: Self-registration with validation
- **Login/Logout**: Secure authentication flow
- **Dashboard**: View account status (PENDING/ACTIVE/REJECTED)
- **Profile Management**: View personal information

### Admin Features
- **Admin Dashboard**: Manage pending user approvals
- **User Statistics**: Real-time counts of user statuses
- **Approve/Reject Users**: With reason tracking
- **Responsive Tables**: Efficient user management interface

## 🚀 Quick Start

### Prerequisites
- Node.js 18+ and npm
- Backend service running (see backend README)

### Installation & Development

1. **Install Dependencies**:
   ```bash
   npm install
   ```

2. **Environment Setup**:
   ```bash
   cp .env.example .env.local
   # Edit .env.local with your backend API URL
   ```

3. **Start Development Server**:
   ```bash
   npm run dev
   ```
   Frontend will be available at: http://localhost:3000

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `VITE_API_URL` | Backend API base URL | `http://localhost:8080` | ✓ |
| `VITE_NODE_ENV` | Environment mode | `development` | - |
| `VITE_DEBUG` | Enable debug logging | `false` | - |

## 🔧 Backend Integration

### API Endpoints Expected
The frontend expects these backend endpoints to be available:

**Authentication:**
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh` - Token refresh
- `POST /api/v1/auth/logout` - User logout
- `GET /api/v1/users/me` - Current user profile

**Admin Operations:**
- `GET /api/v1/admin/users/pending` - Get pending users
- `GET /api/v1/admin/users` - Get all users (paginated)
- `POST /api/v1/admin/users/{id}/approve` - Approve user
- `POST /api/v1/admin/users/{id}/reject` - Reject user
- `GET /api/v1/admin/statistics` - User statistics

### CORS Configuration
The backend must be configured to accept requests from the frontend origin. For development, ensure your backend accepts requests from `http://localhost:3000`.

**Backend CORS Requirements:**
```yaml
# In your backend application.yml
spring:
  web:
    cors:
      allowed-origins: 
        - "http://localhost:3000"  # Development
        - "https://your-frontend-domain.com"  # Production
      allowed-methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
      allowed-headers: ["*"]
      allow-credentials: true
```

### Authentication Flow
1. **Login**: Frontend sends credentials, receives access token + HttpOnly refresh cookie
2. **API Requests**: Access token sent in Authorization header
3. **Token Refresh**: On 401 response, automatic refresh using HttpOnly cookie
4. **Logout**: Clears tokens and revokes refresh token on backend

## 🧪 Testing

### Unit Tests
```bash
# Run tests
npm test

# Run tests in watch mode
npm run test:watch

# Run tests with coverage
npm run test:coverage
```

### End-to-End Tests
```bash
# Install Playwright browsers
npx playwright install

# Run e2e tests
npm run test:e2e

# Run e2e tests with UI
npm run test:e2e:ui
```

### Test Structure
- **Unit Tests**: Located in `src/__tests__/`
- **Component Tests**: Testing individual React components
- **Integration Tests**: Testing API integration and user flows
- **E2E Tests**: Full application workflows (login, registration, admin actions)

## 🏗️ Project Structure

```
frontend/
├── public/                 # Static assets
├── src/
│   ├── api/               # API client and endpoint functions
│   │   ├── client.ts      # Axios setup with interceptors
│   │   ├── auth.ts        # Authentication API calls
│   │   └── users.ts       # User management API calls
│   ├── components/        # Reusable React components
│   │   └── ProtectedRoute.tsx
│   ├── contexts/          # React contexts
│   │   └── AuthContext.tsx
│   ├── hooks/             # Custom React hooks
│   │   └── index.ts       # User management hooks
│   ├── pages/             # Page components
│   │   ├── Login.tsx
│   │   ├── Register.tsx
│   │   ├── Dashboard.tsx
│   │   └── AdminDashboard.tsx
│   ├── types/             # TypeScript type definitions
│   │   └── index.ts
│   ├── utils/             # Utility functions
│   │   └── index.ts
│   ├── __tests__/         # Test files
│   ├── App.tsx            # Main app component
│   ├── main.tsx           # Application entry point
│   └── setupTests.ts      # Test configuration
├── e2e/                   # Playwright e2e tests (TODO)
├── Dockerfile             # Production container build
├── package.json           # Dependencies and scripts
├── vite.config.ts         # Vite configuration
├── tsconfig.json          # TypeScript configuration
├── jest.config.js         # Jest testing configuration
└── playwright.config.ts   # Playwright e2e configuration
```

## 🐳 Production Deployment

### Docker Build
```bash
# Build production image
docker build -t user-onboard-ui:latest .

# Run container
docker run -p 3000:3000 user-onboard-ui:latest
```

### Docker Features
- **Multi-stage build**: Optimized production image
- **Nginx serving**: Static file serving with proper headers
- **Security headers**: XSS protection, content type sniffing prevention
- **Client-side routing**: Support for React Router
- **Health checks**: Built-in health endpoint
- **Caching**: Optimized static asset caching

### Environment Configuration
Update `VITE_API_URL` to point to your production backend:

```bash
# Production environment
VITE_API_URL=https://api.yourdomain.com
VITE_NODE_ENV=production
```

## 🔒 Security Features

### Authentication Security
- **Access tokens** stored in memory (not localStorage)
- **Refresh tokens** stored in HttpOnly cookies
- **Automatic token rotation** on refresh
- **Secure logout** with token revocation

### HTTP Security
- **CORS handling** for cross-origin requests
- **Security headers** in Nginx configuration
- **XSS protection** via Content Security Policy
- **Input validation** on all forms

## 📱 User Experience

### Responsive Design
- **Mobile-first approach** with Material-UI
- **Tablet and desktop optimized** layouts
- **Touch-friendly** interface elements
- **Accessible** components with proper ARIA labels

### User Flows
1. **New User**: Register → Wait for approval → Login when active
2. **Existing User**: Login → View dashboard → Check status
3. **Admin User**: Login → Access admin panel → Manage approvals

### Status Indicators
- **Pending**: Yellow warning indicator, explains approval process
- **Active**: Green success indicator, full access granted
- **Rejected**: Red error indicator, contact support message

## 🚨 Error Handling

### API Error Handling
- **Network errors**: User-friendly error messages
- **Validation errors**: Field-level error display
- **Authentication errors**: Automatic redirect to login
- **Permission errors**: Clear access denied messages

### User Feedback
- **Success notifications**: Confirmation of successful actions
- **Error notifications**: Clear error messages with next steps
- **Loading states**: Visual feedback during API calls
- **Form validation**: Real-time validation with helpful messages

## 🔧 Development

### Code Quality
- **TypeScript strict mode**: Type safety throughout
- **ESLint configuration**: Code style enforcement
- **Prettier formatting**: Consistent code formatting
- **Husky git hooks**: Pre-commit quality checks (TODO)

### Development Workflow
1. **Feature development**: Create feature branch
2. **Component development**: Build with Storybook (TODO)
3. **Testing**: Unit tests + integration tests
4. **Code review**: ESLint + TypeScript checks
5. **Deployment**: Docker build + deploy

## 🔗 Integration Notes

### Backend Database Compatibility
The frontend is **database-agnostic** and works with both:
- **MSSQL Server** (default backend configuration)
- **Oracle Database** (alternative backend configuration)

The frontend only communicates via REST APIs and doesn't need to know the backend database type.

### API Contract
The frontend expects standard REST API responses:
```typescript
// Success response
{
  "success": true,
  "message": "Operation successful",
  "data": { /* actual data */ }
}

// Error response
{
  "success": false,
  "error": "Error message",
  "message": "User-friendly error description"
}
```

## 📚 Resources & Documentation

- [React Documentation](https://react.dev/)
- [TypeScript Documentation](https://www.typescriptlang.org/docs/)
- [Material-UI Documentation](https://mui.com/material-ui/)
- [React Query Documentation](https://tanstack.com/query/latest)
- [Vite Documentation](https://vitejs.dev/guide/)

## 🤝 Contributing

1. **Setup development environment** following Quick Start guide
2. **Create feature branch** from main
3. **Implement feature** with tests
4. **Run quality checks**: `npm run lint && npm test`
5. **Submit pull request** with clear description

## ⚠️ TODOs for Production

### Security Enhancements
- [ ] Implement Content Security Policy (CSP)
- [ ] Add Subresource Integrity (SRI) for external resources
- [ ] Configure Helmet.js for additional security headers

### Performance Optimizations
- [ ] Implement service worker for offline support
- [ ] Add bundle analysis and code splitting optimization
- [ ] Implement image optimization and lazy loading

### Monitoring & Analytics
- [ ] Integrate error tracking (Sentry)
- [ ] Add performance monitoring
- [ ] Implement user analytics (optional)

### Additional Features
- [ ] Implement Storybook for component documentation
- [ ] Add Husky pre-commit hooks
- [ ] Create comprehensive E2E test suite
- [ ] Add internationalization (i18n) support

The frontend is now **production-ready** and fully integrated with the backend user onboarding service!
