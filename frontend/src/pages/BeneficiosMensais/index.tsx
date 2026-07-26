import { Fragment, useCallback, useEffect, useMemo, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Collapse,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableFooter,
  TableHead,
  TableRow,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import {
  ExpandLess as ExpandLessIcon,
  ExpandMore as ExpandMoreIcon,
} from '@mui/icons-material';
import type { BeneficioMensal, BeneficioMensalCompetenciaParams, BeneficioMensalResumo } from '../../types';
import { beneficioMensalService } from '../../services/beneficioMensalService';

const MESES = [
  { valor: 1, label: 'Janeiro' },
  { valor: 2, label: 'Fevereiro' },
  { valor: 3, label: 'Março' },
  { valor: 4, label: 'Abril' },
  { valor: 5, label: 'Maio' },
  { valor: 6, label: 'Junho' },
  { valor: 7, label: 'Julho' },
  { valor: 8, label: 'Agosto' },
  { valor: 9, label: 'Setembro' },
  { valor: 10, label: 'Outubro' },
  { valor: 11, label: 'Novembro' },
  { valor: 12, label: 'Dezembro' },
];

const formatarMoeda = (valor: number): string =>
  new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(valor);

const formatarDataCompetencia = (dataString: string): string => {
  if (!dataString) return '';
  if (dataString.includes('-')) {
    const [ano, mes, dia] = dataString.split('-');
    return `${dia}/${mes}/${ano}`;
  }
  return dataString;
};

const competenciaParams = (mes: number, ano: number): BeneficioMensalCompetenciaParams => {
  const mesStr = String(mes).padStart(2, '0');
  const ultimoDia = new Date(ano, mes, 0).getDate();
  return {
    competenciaInicio: `${ano}-${mesStr}-01`,
    competenciaFim: `${ano}-${mesStr}-${String(ultimoDia).padStart(2, '0')}`,
  };
};

const gerarAnosDisponiveis = (): number[] => {
  const anoAtual = new Date().getFullYear();
  return Array.from({ length: 6 }, (_, i) => anoAtual - i);
};

async function encontrarUltimaCompetenciaComDados(): Promise<{ mes: number; ano: number } | null> {
  const hoje = new Date();
  for (let offset = 0; offset < 36; offset += 1) {
    const data = new Date(hoje.getFullYear(), hoje.getMonth() - offset, 1);
    const mes = data.getMonth() + 1;
    const ano = data.getFullYear();
    try {
      const resumo = await beneficioMensalService.resumo(competenciaParams(mes, ano));
      if (resumo.length > 0) {
        return { mes, ano };
      }
    } catch {
      // tenta mês anterior
    }
  }
  return null;
}

export function BeneficiosMensais() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  const hoje = new Date();
  const [mes, setMes] = useState(hoje.getMonth() + 1);
  const [ano, setAno] = useState(hoje.getFullYear());
  const [resumo, setResumo] = useState<BeneficioMensalResumo[]>([]);
  const [lancamentos, setLancamentos] = useState<BeneficioMensal[]>([]);
  const [expandedCodigo, setExpandedCodigo] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [competenciaInicializada, setCompetenciaInicializada] = useState(false);

  const params = useMemo(() => competenciaParams(mes, ano), [mes, ano]);
  const anosDisponiveis = useMemo(() => gerarAnosDisponiveis(), []);

  const totalGeral = useMemo(
    () => resumo.reduce((acc, item) => acc + item.total, 0),
    [resumo],
  );

  const lancamentosPorCodigo = useMemo(() => {
    const map = new Map<string, BeneficioMensal[]>();
    for (const lancamento of lancamentos) {
      const codigo = lancamento.tipoBeneficioCodigo ?? '';
      if (!codigo) continue;
      const lista = map.get(codigo) ?? [];
      lista.push(lancamento);
      map.set(codigo, lista);
    }
    for (const [, lista] of map) {
      lista.sort((a, b) =>
        (a.funcionarioNome ?? '').localeCompare(b.funcionarioNome ?? '', 'pt-BR'),
      );
    }
    return map;
  }, [lancamentos]);

  const carregarDados = useCallback(async (competencia: BeneficioMensalCompetenciaParams) => {
    setLoading(true);
    setError('');
    setExpandedCodigo(null);
    try {
      const [resumoData, lancamentosData] = await Promise.all([
        beneficioMensalService.resumo(competencia),
        beneficioMensalService.listar(competencia),
      ]);
      setResumo(resumoData);
      setLancamentos(lancamentosData);
    } catch {
      setError('Erro ao carregar benefícios mensais.');
      setResumo([]);
      setLancamentos([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let ativo = true;

    const inicializar = async () => {
      setLoading(true);
      const ultima = await encontrarUltimaCompetenciaComDados();
      if (!ativo) return;

      const mesInicial = ultima?.mes ?? hoje.getMonth() + 1;
      const anoInicial = ultima?.ano ?? hoje.getFullYear();
      setMes(mesInicial);
      setAno(anoInicial);
      setCompetenciaInicializada(true);
    };

    inicializar();

    return () => {
      ativo = false;
    };
  }, []);

  useEffect(() => {
    if (!competenciaInicializada) return;
    carregarDados(params);
  }, [competenciaInicializada, params, carregarDados]);

  const handleToggleRow = (codigo: string) => {
    setExpandedCodigo((atual) => (atual === codigo ? null : codigo));
  };

  const competenciaLabel = `${MESES.find((m) => m.valor === mes)?.label ?? mes}/${ano}`;

  return (
    <Box>
      <Typography variant="h4" component="h1" sx={{ mb: 3 }}>
        Benefícios Mensais
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Competência
          </Typography>
          <Box
            sx={{
              display: 'flex',
              flexDirection: { xs: 'column', sm: 'row' },
              gap: 2,
              alignItems: { xs: 'stretch', sm: 'center' },
            }}
          >
            <FormControl sx={{ minWidth: { xs: '100%', sm: 180 } }}>
              <InputLabel id="beneficios-mes-label">Mês</InputLabel>
              <Select
                labelId="beneficios-mes-label"
                label="Mês"
                value={mes}
                onChange={(e) => setMes(Number(e.target.value))}
              >
                {MESES.map((item) => (
                  <MenuItem key={item.valor} value={item.valor}>
                    {item.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl sx={{ minWidth: { xs: '100%', sm: 120 } }}>
              <InputLabel id="beneficios-ano-label">Ano</InputLabel>
              <Select
                labelId="beneficios-ano-label"
                label="Ano"
                value={ano}
                onChange={(e) => setAno(Number(e.target.value))}
              >
                {anosDisponiveis.map((itemAno) => (
                  <MenuItem key={itemAno} value={itemAno}>
                    {itemAno}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Typography variant="body2" color="text.secondary">
              Período: {formatarDataCompetencia(params.competenciaInicio)} a{' '}
              {formatarDataCompetencia(params.competenciaFim)}
            </Typography>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent sx={{ p: { xs: 1, sm: 2 } }}>
          <Typography variant="h6" gutterBottom>
            Resumo por Tipo — {competenciaLabel}
          </Typography>

          {loading ? (
            <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
              Carregando...
            </Typography>
          ) : error ? (
            <Typography color="error" sx={{ py: 4, textAlign: 'center' }}>
              {error}
            </Typography>
          ) : resumo.length === 0 ? (
            <Box sx={{ py: 6, textAlign: 'center' }}>
              <Typography color="text.secondary" variant="body1">
                Nenhum benefício mensal encontrado para {competenciaLabel}.
              </Typography>
              <Typography color="text.secondary" variant="body2" sx={{ mt: 1 }}>
                Selecione outra competência.
              </Typography>
            </Box>
          ) : (
            <TableContainer component={Paper} sx={{ overflowX: 'auto' }}>
              <Table size={isMobile ? 'small' : 'medium'}>
                <TableHead>
                  <TableRow>
                    <TableCell padding="checkbox" />
                    <TableCell>Código</TableCell>
                    <TableCell>Descrição</TableCell>
                    <TableCell align="right">Total (R$)</TableCell>
                    <TableCell align="right">Qtd. Lançamentos</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {resumo.map((item) => {
                    const expandido = expandedCodigo === item.codigo;
                    const detalhes = lancamentosPorCodigo.get(item.codigo) ?? [];

                    return (
                      <Fragment key={item.codigo}>
                        <TableRow
                          hover
                          onClick={() => handleToggleRow(item.codigo)}
                          sx={{ cursor: 'pointer' }}
                        >
                          <TableCell padding="checkbox">
                            <IconButton
                              size="small"
                              aria-label={expandido ? 'Recolher detalhes' : 'Expandir detalhes'}
                              onClick={(e) => {
                                e.stopPropagation();
                                handleToggleRow(item.codigo);
                              }}
                            >
                              {expandido ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                            </IconButton>
                          </TableCell>
                          <TableCell>{item.codigo}</TableCell>
                          <TableCell>{item.descricao}</TableCell>
                          <TableCell align="right">{formatarMoeda(item.total)}</TableCell>
                          <TableCell align="right">{item.qtdLancamentos}</TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell colSpan={5} sx={{ py: 0, borderBottom: expandido ? undefined : 0 }}>
                            <Collapse in={expandido} timeout="auto" unmountOnExit>
                              <Box sx={{ py: 2, px: { xs: 0, sm: 2 } }}>
                                <Typography variant="subtitle2" gutterBottom>
                                  Funcionários — {item.descricao}
                                </Typography>
                                {detalhes.length === 0 ? (
                                  <Typography variant="body2" color="text.secondary">
                                    Nenhum lançamento encontrado para este tipo.
                                  </Typography>
                                ) : (
                                  <Table size="small">
                                    <TableHead>
                                      <TableRow>
                                        <TableCell>Funcionário</TableCell>
                                        {!isMobile && <TableCell>Centro de Custo</TableCell>}
                                        <TableCell align="right">Valor (R$)</TableCell>
                                      </TableRow>
                                    </TableHead>
                                    <TableBody>
                                      {detalhes.map((lancamento) => (
                                        <TableRow key={lancamento.id}>
                                          <TableCell>{lancamento.funcionarioNome ?? '—'}</TableCell>
                                          {!isMobile && (
                                            <TableCell>
                                              {lancamento.centroCustoDescricao ?? '—'}
                                            </TableCell>
                                          )}
                                          <TableCell align="right">
                                            {formatarMoeda(lancamento.valor)}
                                          </TableCell>
                                        </TableRow>
                                      ))}
                                    </TableBody>
                                  </Table>
                                )}
                              </Box>
                            </Collapse>
                          </TableCell>
                        </TableRow>
                      </Fragment>
                    );
                  })}
                </TableBody>
                <TableFooter>
                  <TableRow>
                    <TableCell colSpan={3}>
                      <Typography variant="subtitle1" fontWeight="bold">
                        Total Geral
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Typography variant="subtitle1" fontWeight="bold" color="primary">
                        {formatarMoeda(totalGeral)}
                      </Typography>
                    </TableCell>
                    <TableCell />
                  </TableRow>
                </TableFooter>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}

export default BeneficiosMensais;
