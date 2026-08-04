import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { 
  Box, 
  Card, 
  CardContent, 
  Typography, 
  Avatar, 
  CircularProgress, 
  Alert, 
  List, 
  ListItem, 
  ListItemAvatar, 
  ListItemText,
  Chip,
  Divider,
  useTheme,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import {
  TrendingUp, 
  TrendingDown, 
  AttachMoney, 
  People, 
  CardGiftcard,
  Assessment,
} from '@mui/icons-material';
import { 
  XAxis, 
  YAxis, 
  Tooltip, 
  ResponsiveContainer, 
  AreaChart,
  Area,
  PieChart as RePieChart,
  Pie,
  Cell 
} from 'recharts';
import { getDashboardStats } from '../../services/dashboardService';
import type { DashboardStats } from '../../services/dashboardService';
import { useNotification } from '../../hooks/useNotification';
import { Notification } from '../../components/Notification';
import { formatMoneyDisplay } from '../../utils/money';

export default function Dashboard() {
  const theme = useTheme();
  const chartColors = theme.palette.charts;
  const areaChartColor = chartColors[0];
  const cardSx = {
    borderRadius: 3,
    boxShadow: theme.shadows[2],
    border: 1,
    borderColor: 'divider' as const,
  };

  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const location = useLocation();
  const navigate = useNavigate();
  const { notification, showNotification, hideNotification } = useNotification();

  useEffect(() => {
    if (location.state?.acessoNegado) {
      showNotification('Acesso negado. Apenas administradores.', 'warning');
      navigate('/dashboard', { replace: true, state: {} });
    }
  }, [location.state, navigate, showNotification]);

  useEffect(() => {
    const loadStats = async () => {
      try {
        setLoading(true);
        const data = await getDashboardStats();
        setStats(data);
      } catch {
        setError('Erro ao carregar dados do dashboard');
      } finally {
        setLoading(false);
      }
    };
    loadStats();
  }, []);

  if (loading) {
  return (
      <Box 
        display="flex" 
        justifyContent="center" 
        alignItems="center" 
        minHeight="400px"
        sx={{ backgroundColor: 'background.default' }}
      >
        <CircularProgress size={60} />
      </Box>
    );
  }

  if (error) return <Alert severity="error">{error}</Alert>;
  if (!stats) return <Alert severity="info">Nenhum dado disponível</Alert>;

  const areaData = (stats.evolucaoMensal ?? []).map(item => ({
    mes: item.mesAno,
    folha: item.valorTotal,
    funcionarios: item.quantidadeFuncionarios,
  }));

  // Dados para gráfico de pizza - Funcionários por linha de negócio
  const funcionariosPorLinhaPieData = stats.porLinhaNegocio.slice(0, 6).map((item, index) => ({
    name: item.descricao.length > 12 ? item.descricao.substring(0, 12) + '...' : item.descricao,
    value: item.quantidadeFuncionarios,
    color: chartColors[index % chartColors.length],
    fullName: item.descricao
  }));

  // Dados para gráfico de pizza - Custo por centro de custo
  const custoPorCentroPieData = stats.porCentroCusto.slice(0, 6).map((item, index) => ({
    name: item.descricao.length > 12 ? item.descricao.substring(0, 12) + '...' : item.descricao,
    value: item.valorTotal,
    color: chartColors[index % chartColors.length],
    fullName: item.descricao
  }));

  // Dados para gráfico de pizza - Custo por linha de negócio
  const custoPorLinhaPieData = stats.porLinhaNegocio.slice(0, 6).map((item, index) => ({
    name: item.descricao.length > 12 ? item.descricao.substring(0, 12) + '...' : item.descricao,
    value: item.valorTotal,
    color: chartColors[index % chartColors.length],
    fullName: item.descricao
  }));

  // Dados para o gráfico de pizza (distribuição por centro de custo - funcionários)
  const funcionariosPorCentroPieData = stats.porCentroCusto.slice(0, 5).map((item, index) => ({
    name: item.descricao.length > 15 ? item.descricao.substring(0, 15) + '...' : item.descricao,
    value: item.quantidadeFuncionarios,
    color: chartColors[index % chartColors.length]
  }));

  const percentualProventos = ((stats.totalProventos / (stats.totalProventos + stats.totalDescontos)) * 100);

  // Função para renderizar legenda customizada
  type PieLegendEntry = {
    name: string;
    value: number;
    color: string;
    fullName?: string;
  };

  const renderLegend = (data: PieLegendEntry[]) => (
    <Box mt={2}>
      {data.map((entry, index) => (
        <Box key={index} display="flex" alignItems="center" mb={1}>
          <Box 
            width={12} 
            height={12} 
            bgcolor={entry.color} 
            borderRadius="50%" 
            mr={1}
          />
          <Typography variant="caption" color="text.secondary" title={entry.fullName || entry.name}>
            {entry.name}: {typeof entry.value === 'number' && entry.value > 1000 
              ? entry.value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
              : entry.value
            }
          </Typography>
        </Box>
      ))}
    </Box>
  );

  return (
    <>
    <Box sx={{ p: 3, backgroundColor: 'background.default', minHeight: '100vh' }}>
      {/* Header */}
      <Box mb={4}>
        <Typography variant="h4" gutterBottom>
          Dashboard Gerencial
        </Typography>
        <Typography variant="subtitle1" color="text.secondary">
          Visão geral do sistema de folha de pagamento
        </Typography>
      </Box>

      {/* Cards de Indicadores Principais - Linha única com mesmo tamanho */}
      <Box display="flex" gap={3} mb={4} sx={{ flexWrap: { xs: 'wrap', lg: 'nowrap' } }}>
        {/* Card Funcionários */}
        <Box flex="1" minWidth={{ xs: 280, lg: 0 }}>
          <Card sx={{ ...cardSx, height: '100%' }}>
            <CardContent sx={{ position: 'relative', zIndex: 1, height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                    Total de Funcionários
                  </Typography>
                  <Typography variant="h3">
                    {stats.totalFuncionarios}
                  </Typography>
</Box>
                <Avatar sx={{ backgroundColor: 'info.light', color: 'info.main', width: 56, height: 56 }}>
                  <People fontSize="large" />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Box>

        {/* Card Custo Mensal */}
        <Box flex="1" minWidth={{ xs: 280, lg: 0 }}>
          <Card sx={{ ...cardSx, height: '100%' }}>
            <CardContent sx={{ position: 'relative', zIndex: 1, height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                    Custo Empresa
                  </Typography>
                  <Typography variant="h4" color="success.main">
                    {formatMoneyDisplay(stats.custoMensalFolha)}
                  </Typography>
</Box>
                <Avatar sx={{ backgroundColor: 'success.light', color: 'success.main', width: 56, height: 56 }}>
                  <AttachMoney fontSize="large" />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Box>

        {/* Card Benefícios */}
        <Box flex="1" minWidth={{ xs: 280, lg: 0 }}>
          <Card sx={{ ...cardSx, height: '100%' }}>
            <CardContent sx={{ position: 'relative', zIndex: 1, height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                    Benefícios Ativos
                  </Typography>
                  <Typography variant="h3" color="warning.main">
                    {stats.totalBeneficiosAtivos}
                  </Typography>
</Box>
                <Avatar sx={{ backgroundColor: 'warning.light', color: 'warning.main', width: 56, height: 56 }}>
                  <CardGiftcard fontSize="large" />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Box>

        {/* Card Relação Proventos/Descontos */}
        <Box flex="1" minWidth={{ xs: 280, lg: 0 }}>
          <Card sx={{ ...cardSx, height: '100%' }}>
            <CardContent sx={{ position: 'relative', zIndex: 1, height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                    Relação P/D
                  </Typography>
                  <Typography variant="h3" color="info.main">
                    {percentualProventos.toFixed(1)}%
                  </Typography>
                  <Chip 
                    label="Proventos" 
                    size="small" 
                    color="info"
                    variant="outlined"
                    sx={{ mt: 1 }} 
                  />
                </Box>
                <Avatar sx={{ backgroundColor: 'info.light', color: 'info.dark', width: 56, height: 56 }}>
                  <Assessment fontSize="large" />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Box>
      </Box>

      {/* Gráfico de Evolução da Folha - Linha completa */}
      <Box mb={4}>
        <Card sx={cardSx}>
          <CardContent>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
              <Typography variant="h6">
                Evolução da Folha de Pagamento
              </Typography>
              <Chip label="Últimos 12 meses" variant="outlined" size="small" />
            </Box>
            {areaData.length > 0 ? (
              <ResponsiveContainer width="100%" height={350}>
                <AreaChart data={areaData}>
                  <defs>
                    <linearGradient id="colorFolha" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor={areaChartColor} stopOpacity={0.8}/>
                      <stop offset="95%" stopColor={areaChartColor} stopOpacity={0.1}/>
                    </linearGradient>
                  </defs>
                  <XAxis dataKey="mes" />
                  <YAxis tickFormatter={(value) => `R$ ${value.toLocaleString()}`} />
                  <Tooltip 
                    formatter={(value, name) => [
                      name === 'folha' ? `R$ ${value.toLocaleString()}` : value,
                      name === 'folha' ? 'Folha de Pagamento' : 'Funcionários'
                    ]}
                  />
                  <Area 
                    type="monotone" 
                    dataKey="folha" 
                    stroke={areaChartColor} 
                    fillOpacity={1} 
                    fill="url(#colorFolha)" 
                    strokeWidth={3}
                  />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <Box
                display="flex"
                alignItems="center"
                justifyContent="center"
                height={350}
              >
                <Typography color="text.secondary" align="center">
                  Nenhuma folha regular encontrada nos últimos 12 meses.
                </Typography>
              </Box>
            )}
          </CardContent>
        </Card>
      </Box>

      {/* Gráficos de Pizza - Todos do mesmo tamanho e alinhados */}
      <Box display="flex" gap={3} mb={4} sx={{ flexWrap: { xs: 'wrap', xl: 'nowrap' } }}>
        {/* Funcionários por Centro de Custo */}
        <Box flex="1" minWidth={{ xs: 350, xl: 0 }}>
          <Card sx={{ ...cardSx, height: '100%' }}>
            <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Typography variant="h6" gutterBottom>
                Funcionários por Centro de Custo
              </Typography>
              <Box flex={1} display="flex" flexDirection="column">
                <ResponsiveContainer width="100%" height={300}>
                  <RePieChart>
                    <Pie
                      data={funcionariosPorCentroPieData}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={100}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      {funcionariosPorCentroPieData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </RePieChart>
                </ResponsiveContainer>
                {renderLegend(funcionariosPorCentroPieData)}
              </Box>
            </CardContent>
          </Card>
        </Box>

        {/* Funcionários por Linha de Negócio */}
        <Box flex="1" minWidth={{ xs: 350, xl: 0 }}>
          <Card sx={{ ...cardSx, height: '100%' }}>
            <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Typography variant="h6" gutterBottom>
                Funcionários por Linha de Negócio
              </Typography>
              <Box flex={1} display="flex" flexDirection="column">
                <ResponsiveContainer width="100%" height={300}>
                  <RePieChart>
                    <Pie
                      data={funcionariosPorLinhaPieData}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={100}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      {funcionariosPorLinhaPieData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </RePieChart>
                </ResponsiveContainer>
                {renderLegend(funcionariosPorLinhaPieData)}
              </Box>
            </CardContent>
          </Card>
        </Box>

        {/* Custo por Centro de Custo */}
        <Box flex="1" minWidth={{ xs: 350, xl: 0 }}>
          <Card sx={{ ...cardSx, height: '100%' }}>
            <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Typography variant="h6" gutterBottom>
                Custo Folha por Centro de Custo
              </Typography>
              <Box flex={1} display="flex" flexDirection="column">
                <ResponsiveContainer width="100%" height={300}>
                  <RePieChart>
                    <Pie
                      data={custoPorCentroPieData}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={100}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      {custoPorCentroPieData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip 
                      formatter={(value) => [
                        value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }),
                        'Custo'
                      ]}
                    />
                  </RePieChart>
                </ResponsiveContainer>
                {renderLegend(custoPorCentroPieData)}
              </Box>
            </CardContent>
          </Card>
        </Box>

        {/* Custo por Linha de Negócio */}
        <Box flex="1" minWidth={{ xs: 350, xl: 0 }}>
          <Card sx={{ ...cardSx, height: '100%' }}>
            <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Typography variant="h6" gutterBottom>
                Custo Folha por Linha de Negócio
      </Typography>
              <Box flex={1} display="flex" flexDirection="column">
                <ResponsiveContainer width="100%" height={300}>
                  <RePieChart>
                    <Pie
                      data={custoPorLinhaPieData}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={100}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      {custoPorLinhaPieData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip 
                      formatter={(value) => [
                        value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }),
                        'Custo'
                      ]}
                    />
                  </RePieChart>
                </ResponsiveContainer>
                {renderLegend(custoPorLinhaPieData)}
              </Box>
            </CardContent>
          </Card>
        </Box>
      </Box>

      {/* Listas de Top Rubricas */}
      <Box display="flex" flexWrap="wrap" gap={3}>
        {/* Top Proventos */}
        <Box flex="1 1 400px" minWidth={400}>
          <Card sx={cardSx}>
            <CardContent>
              <Box display="flex" alignItems="center" mb={2}>
                <Avatar sx={{ backgroundColor: 'success.light', color: 'success.main', mr: 2 }}>
                  <TrendingUp />
                </Avatar>
                <Typography variant="h6">
                  Top 5 Proventos
                </Typography>
              </Box>
              <Divider sx={{ mb: 2 }} />
              <List dense>
                {stats.topProventos.map((item, index) => (
                  <ListItem key={item.id} sx={{ borderRadius: 2, mb: 1, '&:hover': { bgcolor: 'action.hover' } }}>
                    <ListItemAvatar>
                      <Avatar sx={{ 
                        background: `linear-gradient(135deg, ${chartColors[index % chartColors.length]} 0%, ${alpha(chartColors[index % chartColors.length], 0.5)} 100%)`,
                        color: 'common.white',
                        fontWeight: 'bold',
                        fontSize: '0.8rem'
                      }}>
                        #{index + 1}
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={
                        <Typography variant="subtitle2">
                          {item.codigo} - {item.descricao}
                        </Typography>
                      }
                      secondary={
                        <Box>
                          <Typography variant="body2" color="success.main">
                            {item.valorTotal.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {item.quantidadeOcorrencias} ocorrências
                          </Typography>
                        </Box>
                      }
                    />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Box>

        {/* Top Descontos */}
        <Box flex="1 1 400px" minWidth={400}>
          <Card sx={cardSx}>
            <CardContent>
              <Box display="flex" alignItems="center" mb={2}>
                <Avatar sx={{ backgroundColor: 'error.light', color: 'error.main', mr: 2 }}>
                  <TrendingDown />
                </Avatar>
                <Typography variant="h6">
                  Top 5 Descontos
                </Typography>
              </Box>
              <Divider sx={{ mb: 2 }} />
              <List dense>
                {stats.topDescontos.map((item, index) => (
                  <ListItem key={item.id} sx={{ borderRadius: 2, mb: 1, '&:hover': { bgcolor: 'action.hover' } }}>
                    <ListItemAvatar>
                      <Avatar sx={{ 
                        background: `linear-gradient(135deg, ${chartColors[index % chartColors.length]} 0%, ${alpha(chartColors[index % chartColors.length], 0.5)} 100%)`,
                        color: 'common.white',
                        fontWeight: 'bold',
                        fontSize: '0.8rem'
                      }}>
                        #{index + 1}
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={
                        <Typography variant="subtitle2">
                          {item.codigo} - {item.descricao}
                        </Typography>
                      }
                      secondary={
                        <Box>
                          <Typography variant="body2" color="error.main">
                            {item.valorTotal.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {item.quantidadeOcorrencias} ocorrências
                          </Typography>
        </Box>
                      }
                    />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        </Box>
      </Box>
    </Box>
    <Notification
      open={notification.open}
      message={notification.message}
      severity={notification.severity}
      onClose={hideNotification}
    />
    </>
  );
} 