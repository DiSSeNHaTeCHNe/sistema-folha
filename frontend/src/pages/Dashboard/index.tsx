import { Box, Paper, Typography } from '@mui/material';
import {
  People as PeopleIcon,
  AttachMoney as MoneyIcon,
  CardGiftcard as GiftIcon,
} from '@mui/icons-material';

interface StatCardProps {
  title: string;
  value: string;
  icon: React.ReactNode;
}

function StatCard({ title, value, icon }: StatCardProps) {
  return (
    <Paper
      sx={{
        p: 2,
        display: 'flex',
        flexDirection: 'column',
        height: 140,
      }}
    >
      <Box display="flex" gap={2}>
        <Box flex="1">
          <Typography component="h2" variant="h6" color="primary" gutterBottom>
            {title}
          </Typography>
          <Typography component="p" variant="h4">
            {value}
          </Typography>
        </Box>
        <Box display="flex" justifyContent="flex-end" alignItems="center">
          {icon}
        </Box>
      </Box>
    </Paper>
  );
}

export function Dashboard() {
  return (
    <Box display="flex" flexDirection="column" gap={3}>
      <Typography component="h1" variant="h4" gutterBottom>
        Dashboard
      </Typography>
      <Box display="flex" flexWrap="wrap" gap={3}>
        <Box flex="1" minWidth="300px">
          <StatCard
            title="Total de Funcionários"
            value="150"
            icon={<PeopleIcon sx={{ fontSize: 40, color: 'primary.main' }} />}
          />
        </Box>
        <Box flex="1" minWidth="300px">
          <StatCard
            title="Folha de Pagamento"
            value="R$ 450.000,00"
            icon={<MoneyIcon sx={{ fontSize: 40, color: 'primary.main' }} />}
          />
        </Box>
        <Box flex="1" minWidth="300px">
          <StatCard
            title="Benefícios Ativos"
            value="75"
            icon={<GiftIcon sx={{ fontSize: 40, color: 'primary.main' }} />}
          />
        </Box>
      </Box>
    </Box>
  );
} 