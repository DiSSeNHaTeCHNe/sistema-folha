import {
  Assessment,
  AttachMoney,
  CardGiftcard,
  People,
} from '@mui/icons-material';
import { useTheme } from '@mui/material/styles';
import type { ComponentType } from 'react';
import type { DashboardStats } from '../../../services/dashboardService';
import { formatMoneyDisplay } from '../../../utils/money';
import type { WidgetCategoria, WidgetInstance } from '../types';
import { resolveTopN } from '../widgetConfigOptions';
import {
  buildCustoPorCentroPie,
  buildCustoPorLinhaPie,
  buildFuncionariosPorCentroPie,
  buildFuncionariosPorLinhaPie,
} from './chartUtils';
import { DistribuicaoWidget } from './DistribuicaoWidget';
import { EvolucaoMensalWidget } from './EvolucaoMensalWidget';
import { FuncionariosPorCargoWidget } from './FuncionariosPorCargoWidget';
import { KpiWidget } from './KpiWidget';
import { TopRubricasWidget } from './TopRubricasWidget';

export interface WidgetProps {
  instance: WidgetInstance;
  stats?: DashboardStats;
  editMode: boolean;
}

export interface WidgetDefinition {
  id: string;
  titulo: string;
  categoria: WidgetCategoria;
  colSpanPadrao: number;
  rowSpanPadrao: number;
  Component: ComponentType<WidgetProps>;
}

function KpiTotalFuncionarios(props: WidgetProps) {
  return (
    <KpiWidget
      title="Total de Funcionários"
      value={props.stats?.totalFuncionarios ?? 0}
      icon={<People fontSize="large" />}
      color="info"
    />
  );
}

function KpiCustoEmpresa(props: WidgetProps) {
  return (
    <KpiWidget
      title="Custo Empresa"
      value={formatMoneyDisplay(props.stats?.custoMensalFolha ?? 0)}
      icon={<AttachMoney fontSize="large" />}
      color="success"
      valueVariant="h4"
      valueColor="success.main"
    />
  );
}

function KpiBeneficiosAtivos(props: WidgetProps) {
  return (
    <KpiWidget
      title="Benefícios Ativos"
      value={props.stats?.totalBeneficiosAtivos ?? 0}
      icon={<CardGiftcard fontSize="large" />}
      color="warning"
      valueColor="warning.main"
    />
  );
}

function KpiRelacaoPd(props: WidgetProps) {
  const totalProventos = props.stats?.totalProventos ?? 0;
  const totalDescontos = props.stats?.totalDescontos ?? 0;
  const total = totalProventos + totalDescontos;
  const percentual = total > 0 ? ((totalProventos / total) * 100).toFixed(1) : '0.0';

  return (
    <KpiWidget
      title="Relação P/D"
      value={`${percentual}%`}
      icon={<Assessment fontSize="large" />}
      color="info"
      avatarIconTone="dark"
      valueColor="info.main"
    />
  );
}

function GraficoFuncionariosPorCc(props: WidgetProps) {
  const theme = useTheme();
  const topN = resolveTopN(props.instance.widgetId, props.instance.config);
  const data = props.stats ? buildFuncionariosPorCentroPie(props.stats, theme.palette.charts, topN) : [];
  return (
    <DistribuicaoWidget
      title="Funcionários por Centro de Custo"
      data={data}
      tipoVisualizacao={props.instance.config?.tipoVisualizacao}
    />
  );
}

function GraficoFuncionariosPorLinha(props: WidgetProps) {
  const theme = useTheme();
  const topN = resolveTopN(props.instance.widgetId, props.instance.config);
  const data = props.stats ? buildFuncionariosPorLinhaPie(props.stats, theme.palette.charts, topN) : [];
  return (
    <DistribuicaoWidget
      title="Funcionários por Linha de Negócio"
      data={data}
      tipoVisualizacao={props.instance.config?.tipoVisualizacao}
    />
  );
}

function GraficoCustoPorCc(props: WidgetProps) {
  const theme = useTheme();
  const topN = resolveTopN(props.instance.widgetId, props.instance.config);
  const data = props.stats ? buildCustoPorCentroPie(props.stats, theme.palette.charts, topN) : [];
  return (
    <DistribuicaoWidget
      title="Custo Folha por Centro de Custo"
      data={data}
      currency
      tipoVisualizacao={props.instance.config?.tipoVisualizacao}
    />
  );
}

function GraficoCustoPorLinha(props: WidgetProps) {
  const theme = useTheme();
  const topN = resolveTopN(props.instance.widgetId, props.instance.config);
  const data = props.stats ? buildCustoPorLinhaPie(props.stats, theme.palette.charts, topN) : [];
  return (
    <DistribuicaoWidget
      title="Custo Folha por Linha de Negócio"
      data={data}
      currency
      tipoVisualizacao={props.instance.config?.tipoVisualizacao}
    />
  );
}

function ListaTopProventos(props: WidgetProps) {
  return <TopRubricasWidget {...props} variant="proventos" />;
}

function ListaTopDescontos(props: WidgetProps) {
  return <TopRubricasWidget {...props} variant="descontos" />;
}

export const WIDGET_REGISTRY: WidgetDefinition[] = [
  {
    id: 'kpi-total-funcionarios',
    titulo: 'Total de Funcionários',
    categoria: 'KPI',
    colSpanPadrao: 3,
    rowSpanPadrao: 1,
    Component: KpiTotalFuncionarios,
  },
  {
    id: 'kpi-custo-empresa',
    titulo: 'Custo Empresa',
    categoria: 'KPI',
    colSpanPadrao: 3,
    rowSpanPadrao: 1,
    Component: KpiCustoEmpresa,
  },
  {
    id: 'kpi-beneficios-ativos',
    titulo: 'Benefícios Ativos',
    categoria: 'KPI',
    colSpanPadrao: 3,
    rowSpanPadrao: 1,
    Component: KpiBeneficiosAtivos,
  },
  {
    id: 'kpi-relacao-pd',
    titulo: 'Relação P/D',
    categoria: 'KPI',
    colSpanPadrao: 3,
    rowSpanPadrao: 1,
    Component: KpiRelacaoPd,
  },
  {
    id: 'grafico-evolucao-mensal',
    titulo: 'Evolução da Folha',
    categoria: 'GRAFICO',
    colSpanPadrao: 12,
    rowSpanPadrao: 2,
    Component: EvolucaoMensalWidget,
  },
  {
    id: 'grafico-funcionarios-por-cc',
    titulo: 'Funcionários por Centro de Custo',
    categoria: 'GRAFICO',
    colSpanPadrao: 3,
    rowSpanPadrao: 2,
    Component: GraficoFuncionariosPorCc,
  },
  {
    id: 'grafico-funcionarios-por-linha',
    titulo: 'Funcionários por Linha de Negócio',
    categoria: 'GRAFICO',
    colSpanPadrao: 3,
    rowSpanPadrao: 2,
    Component: GraficoFuncionariosPorLinha,
  },
  {
    id: 'grafico-custo-por-cc',
    titulo: 'Custo por Centro de Custo',
    categoria: 'GRAFICO',
    colSpanPadrao: 3,
    rowSpanPadrao: 2,
    Component: GraficoCustoPorCc,
  },
  {
    id: 'grafico-custo-por-linha',
    titulo: 'Custo por Linha de Negócio',
    categoria: 'GRAFICO',
    colSpanPadrao: 3,
    rowSpanPadrao: 2,
    Component: GraficoCustoPorLinha,
  },
  {
    id: 'lista-top-proventos',
    titulo: 'Top Proventos',
    categoria: 'LISTA',
    colSpanPadrao: 6,
    rowSpanPadrao: 2,
    Component: ListaTopProventos,
  },
  {
    id: 'lista-top-descontos',
    titulo: 'Top Descontos',
    categoria: 'LISTA',
    colSpanPadrao: 6,
    rowSpanPadrao: 2,
    Component: ListaTopDescontos,
  },
  {
    id: 'grafico-funcionarios-por-cargo',
    titulo: 'Funcionários por Cargo',
    categoria: 'GRAFICO',
    colSpanPadrao: 6,
    rowSpanPadrao: 2,
    Component: FuncionariosPorCargoWidget,
  },
];

export function getWidgetDefinition(widgetId: string): WidgetDefinition | undefined {
  return WIDGET_REGISTRY.find((entry) => entry.id === widgetId);
}
