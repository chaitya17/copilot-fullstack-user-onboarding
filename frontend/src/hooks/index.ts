import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usersAPI } from '@/api/users';
import { User, UserStatistics } from '@/types';
import { useAuth } from '@/contexts/AuthContext';
import { isAdmin } from '@/utils';

/**
 * Hook for managing pending users (Admin only)
 */
export const usePendingUsers = () => {
  const { user } = useAuth();

  return useQuery({
    queryKey: ['users', 'pending'],
    queryFn: usersAPI.getPendingUsers,
    enabled: user !== null && isAdmin(user.roles),
    refetchOnWindowFocus: false,
  });
};

/**
 * Hook for managing all users with pagination (Admin only)
 */
export const useUsers = (page = 0, size = 10, sortBy = 'createdAt', sortDir = 'desc') => {
  const { user } = useAuth();

  return useQuery({
    queryKey: ['users', 'all', page, size, sortBy, sortDir],
    queryFn: () => usersAPI.getAllUsers(page, size, sortBy, sortDir),
    enabled: user !== null && isAdmin(user.roles),
    keepPreviousData: true,
  });
};

/**
 * Hook for user statistics (Admin only)
 */
export const useUserStatistics = () => {
  const { user } = useAuth();

  return useQuery({
    queryKey: ['users', 'statistics'],
    queryFn: usersAPI.getUserStatistics,
    enabled: user !== null && isAdmin(user.roles),
    refetchInterval: 30000, // Refresh every 30 seconds
  });
};

/**
 * Hook for approving users
 */
export const useApproveUser = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, reason }: { userId: string; reason: string }) =>
      usersAPI.approveUser(userId, reason),
    onSuccess: () => {
      // Invalidate related queries
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });
};

/**
 * Hook for rejecting users
 */
export const useRejectUser = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, reason }: { userId: string; reason: string }) =>
      usersAPI.rejectUser(userId, reason),
    onSuccess: () => {
      // Invalidate related queries
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });
};

/**
 * Hook for managing loading states
 */
export const useLoading = (initialState = false) => {
  const [isLoading, setIsLoading] = useState(initialState);

  const startLoading = () => setIsLoading(true);
  const stopLoading = () => setIsLoading(false);

  return { isLoading, startLoading, stopLoading };
};

/**
 * Hook for managing form validation
 */
export const useFormValidation = <T extends Record<string, any>>(
  initialValues: T,
  validationRules: Record<keyof T, (value: any) => string | null>
) => {
  const [values, setValues] = useState<T>(initialValues);
  const [errors, setErrors] = useState<Partial<Record<keyof T, string>>>({});
  const [touched, setTouched] = useState<Partial<Record<keyof T, boolean>>>({});

  const setValue = (field: keyof T, value: any) => {
    setValues(prev => ({ ...prev, [field]: value }));

    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: undefined }));
    }
  };

  const setTouched = (field: keyof T) => {
    setTouched(prev => ({ ...prev, [field]: true }));
  };

  const validate = (): boolean => {
    const newErrors: Partial<Record<keyof T, string>> = {};
    let isValid = true;

    Object.keys(validationRules).forEach(field => {
      const key = field as keyof T;
      const error = validationRules[key](values[key]);
      if (error) {
        newErrors[key] = error;
        isValid = false;
      }
    });

    setErrors(newErrors);
    setTouched(
      Object.keys(validationRules).reduce(
        (acc, field) => ({ ...acc, [field]: true }),
        {}
      )
    );

    return isValid;
  };

  const reset = () => {
    setValues(initialValues);
    setErrors({});
    setTouched({});
  };

  return {
    values,
    errors,
    touched,
    setValue,
    setTouched,
    validate,
    reset,
  };
};

/**
 * Hook for managing local storage
 */
export const useLocalStorage = <T>(key: string, initialValue: T) => {
  const [storedValue, setStoredValue] = useState<T>(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch (error) {
      console.error(`Error reading localStorage key "${key}":`, error);
      return initialValue;
    }
  });

  const setValue = (value: T | ((val: T) => T)) => {
    try {
      const valueToStore = value instanceof Function ? value(storedValue) : value;
      setStoredValue(valueToStore);
      window.localStorage.setItem(key, JSON.stringify(valueToStore));
    } catch (error) {
      console.error(`Error setting localStorage key "${key}":`, error);
    }
  };

  return [storedValue, setValue] as const;
};
