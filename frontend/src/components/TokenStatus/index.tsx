import { useState, useEffect } from 'react';
import { Box, Typography, Chip, Button, Paper } from '@mui/material';
import { TokenService } from '../../services/tokenService';

export function TokenStatus() {
  const [tokenData, setTokenData] = useState<any>(null);
  const [, setUpdateTrigger] = useState(0);

  const forceUpdate = () => setUpdateTrigger(prev => prev + 1);

  useEffect(() => {
    const updateTokenData = () => {
      const data = TokenService.getTokenData();
      setTokenData(data);
    };

    updateTokenData();
    const interval = setInterval(updateTokenData, 1000);

    return () => clearInterval(interval);
  }, []);

  if (!tokenData) {
    return (
      <Paper sx={{ p: 2, m: 2 }}>
        <Typography variant="h6">Token Status</Typography>
        <Typography color="error">Nenhum token disponível</Typography>
      </Paper>
    );
  }

  const tokenExpiration = TokenService.getTokenExpiration();
  const refreshExpiration = TokenService.getRefreshExpiration();
  const now = new Date();

  const getTimeRemaining = (date: Date | null) => {
    if (!date) return 'N/A';
    const diff = date.getTime() - now.getTime();
    if (diff <= 0) return 'Expirado';
    
    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (days > 0) return `${days}d ${hours % 24}h`;
    if (hours > 0) return `${hours}h ${minutes % 60}m`;
    return `${minutes}m`;
  };

  const getTokenStatus = () => {
    if (TokenService.isRefreshTokenExpired()) {
      return { status: 'Refresh Expirado', color: 'error' as const };
    }
    if (TokenService.isTokenExpired()) {
      return { status: 'Access Expirado', color: 'warning' as const };
    }
    return { status: 'Válido', color: 'success' as const };
  };

  const tokenStatus = getTokenStatus();

  return (
    <Paper sx={{ p: 2, m: 2 }}>
      <Typography variant="h6" gutterBottom>
        Token Status (Debug)
      </Typography>
      
      <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
        <Chip 
          label={tokenStatus.status} 
          color={tokenStatus.color}
          size="small"
        />
        <Button 
          size="small" 
          onClick={forceUpdate}
          variant="outlined"
        >
          Atualizar
        </Button>
      </Box>

      <Typography variant="body2" gutterBottom>
        <strong>Access Token:</strong> {getTimeRemaining(tokenExpiration)}
      </Typography>
      
      <Typography variant="body2" gutterBottom>
        <strong>Refresh Token:</strong> {getTimeRemaining(refreshExpiration)}
      </Typography>

      <Typography variant="body2" color="text.secondary">
        <strong>Token:</strong> {tokenData.token.substring(0, 20)}...
      </Typography>
      
      <Typography variant="body2" color="text.secondary">
        <strong>Refresh:</strong> {tokenData.refreshToken.substring(0, 20)}...
      </Typography>
    </Paper>
  );
} 