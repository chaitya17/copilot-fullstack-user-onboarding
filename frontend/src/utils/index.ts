import { UserStatus } from '@/types';

/**
 * Format user status for display
 */
export const formatUserStatus = (status: UserStatus): string => {
  switch (status) {
    case UserStatus.PENDING:
      return 'Pending Approval';
    case UserStatus.ACTIVE:
      return 'Active';
    case UserStatus.REJECTED:
      return 'Rejected';
    default:
      return 'Unknown';
  }
};

/**
 * Get status color for MUI components
 */
export const getStatusColor = (status: UserStatus): 'warning' | 'success' | 'error' | 'default' => {
  switch (status) {
    case UserStatus.PENDING:
      return 'warning';
    case UserStatus.ACTIVE:
      return 'success';
    case UserStatus.REJECTED:
      return 'error';
    default:
      return 'default';
  }
};

/**
 * Check if user has specific role
 */
export const hasRole = (userRoles: string, role: string): boolean => {
  return userRoles.split(',').map(r => r.trim()).includes(role);
};

/**
 * Check if user is admin
 */
export const isAdmin = (userRoles: string): boolean => {
  return hasRole(userRoles, 'ADMIN');
};

/**
 * Format date for display
 */
export const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

/**
 * Validate email format
 */
export const isValidEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

/**
 * Validate password strength
 */
export const validatePassword = (password: string): { isValid: boolean; errors: string[] } => {
  const errors: string[] = [];

  if (password.length < 8) {
    errors.push('Password must be at least 8 characters long');
  }

  if (!/(?=.*[a-z])/.test(password)) {
    errors.push('Password must contain at least one lowercase letter');
  }

  if (!/(?=.*[A-Z])/.test(password)) {
    errors.push('Password must contain at least one uppercase letter');
  }

  if (!/(?=.*\d)/.test(password)) {
    errors.push('Password must contain at least one number');
  }

  return {
    isValid: errors.length === 0,
    errors,
  };
};

/**
 * Debounce function
 */
export const debounce = <T extends (...args: any[]) => any>(
  func: T,
  delay: number
): ((...args: Parameters<T>) => void) => {
  let timeoutId: NodeJS.Timeout;

  return (...args: Parameters<T>) => {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => func.apply(null, args), delay);
  };
};

/**
 * Handle API error and return user-friendly message
 */
export const getErrorMessage = (error: any): string => {
  if (error?.message) {
    return error.message;
  }

  if (typeof error === 'string') {
    return error;
  }

  return 'An unexpected error occurred. Please try again.';
};
