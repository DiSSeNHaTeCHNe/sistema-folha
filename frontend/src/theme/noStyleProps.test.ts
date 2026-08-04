import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

/**
 * Guarda de props de estilo (TEMAF-09, TEMAF-10, TEMAF-11).
 *
 * Varre apenas os `.tsx` **rastreados** de `src/pages/` e `src/components/` —
 * `pages/DashboardCustomizavel/` está em `.gitignore` e, conforme a spec, fica
 * fora do grep rastreado. `src/theme/**` é isento por design: o caso de controle
 * de `escalaRenderizada.test.tsx` usa `fontWeight` de propósito, para provar que
 * a prop vence o tema.
 */
const FRONTEND_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');

function arquivosRastreados(): string[] {
  const saida = execFileSync(
    'git',
    ['ls-files', '--', 'src/pages/*.tsx', 'src/pages/**/*.tsx', 'src/components/*.tsx', 'src/components/**/*.tsx'],
    { cwd: FRONTEND_ROOT, encoding: 'utf-8' },
  );
  return saida.split('\n').filter(Boolean);
}

function violacoes(detector: (conteudo: string) => boolean): string[] {
  return arquivosRastreados().filter((arquivo) =>
    detector(readFileSync(join(FRONTEND_ROOT, arquivo), 'utf-8')),
  );
}

/** Cada tag de abertura `<Typography ...>` do arquivo, incluindo as multilinha. */
function tagsDeTypography(conteudo: string): string[] {
  return conteudo.match(/<Typography\b[^>]*>/g) ?? [];
}

describe('guarda de props de estilo em pages/components rastreados', () => {
  it('não há prop fontWeight — o peso vem de theme.typography (TEMAF-09)', () => {
    const arquivos = violacoes((conteudo) => /\bfontWeight\s*=/.test(conteudo));

    expect(arquivos, arquivos.join('\n')).toEqual([]);
  });

  it('não há color="primary" em Typography de variante h1-h6 (TEMAF-10)', () => {
    const arquivos = violacoes((conteudo) =>
      tagsDeTypography(conteudo).some(
        (tag) => /variant\s*=\s*"h[1-6]"/.test(tag) && /color\s*=\s*"primary"/.test(tag),
      ),
    );

    expect(arquivos, arquivos.join('\n')).toEqual([]);
  });

  it('não há a forma depreciada color="textSecondary" (TEMAF-11)', () => {
    const arquivos = violacoes((conteudo) => /color\s*=\s*"textSecondary"/.test(conteudo));

    expect(arquivos, arquivos.join('\n')).toEqual([]);
  });
});
