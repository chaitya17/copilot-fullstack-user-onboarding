import React, { useState } from 'react';
import {
  Container,
  Typography,
  Box,
  Card,
  CardContent,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
  CircularProgress,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  CheckCircle,
  Cancel,
  Refresh,
  Person,
  Email,
  Schedule,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import {
  usePendingUsers,
  useUserStatistics,
  useApproveUser,
  useRejectUser,
} from '@/hooks';
import { User, AdminActionFormData } from '@/types';
import { formatUserStatus, getStatusColor, formatDate } from '@/utils';

const AdminDashboard: React.FC = () => {
  const { enqueueSnackbar } = useSnackbar();
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [actionType, setActionType] = useState<'approve' | 'reject' | null>(null);
  const [reason, setReason] = useState('');

  // Queries and mutations
  const { data: pendingUsers, isLoading: loadingPending, refetch: refetchPending } = usePendingUsers();
  const { data: statistics, isLoading: loadingStats } = useUserStatistics();
  const approveUserMutation = useApproveUser();
  const rejectUserMutation = useRejectUser();

  const handleOpenDialog = (user: User, action: 'approve' | 'reject') => {
    setSelectedUser(user);
    setActionType(action);
    setReason('');
  };

  const handleCloseDialog = () => {
    setSelectedUser(null);
    setActionType(null);
    setReason('');
  };

  const handleSubmitAction = async () => {
    if (!selectedUser || !actionType || !reason.trim()) {
      enqueueSnackbar('Please provide a reason for this action', { variant: 'error' });
      return;
    }

    try {
      if (actionType === 'approve') {
        await approveUserMutation.mutateAsync({
          userId: selectedUser.id,
          reason: reason.trim(),
        });
        enqueueSnackbar('User approved successfully', { variant: 'success' });
      } else {
        await rejectUserMutation.mutateAsync({
          userId: selectedUser.id,
          reason: reason.trim(),
        });
        enqueueSnackbar('User rejected successfully', { variant: 'success' });
      }
      handleCloseDialog();
      refetchPending();
    } catch (error) {
      enqueueSnackbar(
        error instanceof Error ? error.message : 'Action failed',
        { variant: 'error' }
      );
    }
  };

  const isActionLoading = approveUserMutation.isPending || rejectUserMutation.isPending;

  return (
    <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ mb: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" gutterBottom>
          Admin Dashboard
        </Typography>
        <Tooltip title="Refresh Data">
          <IconButton onClick={() => refetchPending()} disabled={loadingPending}>
            <Refresh />
          </IconButton>
        </Tooltip>
      </Box>

      {/* Statistics Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <Schedule color="warning" sx={{ mr: 1 }} />
                <Typography variant="h6">Pending</Typography>
              </Box>
              <Typography variant="h4" color="warning.main">
                {loadingStats ? <CircularProgress size={32} /> : statistics?.pending || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <CheckCircle color="success" sx={{ mr: 1 }} />
                <Typography variant="h6">Active</Typography>
              </Box>
              <Typography variant="h4" color="success.main">
                {loadingStats ? <CircularProgress size={32} /> : statistics?.active || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <Cancel color="error" sx={{ mr: 1 }} />
                <Typography variant="h6">Rejected</Typography>
              </Box>
              <Typography variant="h4" color="error.main">
                {loadingStats ? <CircularProgress size={32} /> : statistics?.rejected || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Pending Users Table */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Pending User Approvals
          </Typography>

          {loadingPending ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
              <CircularProgress />
            </Box>
          ) : pendingUsers?.length === 0 ? (
            <Alert severity="info">No pending users at this time.</Alert>
          ) : (
            <TableContainer component={Paper} variant="outlined">
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Email</TableCell>
                    <TableCell>Phone</TableCell>
                    <TableCell>Registration Date</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="center">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {pendingUsers?.map((user) => (
                    <TableRow key={user.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                          <Person sx={{ mr: 1, color: 'text.secondary' }} />
                          {user.firstName} {user.lastName}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                          <Email sx={{ mr: 1, color: 'text.secondary' }} />
                          {user.email}
                        </Box>
                      </TableCell>
                      <TableCell>{user.phone || 'N/A'}</TableCell>
                      <TableCell>{formatDate(user.createdAt)}</TableCell>
                      <TableCell>
                        <Chip
                          label={formatUserStatus(user.status)}
                          color={getStatusColor(user.status)}
                          size="small"
                        />
                      </TableCell>
                      <TableCell align="center">
                        <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
                          <Button
                            variant="contained"
                            color="success"
                            size="small"
                            startIcon={<CheckCircle />}
                            onClick={() => handleOpenDialog(user, 'approve')}
                            disabled={isActionLoading}
                          >
                            Approve
                          </Button>
                          <Button
                            variant="contained"
                            color="error"
                            size="small"
                            startIcon={<Cancel />}
                            onClick={() => handleOpenDialog(user, 'reject')}
                            disabled={isActionLoading}
                          >
                            Reject
                          </Button>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Action Dialog */}
      <Dialog open={!!selectedUser && !!actionType} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {actionType === 'approve' ? 'Approve User' : 'Reject User'}
        </DialogTitle>
        <DialogContent>
          {selectedUser && (
            <>
              <Typography variant="body1" gutterBottom>
                {actionType === 'approve'
                  ? `Are you sure you want to approve ${selectedUser.firstName} ${selectedUser.lastName}?`
                  : `Are you sure you want to reject ${selectedUser.firstName} ${selectedUser.lastName}?`
                }
              </Typography>
              <TextField
                autoFocus
                margin="dense"
                label="Reason"
                fullWidth
                multiline
                rows={3}
                variant="outlined"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder={
                  actionType === 'approve'
                    ? 'e.g., Application meets all requirements'
                    : 'e.g., Incomplete information provided'
                }
                required
              />
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog} disabled={isActionLoading}>
            Cancel
          </Button>
          <Button
            onClick={handleSubmitAction}
            variant="contained"
            color={actionType === 'approve' ? 'success' : 'error'}
            disabled={isActionLoading || !reason.trim()}
          >
            {isActionLoading ? <CircularProgress size={20} /> :
             actionType === 'approve' ? 'Approve' : 'Reject'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default AdminDashboard;
