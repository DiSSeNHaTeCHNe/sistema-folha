import { describe, expect, it } from 'vitest';
import {
  CLASSICO_CHARTS,
  TEMA_PADRAO,
  criarTema,
  isTemaId,
} from './themes';

describe('themes', () => {
  it('preserves classico palette from main.tsx inline theme', () => {
    const theme = criarTema('classico');
    expect(theme.palette.primary.main).toBe('#1976d2');
    expect(theme.palette.secondary.main).toBe('#dc004e');
    expect(theme.palette.background.default).toBe('#f5f5f5');
  });

  it('exposes charts and chrome on classico', () => {
    const theme = criarTema('classico');
    expect(theme.palette.charts).toEqual([...CLASSICO_CHARTS]);
    expect(theme.palette.chrome.bg).toBe('#ffffff');
    expect(theme.palette.chrome.fgAtivo).toBe('#1976d2');
  });

  it('validates tema ids with isTemaId', () => {
    expect(isTemaId('classico')).toBe(true);
    expect(isTemaId(null)).toBe(false);
    expect(isTemaId(undefined)).toBe(false);
    expect(isTemaId('')).toBe(false);
    expect(isTemaId(42)).toBe(false);
    expect(isTemaId('corporate')).toBe(false);
  });

  it('uses classico as default tema', () => {
    expect(TEMA_PADRAO).toBe('classico');
  });
});
