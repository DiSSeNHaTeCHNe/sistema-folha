import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import Typography from '@mui/material/Typography';
import { renderWithProviders } from '../test/renderWithProviders';
import { TEMA_IDS } from './themes';

/**
 * TEMAF-07: a escala precisa ser medida no elemento renderizado, não só no objeto
 * tema — é a única camada que detecta uma prop vencendo a variante (DD-7).
 */

/** Tamanhos alvo da Nota de escala da spec (mockup × 1,415). */
const ESCALA_ESPERADA = [
  { variante: 'h3', px: 27 },
  { variante: 'h4', px: 24 },
  { variante: 'h6', px: 16 },
] as const;

/** jsdom devolve o valor especificado; rem é resolvido contra o root de 16px. */
function paraPx(fontSize: string): number {
  if (fontSize.endsWith('rem')) {
    return Number.parseFloat(fontSize) * 16;
  }
  return Number.parseFloat(fontSize);
}

describe('escala tipográfica renderizada', () => {
  describe.each(TEMA_IDS)('tema %s', (temaId) => {
    it.each(ESCALA_ESPERADA)('renderiza $variante com $px px', ({ variante, px }) => {
      renderWithProviders(<Typography variant={variante}>Amostra</Typography>, { temaId });
      const elemento = screen.getByText('Amostra');
      expect(paraPx(getComputedStyle(elemento).fontSize)).toBeCloseTo(px, 1);
    });
  });

  it('prop inline vence a variante do tema — razão da remoção de props na Fase 3', () => {
    renderWithProviders(
      <>
        <Typography variant="h4">Do tema</Typography>
        <Typography variant="h4" fontWeight="bold">
          Com prop
        </Typography>
      </>,
    );

    expect(getComputedStyle(screen.getByText('Do tema')).fontWeight).toBe('600');
    expect(getComputedStyle(screen.getByText('Com prop')).fontWeight).toBe('700');
  });
});
