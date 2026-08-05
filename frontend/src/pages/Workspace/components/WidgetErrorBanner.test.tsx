import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { WidgetErrorBanner } from './WidgetErrorBanner';
import { renderWithProviders } from '../../../test/renderWithProviders';

describe('WidgetErrorBanner', () => {
  it('renders danger banner with alert role for widget errors (WKS2-37)', () => {
    renderWithProviders(
      <WidgetErrorBanner title="Erro ao carregar" message="Não foi possível carregar os dados deste widget." />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent(/Não foi possível carregar/i);
    expect(screen.getByText('Erro ao carregar')).toBeInTheDocument();
  });

  it('renders warn banner for invalid formula', () => {
    renderWithProviders(
      <WidgetErrorBanner
        variant="warn"
        title="Fórmula inválida"
        message="Revise a definição deste widget."
      />,
    );
    expect(screen.getByRole('status')).toHaveTextContent(/Fórmula inválida/i);
  });

  it('renders optional action slot', () => {
    renderWithProviders(
      <WidgetErrorBanner
        message="Falha temporária."
        action={<button type="button">Recarregar</button>}
      />,
    );
    expect(screen.getByRole('button', { name: 'Recarregar' })).toBeInTheDocument();
  });

  it('keeps message isolated without page-level crash semantics', () => {
    renderWithProviders(<WidgetErrorBanner message="Widget desconhecido" />);
    expect(screen.getByRole('alert')).toHaveTextContent('Widget desconhecido');
    expect(screen.queryByRole('heading', { level: 1 })).not.toBeInTheDocument();
  });
});
