import { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Paper,
  Tabs,
  Tab,
  Box,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  CircularProgress,
} from '@mui/material';
import { Download as DownloadIcon } from '@mui/icons-material';
import { relatorioService } from '../../services/relatorioService';
import type { RelatorioFolha, RelatorioBeneficio } from '../../services/relatorioService';
import { useNotification } from '../../hooks/useNotification';
import { Notification } from '../../components/Notification';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`relatorio-tabpanel-${index}`}
      aria-labelledby={`relatorio-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

export function Relatorios() {
  const [tabValue, setTabValue] = useState(0);
  const [relatoriosFolha, setRelatoriosFolha] = useState<RelatorioFolha[]>([]);
  const [relatoriosBeneficio, setRelatoriosBeneficio] = useState<RelatorioBeneficio[]>([]);
  const [loading, setLoading] = useState(false);
  const { notification, showNotification, hideNotification } = useNotification();

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const carregarRelatorios = async () => {
    try {
      setLoading(true);
      const [folha, beneficio] = await Promise.all([
        relatorioService.listarRelatoriosFolha(),
        relatorioService.listarRelatoriosBeneficio(),
      ]);
      setRelatoriosFolha(folha);
      setRelatoriosBeneficio(beneficio);
    } catch (error) {
      showNotification('Erro ao carregar relatórios', 'error');
    } finally {
      setLoading(false);
    }
  };

  const gerarRelatorio = async () => {
    try {
      setLoading(true);
      const data = new Date();
      const mes = data.getMonth() + 1;
      const ano = data.getFullYear();

      if (tabValue === 0) {
        await relatorioService.gerarRelatorioFolha(mes, ano);
        showNotification('Relatório de folha gerado com sucesso', 'success');
      } else {
        await relatorioService.gerarRelatorioBeneficio(mes, ano);
        showNotification('Relatório de benefícios gerado com sucesso', 'success');
      }

      await carregarRelatorios();
    } catch (error) {
      showNotification('Erro ao gerar relatório', 'error');
    } finally {
      setLoading(false);
    }
  };

  const downloadRelatorio = async (id: number) => {
    try {
      setLoading(true);
      const blob = tabValue === 0
        ? await relatorioService.downloadRelatorioFolha(id)
        : await relatorioService.downloadRelatorioBeneficio(id);

      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `relatorio-${tabValue === 0 ? 'folha' : 'beneficio'}-${id}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      showNotification('Relatório baixado com sucesso', 'success');
    } catch (error) {
      showNotification('Erro ao baixar relatório', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarRelatorios();
  }, []);

  return (
    <Container>
      <Typography variant="h4" component="h1" gutterBottom>
        Relatórios
      </Typography>

      <Paper sx={{ width: '100%', mb: 2 }}>
        <Tabs value={tabValue} onChange={handleTabChange} aria-label="relatorio tabs">
          <Tab label="Folha de Pagamento" />
          <Tab label="Benefícios" />
        </Tabs>

        <TabPanel value={tabValue} index={0}>
          <Box sx={{ mb: 2 }}>
            <Button
              variant="contained"
              onClick={gerarRelatorio}
              disabled={loading}
              startIcon={loading && <CircularProgress size={20} color="inherit" />}
            >
              Gerar Relatório
            </Button>
          </Box>

          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Mês/Ano</TableCell>
                  <TableCell>Total Funcionários</TableCell>
                  <TableCell>Total Folha</TableCell>
                  <TableCell>Total Benefícios</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Data Processamento</TableCell>
                  <TableCell>Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {relatoriosFolha.map((relatorio) => (
                  <TableRow key={relatorio.id}>
                    <TableCell>{`${relatorio.mes}/${relatorio.ano}`}</TableCell>
                    <TableCell>{relatorio.totalFuncionarios}</TableCell>
                    <TableCell>
                      {new Intl.NumberFormat('pt-BR', {
                        style: 'currency',
                        currency: 'BRL',
                      }).format(relatorio.totalFolha)}
                    </TableCell>
                    <TableCell>
                      {new Intl.NumberFormat('pt-BR', {
                        style: 'currency',
                        currency: 'BRL',
                      }).format(relatorio.totalBeneficios)}
                    </TableCell>
                    <TableCell>{relatorio.status}</TableCell>
                    <TableCell>
                      {relatorio.dataProcessamento
                        ? new Date(relatorio.dataProcessamento).toLocaleString('pt-BR')
                        : '-'}
                    </TableCell>
                    <TableCell>
                      <IconButton
                        onClick={() => downloadRelatorio(relatorio.id)}
                        disabled={relatorio.status !== 'PROCESSADO' || loading}
                      >
                        <DownloadIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          <Box sx={{ mb: 2 }}>
            <Button
              variant="contained"
              onClick={gerarRelatorio}
              disabled={loading}
              startIcon={loading && <CircularProgress size={20} color="inherit" />}
            >
              Gerar Relatório
            </Button>
          </Box>

          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Mês/Ano</TableCell>
                  <TableCell>Total Benefícios</TableCell>
                  <TableCell>Total Valor</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Data Processamento</TableCell>
                  <TableCell>Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {relatoriosBeneficio.map((relatorio) => (
                  <TableRow key={relatorio.id}>
                    <TableCell>{`${relatorio.mes}/${relatorio.ano}`}</TableCell>
                    <TableCell>{relatorio.totalBeneficios}</TableCell>
                    <TableCell>
                      {new Intl.NumberFormat('pt-BR', {
                        style: 'currency',
                        currency: 'BRL',
                      }).format(relatorio.totalValor)}
                    </TableCell>
                    <TableCell>{relatorio.status}</TableCell>
                    <TableCell>
                      {relatorio.dataProcessamento
                        ? new Date(relatorio.dataProcessamento).toLocaleString('pt-BR')
                        : '-'}
                    </TableCell>
                    <TableCell>
                      <IconButton
                        onClick={() => downloadRelatorio(relatorio.id)}
                        disabled={relatorio.status !== 'PROCESSADO' || loading}
                      >
                        <DownloadIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </TabPanel>
      </Paper>

      <Notification
        open={notification.open}
        message={notification.message}
        severity={notification.severity}
        onClose={hideNotification}
      />
    </Container>
  );
} 