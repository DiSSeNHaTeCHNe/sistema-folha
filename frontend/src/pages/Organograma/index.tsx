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
  Paper,
  ToggleButtonGroup,
  ToggleButton,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Person as PersonIcon,
  Business as BusinessIcon,
  AccountTree as TreeIcon,
  ViewList as ViewListIcon,
  AccountTreeOutlined as GraphIcon,
} from '@mui/icons-material';
import {
  DndContext,
  closestCenter,
  useSensor,
  useSensors,
  PointerSensor,
  KeyboardSensor,
  DragOverlay,
  useDroppable,
  useDraggable,
} from '@dnd-kit/core';
import type { DragEndEvent, DragStartEvent } from '@dnd-kit/core';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { organogramaService } from '../../services/organogramaService';
import { funcionarioService } from '../../services/funcionarioService';
import { centroCustoService } from '../../services/centroCustoService';
import OrganogramaGrafico from '../../components/OrganogramaGrafico';
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
  expandedNodeId: number | null;
  hoveredNodeId: number | null;
  onToggleExpand: (id: number) => void;
  onHover: (id: number | null) => void;
}> = ({ 
  no, 
  onEdit, 
  onDelete, 
  onAddChild, 
  onRemoveFuncionario, 
  onRemoveCentroCusto,
  expandedNodeId,
  hoveredNodeId,
  onToggleExpand,
  onHover,
}) => {
  // Usar useDroppable para aceitar itens arrastados
  const { setNodeRef, isOver } = useDroppable({
    id: `no-${no.id}`,
    data: {
      type: 'no-organograma',
      noId: no.id,
    },
  });

  const isExpanded = expandedNodeId === no.id;
  const isHovered = hoveredNodeId === no.id;
  const showDetails = isExpanded || isHovered;
  const funcionariosCount = no.funcionarios?.length || 0;
  const centrosCustoCount = no.centrosCusto?.length || 0;

  return (
    <Box mb={2}>
      <Card
        ref={setNodeRef}
        onMouseEnter={() => onHover(no.id)}
        onMouseLeave={() => onHover(null)}
        onClick={() => onToggleExpand(no.id)}
        sx={{
          border: '2px solid',
          borderColor: isOver ? 'primary.main' : isExpanded ? 'primary.main' : 'grey.300',
          bgcolor: isOver ? 'primary.light' : 'background.paper',
          minHeight: showDetails ? 200 : 56,
          transition: 'all 0.3s ease-in-out',
          position: 'relative',
          cursor: 'pointer',
          '&:hover': {
            boxShadow: 3,
          },
        }}
      >
        <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
          {!showDetails ? (
            // MODO COMPACTO - Só título e badges
            <Box display="flex" justifyContent="space-between" alignItems="center">
              <Box display="flex" alignItems="center" gap={1} flex={1}>
                <Typography variant="subtitle1" fontWeight="bold">
                  {no.nome}
                </Typography>
                {funcionariosCount > 0 && (
                  <Chip
                    icon={<PersonIcon />}
                    label={funcionariosCount}
                    size="small"
                    color="primary"
                    sx={{ height: 24 }}
                  />
                )}
                {centrosCustoCount > 0 && (
                  <Chip
                    icon={<BusinessIcon />}
                    label={centrosCustoCount}
                    size="small"
                    color="secondary"
                    sx={{ height: 24 }}
                  />
                )}
              </Box>
              <Box display="flex" alignItems="center" gap={0.5}>
                <IconButton 
                  size="small" 
                  onClick={(e) => { e.stopPropagation(); onAddChild(no.id); }}
                  title="Adicionar filho"
                >
                  <AddIcon fontSize="small" />
                </IconButton>
                <IconButton 
                  size="small" 
                  onClick={(e) => { e.stopPropagation(); onEdit(no); }}
                  title="Editar"
                >
                  <EditIcon fontSize="small" />
                </IconButton>
                <IconButton 
                  size="small" 
                  onClick={(e) => { e.stopPropagation(); onDelete(no.id); }}
                  title="Excluir"
                >
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Box>
            </Box>
          ) : (
            // MODO EXPANDIDO - Todos os detalhes
            <>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Box display="flex" alignItems="center" gap={1}>
                  <Typography variant="h6">{no.nome}</Typography>
                  {isExpanded && (
                    <Chip label="Fixado" size="small" color="primary" variant="outlined" />
                  )}
                </Box>
                <Box>
                  <IconButton 
                    size="small" 
                    onClick={(e) => { e.stopPropagation(); onAddChild(no.id); }}
                  >
                    <AddIcon />
                  </IconButton>
                  <IconButton 
                    size="small" 
                    onClick={(e) => { e.stopPropagation(); onEdit(no); }}
                  >
                    <EditIcon />
                  </IconButton>
                  <IconButton 
                    size="small" 
                    onClick={(e) => { e.stopPropagation(); onDelete(no.id); }}
                  >
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
                  Funcionários ({funcionariosCount})
                </Typography>
                <Box display="flex" flexWrap="wrap" gap={1}>
                  {no.funcionarios?.map((func) => (
                    <Chip
                      key={func.id}
                      label={func.nome}
                      size="small"
                      onDelete={(e) => { e.stopPropagation(); onRemoveFuncionario(no.id, func.id); }}
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
                  Centros de Custo ({centrosCustoCount})
                </Typography>
                <Box display="flex" flexWrap="wrap" gap={1}>
                  {no.centrosCusto?.map((centro) => (
                    <Chip
                      key={centro.id}
                      label={centro.descricao}
                      size="small"
                      onDelete={(e) => { e.stopPropagation(); onRemoveCentroCusto(no.id, centro.id); }}
                      color="secondary"
                      variant="outlined"
                    />
                  ))}
                </Box>
              </Box>
            </>
          )}
        </CardContent>
      </Card>

      
      {/* Nós filhos - renderizar mesmo quando compacto */}
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
              expandedNodeId={expandedNodeId}
              hoveredNodeId={hoveredNodeId}
              onToggleExpand={onToggleExpand}
              onHover={onHover}
            />
          ))}
        </Box>
      )}
    </Box>
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
    isDragging,
  } = useDraggable({
    id: item.id,
    data: item,
  });

  const style = {
    transform: transform ? `translate3d(${transform.x}px, ${transform.y}px, 0)` : undefined,
    opacity: isDragging ? 0.5 : 1,
    zIndex: isDragging ? 9999 : 'auto',
    cursor: isDragging ? 'grabbing' : 'grab',
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
          '&:active': {
            cursor: 'grabbing',
          },
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
  const [activeItem, setActiveItem] = useState<DragItem | null>(null);
  const [viewMode, setViewMode] = useState<'list' | 'graph'>('list');
  const [filtroFuncionario, setFiltroFuncionario] = useState('');
  const [filtroCentroCusto, setFiltroCentroCusto] = useState('');
  const [expandedNodeId, setExpandedNodeId] = useState<number | null>(null);
  const [hoveredNodeId, setHoveredNodeId] = useState<number | null>(null);

  const { register, handleSubmit, reset, setValue } = useForm<NoOrganogramaFormData>();

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    }),
    useSensor(KeyboardSensor)
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

      console.log('🔍 Dados recebidos do backend:', {
        totalNos: nosData.length,
        primeiroNo: nosData[0],
        temFuncionarioIds: nosData[0]?.funcionarioIds,
      });

      // Enriquecer nós com objetos completos de funcionários e centros de custo
      const nosEnriquecidos = nosData.map((no: NoOrganograma) => ({
        ...no,
        funcionarios: no.funcionarioIds 
          ? no.funcionarioIds
              .map((id: number) => funcionariosData.find((f: Funcionario) => f.id === id))
              .filter(Boolean) as Funcionario[]
          : [],
        centrosCusto: no.centroCustoIds
          ? no.centroCustoIds
              .map((id: number) => centrosCustoData.find((cc: CentroCusto) => cc.id === id))
              .filter(Boolean) as CentroCusto[]
          : [],
      }));

      console.log('✅ Nós enriquecidos:', nosEnriquecidos[0]);

      // Construir árvore hierárquica
      const arvore = construirArvore(nosEnriquecidos);
      
      // Obter IDs de funcionários já associados
      const funcionariosAssociadosIds = new Set<number>();
      nosEnriquecidos.forEach((no: NoOrganograma) => {
        if (no.funcionarioIds) {
          no.funcionarioIds.forEach((id: number) => funcionariosAssociadosIds.add(id));
        }
      });
      
      // Filtrar apenas funcionários não associados
      const funcionariosDisponiveis = funcionariosData.filter(
        (f: Funcionario) => !funcionariosAssociadosIds.has(f.id)
      );
      
      console.log('📊 Estatísticas:', {
        totalFuncionarios: funcionariosData.length,
        funcionariosAssociados: funcionariosAssociadosIds.size,
        funcionariosDisponiveis: funcionariosDisponiveis.length,
        nosComFuncionarios: nosEnriquecidos.filter((n: NoOrganograma) => n.funcionarios && n.funcionarios.length > 0).length,
      });
      
      setNos(arvore);
      setFuncionarios(funcionariosDisponiveis);
      setCentrosCusto(centrosCustoData);
    } catch (error) {
      console.error('❌ Erro ao carregar dados:', error);
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
    console.log('📝 onSubmit chamado:', { data, selectedNo });
    
    try {
      if (selectedNo) {
        // Para atualização, enviar DTO completo com os campos existentes
        const payload: Partial<NoOrganograma> = {
          id: selectedNo.id,
          nome: data.nome,
          descricao: data.descricao || '',
          nivel: selectedNo.nivel, // Manter nível existente
          posicao: selectedNo.posicao, // Manter posição existente
          parentId: data.parentId,
          ativo: selectedNo.ativo,
        };
        
        console.log('✏️ Atualizando nó:', selectedNo.id, payload);
        await organogramaService.atualizarNo(selectedNo.id, payload);
        toast.success('Nó atualizado com sucesso');
      } else {
        // Para criação, enviar apenas os campos do formulário
        const payload: NoOrganogramaFormData = {
          nome: data.nome,
          descricao: data.descricao || '',
          parentId: data.parentId,
        };
        
        console.log('➕ Criando novo nó:', payload);
        await organogramaService.criarNo(payload);
        toast.success('Nó criado com sucesso');
      }
      handleCloseDialog();
      await carregarDados();
    } catch (error: any) {
      console.error('❌ Erro ao salvar nó:', error);
      console.error('❌ Detalhes:', error?.response?.data);
      toast.error(error?.response?.data?.message || error?.message || 'Erro ao salvar nó');
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

  const handleDragStart = (event: DragStartEvent) => {
    const { active } = event;
    const activeId = active.id as string;
    
    // Identificar o item sendo arrastado
    if (activeId.startsWith('funcionario-')) {
      const funcionarioId = parseInt(activeId.replace('funcionario-', ''));
      const funcionario = funcionarios.find(f => f.id === funcionarioId);
      if (funcionario) {
        setActiveItem({
          id: activeId,
          type: 'funcionario',
          data: funcionario,
        });
      }
    } else if (activeId.startsWith('centroCusto-')) {
      const centroCustoId = parseInt(activeId.replace('centroCusto-', ''));
      const centroCusto = centrosCusto.find(c => c.id === centroCustoId);
      if (centroCusto) {
        setActiveItem({
          id: activeId,
          type: 'centroCusto',
          data: centroCusto,
        });
      }
    }
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    
    console.log('🎯 DragEnd:', { 
      activeId: active.id, 
      overId: over?.id,
      overData: over?.data
    });
    
    setActiveItem(null);
    
    if (!over) {
      console.log('❌ Sem alvo de drop');
      return;
    }

    const activeId = active.id as string;
    const overId = over.id as string;

    console.log('🔍 Processando drop:', { activeId, overId });

    // Se é um funcionário ou centro de custo sendo arrastado para um nó
    if (activeId.startsWith('funcionario-') && overId.startsWith('no-')) {
      const funcionarioId = parseInt(activeId.replace('funcionario-', ''));
      const noId = parseInt(overId.replace('no-', ''));
      
      console.log('👤 Adicionando funcionário:', { funcionarioId, noId });
      
      try {
        await organogramaService.adicionarFuncionario(noId, funcionarioId);
        toast.success('Funcionário adicionado ao nó');
        carregarDados();
      } catch (error: any) {
        console.error('❌ Erro ao adicionar funcionário:', error);
        toast.error(error?.response?.data?.message || 'Erro ao adicionar funcionário');
      }
    } else if (activeId.startsWith('centroCusto-') && overId.startsWith('no-')) {
      const centroCustoId = parseInt(activeId.replace('centroCusto-', ''));
      const noId = parseInt(overId.replace('no-', ''));
      
      console.log('🏢 Adicionando centro de custo:', { centroCustoId, noId });
      
      try {
        await organogramaService.adicionarCentroCusto(noId, centroCustoId);
        toast.success('Centro de custo adicionado ao nó');
        carregarDados();
      } catch (error: any) {
        console.error('❌ Erro ao adicionar centro de custo:', error);
        toast.error(error?.response?.data?.message || 'Erro ao adicionar centro de custo');
      }
    } else {
      console.log('⚠️ Combinação não reconhecida:', { activeId, overId });
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

  // Filtrar e ordenar funcionários por nome (A -> Z)
  const funcionariosFiltrados = React.useMemo(() => {
    let lista = funcionarios;
    
    // Filtrar por nome se houver filtro
    if (filtroFuncionario.trim()) {
      lista = lista.filter((f) =>
        f.nome.toLowerCase().includes(filtroFuncionario.toLowerCase())
      );
    }
    
    // Ordenar alfabeticamente por nome
    return lista.sort((a, b) => a.nome.localeCompare(b.nome));
  }, [funcionarios, filtroFuncionario]);

  // Filtrar e ordenar centros de custo por descrição (A -> Z)
  const centrosCustoFiltrados = React.useMemo(() => {
    let lista = centrosCusto;
    
    // Filtrar por descrição se houver filtro
    if (filtroCentroCusto.trim()) {
      lista = lista.filter((c) =>
        c.descricao.toLowerCase().includes(filtroCentroCusto.toLowerCase())
      );
    }
    
    // Ordenar alfabeticamente por descrição
    return lista.sort((a, b) => a.descricao.localeCompare(b.descricao));
  }, [centrosCusto, filtroCentroCusto]);

  // Handler para expandir/recolher nó (accordion: só um aberto)
  const handleToggleExpand = (nodeId: number) => {
    setExpandedNodeId(prevId => prevId === nodeId ? null : nodeId);
  };

  // Handler para hover
  const handleHover = (nodeId: number | null) => {
    setHoveredNodeId(nodeId);
  };

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
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      <Box p={3}>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h4" display="flex" alignItems="center" gap={1}>
            <TreeIcon />
            Organograma
          </Typography>
          <Box display="flex" gap={2} alignItems="center">
            <ToggleButtonGroup
              value={viewMode}
              exclusive
              onChange={(_, newMode) => {
                if (newMode !== null) {
                  setViewMode(newMode);
                }
              }}
              size="small"
            >
              <ToggleButton value="list">
                <ViewListIcon sx={{ mr: 1 }} />
                Lista
              </ToggleButton>
              <ToggleButton value="graph">
                <GraphIcon sx={{ mr: 1 }} />
                Gráfico
              </ToggleButton>
            </ToggleButtonGroup>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => handleOpenDialog()}
            >
              Novo Nó Raiz
            </Button>
          </Box>
        </Box>

        {viewMode === 'graph' ? (
          // Modo Gráfico - Visualização tipo mapa mental
          <Box>
            <Paper sx={{ p: 2 }}>
              <Typography variant="h6" mb={2}>
                Visualização em Gráfico - Mapa Mental
              </Typography>
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
                <OrganogramaGrafico
                  nos={nos}
                  onEdit={handleOpenDialog}
                  onDelete={handleDelete}
                  onAddChild={(parentId) => handleOpenDialog(undefined, parentId)}
                  onRemoveFuncionario={handleRemoveFuncionario}
                  onRemoveCentroCusto={handleRemoveCentroCusto}
                  expandedNodeId={expandedNodeId}
                  hoveredNodeId={hoveredNodeId}
                  onToggleExpand={handleToggleExpand}
                  onHover={handleHover}
                />
              )}
            </Paper>
          </Box>
        ) : (
          // Modo Lista - Visualização hierárquica tradicional
          <Box display="flex" gap={3}>
            {/* Área do organograma */}
            <Box flex="2">
              <Paper sx={{ p: 2, minHeight: 600 }}>
                <Typography variant="h6" mb={2}>
                  Estrutura do Organograma
                </Typography>
                
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
                      expandedNodeId={expandedNodeId}
                      hoveredNodeId={hoveredNodeId}
                      onToggleExpand={handleToggleExpand}
                      onHover={handleHover}
                    />
                  ))
                )}
              </Paper>
            </Box>

            {/* Painéis laterais separados - Funcionários e Centros de Custo */}
            <Box flex="1" minWidth={300} display="flex" flexDirection="column" gap={2}>
              {/* Card de Funcionários */}
              <Paper sx={{ p: 2, height: 290, display: 'flex', flexDirection: 'column' }}>
                <Box display="flex" alignItems="center" gap={1} mb={1}>
                  <PersonIcon color="primary" />
                  <Typography variant="h6" flex={1}>
                    Funcionários
                  </Typography>
                  <Chip 
                    label={`${funcionariosFiltrados.length}/${funcionarios.length}`} 
                    size="small" 
                    color="primary"
                  />
                </Box>
                
                <TextField
                  size="small"
                  placeholder="Filtrar por nome..."
                  value={filtroFuncionario}
                  onChange={(e) => setFiltroFuncionario(e.target.value)}
                  sx={{ mb: 2 }}
                  fullWidth
                />
                
                <Typography variant="caption" color="text.secondary" mb={1}>
                  Arraste para associar ao nó
                </Typography>
                
                <Box sx={{ flex: 1, overflow: 'auto' }}>
                  {funcionariosFiltrados.length === 0 ? (
                    <Box 
                      display="flex" 
                      alignItems="center" 
                      justifyContent="center" 
                      height="100%"
                      color="text.secondary"
                    >
                      <Typography variant="body2">
                        {filtroFuncionario ? 'Nenhum funcionário encontrado' : 'Todos associados'}
                      </Typography>
                    </Box>
                  ) : (
                    funcionariosFiltrados.map((funcionario) => (
                      <DraggableItem
                        key={`funcionario-${funcionario.id}`}
                        item={{
                          id: `funcionario-${funcionario.id}`,
                          type: 'funcionario',
                          data: funcionario,
                        }}
                      />
                    ))
                  )}
                </Box>
              </Paper>

              {/* Card de Centros de Custo */}
              <Paper sx={{ p: 2, height: 290, display: 'flex', flexDirection: 'column' }}>
                <Box display="flex" alignItems="center" gap={1} mb={1}>
                  <BusinessIcon color="secondary" />
                  <Typography variant="h6" flex={1}>
                    Centros de Custo
                  </Typography>
                  <Chip 
                    label={`${centrosCustoFiltrados.length}/${centrosCusto.length}`} 
                    size="small" 
                    color="secondary"
                  />
                </Box>
                
                <TextField
                  size="small"
                  placeholder="Filtrar por descrição..."
                  value={filtroCentroCusto}
                  onChange={(e) => setFiltroCentroCusto(e.target.value)}
                  sx={{ mb: 2 }}
                  fullWidth
                />
                
                <Typography variant="caption" color="text.secondary" mb={1}>
                  Arraste para associar ao nó
                </Typography>
                
                <Box sx={{ flex: 1, overflow: 'auto' }}>
                  {centrosCustoFiltrados.length === 0 ? (
                    <Box 
                      display="flex" 
                      alignItems="center" 
                      justifyContent="center" 
                      height="100%"
                      color="text.secondary"
                    >
                      <Typography variant="body2">
                        {filtroCentroCusto ? 'Nenhum centro de custo encontrado' : 'Todos associados'}
                      </Typography>
                    </Box>
                  ) : (
                    centrosCustoFiltrados.map((centroCusto) => (
                      <DraggableItem
                        key={`centroCusto-${centroCusto.id}`}
                        item={{
                          id: `centroCusto-${centroCusto.id}`,
                          type: 'centroCusto',
                          data: centroCusto,
                        }}
                      />
                    ))
                  )}
                </Box>
              </Paper>
            </Box>
          </Box>
        )}

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

      {/* DragOverlay para preview visual durante drag */}
      <DragOverlay
        dropAnimation={{
          duration: 200,
          easing: 'cubic-bezier(0.18, 0.67, 0.6, 1.22)',
        }}
        style={{ zIndex: 10000 }}
      >
        {activeItem ? (
          <Paper
            sx={{
              p: 1,
              cursor: 'grabbing',
              border: '2px solid',
              borderColor: 'primary.main',
              bgcolor: 'background.paper',
              boxShadow: 3,
            }}
          >
            <Typography variant="body2" display="flex" alignItems="center" gap={1}>
              {activeItem.type === 'funcionario' ? <PersonIcon fontSize="small" /> : <BusinessIcon fontSize="small" />}
              {activeItem.type === 'funcionario' 
                ? (activeItem.data as Funcionario).nome 
                : (activeItem.data as CentroCusto).descricao
              }
            </Typography>
          </Paper>
        ) : null}
      </DragOverlay>
    </DndContext>
  );
} 