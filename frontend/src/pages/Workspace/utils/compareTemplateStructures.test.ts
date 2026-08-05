import { describe, expect, it } from 'vitest';
import {
  compareTemplateStructures,
  hasStructureChanges,
} from './compareTemplateStructures';

describe('compareTemplateStructures', () => {
  it('detects added and removed campos', () => {
    const diff = compareTemplateStructures(
      { campos: ['a', 'b'], widgets: [], formulas: [] },
      { campos: ['b', 'c'], widgets: [], formulas: [] },
    );
    expect(diff.camposAdicionados).toEqual(['c']);
    expect(diff.camposRemovidos).toEqual(['a']);
  });

  it('detects added and removed widgets', () => {
    const diff = compareTemplateStructures(
      { campos: [], widgets: ['KPI A'], formulas: [] },
      { campos: [], widgets: ['KPI A', 'Tabela B'], formulas: [] },
    );
    expect(diff.widgetsAdicionados).toEqual(['Tabela B']);
    expect(diff.widgetsRemovidos).toEqual([]);
  });

  it('detects altered formulas', () => {
    const diff = compareTemplateStructures(
      { campos: [], widgets: [], formulas: ['SOMA(x)'] },
      { campos: [], widgets: [], formulas: ['SOMA(y)'] },
    );
    expect(diff.formulasAlteradas).toContain('SOMA(y)');
  });

  it('returns empty diff when structures match', () => {
    const base = { campos: ['a'], widgets: ['w1'], formulas: ['F()'] };
    const diff = compareTemplateStructures(base, base);
    expect(hasStructureChanges(diff)).toBe(false);
  });

  it('hasStructureChanges is true when any category differs', () => {
    const diff = compareTemplateStructures(
      { campos: [], widgets: [], formulas: [] },
      { campos: ['novo'], widgets: [], formulas: [] },
    );
    expect(hasStructureChanges(diff)).toBe(true);
  });
});
