import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import {
  CLASSICO_CHARTS,
  TEMA_PADRAO,
  criarTema,
  isTemaId,
} from './themes';

function lerPrimaryColorRelatorios(): string {
  const ymlPath = resolve(
    dirname(fileURLToPath(import.meta.url)),
    '../../../backend/src/main/resources/application.yml',
  );
  const content = readFileSync(ymlPath, 'utf-8');
  const match = content.match(/primary-color:\s*"([^"]+)"/);
  if (!match?.[1]) {
    throw new Error('relatorios.branding.primary-color não encontrado em application.yml');
  }
  return match[1].toLowerCase();
}

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
    expect(isTemaId('indigo')).toBe(true);
    expect(isTemaId('techne')).toBe(true);
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

  it('registers indigo dark theme with study palette tokens', () => {
    const theme = criarTema('indigo');
    expect(theme.palette.mode).toBe('dark');
    expect(theme.palette.primary.main).toBe('#7F77DD');
    expect(theme.palette.background.default).toBe('#12121A');
    expect(theme.palette.background.paper).toBe('#1C1C28');
    expect(theme.palette.charts.length).toBeGreaterThan(0);
  });

  it('registers techne brand theme with institutional palette tokens', () => {
    const theme = criarTema('techne');
    expect(theme.palette.primary.main).toBe('#7836FC');
    expect(theme.palette.chrome.bg).toBe('#20284E');
    expect(theme.palette.background.default).toBe('#EFF2F7');
    expect(theme.typography.fontFamily).toMatch(/^Poppins/);
    expect(theme.palette.charts.length).toBeGreaterThan(0);
  });

  it('keeps techne primary.main aligned with relatorios branding in application.yml', () => {
    const theme = criarTema('techne');
    expect(theme.palette.primary.main.toLowerCase()).toBe(lerPrimaryColorRelatorios());
  });
});
