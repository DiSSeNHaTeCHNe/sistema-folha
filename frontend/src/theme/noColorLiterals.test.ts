import { readFileSync, readdirSync, statSync } from 'node:fs';
import { extname, join } from 'node:path';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const SRC_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const SCAN_DIRS = ['pages', 'components'].map((dir) => join(SRC_ROOT, dir));
const COLOR_PATTERN = /#[0-9a-fA-F]{3,8}\b|rgba?\(|hsla?\(/;

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
        if (COLOR_PATTERN.test(readFileSync(file, 'utf-8'))) {
          violations.push(file.replace(`${SRC_ROOT}/`, ''));
        }
      }
    }

    expect(violations, violations.join('\n')).toEqual([]);
  });
});
