import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  createWorkspace,
  deleteWorkspace,
  getWorkspace,
  listWorkspaces,
  saveWorkspaceLayout,
} from '../../../services/workspaceService';
import type { Workspace, WorkspaceLayoutWidget, WorkspaceSummary } from '../types';

interface WorkspaceLayoutState {
  saved: Workspace | null;
  draft: Workspace | null;
}

function emptyLayoutState(): WorkspaceLayoutState {
  return { saved: null, draft: null };
}

export function useWorkspaceLayout() {
  const [summaries, setSummaries] = useState<WorkspaceSummary[]>([]);
  const [activeWorkspaceId, setActiveWorkspaceId] = useState<number | null>(null);
  const [layoutsById, setLayoutsById] = useState<Record<number, WorkspaceLayoutState>>({});
  const [editMode, setEditMode] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const activeState = activeWorkspaceId != null ? layoutsById[activeWorkspaceId] : undefined;
  const savedLayout = activeState?.saved ?? null;
  const draftLayout = activeState?.draft ?? null;
  const activeLayout = editMode ? draftLayout : savedLayout;

  const dirty = useMemo(() => {
    if (!editMode || !savedLayout || !draftLayout) {
      return false;
    }
    return JSON.stringify(savedLayout.widgets) !== JSON.stringify(draftLayout.widgets);
  }, [editMode, savedLayout, draftLayout]);

  const loadSummaries = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await listWorkspaces();
      setSummaries(list);
      setActiveWorkspaceId((current) => current ?? list[0]?.id ?? null);
    } catch {
      setError('Erro ao carregar workspaces');
      throw new Error('Erro ao carregar workspaces');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadWorkspaceLayout = useCallback(async (workspaceId: number) => {
    setError(null);
    try {
      const workspace = await getWorkspace(workspaceId);
      setLayoutsById((current) => ({
        ...current,
        [workspaceId]: { saved: workspace, draft: null },
      }));
      return workspace;
    } catch {
      setError('Erro ao carregar layout do workspace');
      throw new Error('Erro ao carregar layout do workspace');
    }
  }, []);

  useEffect(() => {
    void loadSummaries();
  }, [loadSummaries]);

  useEffect(() => {
    if (activeWorkspaceId == null) {
      return;
    }
    if (layoutsById[activeWorkspaceId]?.saved) {
      return;
    }
    void loadWorkspaceLayout(activeWorkspaceId);
  }, [activeWorkspaceId, layoutsById, loadWorkspaceLayout]);

  const switchWorkspace = useCallback((workspaceId: number) => {
    setEditMode(false);
    setActiveWorkspaceId(workspaceId);
  }, []);

  const createNewWorkspace = useCallback(async (nome: string) => {
    const created = await createWorkspace(nome);
    setSummaries((current) => [...current, { id: created.id, nome: created.nome, totalWidgets: 0 }]);
    setLayoutsById((current) => ({
      ...current,
      [created.id]: { saved: created, draft: null },
    }));
    setActiveWorkspaceId(created.id);
    return created;
  }, []);

  const removeWorkspace = useCallback(async (workspaceId: number) => {
    await deleteWorkspace(workspaceId);
    setSummaries((current) => {
      const remaining = current.filter((item) => item.id !== workspaceId);
      setActiveWorkspaceId((currentId) => {
        if (currentId !== workspaceId) {
          return currentId;
        }
        return remaining[0]?.id ?? null;
      });
      return remaining;
    });
    setLayoutsById((current) => {
      const next = { ...current };
      delete next[workspaceId];
      return next;
    });
  }, []);

  const enterEditMode = useCallback(() => {
    if (!savedLayout || activeWorkspaceId == null) {
      return;
    }
    setLayoutsById((current) => ({
      ...current,
      [activeWorkspaceId]: {
        saved: savedLayout,
        draft: structuredClone(savedLayout),
      },
    }));
    setEditMode(true);
  }, [savedLayout, activeWorkspaceId]);

  const cancelEdit = useCallback(() => {
    if (activeWorkspaceId == null) {
      return;
    }
    setLayoutsById((current) => ({
      ...current,
      [activeWorkspaceId]: {
        saved: current[activeWorkspaceId]?.saved ?? null,
        draft: null,
      },
    }));
    setEditMode(false);
  }, [activeWorkspaceId]);

  const updateDraftWidgets = useCallback((widgets: WorkspaceLayoutWidget[]) => {
    if (activeWorkspaceId == null) {
      return;
    }
    setLayoutsById((current) => {
      const state = current[activeWorkspaceId];
      if (!state?.draft) {
        return current;
      }
      return {
        ...current,
        [activeWorkspaceId]: {
          ...state,
          draft: { ...state.draft, widgets },
        },
      };
    });
  }, [activeWorkspaceId]);

  const save = useCallback(async () => {
    if (!draftLayout || activeWorkspaceId == null) {
      return false;
    }
    setSaving(true);
    try {
      const salvo = await saveWorkspaceLayout(activeWorkspaceId, draftLayout.widgets);
      setLayoutsById((current) => ({
        ...current,
        [activeWorkspaceId]: { saved: salvo, draft: null },
      }));
      setSummaries((current) =>
        current.map((item) =>
          item.id === activeWorkspaceId
            ? { ...item, totalWidgets: salvo.widgets.length }
            : item,
        ),
      );
      setEditMode(false);
      return true;
    } catch {
      setError('Erro ao salvar layout');
      return false;
    } finally {
      setSaving(false);
    }
  }, [draftLayout, activeWorkspaceId]);

  const resetActiveLayoutCache = useCallback(() => {
    if (activeWorkspaceId == null) {
      return;
    }
    setLayoutsById((current) => ({
      ...current,
      [activeWorkspaceId]: emptyLayoutState(),
    }));
  }, [activeWorkspaceId]);

  return {
    summaries,
    activeWorkspaceId,
    savedLayout,
    draftLayout,
    activeLayout,
    editMode,
    loading,
    saving,
    error,
    dirty,
    loadSummaries,
    loadWorkspaceLayout,
    switchWorkspace,
    createNewWorkspace,
    removeWorkspace,
    enterEditMode,
    cancelEdit,
    updateDraftWidgets,
    save,
    resetActiveLayoutCache,
    setActiveWorkspaceId,
  };
}
