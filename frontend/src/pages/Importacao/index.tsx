import React, { useState, useRef } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Alert,
  CircularProgress,
  Paper,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
  FormControlLabel,
  Checkbox,
} from '@mui/material';
import {
  CloudUpload as CloudUploadIcon,
  Error as ErrorIcon,
  Description as DescriptionIcon,
  AttachMoney as AttachMoneyIcon,
  CardGiftcard as CardGiftcardIcon,
  HelpOutline as HelpOutlineIcon,
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

  const beneficiosFileRef = useRef<HTMLInputElement>(null);
  const folhaAdpFileRef = useRef<HTMLInputElement>(null);

  const [helpOpen, setHelpOpen] = useState(false);
  const [conflictDialogOpen, setConflictDialogOpen] = useState(false);
  const [conflictMessage, setConflictMessage] = useState('');
  const [pendingFile, setPendingFile] = useState<File | null>(null);

  const [beneficiosFileName, setBeneficiosFileName] = useState('');
  const [folhaAdpFileName, setFolhaAdpFileName] = useState('');
  const [isDecimoTerceiro, setIsDecimoTerceiro] = useState(false);

  const handleFileUpload = async (
    file: File | null,
    tipo: 'beneficios' | 'folhaAdp',
    setState: React.Dispatch<React.SetStateAction<UploadState>>,
    decimoTerceiro?: boolean
  ) => {
    if (!file) {
      toast.error('Por favor, selecione um arquivo');
      return;
    }

    // Validação de tipo de arquivo
    if (tipo === 'folhaAdp' && !file.name.toLowerCase().endsWith('.txt')) {
      toast.error('Para importação de folha ADP, selecione apenas arquivos .txt');
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
        case 'folhaAdp':
          response = await importacaoService.importarFolhaAdp(file, decimoTerceiro || false);
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

        const tipoNome = tipo === 'folhaAdp' ? 'folha ADP' : 'benefícios';
        toast.success(`Arquivo de ${tipoNome} importado com sucesso!`);
        
        // Limpar campos para próxima importação
        if (tipo === 'folhaAdp') {
          setFolhaAdpFileName('');
          setIsDecimoTerceiro(false);
          if (folhaAdpFileRef.current) {
            folhaAdpFileRef.current.value = '';
          }
        } else if (tipo === 'beneficios') {
          setBeneficiosFileName('');
          if (beneficiosFileRef.current) {
            beneficiosFileRef.current.value = '';
          }
        }
      } else {
        setState({
          loading: false,
          success: false,
          error: response.message,
          arquivo: response.arquivo,
        });
        if (response.message && response.message.startsWith('Funcionários não encontrados:')) {
          alert(response.message);
        } else {
          toast.error(response.message);
        }
      }
    } catch (error: any) {
      // Verificar se é um conflito (status 409)
      if (error.response?.status === 409) {
        const conflictData = error.response.data;
        setConflictMessage(conflictData.message);
        setPendingFile(file);
        setConflictDialogOpen(true);
        setState({
          loading: false,
          success: false,
          error: null,
        });
        return;
      }
      
      const errorMessage = error.response?.data?.message || 'Erro ao importar arquivo';
      setState({
        loading: false,
        success: false,
        error: errorMessage,
      });
      if (errorMessage.startsWith('Funcionários não encontrados:')) {
        alert(errorMessage);
      } else {
        toast.error(errorMessage);
      }
    }
  };

  const handleBeneficiosFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    setBeneficiosFileName(file ? file.name : '');
    setBeneficiosState(prev => ({ ...prev, success: false, error: null }));
  };

  const handleFolhaAdpFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    setFolhaAdpFileName(file ? file.name : '');
    setFolhaAdpState(prev => ({ ...prev, success: false, error: null }));
  };

  const handleBeneficiosUpload = () => {
    const file = beneficiosFileRef.current?.files?.[0] || null;
    handleFileUpload(file, 'beneficios', setBeneficiosState);
  };

  const handleFolhaAdpUpload = () => {
    const file = folhaAdpFileRef.current?.files?.[0] || null;
    handleFileUpload(file, 'folhaAdp', setFolhaAdpState, isDecimoTerceiro);
  };

  const resetBeneficiosState = () => {
    setBeneficiosState({
      loading: false,
      success: false,
      error: null,
    });
    setBeneficiosFileName('');
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
    setFolhaAdpFileName('');
    setIsDecimoTerceiro(false);
    if (folhaAdpFileRef.current) {
      folhaAdpFileRef.current.value = '';
    }
  };

  const handleConfirmSubstituicao = async () => {
    setConflictDialogOpen(false);
    if (pendingFile) {
      // Reenviar com flag de confirmação
      setFolhaAdpState({ loading: true, success: false, error: null });
      try {
        const response = await importacaoService.importarFolhaAdp(pendingFile, isDecimoTerceiro, true);
        if (response.success) {
          setFolhaAdpState({
            loading: false,
            success: true,
            error: null,
            registrosProcessados: response.registrosProcessados,
            erros: response.erros,
            arquivo: response.arquivo,
            tamanho: response.tamanho,
          });
          toast.success('Folha ADP importada com sucesso! A folha anterior foi substituída.');
          
          // Limpar campos para próxima importação
          setFolhaAdpFileName('');
          setIsDecimoTerceiro(false);
          if (folhaAdpFileRef.current) {
            folhaAdpFileRef.current.value = '';
          }
        }
      } catch (error: any) {
        const errorMessage = error.response?.data?.message || 'Erro ao importar arquivo';
        setFolhaAdpState({
          loading: false,
          success: false,
          error: errorMessage,
        });
        toast.error(errorMessage);
      } finally {
        setPendingFile(null);
      }
    }
  };

  const handleCancelSubstituicao = () => {
    setConflictDialogOpen(false);
    setPendingFile(null);
    setFolhaAdpState({
      loading: false,
      success: false,
      error: null,
    });
  };

  return (
    <Box p={3}>
      <Box display="flex" alignItems="center" gap={1}>
        <Typography variant="h4" gutterBottom>
          Importação de Dados
        </Typography>
        <IconButton size="small" onClick={() => setHelpOpen(true)}>
          <HelpOutlineIcon fontSize="small" />
        </IconButton>
      </Box>
      <Dialog open={helpOpen} onClose={() => setHelpOpen(false)}>
        <DialogTitle>Ajuda - Importação de Dados</DialogTitle>
        <DialogContent>
          <Typography gutterBottom>
            Faça upload dos arquivos de folha de pagamento (.txt) ou benefícios (.csv).<br/>
            O sistema irá processar os dados e exibir o resultado da importação.<br/>
            Caso algum funcionário não seja encontrado, será exibida uma lista ao final.<br/>
            Utilize o campo abaixo para visualizar o retorno detalhado da importação.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setHelpOpen(false)}>Fechar</Button>
        </DialogActions>
      </Dialog>
      <Typography variant="body1" color="text.secondary" paragraph>
        Faça upload dos arquivos para importar dados de folha de pagamento e benefícios no sistema.
      </Typography>

      <Box display="flex" gap={3} flexWrap="wrap">
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
                  onChange={handleBeneficiosFileChange}
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
                
                <Typography variant="body2" color="primary">
                  Arquivo selecionado: {beneficiosFileName || ''}
                </Typography>
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
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={isDecimoTerceiro}
                      onChange={(e) => setIsDecimoTerceiro(e.target.checked)}
                      color="secondary"
                    />
                  }
                  label="Marcar como folha de 13º salário"
                  sx={{ mb: 2 }}
                />
              </Box>

              <Box mb={2}>
                <input
                  ref={folhaAdpFileRef}
                  type="file"
                  accept=".txt"
                  style={{ display: 'none' }}
                  onChange={handleFolhaAdpFileChange}
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
                
                <Typography variant="body2" color="primary">
                  Arquivo selecionado: {folhaAdpFileName || ''}
                </Typography>
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
            </CardContent>
          </Card>
        </Box>
      </Box>
      <Card sx={{ mt: 4 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Status da Importação
          </Typography>
          {/* Status loading */}
          {(beneficiosState.loading || folhaAdpState.loading) && (
            <Box display="flex" alignItems="center" mb={2}>
              <CircularProgress size={20} sx={{ mr: 1 }} />
              <Typography variant="body2">Processando arquivo...</Typography>
            </Box>
          )}
          {/* Sucesso */}
          {(beneficiosState.success || folhaAdpState.success) && (
            <Alert severity="success" sx={{ mb: 2 }}>
              <Typography variant="body2">
                Importação realizada com sucesso!
                {beneficiosState.success && beneficiosState.arquivo && (<><br />Arquivo: {beneficiosState.arquivo}</>)}
                {folhaAdpState.success && folhaAdpState.arquivo && (<><br />Arquivo: {folhaAdpState.arquivo}</>)}
                {beneficiosState.success && beneficiosState.tamanho && (<><br />Tamanho: {(beneficiosState.tamanho / 1024).toFixed(2)} KB</>)}
                {folhaAdpState.success && folhaAdpState.tamanho && (<><br />Tamanho: {(folhaAdpState.tamanho / 1024).toFixed(2)} KB</>)}
                {beneficiosState.success && beneficiosState.registrosProcessados && (<><br />Registros processados: {beneficiosState.registrosProcessados}</>)}
                {folhaAdpState.success && folhaAdpState.registrosProcessados && (<><br />Registros processados: {folhaAdpState.registrosProcessados}</>)}
              </Typography>
            </Alert>
          )}
          {/* Erro */}
          {(beneficiosState.error || folhaAdpState.error) && (
            <Alert severity="error" sx={{ mb: 2 }}>
              <Typography variant="body2">
                {beneficiosState.error || folhaAdpState.error}
              </Typography>
            </Alert>
          )}
          {/* Lista de erros detalhados */}
          {((beneficiosState.erros && beneficiosState.erros.length > 0) || (folhaAdpState.erros && folhaAdpState.erros.length > 0)) && (
            <Paper sx={{ p: 2, maxHeight: 200, overflow: 'auto' }}>
              <Typography variant="subtitle2" color="error" gutterBottom>
                Erros encontrados:
              </Typography>
              <List dense>
                {beneficiosState.erros && beneficiosState.erros.map((erro, index) => (
                  <ListItem key={"beneficio-"+index}>
                    <ListItemIcon>
                      <ErrorIcon color="error" fontSize="small" />
                    </ListItemIcon>
                    <ListItemText primary={erro} />
                  </ListItem>
                ))}
                {folhaAdpState.erros && folhaAdpState.erros.map((erro, index) => (
                  <ListItem key={"adp-"+index}>
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

      {/* Dialog de Confirmação de Substituição */}
      <Dialog open={conflictDialogOpen} onClose={handleCancelSubstituicao}>
        <DialogTitle>Confirmar Substituição de Folha</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            {conflictMessage}
          </Alert>
          <Typography variant="body2">
            Esta ação irá desativar a folha de pagamento existente e todos os seus registros,
            substituindo-os pelos dados do novo arquivo.
          </Typography>
          <Typography variant="body2" sx={{ mt: 2, fontWeight: 'bold' }}>
            Deseja continuar?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCancelSubstituicao} color="inherit">
            Cancelar
          </Button>
          <Button onClick={handleConfirmSubstituicao} variant="contained" color="warning">
            Confirmar Substituição
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
} 