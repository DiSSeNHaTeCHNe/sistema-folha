import type { Theme } from '@mui/material/styles';

export function widgetCardSx(theme: Theme) {
  return {
    borderRadius: 3,
    boxShadow: theme.shadows[2],
    border: 1,
    borderColor: 'divider' as const,
    height: '100%',
  };
}
