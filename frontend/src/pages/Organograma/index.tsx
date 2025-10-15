import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
  TextField,
  Chip,
  IconButton,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Paper,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  ExpandMore as ExpandMoreIcon,
  Person as PersonIcon,
  Business as BusinessIcon,
  DragIndicator as DragIcon,
  AccountTree as TreeIcon,
} from '@mui/icons-material';
import {
  DndContext,
  closestCenter,
  useSensor,
  useSensors,
  PointerSensor,
  KeyboardSensor,
} from '@dnd-kit/core';
import type { DragEndEvent } from '@dnd-kit/core';
import {
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { organogramaService } from '../../services/organogramaService';
import { funcionarioService } from '../../services/funcionarioService';
import { centroCustoService } from '../../services/centroCustoService';
import type { 
  NoOrganograma, 
  NoOrganogramaFormData, 
  Funcionario, 
  CentroCusto
} from '../../types';

interface NoOrganogramaWithChildren extends NoOrganograma {
  children: NoOrganogramaWithChildren[];
}

interface DragItem {
  id: string;
  type: 'funcionario' | 'centroCusto';
  data: Funcionario | CentroCusto;
}

// Componente para um nó do organograma com drag & drop
const NoOrganogramaCard: React.FC<{
  no: NoOrganogramaWithChildren;
  onEdit: (no: NoOrganograma) => void;
  onDelete: (id: number) => void;
  onAddChild: (parentId: number) => void;
  onRemoveFuncionario: (noId: number, funcionarioId: number) => void;
  onRemoveCentroCusto: (noId: number, centroCustoId: number) => void;
}> = ({ no, onEdit, onDelete, onAddChild, onRemoveFuncionario, onRemoveCentroCusto }) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: `no-${no.id}`,
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  return (
    <div ref={setNodeRef} style={style}>
      <Card
        sx={{
          mb: 2,
          border: '2px solid',
          borderColor: isDragging ? 'primary.main' : 'grey.300',
          minHeight: 200,
        }}
      >
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Box display="flex" alignItems="center" gap={1}>
              <IconButton size="small" {...attributes} {...listeners}>
                <DragIcon />
              </IconButton>
              <Typography variant="h6">{no.nome}</Typography>
            </Box>
            <Box>
              <IconButton size="small" onClick={() => onAddChild(no.id)}>
                <AddIcon />
              </IconButton>
              <IconButton size="small" onClick={() => onEdit(no)}>
                <EditIcon />
              </IconButton>
              <IconButton size="small" onClick={() => onDelete(no.id)}>
                <DeleteIcon />
              </IconButton>
            </Box>
          </Box>

          {no.descricao && (
            <Typography variant="body2" color="textSecondary" mb={2}>
              {no.descricao}
            </Typography>
          )}

          {/* Funcionários */}
          <Box mb={2}>
            <Typography variant="subtitle2" display="flex" alignItems="center" gap={1} mb={1}>
              <PersonIcon fontSize="small" />
              Funcionários ({no.funcionarios?.length || 0})
            </Typography>
            <Box display="flex" flexWrap="wrap" gap={1}>
              {no.funcionarios?.map((func) => (
                <Chip
                  key={func.id}
                  label={func.nome}
                  size="small"
                  onDelete={() => onRemoveFuncionario(no.id, func.id)}
                  color="primary"
                  variant="outlined"
                />
              ))}
            </Box>
          </Box>

          {/* Centros de Custo */}
          <Box>
            <Typography variant="subtitle2" display="flex" alignItems="center" gap={1} mb={1}>
              <BusinessIcon fontSize="small" />
              Centros de Custo ({no.centrosCusto?.length || 0})
            </Typography>
            <Box display="flex" flexWrap="wrap" gap={1}>
              {no.centrosCusto?.map((centro) => (
                <Chip
                  key={centro.id}
                  label={centro.descricao}
                  size="small"
                  onDelete={() => onRemoveCentroCusto(no.id, centro.id)}
                  color="secondary"
                  variant="outlined"
                />
              ))}
            </Box>
          </Box>
        </CardContent>
      </Card>

      {/* Nós filhos */}
      {no.children && no.children.length > 0 && (
        <Box ml={4} mt={2}>
          {no.children.map((child) => (
            <NoOrganogramaCard
              key={child.id}
              no={child}
              onEdit={onEdit}
              onDelete={onDelete}
              onAddChild={onAddChild}
              onRemoveFuncionario={onRemoveFuncionario}
              onRemoveCentroCusto={onRemoveCentroCusto}
            />
          ))}
        </Box>
      )}
    </div>
  );
};

// Componente para item arrastável (funcionário ou centro de custo)
const DraggableItem: React.FC<{
  item: DragItem;
}> = ({ item }) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: item.id,
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  return (
    <div ref={setNodeRef} style={style} {...attributes} {...listeners}>
      <Paper
        sx={{
          p: 1,
          mb: 1,
          cursor: 'grab',
          '&:hover': { bgcolor: 'grey.100' },
          border: isDragging ? '2px solid' : '1px solid',
          borderColor: isDragging ? 'primary.main' : 'grey.300',
        }}
      >
        <Typography variant="body2" display="flex" alignItems="center" gap={1}>
          {item.type === 'funcionario' ? <PersonIcon fontSize="small" /> : <BusinessIcon fontSize="small" />}
          {item.type === 'funcionario' 
            ? (item.data as Funcionario).nome 
            : (item.data as CentroCusto).descricao
          }
        </Typography>
      </Paper>
    </div>
  );
};

export default function Organograma() {
  const [nos, setNos] = useState<NoOrganogramaWithChildren[]>([]);
  const [funcionarios, setFuncionarios] = useState<Funcionario[]>([]);
  const [centrosCusto, setCentrosCusto] = useState<CentroCusto[]>([]);
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedNo, setSelectedNo] = useState<NoOrganograma | null>(null);
  const [parentIdForNew, setParentIdForNew] = useState<number | undefined>();
  const [loading, setLoading] = useState(true);

  const { register, handleSubmit, reset, setValue } = useForm<NoOrganogramaFormData>();

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  useEffect(() => {
    carregarDados();
  }, []);

  const carregarDados = async () => {
    try {
      setLoading(true);
      const [nosData, funcionariosData, centrosCustoData] = await Promise.all([
        organogramaService.listarTodos(),
        funcionarioService.listar(),
        centroCustoService.listarTodos(),
      ]);

      // Construir árvore hierárquica
      const arvore = construirArvore(nosData);
      setNos(arvore);
      setFuncionarios(funcionariosData);
      setCentrosCusto(centrosCustoData);
    } catch (error) {
      toast.error('Erro ao carregar dados do organograma');
    } finally {
      setLoading(false);
    }
  };

  const construirArvore = (nos: NoOrganograma[]): NoOrganogramaWithChildren[] => {
    const nosMap = new Map<number, NoOrganogramaWithChildren>();
    const raizes: NoOrganogramaWithChildren[] = [];

    // Criar mapa de nós
    nos.forEach(no => {
      nosMap.set(no.id, { 
        ...no, 
        children: [],
        funcionarios: no.funcionarios || [],
        centrosCusto: no.centrosCusto || []
      });
    });

    // Construir hierarquia
    nos.forEach(no => {
      const noComChildren = nosMap.get(no.id)!;
      if (no.parentId) {
        const parent = nosMap.get(no.parentId);
        if (parent) {
          parent.children.push(noComChildren);
        }
      } else {
        raizes.push(noComChildren);
      }
    });

    // Ordenar por posição
    const ordenarPorPosicao = (nos: NoOrganogramaWithChildren[]) => {
      nos.sort((a, b) => a.posicao - b.posicao);
      nos.forEach(no => ordenarPorPosicao(no.children));
    };

    ordenarPorPosicao(raizes);
    return raizes;
  };

  const handleOpenDialog = (no?: NoOrganograma, parentId?: number) => {
    if (no) {
      setSelectedNo(no);
      setValue('nome', no.nome);
      setValue('descricao', no.descricao || '');
      setValue('parentId', no.parentId);
    } else {
      setSelectedNo(null);
      reset();
      setValue('parentId', parentId);
    }
    setParentIdForNew(parentId);
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setSelectedNo(null);
    setParentIdForNew(undefined);
    reset();
  };

  const onSubmit = async (data: NoOrganogramaFormData) => {
    try {
      if (selectedNo) {
        await organogramaService.atualizarNo(selectedNo.id, data);
        toast.success('Nó atualizado com sucesso');
      } else {
        await organogramaService.criarNo(data);
        toast.success('Nó criado com sucesso');
      }
      handleCloseDialog();
      carregarDados();
    } catch (error) {
      toast.error('Erro ao salvar nó');
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Tem certeza que deseja excluir este nó? Todos os subnós também serão excluídos.')) {
      try {
        await organogramaService.removerNo(id);
        toast.success('Nó excluído com sucesso');
        carregarDados();
      } catch (error) {
        toast.error('Erro ao excluir nó');
      }
    }
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    
    if (!over) return;

    const activeId = active.id as string;
    const overId = over.id as string;

    // Se é um funcionário ou centro de custo sendo arrastado para um nó
    if (activeId.startsWith('funcionario-') && overId.startsWith('no-')) {
      const funcionarioId = parseInt(activeId.replace('funcionario-', ''));
      const noId = parseInt(overId.replace('no-', ''));
      
      try {
        await organogramaService.adicionarFuncionario(noId, funcionarioId);
        toast.success('Funcionário adicionado ao nó');
        carregarDados();
      } catch (error) {
        toast.error('Erro ao adicionar funcionário');
      }
    }

    if (activeId.startsWith('centroCusto-') && overId.startsWith('no-')) {
      const centroCustoId = parseInt(activeId.replace('centroCusto-', ''));
      const noId = parseInt(overId.replace('no-', ''));
      
      try {
        await organogramaService.adicionarCentroCusto(noId, centroCustoId);
        toast.success('Centro de custo adicionado ao nó');
        carregarDados();
      } catch (error) {
        toast.error('Erro ao adicionar centro de custo');
      }
    }
  };

  const handleRemoveFuncionario = async (noId: number, funcionarioId: number) => {
    try {
      await organogramaService.removerFuncionario(noId, funcionarioId);
      toast.success('Funcionário removido do nó');
      carregarDados();
    } catch (error) {
      toast.error('Erro ao remover funcionário');
    }
  };

  const handleRemoveCentroCusto = async (noId: number, centroCustoId: number) => {
    try {
      await organogramaService.removerCentroCusto(noId, centroCustoId);
      toast.success('Centro de custo removido do nó');
      carregarDados();
    } catch (error) {
      toast.error('Erro ao remover centro de custo');
    }
  };

  const getAllNodeIds = (no: NoOrganogramaWithChildren): number[] => {
    return [no.id, ...no.children.flatMap(child => getAllNodeIds(child))];
  };

  const nosIds = nos.flatMap(no => getAllNodeIds(no)).map(id => `no-${id}`);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="400px">
        <Typography>Carregando organograma...</Typography>
      </Box>
    );
  }

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragEnd={handleDragEnd}
    >
      <Box p={3}>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h4" display="flex" alignItems="center" gap={1}>
            <TreeIcon />
            Organograma
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            Novo Nó Raiz
          </Button>
        </Box>

        <Box display="flex" gap={3}>
          {/* Área do organograma */}
          <Box flex="2">
            <Paper sx={{ p: 2, minHeight: 600 }}>
              <Typography variant="h6" mb={2}>
                Estrutura do Organograma
              </Typography>
              
              <SortableContext items={nosIds}>
                {nos.length === 0 ? (
                  <Box 
                    display="flex" 
                    flexDirection="column" 
                    alignItems="center" 
                    justifyContent="center" 
                    height={400}
                    color="text.secondary"
                  >
                    <TreeIcon sx={{ fontSize: 64, mb: 2 }} />
                    <Typography variant="h6">Nenhum nó criado</Typography>
                    <Typography>Clique em "Novo Nó Raiz" para começar</Typography>
                  </Box>
                ) : (
                  nos.map((no) => (
                    <NoOrganogramaCard
                      key={no.id}
                      no={no}
                      onEdit={handleOpenDialog}
                      onDelete={handleDelete}
                      onAddChild={(parentId) => handleOpenDialog(undefined, parentId)}
                      onRemoveFuncionario={handleRemoveFuncionario}
                      onRemoveCentroCusto={handleRemoveCentroCusto}
                    />
                  ))
                )}
              </SortableContext>
            </Paper>
          </Box>

          {/* Painel lateral com funcionários e centros de custo */}
          <Box flex="1" minWidth={300}>
            <Paper sx={{ p: 2, height: 600, overflow: 'auto' }}>
              <Typography variant="h6" mb={2}>
                Arrastar para Associar
              </Typography>
              
              {/* Funcionários */}
              <Accordion defaultExpanded>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography display="flex" alignItems="center" gap={1}>
                    <PersonIcon />
                    Funcionários ({funcionarios.length})
                  </Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <SortableContext items={funcionarios.map(f => `funcionario-${f.id}`)}>
                    {funcionarios.map((funcionario) => (
                      <DraggableItem
                        key={`funcionario-${funcionario.id}`}
                        item={{
                          id: `funcionario-${funcionario.id}`,
                          type: 'funcionario',
                          data: funcionario,
                        }}
                      />
                    ))}
                  </SortableContext>
                </AccordionDetails>
              </Accordion>

              {/* Centros de Custo */}
              <Accordion defaultExpanded>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography display="flex" alignItems="center" gap={1}>
                    <BusinessIcon />
                    Centros de Custo ({centrosCusto.length})
                  </Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <SortableContext items={centrosCusto.map(c => `centroCusto-${c.id}`)}>
                    {centrosCusto.map((centroCusto) => (
                      <DraggableItem
                        key={`centroCusto-${centroCusto.id}`}
                        item={{
                          id: `centroCusto-${centroCusto.id}`,
                          type: 'centroCusto',
                          data: centroCusto,
                        }}
                      />
                    ))}
                  </SortableContext>
                </AccordionDetails>
              </Accordion>
            </Paper>
          </Box>
        </Box>

        {/* Dialog para criar/editar nó */}
        <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
          <form onSubmit={handleSubmit(onSubmit)}>
            <DialogTitle>
              {selectedNo ? 'Editar Nó' : 'Criar Novo Nó'}
            </DialogTitle>
            <DialogContent>
              <TextField
                {...register('nome', { required: 'Nome é obrigatório' })}
                label="Nome"
                fullWidth
                margin="normal"
              />
              <TextField
                {...register('descricao')}
                label="Descrição"
                fullWidth
                margin="normal"
                multiline
                rows={3}
              />
              {parentIdForNew && (
                <Typography variant="body2" color="textSecondary" mt={1}>
                  Este nó será criado como filho do nó selecionado.
                </Typography>
              )}
            </DialogContent>
            <DialogActions>
              <Button onClick={handleCloseDialog}>Cancelar</Button>
              <Button type="submit" variant="contained">
                {selectedNo ? 'Salvar' : 'Criar'}
              </Button>
            </DialogActions>
          </form>
        </Dialog>
      </Box>
    </DndContext>
  );
} 