import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { SourceBadge, resolveWidgetSource } from './SourceBadge';
import { renderWithProviders } from '../../../test/renderWithProviders';

describe('SourceBadge', () => {
  it('renders DATASET badge (WKS2-10)', () => {
    renderWithProviders(<SourceBadge source="DATASET" />);
    expect(screen.getByText('DATASET')).toBeInTheDocument();
  });

  it('renders SISTEMA badge (WKS2-10)', () => {
    renderWithProviders(<SourceBadge source="SISTEMA" />);
    expect(screen.getByText('SISTEMA')).toBeInTheDocument();
  });

  it('resolveWidgetSource returns SISTEMA for catalog widgets', () => {
    expect(
      resolveWidgetSource({ widgetId: 'kpi-total-funcionarios', userWidgetDefinitionId: null }, []),
    ).toBe('SISTEMA');
  });

  it('resolveWidgetSource returns DATASET when user widget references dataset', () => {
    expect(
      resolveWidgetSource(
        { userWidgetDefinitionId: 7 },
        [{ id: 7, fontes: [{ kind: 'DATASET' }] }],
      ),
    ).toBe('DATASET');
  });
});
