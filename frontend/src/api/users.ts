import { apiRequest } from './client';
import { ApiResponse, User, PaginatedResponse, AdminActionRequest, UserStatistics } from '@/types';

// Users API endpoints
export const usersAPI = {
  /**
   * Get user by ID
   * GET /api/v1/users/{userId}
   */
  getUserById: async (userId: string): Promise<User> => {
    const response = await apiRequest.get<ApiResponse<User>>(`/api/v1/users/${userId}`);

    if (response.success && response.data) {
      return response.data;
    }

    throw new Error(response.error || 'Failed to fetch user');
  },

  /**
   * Get all pending users (Admin only)
   * GET /api/v1/admin/users/pending
   */
  getPendingUsers: async (): Promise<User[]> => {
    const response = await apiRequest.get<ApiResponse<User[]>>('/api/v1/admin/users/pending');

    if (response.success && response.data) {
      return response.data;
    }

    throw new Error(response.error || 'Failed to fetch pending users');
  },

  /**
   * Get all users with pagination (Admin only)
   * GET /api/v1/admin/users
   */
  getAllUsers: async (page = 0, size = 10, sortBy = 'createdAt', sortDir = 'desc'): Promise<PaginatedResponse<User>> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sortBy,
      sortDir,
    });

    const response = await apiRequest.get<ApiResponse<PaginatedResponse<User>>>(
      `/api/v1/admin/users?${params}`
    );

    if (response.success && response.data) {
      return response.data;
    }

    throw new Error(response.error || 'Failed to fetch users');
  },

  /**
   * Approve user (Admin only)
   * POST /api/v1/admin/users/{userId}/approve
   */
  approveUser: async (userId: string, reason: string): Promise<User> => {
    const request: AdminActionRequest = {
      action: 'APPROVE',
      reason,
    };

    const response = await apiRequest.post<ApiResponse<User>>(
      `/api/v1/admin/users/${userId}/approve`,
      request
    );

    if (response.success && response.data) {
      return response.data;
    }

    throw new Error(response.error || 'Failed to approve user');
  },

  /**
   * Reject user (Admin only)
   * POST /api/v1/admin/users/{userId}/reject
   */
  rejectUser: async (userId: string, reason: string): Promise<User> => {
    const request: AdminActionRequest = {
      action: 'REJECT',
      reason,
    };

    const response = await apiRequest.post<ApiResponse<User>>(
      `/api/v1/admin/users/${userId}/reject`,
      request
    );

    if (response.success && response.data) {
      return response.data;
    }

    throw new Error(response.error || 'Failed to reject user');
  },

  /**
   * Get user statistics (Admin only)
   * GET /api/v1/admin/statistics
   */
  getUserStatistics: async (): Promise<UserStatistics> => {
    const response = await apiRequest.get<ApiResponse<UserStatistics>>('/api/v1/admin/statistics');

    if (response.success && response.data) {
      return response.data;
    }

    throw new Error(response.error || 'Failed to fetch user statistics');
  },
};
