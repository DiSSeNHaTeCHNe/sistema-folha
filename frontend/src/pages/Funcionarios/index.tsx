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
} from '@mui/icons-material';
import type { Funcionario } from '../../types';

export function Funcionarios() {
  const [searchTerm, setSearchTerm] = useState('');

  // Mock data - substituir por dados reais da API
  const funcionarios: Funcionario[] = [
    {
      id: 1,
      nome: 'João Silva',
      cargo: 'Desenvolvedor',
      centroCusto: 'TI',
      linhaNegocio: 'Desenvolvimento',
      idExterno: '12345',
      dataAdmissao: '2023-01-01',
      sexo: 'M',
      tipoSalario: 'MENSAL',
      funcao: 'Desenvolvedor Full Stack',
      depIrrf: 0,
      depSalFamilia: 0,
      vinculo: 'CLT',
    },
    // Adicionar mais funcionários aqui
  ];

  const handleSearch = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(event.target.value);
  };

  const filteredFuncionarios = funcionarios.filter((funcionario) =>
    funcionario.nome.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography component="h1" variant="h4">
          Funcionários
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => {
            // Implementar criação de funcionário
          }}
        >
          Novo Funcionário
        </Button>
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
              <TableCell>Nome</TableCell>
              <TableCell>Cargo</TableCell>
              <TableCell>Centro de Custo</TableCell>
              <TableCell>Linha de Negócio</TableCell>
              <TableCell>Data de Admissão</TableCell>
              <TableCell>Ações</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredFuncionarios.map((funcionario) => (
              <TableRow key={funcionario.id}>
                <TableCell>{funcionario.nome}</TableCell>
                <TableCell>{funcionario.cargo}</TableCell>
                <TableCell>{funcionario.centroCusto}</TableCell>
                <TableCell>{funcionario.linhaNegocio}</TableCell>
                <TableCell>{funcionario.dataAdmissao}</TableCell>
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