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
  Backdrop,
  CircularProgress,
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

const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) {
      return response.data.message;
    }
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
};

const resolverFuncionariosPorIds = (
  ids: number[] | undefined,
  funcionarios: Funcionario[],
): Funcionario[] => {
  if (!ids) {
    return [];
  }
  return ids
    .map((id) => funcionarios.find((f) => f.id === id))
    .filter((f): f is Funcionario => f != null);
};

const resolverCentrosCustoPorIds = (
  ids: number[] | undefined,
  centrosCusto: CentroCusto[],
): CentroCusto[] => {
  if (!ids) {
    return [];
  }
  return ids
    .map((id) => centrosCusto.find((cc) => cc.id === id))
    .filter((cc): cc is CentroCusto => cc != null);
};

const enriquecerNosOrganograma = (
  nosData: NoOrganograma[],
  funcionariosData: Funcionario[],
  centrosCustoData: CentroCusto[],
): NoOrganograma[] =>
  nosData.map((no) => ({
    ...no,
    funcionarios: resolverFuncionariosPorIds(no.funcionarioIds, funcionariosData),
    centrosCusto: resolverCentrosCustoPorIds(no.centroCustoIds, centrosCustoData),
  }));

const construirArvoreOrganograma = (
  nos: NoOrganograma[],
): NoOrganogramaWithChildren[] => {
  const nosMap = new Map<number, NoOrganogramaWithChildren>();
  const raizes: NoOrganogramaWithChildren[] = [];

  nos.forEach((no) => {
    nosMap.set(no.id, {
      ...no,
      children: [],
      funcionarios: no.funcionarios || [],
      centrosCusto: no.centrosCusto || [],
    });
  });

  nos.forEach((no) => {
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

  const ordenarPorPosicao = (nosOrdenaveis: NoOrganogramaWithChildren[]) => {
    nosOrdenaveis.sort((a, b) => a.posicao - b.posicao);
    nosOrdenaveis.forEach((no) => ordenarPorPosicao(no.children));
  };

  ordenarPorPosicao(raizes);
  return raizes;
};

const coletarFuncionariosAssociadosIds = (nos: NoOrganograma[]): Set<number> => {
  const funcionariosAssociadosIds = new Set<number>();
  nos.forEach((no) => {
    no.funcionarioIds?.forEach((id) => funcionariosAssociadosIds.add(id));
  });
  return funcionariosAssociadosIds;
};

const filtrarFuncionariosDisponiveis = (
  funcionarios: Funcionario[],
  associadosIds: Set<number>,
): Funcionario[] => funcionarios.filter((f) => !associadosIds.has(f.id));

const resolveNoCardBorderColor = (isOver: boolean, isExpanded: boolean): string => {
  if (isOver || isExpanded) {
    return 'primary.main';
  }
  return 'grey.300';
};

const filtrarEOrdenarFuncionarios = (
  funcionarios: Funcionario[],
  filtro: string,
): Funcionario[] => {
  let lista = funcionarios;
  if (filtro.trim()) {
    lista = lista.filter((f) =>
      f.nome.toLowerCase().includes(filtro.toLowerCase()),
    );
  }
  return lista.sort((a, b) => a.nome.localeCompare(b.nome));
};

const filtrarEOrdenarCentrosCusto = (
  centrosCusto: CentroCusto[],
  filtro: string,
): CentroCusto[] => {
  let lista = centrosCusto;
  if (filtro.trim()) {
    lista = lista.filter((c) =>
      c.descricao.toLowerCase().includes(filtro.toLowerCase()),
    );
  }
  return lista.sort((a, b) => a.descricao.localeCompare(b.descricao));
};

const parsePrefixedDragId = (activeId: string, prefix: string): number =>
  Number.parseInt(activeId.replace(`${prefix}-`, ''), 10);

const parseNoDragId = (overId: string): number =>
  Number.parseInt(overId.replace('no-', ''), 10);

const resolveDragItemFromActiveId = (
  activeId: string,
  funcionarios: Funcionario[],
  centrosCusto: CentroCusto[],
): DragItem | null => {
  if (activeId.startsWith('funcionario-')) {
    const funcionarioId = Number.parseInt(activeId.replace('funcionario-', ''), 10);
    const funcionario = funcionarios.find((f) => f.id === funcionarioId);
    if (funcionario) {
      return { id: activeId, type: 'funcionario', data: funcionario };
    }
    return null;
  }
  if (activeId.startsWith('centroCusto-')) {
    const centroCustoId = Number.parseInt(activeId.replace('centroCusto-', ''), 10);
    const centroCusto = centrosCusto.find((c) => c.id === centroCustoId);
    if (centroCusto) {
      return { id: activeId, type: 'centroCusto', data: centroCusto };
    }
  }
  return null;
};

interface OrganogramaDataSetters {
  setNos: React.Dispatch<React.SetStateAction<NoOrganogramaWithChildren[]>>;
  setFuncionarios: React.Dispatch<React.SetStateAction<Funcionario[]>>;
  setCentrosCusto: React.Dispatch<React.SetStateAction<CentroCusto[]>>;
  setLoading: React.Dispatch<React.SetStateAction<boolean>>;
  setUpdating: React.Dispatch<React.SetStateAction<boolean>>;
}

const carregarDadosOrganograma = async (
  silencioso: boolean,
  setters: OrganogramaDataSetters,
): Promise<void> => {
  const { setNos, setFuncionarios, setCentrosCusto, setLoading, setUpdating } = setters;
  try {
    if (!silencioso) {
      setLoading(true);
    } else {
      setUpdating(true);
    }

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

    const nosEnriquecidos = enriquecerNosOrganograma(
      nosData,
      funcionariosData,
      centrosCustoData,
    );

    console.log('✅ Nós enriquecidos:', nosEnriquecidos[0]);

    const arvore = construirArvoreOrganograma(nosEnriquecidos);
    const funcionariosAssociadosIds = coletarFuncionariosAssociadosIds(nosEnriquecidos);
    const funcionariosDisponiveis = filtrarFuncionariosDisponiveis(
      funcionariosData,
      funcionariosAssociadosIds,
    );

    console.log('📊 Estatísticas:', {
      totalFuncionarios: funcionariosData.length,
      funcionariosAssociados: funcionariosAssociadosIds.size,
      funcionariosDisponiveis: funcionariosDisponiveis.length,
      nosComFuncionarios: nosEnriquecidos.filter(
        (n: NoOrganograma) => n.funcionarios && n.funcionarios.length > 0,
      ).length,
    });

    setNos(arvore);
    setFuncionarios(funcionariosDisponiveis);
    setCentrosCusto(centrosCustoData);
  } catch (error) {
    console.error('❌ Erro ao carregar dados:', error);
    toast.error('Erro ao carregar dados do organograma');
  } finally {
    setLoading(false);
    setUpdating(false);
  }
};

interface DropAssociacaoRequest {
  activeId: string;
  overId: string;
  prefix: 'funcionario' | 'centroCusto';
  associar: (noId: number, itemId: number) => Promise<unknown>;
  mensagemSucesso: string;
  mensagemErro: string;
  setUpdating: React.Dispatch<React.SetStateAction<boolean>>;
  carregarDados: (silencioso?: boolean) => Promise<void>;
}

const executarDropAssociacao = async (request: DropAssociacaoRequest): Promise<boolean> => {
  const {
    activeId,
    overId,
    prefix,
    associar,
    mensagemSucesso,
    mensagemErro,
    setUpdating,
    carregarDados,
  } = request;
  if (!activeId.startsWith(`${prefix}-`) || !overId.startsWith('no-')) {
    return false;
  }

  const itemId = parsePrefixedDragId(activeId, prefix);
  const noId = parseNoDragId(overId);

  console.log(`🔍 Adicionando ${prefix}:`, { itemId, noId });

  try {
    setUpdating(true);
    await associar(noId, itemId);
    toast.success(mensagemSucesso);
    await carregarDados(true);
  } catch (error: unknown) {
    console.error(`❌ Erro ao adicionar ${prefix}:`, error);
    toast.error(getApiErrorMessage(error, mensagemErro));
    setUpdating(false);
  }

  return true;
};

const buildNoUpdatePayload = (
  selectedNo: NoOrganograma,
  data: NoOrganogramaFormData,
): Partial<NoOrganograma> => ({
  id: selectedNo.id,
  nome: data.nome,
  descricao: data.descricao || '',
  nivel: selectedNo.nivel,
  posicao: selectedNo.posicao,
  parentId: data.parentId,
  ativo: selectedNo.ativo,
});

const buildNoCreatePayload = (data: NoOrganogramaFormData): NoOrganogramaFormData => ({
  nome: data.nome,
  descricao: data.descricao || '',
  parentId: data.parentId,
});

const salvarNoOrganograma = async (
  selectedNo: NoOrganograma | null,
  data: NoOrganogramaFormData,
  setUpdating: React.Dispatch<React.SetStateAction<boolean>>,
  onSuccess: () => Promise<void>,
): Promise<void> => {
  try {
    setUpdating(true);

    if (selectedNo) {
      const payload = buildNoUpdatePayload(selectedNo, data);
      console.log('✏️ Atualizando nó:', selectedNo.id, payload);
      await organogramaService.atualizarNo(selectedNo.id, payload);
      toast.success('Nó atualizado com sucesso');
    } else {
      const payload = buildNoCreatePayload(data);
      console.log('➕ Criando novo nó:', payload);
      await organogramaService.criarNo(payload);
      toast.success('Nó criado com sucesso');
    }

    await onSuccess();
  } catch (error: unknown) {
    console.error('❌ Erro ao salvar nó:', error);
    if (typeof error === 'object' && error !== null && 'response' in error) {
      console.error('❌ Detalhes:', (error as { response?: { data?: unknown } }).response?.data);
    }
    toast.error(getApiErrorMessage(error, 'Erro ao salvar nó'));
    setUpdating(false);
  }
};

const executarOperacaoComAtualizacao = async (
  operacao: () => Promise<void>,
  mensagemSucesso: string,
  mensagemErro: string,
  setUpdating: React.Dispatch<React.SetStateAction<boolean>>,
  carregarDados: (silencioso?: boolean) => Promise<void>,
): Promise<void> => {
  try {
    setUpdating(true);
    await operacao();
    toast.success(mensagemSucesso);
    await carregarDados(true);
  } catch {
    toast.error(mensagemErro);
    setUpdating(false);
  }
};

const excluirNoOrganograma = async (
  id: number,
  setUpdating: React.Dispatch<React.SetStateAction<boolean>>,
  carregarDados: (silencioso?: boolean) => Promise<void>,
): Promise<void> => {
  if (!window.confirm('Tem certeza que deseja excluir este nó? Todos os subnós também serão excluídos.')) {
    return;
  }

  try {
    setUpdating(true);
    await organogramaService.removerNo(id);
    toast.success('Nó excluído com sucesso');
    await carregarDados(true);
  } catch {
    toast.error('Erro ao excluir nó');
    setUpdating(false);
  }
};

interface OrganogramaDragEndDeps {
  setActiveItem: React.Dispatch<React.SetStateAction<DragItem | null>>;
  setUpdating: React.Dispatch<React.SetStateAction<boolean>>;
  carregarDados: (silencioso?: boolean) => Promise<void>;
}

const handleOrganogramaDragEnd = async (
  event: DragEndEvent,
  deps: OrganogramaDragEndDeps,
): Promise<void> => {
  const { active, over } = event;
  const { setActiveItem, setUpdating, carregarDados } = deps;

  console.log('🎯 DragEnd:', {
    activeId: active.id,
    overId: over?.id,
    overData: over?.data,
  });

  setActiveItem(null);

  if (!over) {
    console.log('❌ Sem alvo de drop');
    return;
  }

  const activeId = active.id as string;
  const overId = over.id as string;

  console.log('🔍 Processando drop:', { activeId, overId });

  const dropBase = { activeId, overId, setUpdating, carregarDados };
  const processado =
    (await executarDropAssociacao({
      ...dropBase,
      prefix: 'funcionario',
      associar: organogramaService.adicionarFuncionario,
      mensagemSucesso: 'Funcionário adicionado ao nó',
      mensagemErro: 'Erro ao adicionar funcionário',
    })) ||
    (await executarDropAssociacao({
      ...dropBase,
      prefix: 'centroCusto',
      associar: organogramaService.adicionarCentroCusto,
      mensagemSucesso: 'Centro de custo adicionado ao nó',
      mensagemErro: 'Erro ao adicionar centro de custo',
    }));

  if (!processado) {
    console.log('⚠️ Combinação não reconhecida:', { activeId, overId });
  }
};

interface OrganogramaDialogFormDeps {
  setSelectedNo: React.Dispatch<React.SetStateAction<NoOrganograma | null>>;
  setValue: ReturnType<typeof useForm<NoOrganogramaFormData>>['setValue'];
  reset: ReturnType<typeof useForm<NoOrganogramaFormData>>['reset'];
  setParentIdForNew: React.Dispatch<React.SetStateAction<number | undefined>>;
  setOpenDialog: React.Dispatch<React.SetStateAction<boolean>>;
}

const openOrganogramaDialog = (
  no: NoOrganograma | undefined,
  parentId: number | undefined,
  deps: OrganogramaDialogFormDeps,
): void => {
  const { setSelectedNo, setValue, reset, setParentIdForNew, setOpenDialog } = deps;
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

const TREE_BRANCH_Y = 28;
const TREE_LINE_WIDTH = '2px';

interface OrganogramaTreeContext {
  /** true quando o nó tem pai (não é raiz dentro de um grupo de filhos) */
  hasParent: boolean;
  /** true quando é o último filho entre irmãos */
  isLastSibling: boolean;
}

interface OrganogramaTreeBranchProps {
  treeContext?: OrganogramaTreeContext;
  children: React.ReactNode;
}

/** Decorative tree connectors — no interactive role; pointer events disabled on lines. */
const OrganogramaTreeBranch: React.FC<OrganogramaTreeBranchProps> = ({
  treeContext,
  children,
}) => {
  const hasParent = treeContext?.hasParent ?? false;
  const isLastSibling = treeContext?.isLastSibling ?? false;

  return (
    <Box
      sx={(theme) => ({
        position: 'relative',
        ...(hasParent && {
          pl: 2,
          '&::before': {
            content: '""',
            position: 'absolute',
            left: 0,
            top: 0,
            height: isLastSibling ? `${TREE_BRANCH_Y}px` : '100%',
            borderLeft: `${TREE_LINE_WIDTH} solid`,
            borderColor: theme.palette.divider,
            pointerEvents: 'none',
          },
          '&::after': {
            content: '""',
            position: 'absolute',
            left: 0,
            top: TREE_BRANCH_Y,
            width: 16,
            borderTop: `${TREE_LINE_WIDTH} solid`,
            borderColor: theme.palette.divider,
            pointerEvents: 'none',
          },
        }),
      })}
    >
      {children}
    </Box>
  );
};

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
  treeContext?: OrganogramaTreeContext;
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
  treeContext,
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
    <OrganogramaTreeBranch treeContext={treeContext}>
      <Box mb={2}>
        <Card
          ref={setNodeRef}
          onMouseEnter={() => onHover(no.id)}
          onMouseLeave={() => onHover(null)}
          onClick={() => onToggleExpand(no.id)}
          sx={{
            border: '2px solid',
            borderColor: resolveNoCardBorderColor(isOver, isExpanded),
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

        {no.children && no.children.length > 0 && (
          <Box ml={4} mt={2} sx={{ position: 'relative', pl: 0 }}>
            {no.children.map((child, index) => (
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
                treeContext={{
                  hasParent: true,
                  isLastSibling: index === no.children.length - 1,
                }}
              />
            ))}
          </Box>
        )}
      </Box>
    </OrganogramaTreeBranch>
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

const toggleExpandedNodeId = (
  nodeId: number,
  setExpandedNodeId: React.Dispatch<React.SetStateAction<number | null>>,
): void => {
  setExpandedNodeId((prevId) => (prevId === nodeId ? null : nodeId));
};

const OrganogramaEmptyNodesPlaceholder: React.FC = () => (
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
    <Typography>Clique em &quot;Novo Nó Raiz&quot; para começar</Typography>
  </Box>
);

interface OrganogramaGraphSectionProps {
  nos: NoOrganogramaWithChildren[];
  onEdit: (no: NoOrganograma) => void;
  onDelete: (id: number) => void;
  onAddChild: (parentId: number) => void;
  onRemoveFuncionario: (noId: number, funcionarioId: number) => void;
  onRemoveCentroCusto: (noId: number, centroCustoId: number) => void;
  expandedNodeId: number | null;
  hoveredNodeId: number | null;
  onToggleExpand: (id: number) => void;
  onHover: (id: number | null) => void;
}

const OrganogramaGraphSection: React.FC<OrganogramaGraphSectionProps> = ({
  nos,
  onEdit,
  onDelete,
  onAddChild,
  onRemoveFuncionario,
  onRemoveCentroCusto,
  expandedNodeId,
  hoveredNodeId,
  onToggleExpand,
  onHover,
}) => (
  <Box>
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" mb={2}>
        Visualização em Gráfico - Mapa Mental
      </Typography>
      {nos.length === 0 ? (
        <OrganogramaEmptyNodesPlaceholder />
      ) : (
        <OrganogramaGrafico
          nos={nos}
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
      )}
    </Paper>
  </Box>
);

interface OrganogramaListSectionProps extends OrganogramaGraphSectionProps {
  funcionariosFiltrados: Funcionario[];
  funcionarios: Funcionario[];
  centrosCustoFiltrados: CentroCusto[];
  centrosCusto: CentroCusto[];
  filtroFuncionario: string;
  filtroCentroCusto: string;
  onFiltroFuncionarioChange: (value: string) => void;
  onFiltroCentroCustoChange: (value: string) => void;
}

const OrganogramaListSection: React.FC<OrganogramaListSectionProps> = ({
  nos,
  onEdit,
  onDelete,
  onAddChild,
  onRemoveFuncionario,
  onRemoveCentroCusto,
  expandedNodeId,
  hoveredNodeId,
  onToggleExpand,
  onHover,
  funcionariosFiltrados,
  funcionarios,
  centrosCustoFiltrados,
  centrosCusto,
  filtroFuncionario,
  filtroCentroCusto,
  onFiltroFuncionarioChange,
  onFiltroCentroCustoChange,
}) => (
  <Box display="flex" gap={3}>
    <Box flex="2">
      <Paper sx={{ p: 2, minHeight: 600 }}>
        <Typography variant="h6" mb={2}>
          Estrutura do Organograma
        </Typography>
        {nos.length === 0 ? (
          <OrganogramaEmptyNodesPlaceholder />
        ) : (
          nos.map((no) => (
            <NoOrganogramaCard
              key={no.id}
              no={no}
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
          ))
        )}
      </Paper>
    </Box>
    <Box flex="1" minWidth={300} display="flex" flexDirection="column" gap={2}>
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
          onChange={(e) => onFiltroFuncionarioChange(e.target.value)}
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
          onChange={(e) => onFiltroCentroCustoChange(e.target.value)}
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
);

export default function Organograma() {
  const [nos, setNos] = useState<NoOrganogramaWithChildren[]>([]);
  const [funcionarios, setFuncionarios] = useState<Funcionario[]>([]);
  const [centrosCusto, setCentrosCusto] = useState<CentroCusto[]>([]);
  const [openDialog, setOpenDialog] = useState(false);
  const [selectedNo, setSelectedNo] = useState<NoOrganograma | null>(null);
  const [parentIdForNew, setParentIdForNew] = useState<number | undefined>();
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false); // Novo estado para operações
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
    void carregarDados();
  }, []);

  const dataSetters: OrganogramaDataSetters = {
    setNos,
    setFuncionarios,
    setCentrosCusto,
    setLoading,
    setUpdating,
  };

  const carregarDados = async (silencioso = false) => {
    await carregarDadosOrganograma(silencioso, dataSetters);
  };

  const dialogFormDeps: OrganogramaDialogFormDeps = {
    setSelectedNo,
    setValue,
    reset,
    setParentIdForNew,
    setOpenDialog,
  };

  const handleOpenDialog = (no?: NoOrganograma, parentId?: number) => {
    openOrganogramaDialog(no, parentId, dialogFormDeps);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setSelectedNo(null);
    setParentIdForNew(undefined);
    reset();
  };

  const onSubmit = async (data: NoOrganogramaFormData) => {
    console.log('📝 onSubmit chamado:', { data, selectedNo });
    await salvarNoOrganograma(selectedNo, data, setUpdating, async () => {
      handleCloseDialog();
      await carregarDados(true);
    });
  };

  const handleDelete = async (id: number) => {
    await excluirNoOrganograma(id, setUpdating, carregarDados);
  };

  const handleDragStart = (event: DragStartEvent) => {
    const activeId = event.active.id as string;
    const item = resolveDragItemFromActiveId(activeId, funcionarios, centrosCusto);
    if (item) {
      setActiveItem(item);
    }
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    await handleOrganogramaDragEnd(event, {
      setActiveItem,
      setUpdating,
      carregarDados,
    });
  };

  const handleRemoveFuncionario = async (noId: number, funcionarioId: number) => {
    await executarOperacaoComAtualizacao(
      () => organogramaService.removerFuncionario(noId, funcionarioId),
      'Funcionário removido do nó',
      'Erro ao remover funcionário',
      setUpdating,
      carregarDados,
    );
  };

  const handleRemoveCentroCusto = async (noId: number, centroCustoId: number) => {
    await executarOperacaoComAtualizacao(
      () => organogramaService.removerCentroCusto(noId, centroCustoId),
      'Centro de custo removido do nó',
      'Erro ao remover centro de custo',
      setUpdating,
      carregarDados,
    );
  };

  const funcionariosFiltrados = React.useMemo(
    () => filtrarEOrdenarFuncionarios(funcionarios, filtroFuncionario),
    [funcionarios, filtroFuncionario],
  );

  const centrosCustoFiltrados = React.useMemo(
    () => filtrarEOrdenarCentrosCusto(centrosCusto, filtroCentroCusto),
    [centrosCusto, filtroCentroCusto],
  );

  // Handler para expandir/recolher nó (accordion: só um aberto)
  const handleToggleExpand = (nodeId: number) => {
    toggleExpandedNodeId(nodeId, setExpandedNodeId);
  };

  // Handler para hover
  const handleHover = (nodeId: number | null) => {
    setHoveredNodeId(nodeId);
  };

  if (loading) {
    return (
      <Box display="flex" flexDirection="column" justifyContent="center" alignItems="center" height="calc(100vh - 200px)" gap={2}>
        <CircularProgress size={60} />
        <Typography variant="h6" color="text.secondary">
          Carregando organograma...
        </Typography>
      </Box>
    );
  }

  return (
    <>
      {/* Backdrop com spinner durante operações */}
      <Backdrop
        sx={{
          color: 'common.white',
          zIndex: (theme) => theme.zIndex.drawer + 1,
          backdropFilter: 'blur(2px)',
        }}
        open={updating}
      >
        <Box display="flex" flexDirection="column" alignItems="center" gap={2}>
          <CircularProgress color="inherit" size={60} />
          <Typography variant="h6">Processando...</Typography>
        </Box>
      </Backdrop>

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
          <OrganogramaGraphSection
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
        ) : (
          <OrganogramaListSection
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
            funcionariosFiltrados={funcionariosFiltrados}
            funcionarios={funcionarios}
            centrosCustoFiltrados={centrosCustoFiltrados}
            centrosCusto={centrosCusto}
            filtroFuncionario={filtroFuncionario}
            filtroCentroCusto={filtroCentroCusto}
            onFiltroFuncionarioChange={setFiltroFuncionario}
            onFiltroCentroCustoChange={setFiltroCentroCusto}
          />
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
    </>
  );
} 