import type { TemplateStructureResumo } from '../pages/Workspace/types';

export interface TemplateStructureDiff {
  camposAdicionados: string[];
  camposRemovidos: string[];
  widgetsAdicionados: string[];
  widgetsRemovidos: string[];
  formulasAlteradas: string[];
}

function listDiff(before: string[] = [], after: string[] = []): { added: string[]; removed: string[] } {
  const beforeSet = new Set(before);
  const afterSet = new Set(after);
  return {
    added: after.filter((item) => !beforeSet.has(item)),
    removed: before.filter((item) => !afterSet.has(item)),
  };
}

function formulaDiff(before: string[] = [], after: string[] = []): string[] {
  const beforeSet = new Set(before);
  const changed: string[] = [];
  for (const formula of after) {
    if (!beforeSet.has(formula)) {
      changed.push(formula);
    }
  }
  for (const formula of before) {
    if (!after.includes(formula) && !changed.includes(formula)) {
      changed.push(formula);
    }
  }
  return changed;
}

export function compareTemplateStructures(
  installed: TemplateStructureResumo,
  latest: TemplateStructureResumo,
): TemplateStructureDiff {
  const campos = listDiff(installed.campos, latest.campos);
  const widgets = listDiff(installed.widgets, latest.widgets);

  return {
    camposAdicionados: campos.added,
    camposRemovidos: campos.removed,
    widgetsAdicionados: widgets.added,
    widgetsRemovidos: widgets.removed,
    formulasAlteradas: formulaDiff(installed.formulas, latest.formulas),
  };
}

export function hasStructureChanges(diff: TemplateStructureDiff): boolean {
  return (
    diff.camposAdicionados.length > 0 ||
    diff.camposRemovidos.length > 0 ||
    diff.widgetsAdicionados.length > 0 ||
    diff.widgetsRemovidos.length > 0 ||
    diff.formulasAlteradas.length > 0
  );
}
