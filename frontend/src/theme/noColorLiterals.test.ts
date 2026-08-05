import { readFileSync, readdirSync, statSync } from 'node:fs';
import { extname, join } from 'node:path';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const SRC_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const SCAN_DIRS = ['pages', 'components'].map((dir) => join(SRC_ROOT, dir));
/** WKS2-35 canonical token module — hex literals are intentional here. */
const ALLOWLIST = new Set(['pages/Workspace/workspaceTheme.ts']);
const COLOR_PATTERN = /#[0-9a-fA-F]{3,8}\b|rgba?\(|hsla?\(/;

/**
 * Quick 014: hexadecimais e `rgba(...)` já eram barrados, mas a cor **nomeada**
 * passava — e foi por essa fresta que `color: 'white'` sobreviveu duas vezes sobre um
 * fundo `.light` que a quick 012 tornou tint claro (`Funcionarios/index.tsx:534`,
 * corrigido pela 013, e `:556`, corrigido pela 014, ambos rendendo ~1,2:1 na tela).
 * Tokens da paleta como `'common.white'` seguem válidos: o ponto antes do nome impede
 * o casamento.
 */
const NAMED_COLOR_PATTERN = /['"](white|black)['"]/;

function collectSourceFiles(dir: string): string[] {
  const files: string[] = [];
  for (const entry of readdirSync(dir)) {
    const fullPath = join(dir, entry);
    if (statSync(fullPath).isDirectory()) {
      files.push(...collectSourceFiles(fullPath));
      continue;
    }
    if (extname(fullPath) === '.ts' || extname(fullPath) === '.tsx') {
      files.push(fullPath);
    }
  }
  return files;
}

describe('no color literals in pages/components', () => {
  it('does not contain hex, rgba or hsl color literals', () => {
    const violations: string[] = [];

    for (const dir of SCAN_DIRS) {
      for (const file of collectSourceFiles(dir)) {
        const relative = file.replace(`${SRC_ROOT}/`, '');
        if (ALLOWLIST.has(relative)) {
          continue;
        }
        if (COLOR_PATTERN.test(readFileSync(file, 'utf-8'))) {
          violations.push(relative);
        }
      }
    }

    expect(violations, violations.join('\n')).toEqual([]);
  });

  it('does not contain named color literals', () => {
    const violations: string[] = [];

    for (const dir of SCAN_DIRS) {
      for (const file of collectSourceFiles(dir)) {
        if (NAMED_COLOR_PATTERN.test(readFileSync(file, 'utf-8'))) {
          violations.push(file.replace(`${SRC_ROOT}/`, ''));
        }
      }
    }

    expect(violations, violations.join('\n')).toEqual([]);
  });
});
