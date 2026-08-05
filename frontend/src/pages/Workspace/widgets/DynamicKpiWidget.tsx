import { AttachMoney } from '@mui/icons-material';
import { formatMoneyDisplay } from '../../../utils/money';
import { KpiWidget } from '../../MeuDashboard/widgets/KpiWidget';
import type { WorkspaceWidgetData } from '../types';

interface DynamicKpiWidgetProps {
  title: string;
  data: WorkspaceWidgetData;
}

function resolveKpiValue(data: WorkspaceWidgetData): string {
  const raw = data.valores.total ?? data.valores.valor ?? Object.values(data.valores)[0];
  if (raw == null) {
    return '-';
  }
  if (raw.includes(',') || raw.includes('R$') || raw.includes('.')) {
    return formatMoneyDisplay(raw);
  }
  return raw;
}

export function DynamicKpiWidget({ title, data }: DynamicKpiWidgetProps) {
  return (
    <KpiWidget
      title={title}
      value={resolveKpiValue(data)}
      icon={<AttachMoney fontSize="large" />}
      color="success"
      valueColor="success.main"
    />
  );
}
