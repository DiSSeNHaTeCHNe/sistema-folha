import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { InfoBanner } from './InfoBanner';
import { renderWithProviders } from '../../../test/renderWithProviders';

describe('InfoBanner', () => {
  it('renders info banner with status role for non-critical notices (WKS2-36)', () => {
    renderWithProviders(
      <InfoBanner variant="info" title="Dica">
        Nova versão disponível no catálogo.
      </InfoBanner>,
    );

    expect(screen.getByRole('status', { name: '' })).toHaveTextContent('Nova versão disponível no catálogo.');
    expect(screen.getByText('Dica')).toBeInTheDocument();
  });

  it('renders warn banner with status role for quota warnings', () => {
    renderWithProviders(
      <InfoBanner variant="warn">Você está próximo do limite de datasets.</InfoBanner>,
    );

    expect(screen.getByRole('status')).toHaveTextContent(/próximo do limite/i);
  });

  it('renders danger banner with alert role for blocking errors (WKS2-37)', () => {
    renderWithProviders(
      <InfoBanner variant="danger">Não foi possível carregar os datasets.</InfoBanner>,
    );

    expect(screen.getByRole('alert')).toHaveTextContent(/Não foi possível carregar/i);
  });

  it('renders ai banner for IA-related messages', () => {
    renderWithProviders(
      <InfoBanner variant="ai">Proposta de IA expira em 72 horas.</InfoBanner>,
    );

    expect(screen.getByRole('status')).toHaveTextContent(/Proposta de IA/i);
  });
});
