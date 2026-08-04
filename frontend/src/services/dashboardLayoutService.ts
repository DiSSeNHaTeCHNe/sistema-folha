import api from './api';
import type { DashboardLayout, WidgetCatalogItem } from '../pages/MeuDashboard/types';

export async function getDashboardLayout(): Promise<DashboardLayout> {
  const response = await api.get<DashboardLayout>('/dashboard/layout');
  return response.data;
}

export async function saveDashboardLayout(layout: DashboardLayout): Promise<DashboardLayout> {
  const response = await api.put<DashboardLayout>('/dashboard/layout', layout);
  return response.data;
}

export async function resetDashboardLayout(): Promise<void> {
  await api.delete('/dashboard/layout');
}

export async function getWidgetCatalog(): Promise<WidgetCatalogItem[]> {
  const response = await api.get<WidgetCatalogItem[]>('/dashboard/widgets/catalog');
  return response.data;
}
