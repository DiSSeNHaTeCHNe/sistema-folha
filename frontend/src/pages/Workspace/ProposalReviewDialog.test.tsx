import { describe, expect, it, vi } from 'vitest';
import { fireEvent, screen } from '@testing-library/react';
import { ProposalReviewDialog } from './ProposalReviewDialog';
import { renderWithProviders } from '../../test/renderWithProviders';
import type { WorkspaceProposal } from './types';

const sampleProposal: WorkspaceProposal = {
  id: 1,
  status: 'PENDENTE',
  solicitanteUsuarioId: 10,
  dataCriacao: '2026-08-05T10:00:00',
  dataExpiracao: '2026-08-08T10:00:00',
  dataResolucao: null,
  payload: {
    kind: 'WIDGET',
    nome: 'Resumo folha',
    tipoWidget: 'KPI',
    formula: 'SOMA(total_liquido)',
    descricao: 'Widget KPI sugerido',
  },
};

const workspaces = [{ id: 5, nome: 'Principal', totalWidgets: 0 }];

describe('ProposalReviewDialog', () => {
  it('shows loading state', () => {
    renderWithProviders(
      <ProposalReviewDialog
        open
        proposal={null}
        workspaces={workspaces}
        loading
        onClose={vi.fn()}
        onConfirm={vi.fn()}
        onDiscard={vi.fn()}
      />,
    );
    expect(screen.getByLabelText('Carregando proposta')).toBeInTheDocument();
  });

  it('shows error message', () => {
    renderWithProviders(
      <ProposalReviewDialog
        open
        proposal={null}
        workspaces={workspaces}
        error="Capacidade não disponível"
        onClose={vi.fn()}
        onConfirm={vi.fn()}
        onDiscard={vi.fn()}
      />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Capacidade não disponível');
  });

  it('renders widget proposal details', () => {
    renderWithProviders(
      <ProposalReviewDialog
        open
        proposal={sampleProposal}
        workspaces={workspaces}
        onClose={vi.fn()}
        onConfirm={vi.fn()}
        onDiscard={vi.fn()}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Revisar proposta' })).toBeInTheDocument();
    expect(screen.getByText(/Novo widget: Resumo folha/)).toBeInTheDocument();
    expect(screen.getByText(/SOMA\(total_liquido\)/)).toBeInTheDocument();
  });

  it('renders dataset fields when proposal is DATASET', () => {
    const datasetProposal: WorkspaceProposal = {
      ...sampleProposal,
      payload: {
        kind: 'DATASET',
        nome: 'Previsão',
        campos: [{ nome: 'competencia', tipo: 'DATA', obrigatorio: true }],
        descricao: 'Dataset sugerido',
      },
    };
    renderWithProviders(
      <ProposalReviewDialog
        open
        proposal={datasetProposal}
        workspaces={workspaces}
        onClose={vi.fn()}
        onConfirm={vi.fn()}
        onDiscard={vi.fn()}
      />,
    );
    expect(screen.getByText(/competencia \(DATA\)/)).toBeInTheDocument();
  });

  it('requires workspace for template install proposals', () => {
    const templateProposal: WorkspaceProposal = {
      ...sampleProposal,
      payload: {
        kind: 'TEMPLATE_INSTALL',
        nome: 'Orçamento CC',
        templateId: 2,
        descricao: 'Instalar template',
      },
    };
    renderWithProviders(
      <ProposalReviewDialog
        open
        proposal={templateProposal}
        workspaces={workspaces}
        onClose={vi.fn()}
        onConfirm={vi.fn()}
        onDiscard={vi.fn()}
      />,
    );
    expect(screen.getByRole('combobox', { name: 'Workspace destino' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Confirmar' })).not.toBeDisabled();
  });

  it('calls onConfirm without workspace for widget proposal', () => {
    const onConfirm = vi.fn();
    renderWithProviders(
      <ProposalReviewDialog
        open
        proposal={sampleProposal}
        workspaces={workspaces}
        onClose={vi.fn()}
        onConfirm={onConfirm}
        onDiscard={vi.fn()}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }));
    expect(onConfirm).toHaveBeenCalledWith(undefined);
  });

  it('calls onConfirm with workspace id for template install', () => {
    const onConfirm = vi.fn();
    const templateProposal: WorkspaceProposal = {
      ...sampleProposal,
      payload: {
        kind: 'TEMPLATE_INSTALL',
        nome: 'Orçamento CC',
        templateId: 2,
      },
    };
    renderWithProviders(
      <ProposalReviewDialog
        open
        proposal={templateProposal}
        workspaces={workspaces}
        onClose={vi.fn()}
        onConfirm={onConfirm}
        onDiscard={vi.fn()}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }));
    expect(onConfirm).toHaveBeenCalledWith(5);
  });

  it('calls onDiscard when user discards', () => {
    const onDiscard = vi.fn();
    renderWithProviders(
      <ProposalReviewDialog
        open
        proposal={sampleProposal}
        workspaces={workspaces}
        onClose={vi.fn()}
        onConfirm={vi.fn()}
        onDiscard={onDiscard}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Descartar' }));
    expect(onDiscard).toHaveBeenCalledOnce();
  });

  it('disables actions while submitting', () => {
    renderWithProviders(
      <ProposalReviewDialog
        open
        proposal={sampleProposal}
        workspaces={workspaces}
        submitting
        onClose={vi.fn()}
        onConfirm={vi.fn()}
        onDiscard={vi.fn()}
      />,
    );
    expect(screen.getByRole('button', { name: 'Confirmar' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Descartar' })).toBeDisabled();
  });
});
