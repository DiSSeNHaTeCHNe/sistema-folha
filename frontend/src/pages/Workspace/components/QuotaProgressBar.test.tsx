import { describe, expect, it } from 'vitest';
import { fireEvent, screen } from '@testing-library/react';
import { QuotaProgressBar } from './QuotaProgressBar';
import { renderWithProviders } from '../../../test/renderWithProviders';

describe('QuotaProgressBar', () => {
  it('displays label and N de M count (WKS2-04)', () => {
    renderWithProviders(<QuotaProgressBar label="Datasets" current={3} max={20} />);

    expect(screen.getByText('Datasets')).toBeInTheDocument();
    expect(screen.getByText('3 de 20')).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: 'Datasets: 3 de 20' })).toHaveAttribute('aria-valuenow', '3');
  });

  it('shows warn chip when usage is above 80%', () => {
    renderWithProviders(<QuotaProgressBar label="Linhas" current={420} max={500} />);

    expect(screen.getByText('420 de 500')).toBeInTheDocument();
    expect(screen.getByText('420 de 500')).toHaveStyle({ fontWeight: '600' });
  });

  it('shows tooltip when quota is at 100% (WKS2-14)', async () => {
    renderWithProviders(<QuotaProgressBar label="Workspaces" current={10} max={10} />);

    expect(screen.getByText('10 de 10')).toBeInTheDocument();
    fireEvent.mouseOver(screen.getByRole('progressbar', { name: 'Workspaces: 10 de 10' }));
    expect(await screen.findByRole('tooltip')).toHaveTextContent(/Limite atingido/i);
  });
});
