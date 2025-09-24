You are an expert React + TypeScript frontend developer. Inside the already-created workspace frontend/ folder, generate a production-ready Single Page App "user-onboard-ui" that integrates with the backend service created earlier.

(This prompt is unchanged except we will document that backend DB type is irrelevant to the frontend; only note where the frontend expects `FRONTEND_API_URL` and any CORS differences.)

Tech & versions & tools:
- React 18+, TypeScript, Vite.
- React Router 6, React Query, Axios (with interceptors), MUI (Material UI), notistack.
- Auth: access token in memory, refresh token via HttpOnly cookie; Axios interceptor for 401 -> `/api/v1/auth/refresh`.
- Testing: Jest + React Testing Library; Playwright stub.

Functional pages/components:
- Sign Up (self-register), Login, Dashboard (user sees status PENDING/ACTIVE/REJECTED), Admin Dashboard (list pending, approve/reject).
- API client `src/api/client.ts`, `src/api/auth.ts`, `src/api/users.ts`.
- Dockerfile multi-stage: build and serve via nginx.

Deliverables:
- Full frontend project files, `.env.example`, tests, Dockerfile, README.

Additional note for integration: Document CORS expectations and the env var `VITE_API_URL`. No backend DB changes are needed for frontend. Provide clear TODOs where backend endpoints contract is assumed.
