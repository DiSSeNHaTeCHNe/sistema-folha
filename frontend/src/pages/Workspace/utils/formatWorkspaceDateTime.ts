const EMPTY_LABEL = '—';

export function formatWorkspaceDateTime(iso: string | null | undefined): string {
  if (iso == null || iso === '') {
    return EMPTY_LABEL;
  }
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return EMPTY_LABEL;
  }
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
