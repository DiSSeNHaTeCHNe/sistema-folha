import { Button, Container, Typography } from '@mui/material';
import { Notification } from '../../components/Notification';
import { useNotification } from '../../hooks/useNotification';

export function Example() {
  const { notification, showNotification, hideNotification } = useNotification();

  const handleShowSuccess = () => {
    showNotification('Operação realizada com sucesso!', 'success');
  };

  const handleShowError = () => {
    showNotification('Ocorreu um erro ao realizar a operação.', 'error');
  };

  const handleShowWarning = () => {
    showNotification('Atenção! Esta ação pode ter consequências.', 'warning');
  };

  const handleShowInfo = () => {
    showNotification('Informação importante para o usuário.', 'info');
  };

  return (
    <Container>
      <Typography variant="h4" component="h1" gutterBottom>
        Exemplo de Notificações
      </Typography>

      <div style={{ display: 'flex', gap: '1rem', marginBottom: '2rem' }}>
        <Button variant="contained" color="success" onClick={handleShowSuccess}>
          Sucesso
        </Button>
        <Button variant="contained" color="error" onClick={handleShowError}>
          Erro
        </Button>
        <Button variant="contained" color="warning" onClick={handleShowWarning}>
          Alerta
        </Button>
        <Button variant="contained" color="info" onClick={handleShowInfo}>
          Informação
        </Button>
      </div>

      <Notification
        open={notification.open}
        message={notification.message}
        severity={notification.severity}
        onClose={hideNotification}
      />
    </Container>
  );
} 