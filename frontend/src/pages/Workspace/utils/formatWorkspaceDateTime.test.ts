import { describe, expect, it } from 'vitest';
import { formatWorkspaceDateTime } from './formatWorkspaceDateTime';

describe('formatWorkspaceDateTime', () => {
  it('formats ISO timestamp to pt-BR dd/MM/yyyy HH:mm', () => {
    const formatted = formatWorkspaceDateTime('2026-08-01T14:30:00Z');
    expect(formatted).toContain('01/08/2026');
    expect(formatted).toMatch(/\d{2}:\d{2}/);
  });

  it('returns em dash for null and undefined', () => {
    expect(formatWorkspaceDateTime(null)).toBe('—');
    expect(formatWorkspaceDateTime(undefined)).toBe('—');
  });

  it('returns em dash for invalid strings', () => {
    expect(formatWorkspaceDateTime('not-a-date')).toBe('—');
    expect(formatWorkspaceDateTime('')).toBe('—');
  });
});
