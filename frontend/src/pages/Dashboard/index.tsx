import { Grid, Paper, Typography } from '@mui/material';
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
      <Grid container spacing={2}>
        <Grid item xs={8}>
          <Typography component="h2" variant="h6" color="primary" gutterBottom>
            {title}
          </Typography>
          <Typography component="p" variant="h4">
            {value}
          </Typography>
        </Grid>
        <Grid item xs={4} sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          {icon}
        </Grid>
      </Grid>
    </Paper>
  );
}

export function Dashboard() {
  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Typography component="h1" variant="h4" gutterBottom>
          Dashboard
        </Typography>
      </Grid>
      <Grid item xs={12} md={4}>
        <StatCard
          title="Total de Funcionários"
          value="150"
          icon={<PeopleIcon sx={{ fontSize: 40, color: 'primary.main' }} />}
        />
      </Grid>
      <Grid item xs={12} md={4}>
        <StatCard
          title="Folha de Pagamento"
          value="R$ 450.000,00"
          icon={<MoneyIcon sx={{ fontSize: 40, color: 'primary.main' }} />}
        />
      </Grid>
      <Grid item xs={12} md={4}>
        <StatCard
          title="Benefícios Ativos"
          value="75"
          icon={<GiftIcon sx={{ fontSize: 40, color: 'primary.main' }} />}
        />
      </Grid>
    </Grid>
  );
} 