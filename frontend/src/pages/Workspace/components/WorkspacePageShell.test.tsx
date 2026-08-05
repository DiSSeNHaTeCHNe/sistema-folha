import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { Button } from '@mui/material';
import { WorkspacePageShell } from './WorkspacePageShell';
import { renderWithProviders } from '../../../test/renderWithProviders';
import { colors } from '../workspaceTheme';

describe('WorkspacePageShell', () => {
  it('renders title and subtitle for hub pages (WKS2-35)', () => {
    renderWithProviders(
      <WorkspacePageShell title="Meus workspaces" subtitle="Gerencie seus painéis personalizados">
        <p>Conteúdo</p>
      </WorkspacePageShell>,
    );

    expect(screen.getByRole('heading', { name: 'Meus workspaces', level: 1 })).toBeInTheDocument();
    expect(screen.getByText('Gerencie seus painéis personalizados')).toBeInTheDocument();
    expect(screen.getByText('Conteúdo')).toBeInTheDocument();
  });

  it('renders actions slot in the header toolbar', () => {
    renderWithProviders(
      <WorkspacePageShell
        title="Meus dados"
        actions={<Button type="button">Novo dataset</Button>}
      >
        <p>Tabela</p>
      </WorkspacePageShell>,
    );

    expect(screen.getByRole('button', { name: 'Novo dataset' })).toBeInTheDocument();
  });

  it('applies Techne page background token (#EFF2F7)', () => {
    const { container } = renderWithProviders(
      <WorkspacePageShell title="Catálogo">
        <p>Cards</p>
      </WorkspacePageShell>,
    );

    const shell = container.firstElementChild as HTMLElement;
    expect(shell).toHaveStyle({ backgroundColor: colors.page });
  });
});
