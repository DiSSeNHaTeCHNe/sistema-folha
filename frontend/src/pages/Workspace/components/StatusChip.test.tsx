import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { StatusChip } from './StatusChip';
import { renderWithProviders } from '../../../test/renderWithProviders';
import { chipVariants } from '../workspaceTheme';

describe('StatusChip', () => {
  it('renders info variant for neutral status (WKS2-36)', () => {
    renderWithProviders(<StatusChip variant="info" label="Versão 2" />);
    const chip = screen.getByText('Versão 2');
    expect(chip).toBeInTheDocument();
    expect(chip).toHaveStyle({ color: chipVariants.info.color });
  });

  it('renders ok variant for healthy quota state', () => {
    renderWithProviders(<StatusChip variant="ok" label="Dentro do limite" />);
    expect(screen.getByText('Dentro do limite')).toHaveStyle({ color: chipVariants.ok.color });
  });

  it('renders warn variant when quota is near limit', () => {
    renderWithProviders(<StatusChip variant="warn" label="18 de 20" />);
    expect(screen.getByText('18 de 20')).toHaveStyle({ color: chipVariants.warn.color });
  });

  it('renders danger variant when quota is exceeded or blocked', () => {
    renderWithProviders(<StatusChip variant="danger" label="Limite atingido" />);
    expect(screen.getByText('Limite atingido')).toHaveStyle({ color: chipVariants.danger.color });
  });

  it('renders ai variant for IA-related status', () => {
    renderWithProviders(<StatusChip variant="ai" label="Sugestão IA" />);
    expect(screen.getByText('Sugestão IA')).toHaveStyle({ color: chipVariants.ai.color });
  });
});
