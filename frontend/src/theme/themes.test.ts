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
    expect(theme.palette.chrome.fgAtivo).toBe('#0d47a1');
  });

  it('validates tema ids with isTemaId', () => {
    expect(isTemaId('classico')).toBe(true);
    expect(isTemaId(null)).toBe(false);
    expect(isTemaId(undefined)).toBe(false);
    expect(isTemaId('')).toBe(false);
    expect(isTemaId(42)).toBe(false);
    expect(isTemaId('corporate')).toBe(true);
    expect(isTemaId('soft')).toBe(true);
    expect(isTemaId('indigo')).toBe(false);
  });

  it('uses classico as default tema', () => {
    expect(TEMA_PADRAO).toBe('classico');
  });

  it('registers corporate theme with study palette tokens', () => {
    const theme = criarTema('corporate');
    expect(theme.palette.primary.main).toBe('#3B82F6');
    expect(theme.palette.chrome.bg).toBe('#0F172A');
    expect(theme.palette.background.default).toBe('#F4F6F8');
    expect(theme.palette.charts.length).toBeGreaterThan(0);
  });

  it('registers soft theme with study palette tokens', () => {
    const theme = criarTema('soft');
    expect(theme.palette.primary.main).toBe('#1D9E75');
    expect(theme.palette.chrome.bg).toBe('#F4F2EC');
    expect(theme.palette.background.default).toBe('#FBFAF7');
    expect(theme.palette.charts.length).toBeGreaterThan(0);
  });
});
