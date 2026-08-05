import { describe, expect, it, vi } from 'vitest';
import { fireEvent, screen } from '@testing-library/react';
import { TemplateUpgradeBanner } from './TemplateUpgradeBanner';
import { renderWithProviders } from '../../../test/renderWithProviders';

describe('TemplateUpgradeBanner', () => {
  it('shows version indicator', () => {
    renderWithProviders(
      <TemplateUpgradeBanner
        templateName="Orçamento"
        versaoInstalada={1}
        versaoDisponivel={2}
        onUpgrade={vi.fn()}
      />,
    );
    expect(screen.getByRole('status')).toHaveTextContent(/v1 → v2/);
  });

  it('labels update as optional', () => {
    renderWithProviders(
      <TemplateUpgradeBanner
        templateName="Orçamento"
        versaoInstalada={1}
        versaoDisponivel={2}
        onUpgrade={vi.fn()}
      />,
    );
    expect(screen.getByText(/Atualização é opcional/i)).toBeInTheDocument();
  });

  it('calls onUpgrade when user clicks update', () => {
    const onUpgrade = vi.fn();
    renderWithProviders(
      <TemplateUpgradeBanner
        templateName="Orçamento"
        versaoInstalada={1}
        versaoDisponivel={2}
        onUpgrade={onUpgrade}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Atualizar template Orçamento para versão 2' }));
    expect(onUpgrade).toHaveBeenCalledOnce();
  });

  it('disables button while upgrading', () => {
    renderWithProviders(
      <TemplateUpgradeBanner
        templateName="Orçamento"
        versaoInstalada={1}
        versaoDisponivel={2}
        upgrading
        onUpgrade={vi.fn()}
      />,
    );
    expect(screen.getByRole('button', { name: 'Atualizar template Orçamento para versão 2' })).toBeDisabled();
    expect(screen.getByText('Atualizando…')).toBeInTheDocument();
  });

  it('includes template name in accessible label', () => {
    renderWithProviders(
      <TemplateUpgradeBanner
        templateName="KPI Vendas"
        versaoInstalada={2}
        versaoDisponivel={4}
        onUpgrade={vi.fn()}
      />,
    );
    expect(screen.getByRole('status', { name: 'Atualização disponível para KPI Vendas' })).toBeInTheDocument();
  });
});
