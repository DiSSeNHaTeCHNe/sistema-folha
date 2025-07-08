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
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Upload as UploadIcon,
} from '@mui/icons-material';
import type { Beneficio } from '../../types';

export function Beneficios() {
  const [searchTerm, setSearchTerm] = useState('');

  // Mock data - substituir por dados reais da API
  const beneficios: Beneficio[] = [
    {
      id: 1,
      funcionarioId: 1,
      funcionarioNome: 'João Silva',
      descricao: 'Vale Refeição',
      valor: 500.00,
      dataInicio: '2024-01-01',
      dataFim: '2024-12-31',
      observacao: 'Benefício mensal',
    },
    // Adicionar mais benefícios aqui
  ];

  const handleSearch = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(event.target.value);
  };

  const filteredBeneficios = beneficios.filter((beneficio) =>
    beneficio.funcionarioNome.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography component="h1" variant="h4">
          Benefícios
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
            Novo Benefício
          </Button>
        </Box>
      </Box>

      <Box sx={{ mb: 3 }}>
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
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Funcionário</TableCell>
              <TableCell>Descrição</TableCell>
              <TableCell>Valor</TableCell>
              <TableCell>Período</TableCell>
              <TableCell>Observação</TableCell>
              <TableCell>Ações</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredBeneficios.map((beneficio) => (
              <TableRow key={beneficio.id}>
                <TableCell>{beneficio.funcionarioNome}</TableCell>
                <TableCell>{beneficio.descricao}</TableCell>
                <TableCell>
                  {new Intl.NumberFormat('pt-BR', {
                    style: 'currency',
                    currency: 'BRL',
                  }).format(beneficio.valor)}
                </TableCell>
                <TableCell>
                  {beneficio.dataInicio} a {beneficio.dataFim}
                </TableCell>
                <TableCell>{beneficio.observacao}</TableCell>
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