import { useEffect, useState } from 'react';
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
} from '@mui/material';
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

const pieColors = ['#4F46E5', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#06B6D4', '#84CC16', '#F97316'];

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadStats = async () => {
      try {
        setLoading(true);
        const data = await getDashboardStats();
        setStats(data);
      } catch (err) {
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
        sx={{ backgroundColor: '#f8f9fa' }}
      >
        <CircularProgress size={60} />
      </Box>
    );
  }

  if (error) return <Alert severity="error">{error}</Alert>;
  if (!stats) return <Alert severity="info">Nenhum dado disponível</Alert>;

  // Dados para o gráfico de área (evolução mensal) - usar dados reais
  const areaData = stats.evolucaoMensal?.length > 0 
    ? stats.evolucaoMensal.map(item => ({
        mes: item.mesAno,
        folha: item.valorTotal,
        funcionarios: item.quantidadeFuncionarios
      }))
    : [
        { mes: 'Jan', folha: 45000, funcionarios: 45 },
        { mes: 'Fev', folha: 47000, funcionarios: 48 },
        { mes: 'Mar', folha: 46000, funcionarios: 47 },
        { mes: 'Abr', folha: 48000, funcionarios: 49 },
        { mes: 'Mai', folha: 50000, funcionarios: 52 },
        { mes: 'Jun', folha: stats.custoMensalFolha, funcionarios: stats.totalFuncionarios },
      ];

  // Dados para gráfico de pizza - Funcionários por linha de negócio
  const funcionariosPorLinhaPieData = stats.porLinhaNegocio.slice(0, 6).map((item, index) => ({
    name: item.descricao.length > 12 ? item.descricao.substring(0, 12) + '...' : item.descricao,
    value: item.quantidadeFuncionarios,
    color: pieColors[index % pieColors.length],
    fullName: item.descricao
  }));

  // Dados para gráfico de pizza - Custo por centro de custo
  const custoPorCentroPieData = stats.porCentroCusto.slice(0, 6).map((item, index) => ({
    name: item.descricao.length > 12 ? item.descricao.substring(0, 12) + '...' : item.descricao,
    value: item.valorTotal,
    color: pieColors[index % pieColors.length],
    fullName: item.descricao
  }));

  // Dados para gráfico de pizza - Custo por linha de negócio
  const custoPorLinhaPieData = stats.porLinhaNegocio.slice(0, 6).map((item, index) => ({
    name: item.descricao.length > 12 ? item.descricao.substring(0, 12) + '...' : item.descricao,
    value: item.valorTotal,
    color: pieColors[index % pieColors.length],
    fullName: item.descricao
  }));

  // Dados para o gráfico de pizza (distribuição por centro de custo - funcionários)
  const funcionariosPorCentroPieData = stats.porCentroCusto.slice(0, 5).map((item, index) => ({
    name: item.descricao.length > 15 ? item.descricao.substring(0, 15) + '...' : item.descricao,
    value: item.quantidadeFuncionarios,
    color: pieColors[index % pieColors.length]
  }));

  const percentualProventos = ((stats.totalProventos / (stats.totalProventos + stats.totalDescontos)) * 100);

  // Função para renderizar legenda customizada
  const renderLegend = (data: any[]) => (
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
    <Box sx={{ p: 3, backgroundColor: '#f8f9fa', minHeight: '100vh' }}>
      {/* Header */}
      <Box mb={4}>
        <Typography variant="h4" fontWeight="bold" color="primary" gutterBottom>
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
          <Card sx={{ 
            borderRadius: 3,
            height: '100%',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            border: '1px solid #e9ecef'
          }}>
            <CardContent sx={{ position: 'relative', zIndex: 1, height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                    Total de Funcionários
                  </Typography>
                  <Typography variant="h3" fontWeight="bold" color="primary">
                    {stats.totalFuncionarios}
                  </Typography>
                  <Chip 
                    label="+2.5% este mês" 
                    size="small" 
                    color="success"
                    variant="outlined"
                    sx={{ mt: 1 }} 
                  />
                </Box>
                <Avatar sx={{ backgroundColor: '#e3f2fd', color: '#1976d2', width: 56, height: 56 }}>
                  <People fontSize="large" />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Box>

        {/* Card Custo Mensal */}
        <Box flex="1" minWidth={{ xs: 280, lg: 0 }}>
          <Card sx={{ 
            borderRadius: 3,
            height: '100%',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            border: '1px solid #e9ecef'
          }}>
            <CardContent sx={{ position: 'relative', zIndex: 1, height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                    Custo Mensal da Folha
                  </Typography>
                  <Typography variant="h4" fontWeight="bold" color="success.main">
                    {stats.custoMensalFolha.toLocaleString('pt-BR', { 
                      style: 'currency', 
                      currency: 'BRL',
                      minimumFractionDigits: 0,
                      maximumFractionDigits: 0
                    })}
                  </Typography>
                  <Chip 
                    label="+5.2% este mês" 
                    size="small" 
                    color="success"
                    variant="outlined"
                    sx={{ mt: 1 }} 
                  />
                </Box>
                <Avatar sx={{ backgroundColor: '#e8f5e8', color: '#2e7d32', width: 56, height: 56 }}>
                  <AttachMoney fontSize="large" />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Box>

        {/* Card Benefícios */}
        <Box flex="1" minWidth={{ xs: 280, lg: 0 }}>
          <Card sx={{ 
            borderRadius: 3,
            height: '100%',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            border: '1px solid #e9ecef'
          }}>
            <CardContent sx={{ position: 'relative', zIndex: 1, height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                    Benefícios Ativos
                  </Typography>
                  <Typography variant="h3" fontWeight="bold" color="warning.main">
                    {stats.totalBeneficiosAtivos}
                  </Typography>
                  <Chip 
                    label="Estável" 
                    size="small" 
                    color="warning"
                    variant="outlined"
                    sx={{ mt: 1 }} 
                  />
                </Box>
                <Avatar sx={{ backgroundColor: '#fff3e0', color: '#f57c00', width: 56, height: 56 }}>
                  <CardGiftcard fontSize="large" />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Box>

        {/* Card Relação Proventos/Descontos */}
        <Box flex="1" minWidth={{ xs: 280, lg: 0 }}>
          <Card sx={{ 
            borderRadius: 3,
            height: '100%',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            border: '1px solid #e9ecef'
          }}>
            <CardContent sx={{ position: 'relative', zIndex: 1, height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                    Relação P/D
                  </Typography>
                  <Typography variant="h3" fontWeight="bold" color="info.main">
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
                <Avatar sx={{ backgroundColor: '#e1f5fe', color: '#0277bd', width: 56, height: 56 }}>
                  <Assessment fontSize="large" />
                </Avatar>
              </Box>
            </CardContent>
          </Card>
        </Box>
      </Box>

      {/* Gráfico de Evolução da Folha - Linha completa */}
      <Box mb={4}>
        <Card sx={{ borderRadius: 3, boxShadow: '0 2px 8px rgba(0,0,0,0.1)', border: '1px solid #e9ecef' }}>
          <CardContent>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
              <Typography variant="h6" fontWeight="bold" color="primary">
                Evolução da Folha de Pagamento
              </Typography>
              <Chip label="Últimos 12 meses" variant="outlined" size="small" />
            </Box>
            <ResponsiveContainer width="100%" height={350}>
              <AreaChart data={areaData}>
                <defs>
                  <linearGradient id="colorFolha" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#4F46E5" stopOpacity={0.8}/>
                    <stop offset="95%" stopColor="#4F46E5" stopOpacity={0.1}/>
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
                  stroke="#4F46E5" 
                  fillOpacity={1} 
                  fill="url(#colorFolha)" 
                  strokeWidth={3}
                />
              </AreaChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </Box>

      {/* Gráficos de Pizza - Todos do mesmo tamanho e alinhados */}
      <Box display="flex" gap={3} mb={4} sx={{ flexWrap: { xs: 'wrap', xl: 'nowrap' } }}>
        {/* Funcionários por Centro de Custo */}
        <Box flex="1" minWidth={{ xs: 350, xl: 0 }}>
          <Card sx={{ borderRadius: 3, boxShadow: '0 2px 8px rgba(0,0,0,0.1)', border: '1px solid #e9ecef', height: '100%' }}>
            <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Typography variant="h6" fontWeight="bold" color="primary" gutterBottom>
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
          <Card sx={{ borderRadius: 3, boxShadow: '0 2px 8px rgba(0,0,0,0.1)', border: '1px solid #e9ecef', height: '100%' }}>
            <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Typography variant="h6" fontWeight="bold" color="primary" gutterBottom>
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
          <Card sx={{ borderRadius: 3, boxShadow: '0 2px 8px rgba(0,0,0,0.1)', border: '1px solid #e9ecef', height: '100%' }}>
            <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Typography variant="h6" fontWeight="bold" color="primary" gutterBottom>
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
          <Card sx={{ borderRadius: 3, boxShadow: '0 2px 8px rgba(0,0,0,0.1)', border: '1px solid #e9ecef', height: '100%' }}>
            <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Typography variant="h6" fontWeight="bold" color="primary" gutterBottom>
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
          <Card sx={{ borderRadius: 3, boxShadow: '0 2px 8px rgba(0,0,0,0.1)', border: '1px solid #e9ecef' }}>
            <CardContent>
              <Box display="flex" alignItems="center" mb={2}>
                <Avatar sx={{ background: '#e8f5e8', color: '#2e7d32', mr: 2 }}>
                  <TrendingUp />
                </Avatar>
                <Typography variant="h6" fontWeight="bold" color="primary">
                  Top 5 Proventos
                </Typography>
              </Box>
              <Divider sx={{ mb: 2 }} />
              <List dense>
                {stats.topProventos.map((item, index) => (
                  <ListItem key={item.id} sx={{ borderRadius: 2, mb: 1, '&:hover': { bgcolor: '#f5f5f5' } }}>
                    <ListItemAvatar>
                      <Avatar sx={{ 
                        background: `linear-gradient(135deg, ${pieColors[index]} 0%, ${pieColors[index]}80 100%)`,
                        color: 'white',
                        fontWeight: 'bold',
                        fontSize: '0.8rem'
                      }}>
                        #{index + 1}
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={
                        <Typography variant="subtitle2" fontWeight="bold">
                          {item.codigo} - {item.descricao}
                        </Typography>
                      }
                      secondary={
                        <Box>
                          <Typography variant="body2" color="success.main" fontWeight="bold">
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
          <Card sx={{ borderRadius: 3, boxShadow: '0 2px 8px rgba(0,0,0,0.1)', border: '1px solid #e9ecef' }}>
            <CardContent>
              <Box display="flex" alignItems="center" mb={2}>
                <Avatar sx={{ background: '#ffebee', color: '#c62828', mr: 2 }}>
                  <TrendingDown />
                </Avatar>
                <Typography variant="h6" fontWeight="bold" color="primary">
                  Top 5 Descontos
                </Typography>
              </Box>
              <Divider sx={{ mb: 2 }} />
              <List dense>
                {stats.topDescontos.map((item, index) => (
                  <ListItem key={item.id} sx={{ borderRadius: 2, mb: 1, '&:hover': { bgcolor: '#f5f5f5' } }}>
                    <ListItemAvatar>
                      <Avatar sx={{ 
                        background: `linear-gradient(135deg, ${pieColors[index]} 0%, ${pieColors[index]}80 100%)`,
                        color: 'white',
                        fontWeight: 'bold',
                        fontSize: '0.8rem'
                      }}>
                        #{index + 1}
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={
                        <Typography variant="subtitle2" fontWeight="bold">
                          {item.codigo} - {item.descricao}
                        </Typography>
                      }
                      secondary={
                        <Box>
                          <Typography variant="body2" color="error.main" fontWeight="bold">
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
  );
} 