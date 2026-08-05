import { describe, expect, it, vi } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { WorkspaceEmptyState, WorkspaceSwitcher } from './WorkspaceSwitcher';
import { renderWithProviders } from '../../test/renderWithProviders';

describe('WorkspaceSwitcher', () => {
  const summaries = [
    { id: 1, nome: 'Planejamento', totalWidgets: 2 },
    { id: 2, nome: 'Trimestral', totalWidgets: 1 },
  ];

  it('renders workspace select with options', () => {
    renderWithProviders(
      <WorkspaceSwitcher
        summaries={summaries}
        activeWorkspaceId={1}
        onSwitch={vi.fn()}
        onCreate={vi.fn()}
        onDelete={vi.fn()}
      />,
    );
    expect(screen.getByLabelText('Workspace')).toBeInTheDocument();
    fireEvent.mouseDown(screen.getByLabelText('Workspace'));
    expect(screen.getByRole('option', { name: 'Planejamento' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Trimestral' })).toBeInTheDocument();
  });

  it('calls onSwitch when selection changes', () => {
    const onSwitch = vi.fn();
    renderWithProviders(
      <WorkspaceSwitcher
        summaries={summaries}
        activeWorkspaceId={1}
        onSwitch={onSwitch}
        onCreate={vi.fn()}
        onDelete={vi.fn()}
      />,
    );
    fireEvent.mouseDown(screen.getByLabelText('Workspace'));
    fireEvent.click(screen.getByRole('option', { name: 'Trimestral' }));
    expect(onSwitch).toHaveBeenCalledWith(2);
  });

  it('opens create dialog and submits nome', async () => {
    const onCreate = vi.fn().mockResolvedValue(undefined);
    renderWithProviders(
      <WorkspaceSwitcher
        summaries={summaries}
        activeWorkspaceId={1}
        onSwitch={vi.fn()}
        onCreate={onCreate}
        onDelete={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: /Novo workspace/i }));
    fireEvent.change(screen.getByLabelText('Nome do workspace'), { target: { value: 'Anual' } });
    fireEvent.click(screen.getByRole('button', { name: 'Criar' }));

    await waitFor(() => expect(onCreate).toHaveBeenCalledWith('Anual'));
  });

  it('shows delete control when multiple workspaces exist', async () => {
    const onDelete = vi.fn().mockResolvedValue(undefined);
    renderWithProviders(
      <WorkspaceSwitcher
        summaries={summaries}
        activeWorkspaceId={1}
        onSwitch={vi.fn()}
        onCreate={vi.fn()}
        onDelete={onDelete}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Excluir workspace atual' }));
    fireEvent.click(screen.getByRole('menuitem', { name: /Confirmar exclusão/i }));
    await waitFor(() => expect(onDelete).toHaveBeenCalledWith(1));
  });

  it('disables controls when disabled prop is true', () => {
    renderWithProviders(
      <WorkspaceSwitcher
        summaries={summaries}
        activeWorkspaceId={1}
        onSwitch={vi.fn()}
        onCreate={vi.fn()}
        onDelete={vi.fn()}
        disabled
      />,
    );
    expect(screen.getByRole('button', { name: /Novo workspace/i })).toBeDisabled();
  });

  it('WorkspaceEmptyState announces empty status', () => {
    renderWithProviders(<WorkspaceEmptyState />);
    expect(screen.getByRole('status', { name: 'Nenhum workspace configurado' })).toBeInTheDocument();
    expect(screen.getByText(/Nenhum workspace ainda/i)).toBeInTheDocument();
  });
});
