import { apiRequest, TokenManager } from './client';
import { LoginRequest, RegisterRequest, AuthResponse, ApiResponse, User } from '@/types';

// Authentication API endpoints
export const authAPI = {
  /**
   * User login
   * POST /api/v1/auth/login
   */
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await apiRequest.post<ApiResponse<AuthResponse['data']>>(
      '/api/v1/auth/login',
      credentials
    );

    if (response.success && response.data) {
      // Store access token in memory
      TokenManager.setAccessToken(response.data.accessToken);

      return {
        success: true,
        message: response.message,
        data: response.data,
      };
    }

    throw new Error(response.error || 'Login failed');
  },

  /**
   * User registration
   * POST /api/v1/auth/register
   */
  register: async (userData: RegisterRequest): Promise<ApiResponse<User>> => {
    return apiRequest.post<ApiResponse<User>>('/api/v1/auth/register', userData);
  },

  /**
   * Refresh access token using HttpOnly cookie
   * POST /api/v1/auth/refresh
   */
  refreshToken: async (): Promise<AuthResponse> => {
    const response = await apiRequest.post<ApiResponse<AuthResponse['data']>>(
      '/api/v1/auth/refresh'
    );

    if (response.success && response.data) {
      // Update access token in memory
      TokenManager.setAccessToken(response.data.accessToken);

      return {
        success: true,
        message: response.message,
        data: response.data,
      };
    }

    throw new Error(response.error || 'Token refresh failed');
  },

  /**
   * User logout
   * POST /api/v1/auth/logout
   */
  logout: async (): Promise<void> => {
    try {
      await apiRequest.post('/api/v1/auth/logout');
    } finally {
      // Clear tokens regardless of API call result
      TokenManager.clearTokens();
    }
  },

  /**
   * Get current user profile
   * GET /api/v1/users/me
   */
  getCurrentUser: async (): Promise<User> => {
    const response = await apiRequest.get<ApiResponse<User>>('/api/v1/users/me');

    if (response.success && response.data) {
      return response.data;
    }

    throw new Error(response.error || 'Failed to fetch user profile');
  },

  /**
   * Check if user is authenticated
   */
  isAuthenticated: (): boolean => {
    return TokenManager.getAccessToken() !== null;
  },

  /**
   * Clear authentication state
   */
  clearAuth: (): void => {
    TokenManager.clearTokens();
  },
};
