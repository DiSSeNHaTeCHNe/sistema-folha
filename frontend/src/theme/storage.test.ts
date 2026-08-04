import { afterEach, describe, expect, it, vi } from 'vitest';
import { TEMA_PADRAO } from './themes';
import { gravarTema, lerTemaSalvo } from './storage';

const CHAVE = 'sistema-folha:tema';

describe('storage', () => {
  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('returns default when key is absent', () => {
    expect(lerTemaSalvo()).toBe(TEMA_PADRAO);
  });

  it('returns default when stored value is empty string', () => {
    localStorage.setItem(CHAVE, '');
    expect(lerTemaSalvo()).toBe(TEMA_PADRAO);
  });

  it('returns default when stored id is unknown', () => {
    localStorage.setItem(CHAVE, 'corporate');
    expect(lerTemaSalvo()).toBe(TEMA_PADRAO);
  });

  it('returns default when stored value is not a string', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => 123 as unknown as string);
    expect(lerTemaSalvo()).toBe(TEMA_PADRAO);
  });

  it('returns default when getItem throws', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('storage unavailable');
    });
    expect(lerTemaSalvo()).toBe(TEMA_PADRAO);
  });

  it('does not propagate when setItem throws', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota exceeded');
    });
    expect(() => gravarTema('classico')).not.toThrow();
  });

  it('persists a valid tema id', () => {
    gravarTema('classico');
    expect(localStorage.getItem(CHAVE)).toBe('classico');
    expect(lerTemaSalvo()).toBe('classico');
  });
});
