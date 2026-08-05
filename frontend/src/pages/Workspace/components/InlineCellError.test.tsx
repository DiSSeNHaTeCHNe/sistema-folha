import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { InlineCellError } from './InlineCellError';
import { renderWithProviders } from '../../../test/renderWithProviders';

describe('InlineCellError', () => {
  it('renders inline error message with alert role (WKS2-15)', () => {
    renderWithProviders(<InlineCellError message="Esperado número" />);
    expect(screen.getByRole('alert')).toHaveTextContent('Esperado número');
  });

  it('does not affect sibling content when used in isolation', () => {
    renderWithProviders(
      <>
        <span>Valor válido</span>
        <InlineCellError message="Data inválida" />
      </>,
    );
    expect(screen.getByText('Valor válido')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Data inválida');
  });
});
