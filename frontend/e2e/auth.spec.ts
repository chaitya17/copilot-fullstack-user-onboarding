import { test, expect } from '@playwright/test';

test.describe('User Authentication Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to the application
    await page.goto('/');
  });

  test('should redirect unauthenticated user to login', async ({ page }) => {
    // Should redirect to login page
    await expect(page).toHaveURL('/login');
    await expect(page.locator('h1')).toContainText('Sign In');
  });

  test('should display registration form', async ({ page }) => {
    await page.goto('/register');

    await expect(page.locator('h1')).toContainText('Sign Up');
    await expect(page.getByLabel('First Name')).toBeVisible();
    await expect(page.getByLabel('Last Name')).toBeVisible();
    await expect(page.getByLabel('Email Address')).toBeVisible();
    await expect(page.getByLabel('Password')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sign Up' })).toBeVisible();
  });

  test('should show validation errors on empty login form', async ({ page }) => {
    await page.goto('/login');

    // Try to submit empty form
    await page.getByRole('button', { name: 'Sign In' }).click();

    // Check for validation errors
    await expect(page.getByText('Email is required')).toBeVisible();
    await expect(page.getByText('Password is required')).toBeVisible();
  });

  test('should show validation errors on invalid email', async ({ page }) => {
    await page.goto('/login');

    // Enter invalid email
    await page.getByLabel('Email Address').fill('invalid-email');
    await page.getByLabel('Email Address').blur();

    // Check for email validation error
    await expect(page.getByText('Please enter a valid email address')).toBeVisible();
  });

  test('should navigate between login and register pages', async ({ page }) => {
    await page.goto('/login');

    // Click on register link
    await page.getByText('Don\'t have an account? Sign Up').click();
    await expect(page).toHaveURL('/register');

    // Click on login link
    await page.getByText('Already have an account? Sign In').click();
    await expect(page).toHaveURL('/login');
  });

  // TODO: Add authenticated user tests when backend is available
  // test('should login with valid credentials', async ({ page }) => {
  //   await page.goto('/login');
  //
  //   await page.getByLabel('Email Address').fill('admin@useronboard.com');
  //   await page.getByLabel('Password').fill('admin123');
  //   await page.getByRole('button', { name: 'Sign In' }).click();
  //
  //   await expect(page).toHaveURL('/dashboard');
  //   await expect(page.locator('h4')).toContainText('Dashboard');
  // });
});
