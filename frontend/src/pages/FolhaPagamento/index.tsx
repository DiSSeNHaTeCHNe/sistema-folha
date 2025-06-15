import { useState } from 'react';
import {
  Box,
  Button,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  IconButton,
  TextField,
  InputAdornment,
  Grid,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Upload as UploadIcon,
} from '@mui/icons-material';
import type { FolhaPagamento } from '../../types';

export function FolhaPagamento() {
  const [searchTerm, setSearchTerm] = useState('');
  const [mes, setMes] = useState('');
  const [ano, setAno] = useState('');

  // Mock data - substituir por dados reais da API
  const folhaPagamento: FolhaPagamento[] = [
    {
      id: 1,
      funcionarioId: 1,
      funcionarioNome: 'João Silva',
      rubricaId: 1,
      rubricaCodigo: 'SAL',
      rubricaDescricao: 'Salário Base',
      dataInicio: '2024-03-01',
      dataFim: '2024-03-31',
      valor: 5000.00,
      quantidade: 1,
      baseCalculo: 5000.00,
    },
    // Adicionar mais registros aqui
  ];

  const handleSearch = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(event.target.value);
  };

  const filteredFolha = folhaPagamento.filter((item) =>
    item.funcionarioNome.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography component="h1" variant="h4">
          Folha de Pagamento
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<UploadIcon />}
            onClick={() => {
              // Implementar importação
            }}
            sx={{ mr: 2 }}
          >
            Importar
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              // Implementar criação
            }}
          >
            Novo Registro
          </Button>
        </Box>
      </Box>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            variant="outlined"
            placeholder="Buscar funcionário..."
            value={searchTerm}
            onChange={handleSearch}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
          />
        </Grid>
        <Grid item xs={6} md={3}>
          <TextField
            fullWidth
            label="Mês"
            type="number"
            value={mes}
            onChange={(e) => setMes(e.target.value)}
            inputProps={{ min: 1, max: 12 }}
          />
        </Grid>
        <Grid item xs={6} md={3}>
          <TextField
            fullWidth
            label="Ano"
            type="number"
            value={ano}
            onChange={(e) => setAno(e.target.value)}
            inputProps={{ min: 2000, max: 2100 }}
          />
        </Grid>
      </Grid>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Funcionário</TableCell>
              <TableCell>Rubrica</TableCell>
              <TableCell>Período</TableCell>
              <TableCell>Valor</TableCell>
              <TableCell>Quantidade</TableCell>
              <TableCell>Base de Cálculo</TableCell>
              <TableCell>Ações</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredFolha.map((item) => (
              <TableRow key={item.id}>
                <TableCell>{item.funcionarioNome}</TableCell>
                <TableCell>
                  {item.rubricaCodigo} - {item.rubricaDescricao}
                </TableCell>
                <TableCell>
                  {item.dataInicio} a {item.dataFim}
                </TableCell>
                <TableCell>
                  {new Intl.NumberFormat('pt-BR', {
                    style: 'currency',
                    currency: 'BRL',
                  }).format(item.valor)}
                </TableCell>
                <TableCell>{item.quantidade}</TableCell>
                <TableCell>
                  {new Intl.NumberFormat('pt-BR', {
                    style: 'currency',
                    currency: 'BRL',
                  }).format(item.baseCalculo)}
                </TableCell>
                <TableCell>
                  <IconButton
                    color="primary"
                    onClick={() => {
                      // Implementar edição
                    }}
                  >
                    <EditIcon />
                  </IconButton>
                  <IconButton
                    color="error"
                    onClick={() => {
                      // Implementar exclusão
                    }}
                  >
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
} 