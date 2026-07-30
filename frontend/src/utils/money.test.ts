import { describe, expect, it } from 'vitest';
import { formatMoneyDisplay } from './money';

describe('formatMoneyDisplay', () => {
  it('returns dash for null, undefined and empty string', () => {
    expect(formatMoneyDisplay(null)).toBe('-');
    expect(formatMoneyDisplay(undefined)).toBe('-');
    expect(formatMoneyDisplay('')).toBe('-');
  });

  it('formats numeric strings and numbers as BRL currency', () => {
    expect(formatMoneyDisplay('1234.56')).toMatch(/1\.234,56/);
    expect(formatMoneyDisplay(99.9)).toMatch(/99,90/);
  });

  it('accepts comma decimal separator from API strings', () => {
    expect(formatMoneyDisplay('1234,56')).toMatch(/1\.234,56/);
  });

  it('returns dash for non-numeric values', () => {
    expect(formatMoneyDisplay('abc')).toBe('-');
  });
});
