import React, { useMemo } from 'react';
import ReactFlow, {
  type Node,
  type Edge,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  MarkerType,
  Panel,
  Handle,
  Position,
} from 'reactflow';
import 'reactflow/dist/style.css';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Chip,
  IconButton,
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  Add as AddIcon,
  Person as PersonIcon,
  Business as BusinessIcon,
} from '@mui/icons-material';
import type { NoOrganograma, Funcionario, CentroCusto } from '../../types';

interface NoOrganogramaWithChildren extends NoOrganograma {
  children: NoOrganogramaWithChildren[];
}

interface OrganogramaGraficoProps {
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

// Componente customizado para cada nó do organograma
const NoOrganogramaNode = ({ data }: { data: any }) => {
  const no = data.no as NoOrganogramaWithChildren;
  const isExpanded = data.expandedNodeId === no.id;
  const isHovered = data.hoveredNodeId === no.id;
  const showDetails = isExpanded || isHovered;
  const funcionariosCount = no.funcionarios?.length || 0;
  const centrosCustoCount = no.centrosCusto?.length || 0;

  return (
    <>
      {/* Handle de entrada (conexão vindo do pai) */}
      <Handle
        type="target"
        position={Position.Left}
        style={{
          background: '#1976d2',
          width: 12,
          height: 12,
          border: '2px solid white',
        }}
      />
      
      <Card
        onMouseEnter={() => data.onHover(no.id)}
        onMouseLeave={() => data.onHover(null)}
        onClick={() => data.onToggleExpand(no.id)}
        sx={{
          minWidth: showDetails ? 280 : 200,
          maxWidth: showDetails ? 320 : 250,
          border: '2px solid',
          borderColor: isExpanded ? 'primary.main' : 'primary.main',
          boxShadow: 3,
          bgcolor: 'background.paper',
          cursor: 'pointer',
          transition: 'all 0.3s ease-in-out',
          '&:hover': {
            boxShadow: 6,
          },
        }}
      >
      <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
        {!showDetails ? (
          // MODO COMPACTO - Só título e badges
          <Box>
            <Box display="flex" alignItems="center" justifyContent="space-between" gap={0.5} mb={0.5}>
              <Typography variant="body2" fontWeight="bold" flex={1} noWrap>
                {no.nome}
              </Typography>
              <Box display="flex" gap={0.25}>
                <IconButton 
                  size="small" 
                  onClick={(e) => { e.stopPropagation(); data.onAddChild(no.id); }}
                  title="Adicionar filho"
                  sx={{ p: 0.25 }}
                >
                  <AddIcon sx={{ fontSize: 16 }} />
                </IconButton>
                <IconButton 
                  size="small" 
                  onClick={(e) => { e.stopPropagation(); data.onEdit(no); }}
                  title="Editar"
                  sx={{ p: 0.25 }}
                >
                  <EditIcon sx={{ fontSize: 16 }} />
                </IconButton>
                <IconButton 
                  size="small" 
                  onClick={(e) => { e.stopPropagation(); data.onDelete(no.id); }}
                  title="Excluir"
                  sx={{ p: 0.25 }}
                >
                  <DeleteIcon sx={{ fontSize: 16 }} />
                </IconButton>
              </Box>
            </Box>
            <Box display="flex" gap={0.5}>
              {funcionariosCount > 0 && (
                <Chip
                  icon={<PersonIcon />}
                  label={funcionariosCount}
                  size="small"
                  color="primary"
                  sx={{ height: 22, fontSize: '0.7rem' }}
                />
              )}
              {centrosCustoCount > 0 && (
                <Chip
                  icon={<BusinessIcon />}
                  label={centrosCustoCount}
                  size="small"
                  color="secondary"
                  sx={{ height: 22, fontSize: '0.7rem' }}
                />
              )}
            </Box>
          </Box>
        ) : (
          // MODO EXPANDIDO - Todos os detalhes
          <>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
              <Typography variant="subtitle1" fontWeight="bold" noWrap flex={1}>
                {no.nome}
              </Typography>
              <Box>
                <IconButton size="small" onClick={(e) => { e.stopPropagation(); data.onAddChild(no.id); }}>
                  <AddIcon fontSize="small" />
                </IconButton>
                <IconButton size="small" onClick={(e) => { e.stopPropagation(); data.onEdit(no); }}>
                  <EditIcon fontSize="small" />
                </IconButton>
                <IconButton size="small" onClick={(e) => { e.stopPropagation(); data.onDelete(no.id); }}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Box>
            </Box>

            {no.descricao && (
              <Typography
                variant="body2"
                color="textSecondary"
                mb={1.5}
                sx={{
                  fontSize: '0.75rem',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  display: '-webkit-box',
                  WebkitLineClamp: 2,
                  WebkitBoxOrient: 'vertical',
                }}
              >
                {no.descricao}
              </Typography>
            )}

            {/* Funcionários */}
            {funcionariosCount > 0 && (
              <Box mb={1}>
                <Typography
                  variant="caption"
                  display="flex"
                  alignItems="center"
                  gap={0.5}
                  mb={0.5}
                  color="text.secondary"
                >
                  <PersonIcon sx={{ fontSize: 14 }} />
                  Funcionários ({funcionariosCount})
                </Typography>
                <Box display="flex" flexWrap="wrap" gap={0.5}>
                  {no.funcionarios?.slice(0, 3).map((func: Funcionario) => (
                    <Chip
                      key={func.id}
                      label={func.nome}
                      size="small"
                      onDelete={(e) => { e.stopPropagation(); data.onRemoveFuncionario(no.id, func.id); }}
                      color="primary"
                      variant="outlined"
                      sx={{ fontSize: '0.7rem', height: 20 }}
                    />
                  ))}
                  {funcionariosCount > 3 && (
                    <Chip
                      label={`+${funcionariosCount - 3}`}
                      size="small"
                      sx={{ fontSize: '0.7rem', height: 20 }}
                    />
                  )}
                </Box>
              </Box>
            )}

            {/* Centros de Custo */}
            {centrosCustoCount > 0 && (
              <Box>
                <Typography
                  variant="caption"
                  display="flex"
                  alignItems="center"
                  gap={0.5}
                  mb={0.5}
                  color="text.secondary"
                >
                  <BusinessIcon sx={{ fontSize: 14 }} />
                  Centros de Custo ({centrosCustoCount})
                </Typography>
                <Box display="flex" flexWrap="wrap" gap={0.5}>
                  {no.centrosCusto?.slice(0, 2).map((centro: CentroCusto) => (
                    <Chip
                      key={centro.id}
                      label={centro.descricao}
                      size="small"
                      onDelete={(e) => { e.stopPropagation(); data.onRemoveCentroCusto(no.id, centro.id); }}
                      color="secondary"
                      variant="outlined"
                      sx={{ fontSize: '0.7rem', height: 20 }}
                    />
                  ))}
                  {centrosCustoCount > 2 && (
                    <Chip
                      label={`+${centrosCustoCount - 2}`}
                      size="small"
                      sx={{ fontSize: '0.7rem', height: 20 }}
                    />
                  )}
                </Box>
              </Box>
            )}
          </>
        )}
      </CardContent>
    </Card>
    
    {/* Handle de saída (conexão para os filhos) */}
    <Handle
      type="source"
      position={Position.Right}
      style={{
        background: '#1976d2',
        width: 12,
        height: 12,
        border: '2px solid white',
      }}
    />
  </>
  );
};

// Tipos de nós customizados (definido fora do componente para evitar recriação)
const nodeTypes = {
  noOrganograma: NoOrganogramaNode,
};

export default function OrganogramaGrafico({
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
}: OrganogramaGraficoProps) {
  // Converter a estrutura hierárquica em nós e arestas para o ReactFlow
  const { nodes: initialNodes, edges: initialEdges } = useMemo(() => {
    const nodes: Node[] = [];
    const edges: Edge[] = [];
    const levelWidth = 350; // Espaçamento horizontal entre níveis
    const nodeHeight = 200; // Espaçamento vertical entre nós irmãos

    // Função recursiva para processar a árvore
    const processNode = (
      no: NoOrganogramaWithChildren,
      x: number,
      y: number,
      level: number
    ): { minY: number; maxY: number } => {
      // Adicionar nó
      nodes.push({
        id: `no-${no.id}`,
        type: 'noOrganograma',
        position: { x, y },
        data: {
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
        },
      });

      // Se tem filhos, processar recursivamente
      if (no.children && no.children.length > 0) {
        let currentY = y;
        let childrenMinY = Infinity;
        let childrenMaxY = -Infinity;

        no.children.forEach((child) => {
          const childResult = processNode(
            child,
            x + levelWidth,
            currentY,
            level + 1
          );

          // Adicionar aresta do pai para o filho
          edges.push({
            id: `edge-${no.id}-${child.id}`,
            source: `no-${no.id}`,
            target: `no-${child.id}`,
            type: 'smoothstep',
            animated: false,
            style: { 
              stroke: '#1976d2', 
              strokeWidth: 3
            },
            markerEnd: {
              type: MarkerType.ArrowClosed,
              color: '#1976d2',
              width: 20,
              height: 20,
            },
          });

          childrenMinY = Math.min(childrenMinY, childResult.minY);
          childrenMaxY = Math.max(childrenMaxY, childResult.maxY);
          currentY = childResult.maxY + nodeHeight;
        });

        // Centralizar o nó pai em relação aos filhos
        const middleY = (childrenMinY + childrenMaxY) / 2;
        const currentNode = nodes.find((n) => n.id === `no-${no.id}`);
        if (currentNode) {
          currentNode.position.y = middleY;
        }

        return { minY: Math.min(y, childrenMinY), maxY: Math.max(y, childrenMaxY) };
      }

      return { minY: y, maxY: y };
    };

    // Processar todas as raízes
    let currentY = 0;
    nos.forEach((noRaiz) => {
      const result = processNode(noRaiz, 0, currentY, 0);
      currentY = result.maxY + nodeHeight;
    });

    console.log('🔗 ReactFlow - Nodes:', nodes.length, 'Edges:', edges.length);
    if (edges.length > 0) {
      console.log('🔗 Primeira edge:', edges[0]);
    }

    return { nodes, edges };
  }, [nos, onEdit, onDelete, onAddChild, onRemoveFuncionario, onRemoveCentroCusto]);

  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  // Atualizar nós quando os dados mudarem
  React.useEffect(() => {
    console.log('📊 Atualizando ReactFlow - Nodes:', initialNodes.length, 'Edges:', initialEdges.length);
    setNodes(initialNodes);
    setEdges(initialEdges);
  }, [initialNodes, initialEdges, setNodes, setEdges]);

  return (
    <Box sx={{ width: '100%', height: 600, border: '1px solid', borderColor: 'grey.300' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ padding: 0.2 }}
        attributionPosition="bottom-left"
        minZoom={0.1}
        maxZoom={2}
        defaultEdgeOptions={{
          type: 'smoothstep',
          animated: false,
          style: { strokeWidth: 3, stroke: '#1976d2' },
        }}
      >
        <Background />
        <Controls />
        <MiniMap
          nodeColor={() => '#1976d2'}
          nodeStrokeWidth={3}
          zoomable
          pannable
        />
        <Panel position="top-right">
          <Box
            sx={{
              bgcolor: 'background.paper',
              p: 1.5,
              borderRadius: 1,
              boxShadow: 2,
            }}
          >
            <Typography variant="caption" color="text.secondary">
              💡 Use o scroll do mouse para zoom, arraste para navegar
            </Typography>
          </Box>
        </Panel>
      </ReactFlow>
    </Box>
  );
}

