import React from 'react';
import {
  Container,
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  Chip,
  Button,
  Alert,
  Grid,
} from '@mui/material';
import {
  Person,
  Email,
  Phone,
  CalendarToday,
  CheckCircle,
  Schedule,
  Cancel,
} from '@mui/icons-material';
import { useAuth } from '@/contexts/AuthContext';
import { UserStatus } from '@/types';
import { formatUserStatus, getStatusColor, formatDate, isAdmin } from '@/utils';

const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();

  if (!user) {
    return null; // This should be handled by routing protection
  }

  const getStatusIcon = (status: UserStatus) => {
    switch (status) {
      case UserStatus.PENDING:
        return <Schedule color="warning" />;
      case UserStatus.ACTIVE:
        return <CheckCircle color="success" />;
      case UserStatus.REJECTED:
        return <Cancel color="error" />;
      default:
        return <Schedule />;
    }
  };

  const getStatusMessage = (status: UserStatus) => {
    switch (status) {
      case UserStatus.PENDING:
        return 'Your account is currently under review. You will receive an email notification once your account has been approved.';
      case UserStatus.ACTIVE:
        return 'Welcome! Your account is active and you have full access to the platform.';
      case UserStatus.REJECTED:
        return 'Your account application was not approved. Please contact support if you have questions.';
      default:
        return 'Account status unknown.';
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      console.error('Logout error:', error);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" gutterBottom>
          Dashboard
        </Typography>
        <Typography variant="subtitle1" color="text.secondary">
          Welcome back, {user.firstName} {user.lastName}
        </Typography>
      </Box>

      <Grid container spacing={3}>
        {/* Account Status Card */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                {getStatusIcon(user.status)}
                <Typography variant="h6" sx={{ ml: 1 }}>
                  Account Status
                </Typography>
                <Chip
                  label={formatUserStatus(user.status)}
                  color={getStatusColor(user.status)}
                  sx={{ ml: 'auto' }}
                />
              </Box>

              <Alert
                severity={user.status === UserStatus.ACTIVE ? 'success' :
                         user.status === UserStatus.PENDING ? 'warning' : 'error'}
                sx={{ mb: 2 }}
              >
                {getStatusMessage(user.status)}
              </Alert>

              {user.status === UserStatus.ACTIVE && isAdmin(user.roles) && (
                <Alert severity="info" sx={{ mt: 2 }}>
                  You have administrator privileges. You can manage user approvals in the admin panel.
                </Alert>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Quick Actions Card */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Quick Actions
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                {isAdmin(user.roles) && user.status === UserStatus.ACTIVE && (
                  <Button
                    variant="contained"
                    href="/admin"
                    fullWidth
                  >
                    Admin Panel
                  </Button>
                )}
                <Button
                  variant="outlined"
                  onClick={handleLogout}
                  fullWidth
                >
                  Logout
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Profile Information Card */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Profile Information
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6} md={3}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <Person sx={{ mr: 1, color: 'text.secondary' }} />
                    <Typography variant="body2" color="text.secondary">
                      Name
                    </Typography>
                  </Box>
                  <Typography variant="body1">
                    {user.firstName} {user.lastName}
                  </Typography>
                </Grid>

                <Grid item xs={12} sm={6} md={3}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <Email sx={{ mr: 1, color: 'text.secondary' }} />
                    <Typography variant="body2" color="text.secondary">
                      Email
                    </Typography>
                  </Box>
                  <Typography variant="body1">{user.email}</Typography>
                </Grid>

                {user.phone && (
                  <Grid item xs={12} sm={6} md={3}>
                    <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                      <Phone sx={{ mr: 1, color: 'text.secondary' }} />
                      <Typography variant="body2" color="text.secondary">
                        Phone
                      </Typography>
                    </Box>
                    <Typography variant="body1">{user.phone}</Typography>
                  </Grid>
                )}

                <Grid item xs={12} sm={6} md={3}>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <CalendarToday sx={{ mr: 1, color: 'text.secondary' }} />
                    <Typography variant="body2" color="text.secondary">
                      Member Since
                    </Typography>
                  </Box>
                  <Typography variant="body1">
                    {formatDate(user.createdAt)}
                  </Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Container>
  );
};

export default Dashboard;
