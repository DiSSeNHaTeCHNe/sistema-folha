import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  getDashboardLayout,
  getWidgetCatalog,
  resetDashboardLayout,
  saveDashboardLayout,
} from '../../../services/dashboardLayoutService';
import { getWidgetDefinition } from '../widgets/registry';
import type { DashboardLayout, WidgetCatalogItem, WidgetInstance } from '../types';
import { gravarLayoutCache, lerLayoutCache } from '../storage';

function filtrarWidgetsConhecidos(widgets: WidgetInstance[]): WidgetInstance[] {
  return widgets.filter((widget) => getWidgetDefinition(widget.widgetId) != null);
}

function normalizarLayout(layout: DashboardLayout): DashboardLayout {
  const widgets = filtrarWidgetsConhecidos(layout.widgets);
  return { ...layout, widgets };
}

export function useDashboardLayout() {
  const [savedLayout, setSavedLayout] = useState<DashboardLayout | null>(null);
  const [draftLayout, setDraftLayout] = useState<DashboardLayout | null>(null);
  const [catalog, setCatalog] = useState<WidgetCatalogItem[]>([]);
  const [editMode, setEditMode] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const activeLayout = editMode ? draftLayout : savedLayout;

  const dirty = useMemo(() => {
    if (!editMode || !savedLayout || !draftLayout) {
      return false;
    }
    return JSON.stringify(savedLayout.widgets) !== JSON.stringify(draftLayout.widgets)
      || savedLayout.nome !== draftLayout.nome;
  }, [editMode, savedLayout, draftLayout]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const cached = lerLayoutCache();
      const [layoutRemoto, catalogo] = await Promise.all([getDashboardLayout(), getWidgetCatalog()]);
      const layoutNormalizado = normalizarLayout(layoutRemoto);
      setSavedLayout(layoutNormalizado);
      setCatalog(catalogo);
      gravarLayoutCache(layoutNormalizado);
      if (cached && cached.id === layoutNormalizado.id) {
        gravarLayoutCache(layoutNormalizado);
      }
    } catch {
      const cached = lerLayoutCache();
      if (cached) {
        setSavedLayout(normalizarLayout(cached));
      }
      setError('Erro ao carregar layout do dashboard');
      throw new Error('Erro ao carregar layout do dashboard');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const enterEditMode = useCallback(() => {
    if (!savedLayout) {
      return;
    }
    setDraftLayout(structuredClone(savedLayout));
    setEditMode(true);
  }, [savedLayout]);

  const cancelEdit = useCallback(() => {
    setDraftLayout(null);
    setEditMode(false);
  }, []);

  const updateDraftWidgets = useCallback((widgets: WidgetInstance[]) => {
    setDraftLayout((current) => (current ? { ...current, widgets } : current));
  }, []);

  const save = useCallback(async () => {
    if (!draftLayout) {
      return false;
    }
    setSaving(true);
    try {
      const salvo = normalizarLayout(await saveDashboardLayout(draftLayout));
      setSavedLayout(salvo);
      setDraftLayout(null);
      setEditMode(false);
      gravarLayoutCache(salvo);
      return true;
    } catch {
      setError('Erro ao salvar layout');
      return false;
    } finally {
      setSaving(false);
    }
  }, [draftLayout]);

  const resetToDefault = useCallback(async () => {
    setSaving(true);
    try {
      await resetDashboardLayout();
      await load();
      setDraftLayout(null);
      setEditMode(false);
      return true;
    } catch {
      setError('Erro ao restaurar layout padrão');
      return false;
    } finally {
      setSaving(false);
    }
  }, [load]);

  return {
    savedLayout,
    draftLayout,
    activeLayout,
    catalog,
    editMode,
    loading,
    saving,
    error,
    dirty,
    load,
    enterEditMode,
    cancelEdit,
    updateDraftWidgets,
    save,
    resetToDefault,
    setDraftLayout,
  };
}
