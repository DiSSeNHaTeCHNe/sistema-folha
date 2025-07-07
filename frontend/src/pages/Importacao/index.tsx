import React, { useState, useRef } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Alert,
  CircularProgress,
  Divider,
  Grid,
  Paper,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
} from '@mui/material';
import {
  CloudUpload as CloudUploadIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Description as DescriptionIcon,
  AttachMoney as AttachMoneyIcon,
  CardGiftcard as CardGiftcardIcon,
} from '@mui/icons-material';
import { toast } from 'react-toastify';
import { importacaoService } from '../../services/importacaoService';
import type { ImportacaoResponse } from '../../types';

interface UploadState {
  loading: boolean;
  success: boolean;
  error: string | null;
  registrosProcessados?: number;
  erros?: string[];
  arquivo?: string;
  tamanho?: number;
}

export default function Importacao() {
  const [folhaState, setFolhaState] = useState<UploadState>({
    loading: false,
    success: false,
    error: null,
  });
  
  const [beneficiosState, setBeneficiosState] = useState<UploadState>({
    loading: false,
    success: false,
    error: null,
  });

  const [folhaAdpState, setFolhaAdpState] = useState<UploadState>({
    loading: false,
    success: false,
    error: null,
  });

  const folhaFileRef = useRef<HTMLInputElement>(null);
  const beneficiosFileRef = useRef<HTMLInputElement>(null);
  const folhaAdpFileRef = useRef<HTMLInputElement>(null);

  const handleFileUpload = async (
    file: File | null,
    tipo: 'folha' | 'beneficios' | 'folhaAdp',
    setState: React.Dispatch<React.SetStateAction<UploadState>>
  ) => {
    if (!file) {
      toast.error('Por favor, selecione um arquivo');
      return;
    }

    // Validação de tipo de arquivo
    if ((tipo === 'folha' || tipo === 'folhaAdp') && !file.name.toLowerCase().endsWith('.txt')) {
      toast.error('Para importação de folha, selecione apenas arquivos .txt');
      return;
    }

    if (tipo === 'beneficios' && !file.name.toLowerCase().endsWith('.csv')) {
      toast.error('Para importação de benefícios, selecione apenas arquivos .csv');
      return;
    }

    setState({
      loading: true,
      success: false,
      error: null,
    });

    try {
      let response: ImportacaoResponse;
      
      switch (tipo) {
        case 'folha':
          response = await importacaoService.importarFolha(file);
          break;
        case 'folhaAdp':
          response = await importacaoService.importarFolhaAdp(file);
          break;
        case 'beneficios':
          response = await importacaoService.importarBeneficios(file);
          break;
        default:
          throw new Error('Tipo de importação não suportado');
      }

      if (response.success) {
        setState({
          loading: false,
          success: true,
          error: null,
          registrosProcessados: response.registrosProcessados,
          erros: response.erros,
          arquivo: response.arquivo,
          tamanho: response.tamanho,
        });

        const tipoNome = tipo === 'folha' ? 'folha' : tipo === 'folhaAdp' ? 'folha ADP' : 'benefícios';
        toast.success(`Arquivo de ${tipoNome} importado com sucesso!`);
      } else {
        setState({
          loading: false,
          success: false,
          error: response.message,
          arquivo: response.arquivo,
        });
        toast.error(response.message);
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Erro ao importar arquivo';
      setState({
        loading: false,
        success: false,
        error: errorMessage,
      });
      toast.error(errorMessage);
    }
  };

  const handleFolhaUpload = () => {
    const file = folhaFileRef.current?.files?.[0] || null;
    handleFileUpload(file, 'folha', setFolhaState);
  };

  const handleBeneficiosUpload = () => {
    const file = beneficiosFileRef.current?.files?.[0] || null;
    handleFileUpload(file, 'beneficios', setBeneficiosState);
  };

  const handleFolhaAdpUpload = () => {
    const file = folhaAdpFileRef.current?.files?.[0] || null;
    handleFileUpload(file, 'folhaAdp', setFolhaAdpState);
  };

  const resetFolhaState = () => {
    setFolhaState({
      loading: false,
      success: false,
      error: null,
    });
    if (folhaFileRef.current) {
      folhaFileRef.current.value = '';
    }
  };

  const resetBeneficiosState = () => {
    setBeneficiosState({
      loading: false,
      success: false,
      error: null,
    });
    if (beneficiosFileRef.current) {
      beneficiosFileRef.current.value = '';
    }
  };

  const resetFolhaAdpState = () => {
    setFolhaAdpState({
      loading: false,
      success: false,
      error: null,
    });
    if (folhaAdpFileRef.current) {
      folhaAdpFileRef.current.value = '';
    }
  };

  return (
    <Box p={3}>
      <Typography variant="h4" gutterBottom>
        Importação de Dados
      </Typography>
      
      <Typography variant="body1" color="text.secondary" paragraph>
        Faça upload dos arquivos para importar dados de folha de pagamento e benefícios no sistema.
      </Typography>

      <Box display="flex" gap={3} flexWrap="wrap">
        {/* Importação de Folha de Pagamento */}
        <Box flex="1" minWidth="400px">
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" mb={2}>
                <AttachMoneyIcon color="primary" sx={{ mr: 1 }} />
                <Typography variant="h6">
                  Importação de Folha de Pagamento
                </Typography>
              </Box>
              
              <Typography variant="body2" color="text.secondary" paragraph>
                Importe arquivos de texto (.txt) com layout de posições fixas contendo dados da folha de pagamento.
              </Typography>

              <Box mb={2}>
                <input
                  ref={folhaFileRef}
                  type="file"
                  accept=".txt"
                  style={{ display: 'none' }}
                  onChange={() => {
                    setFolhaState(prev => ({ ...prev, success: false, error: null }));
                  }}
                />
                <Button
                  variant="outlined"
                  startIcon={<CloudUploadIcon />}
                  onClick={() => folhaFileRef.current?.click()}
                  fullWidth
                  sx={{ mb: 1 }}
                >
                  Selecionar Arquivo (.txt)
                </Button>
                
                {folhaFileRef.current?.files?.[0] && (
                  <Typography variant="body2" color="primary">
                    Arquivo selecionado: {folhaFileRef.current.files[0].name}
                  </Typography>
                )}
              </Box>

              <Box display="flex" gap={1}>
                <Button
                  variant="contained"
                  onClick={handleFolhaUpload}
                  disabled={folhaState.loading || !folhaFileRef.current?.files?.[0]}
                  startIcon={folhaState.loading ? <CircularProgress size={20} /> : <DescriptionIcon />}
                  fullWidth
                >
                  {folhaState.loading ? 'Importando...' : 'Importar Folha'}
                </Button>
                
                {folhaState.success && (
                  <Button
                    variant="outlined"
                    onClick={resetFolhaState}
                    size="small"
                  >
                    Novo
                  </Button>
                )}
              </Box>

              {/* Status da importação */}
              {folhaState.loading && (
                <Box display="flex" alignItems="center" mt={2}>
                  <CircularProgress size={20} sx={{ mr: 1 }} />
                  <Typography variant="body2">Processando arquivo...</Typography>
                </Box>
              )}

              {folhaState.success && (
                <Alert severity="success" sx={{ mt: 2 }}>
                  <Typography variant="body2">
                    Importação realizada com sucesso!
                    {folhaState.arquivo && (
                      <>
                        <br />
                        Arquivo: {folhaState.arquivo}
                      </>
                    )}
                    {folhaState.tamanho && (
                      <>
                        <br />
                        Tamanho: {(folhaState.tamanho / 1024).toFixed(2)} KB
                      </>
                    )}
                    {folhaState.registrosProcessados && (
                      <>
                        <br />
                        Registros processados: {folhaState.registrosProcessados}
                      </>
                    )}
                  </Typography>
                </Alert>
              )}

              {folhaState.error && (
                <Alert severity="error" sx={{ mt: 2 }}>
                  <Typography variant="body2">{folhaState.error}</Typography>
                </Alert>
              )}

              {folhaState.erros && folhaState.erros.length > 0 && (
                <Paper sx={{ mt: 2, p: 2, maxHeight: 200, overflow: 'auto' }}>
                  <Typography variant="subtitle2" color="error" gutterBottom>
                    Erros encontrados:
                  </Typography>
                  <List dense>
                    {folhaState.erros.map((erro, index) => (
                      <ListItem key={index}>
                        <ListItemIcon>
                          <ErrorIcon color="error" fontSize="small" />
                        </ListItemIcon>
                        <ListItemText primary={erro} />
                      </ListItem>
                    ))}
                  </List>
                </Paper>
              )}
            </CardContent>
          </Card>
        </Box>

        {/* Importação de Benefícios */}
        <Box flex="1" minWidth="400px">
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" mb={2}>
                <CardGiftcardIcon color="primary" sx={{ mr: 1 }} />
                <Typography variant="h6">
                  Importação de Benefícios
                </Typography>
              </Box>
              
              <Typography variant="body2" color="text.secondary" paragraph>
                Importe arquivos CSV (.csv) contendo dados de benefícios dos funcionários.
              </Typography>

              <Box mb={2}>
                <input
                  ref={beneficiosFileRef}
                  type="file"
                  accept=".csv"
                  style={{ display: 'none' }}
                  onChange={() => {
                    setBeneficiosState(prev => ({ ...prev, success: false, error: null }));
                  }}
                />
                <Button
                  variant="outlined"
                  startIcon={<CloudUploadIcon />}
                  onClick={() => beneficiosFileRef.current?.click()}
                  fullWidth
                  sx={{ mb: 1 }}
                >
                  Selecionar Arquivo (.csv)
                </Button>
                
                {beneficiosFileRef.current?.files?.[0] && (
                  <Typography variant="body2" color="primary">
                    Arquivo selecionado: {beneficiosFileRef.current.files[0].name}
                  </Typography>
                )}
              </Box>

              <Box display="flex" gap={1}>
                <Button
                  variant="contained"
                  onClick={handleBeneficiosUpload}
                  disabled={beneficiosState.loading || !beneficiosFileRef.current?.files?.[0]}
                  startIcon={beneficiosState.loading ? <CircularProgress size={20} /> : <DescriptionIcon />}
                  fullWidth
                >
                  {beneficiosState.loading ? 'Importando...' : 'Importar Benefícios'}
                </Button>
                
                {beneficiosState.success && (
                  <Button
                    variant="outlined"
                    onClick={resetBeneficiosState}
                    size="small"
                  >
                    Novo
                  </Button>
                )}
              </Box>

              {/* Status da importação */}
              {beneficiosState.loading && (
                <Box display="flex" alignItems="center" mt={2}>
                  <CircularProgress size={20} sx={{ mr: 1 }} />
                  <Typography variant="body2">Processando arquivo...</Typography>
                </Box>
              )}

              {beneficiosState.success && (
                <Alert severity="success" sx={{ mt: 2 }}>
                  <Typography variant="body2">
                    Importação realizada com sucesso!
                    {beneficiosState.arquivo && (
                      <>
                        <br />
                        Arquivo: {beneficiosState.arquivo}
                      </>
                    )}
                    {beneficiosState.tamanho && (
                      <>
                        <br />
                        Tamanho: {(beneficiosState.tamanho / 1024).toFixed(2)} KB
                      </>
                    )}
                    {beneficiosState.registrosProcessados && (
                      <>
                        <br />
                        Registros processados: {beneficiosState.registrosProcessados}
                      </>
                    )}
                  </Typography>
                </Alert>
              )}

              {beneficiosState.error && (
                <Alert severity="error" sx={{ mt: 2 }}>
                  <Typography variant="body2">{beneficiosState.error}</Typography>
                </Alert>
              )}

              {beneficiosState.erros && beneficiosState.erros.length > 0 && (
                <Paper sx={{ mt: 2, p: 2, maxHeight: 200, overflow: 'auto' }}>
                  <Typography variant="subtitle2" color="error" gutterBottom>
                    Erros encontrados:
                  </Typography>
                  <List dense>
                    {beneficiosState.erros.map((erro, index) => (
                      <ListItem key={index}>
                        <ListItemIcon>
                          <ErrorIcon color="error" fontSize="small" />
                        </ListItemIcon>
                        <ListItemText primary={erro} />
                      </ListItem>
                    ))}
                  </List>
                </Paper>
              )}
            </CardContent>
          </Card>
        </Box>

        {/* Importação de Folha ADP */}
        <Box flex="1" minWidth="400px">
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" mb={2}>
                <AttachMoneyIcon color="secondary" sx={{ mr: 1 }} />
                <Typography variant="h6">
                  Importação de Folha ADP
                </Typography>
              </Box>
              
              <Typography variant="body2" color="text.secondary" paragraph>
                Importe arquivos de texto (.txt) com layout específico do ADP contendo dados da folha de pagamento.
              </Typography>

              <Box mb={2}>
                <input
                  ref={folhaAdpFileRef}
                  type="file"
                  accept=".txt"
                  style={{ display: 'none' }}
                  onChange={() => {
                    setFolhaAdpState(prev => ({ ...prev, success: false, error: null }));
                  }}
                />
                <Button
                  variant="outlined"
                  startIcon={<CloudUploadIcon />}
                  onClick={() => folhaAdpFileRef.current?.click()}
                  fullWidth
                  sx={{ mb: 1 }}
                >
                  Selecionar Arquivo (.txt)
                </Button>
                
                {folhaAdpFileRef.current?.files?.[0] && (
                  <Typography variant="body2" color="primary">
                    Arquivo selecionado: {folhaAdpFileRef.current.files[0].name}
                  </Typography>
                )}
              </Box>

              <Box display="flex" gap={1}>
                <Button
                  variant="contained"
                  onClick={handleFolhaAdpUpload}
                  disabled={folhaAdpState.loading || !folhaAdpFileRef.current?.files?.[0]}
                  startIcon={folhaAdpState.loading ? <CircularProgress size={20} /> : <DescriptionIcon />}
                  fullWidth
                >
                  {folhaAdpState.loading ? 'Importando...' : 'Importar Folha ADP'}
                </Button>
                
                {folhaAdpState.success && (
                  <Button
                    variant="outlined"
                    onClick={resetFolhaAdpState}
                    size="small"
                  >
                    Novo
                  </Button>
                )}
              </Box>

              {/* Status da importação */}
              {folhaAdpState.loading && (
                <Box display="flex" alignItems="center" mt={2}>
                  <CircularProgress size={20} sx={{ mr: 1 }} />
                  <Typography variant="body2">Processando arquivo...</Typography>
                </Box>
              )}

              {folhaAdpState.success && (
                <Alert severity="success" sx={{ mt: 2 }}>
                  <Typography variant="body2">
                    Importação realizada com sucesso!
                    {folhaAdpState.arquivo && (
                      <>
                        <br />
                        Arquivo: {folhaAdpState.arquivo}
                      </>
                    )}
                    {folhaAdpState.tamanho && (
                      <>
                        <br />
                        Tamanho: {(folhaAdpState.tamanho / 1024).toFixed(2)} KB
                      </>
                    )}
                    {folhaAdpState.registrosProcessados && (
                      <>
                        <br />
                        Registros processados: {folhaAdpState.registrosProcessados}
                      </>
                    )}
                  </Typography>
                </Alert>
              )}

              {folhaAdpState.error && (
                <Alert severity="error" sx={{ mt: 2 }}>
                  <Typography variant="body2">{folhaAdpState.error}</Typography>
                </Alert>
              )}

              {folhaAdpState.erros && folhaAdpState.erros.length > 0 && (
                <Paper sx={{ mt: 2, p: 2, maxHeight: 200, overflow: 'auto' }}>
                  <Typography variant="subtitle2" color="error" gutterBottom>
                    Erros encontrados:
                  </Typography>
                  <List dense>
                    {folhaAdpState.erros.map((erro, index) => (
                      <ListItem key={index}>
                        <ListItemIcon>
                          <ErrorIcon color="error" fontSize="small" />
                        </ListItemIcon>
                        <ListItemText primary={erro} />
                      </ListItem>
                    ))}
                  </List>
                </Paper>
              )}
            </CardContent>
          </Card>
        </Box>
      </Box>

      {/* Instruções */}
      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Instruções de Importação
          </Typography>
          
          <Box display="flex" gap={2} flexWrap="wrap">
            <Box flex="1" minWidth="300px">
              <Typography variant="subtitle1" gutterBottom>
                <strong>Folha de Pagamento (.txt)</strong>
              </Typography>
              <Typography variant="body2" paragraph>
                O arquivo deve conter:
              </Typography>
              <List dense>
                <ListItem>
                  <ListItemIcon>
                    <CheckCircleIcon color="success" fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary="Linha com período de competência (ex: Competência: 01/01/2024 a 31/01/2024)" />
                </ListItem>
                <ListItem>
                  <ListItemIcon>
                    <CheckCircleIcon color="success" fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary="Linha com centro de custo, ID e nome do funcionário (ex: 258 SERVICOS - EDU 273 RENATO AMANCIO DA SILVA)" />
                </ListItem>
                <ListItem>
                  <ListItemIcon>
                    <CheckCircleIcon color="success" fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary="Linhas com rubricas (código, descrição, quantidade, base cálculo, valor)" />
                </ListItem>
              </List>
            </Box>
            
            <Box flex="1" minWidth="300px">
              <Typography variant="subtitle1" gutterBottom>
                <strong>Benefícios (.csv)</strong>
              </Typography>
              <Typography variant="body2" paragraph>
                O arquivo deve conter as colunas:
              </Typography>
              <List dense>
                <ListItem>
                  <ListItemIcon>
                    <CheckCircleIcon color="success" fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary="ID do funcionário" />
                </ListItem>
                <ListItem>
                  <ListItemIcon>
                    <CheckCircleIcon color="success" fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary="Descrição do benefício" />
                </ListItem>
                <ListItem>
                  <ListItemIcon>
                    <CheckCircleIcon color="success" fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary="Valor" />
                </ListItem>
                <ListItem>
                  <ListItemIcon>
                    <CheckCircleIcon color="success" fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary="Data de início (dd/MM/yyyy)" />
                </ListItem>
                <ListItem>
                  <ListItemIcon>
                    <CheckCircleIcon color="success" fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary="Data de fim (opcional, dd/MM/yyyy)" />
                </ListItem>
              </List>
            </Box>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
} 