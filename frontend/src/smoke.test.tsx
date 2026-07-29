import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { Usuario } from './types';
import { isAdmin } from './utils/permissions';

describe('vitest baseline', () => {
  it('renders a smoke target by role', () => {
    render(<button type="button">Vitest baseline</button>);
    expect(screen.getByRole('button', { name: 'Vitest baseline' })).toBeInTheDocument();
  });

  it('evaluates isAdmin helper', () => {
    const admin: Usuario = {
      id: 1,
      login: 'admin',
      nome: 'Admin',
      permissoes: ['ADMIN'],
    };
    expect(isAdmin(admin)).toBe(true);
    expect(isAdmin(null)).toBe(false);
  });
});
