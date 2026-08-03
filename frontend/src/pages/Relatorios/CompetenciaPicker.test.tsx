import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { CompetenciaPicker, competenciaFromDate } from './CompetenciaPicker';
import { RelatorioStatusBadge } from './RelatorioStatusBadge';
import { RelatorioCatalogCard } from './RelatorioCatalogCard';
import AssessmentIcon from '@mui/icons-material/Assessment';

describe('CompetenciaPicker', () => {
  it('renders with aria-label for accessibility', () => {
    render(
      <CompetenciaPicker value={{ mes: 6, ano: 2026 }} onChange={() => {}} />,
    );
    expect(screen.getByLabelText('Selecionar competência mês e ano')).toBeInTheDocument();
  });

  it('maps Date to { mes, ano } payload', () => {
    expect(competenciaFromDate(new Date(2024, 0, 15))).toEqual({ mes: 1, ano: 2024 });
    expect(competenciaFromDate(new Date(2026, 5, 1))).toEqual({ mes: 6, ano: 2026 });
    expect(competenciaFromDate(null)).toBeNull();
  });

  it('emits { mes, ano } through onChange when parent updates selection', () => {
    function Harness() {
      const [competencia, setCompetencia] = useState({ mes: 6, ano: 2026 });
      return (
        <>
          <CompetenciaPicker value={competencia} onChange={setCompetencia} />
          <p>
            Selecionado: {competencia.mes}/{competencia.ano}
          </p>
        </>
      );
    }
    render(<Harness />);
    expect(screen.getByText('Selecionado: 6/2026')).toBeInTheDocument();
  });
});

describe('RelatorioStatusBadge', () => {
  it.each([
    ['PENDENTE', 'Pendente'],
    ['PROCESSADO', 'Processado'],
    ['ERRO', 'Erro'],
  ] as const)('renders %s status with label %s', (status, label) => {
    render(<RelatorioStatusBadge status={status} />);
    expect(screen.getByLabelText(`Status do relatório: ${label}`)).toBeInTheDocument();
  });
});

describe('RelatorioCatalogCard', () => {
  it('renders generate action with aria-label when no status', () => {
    render(
      <RelatorioCatalogCard
        title="Executivo de Folha"
        description="PDF com KPIs e breakdowns."
        icon={<AssessmentIcon />}
        onGenerate={() => {}}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Executivo de Folha' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Gerar Executivo de Folha' })).toBeInTheDocument();
  });

  it('shows pending state and disables re-generation', () => {
    render(
      <RelatorioCatalogCard
        title="Executivo de Folha"
        description="PDF com KPIs."
        icon={<AssessmentIcon />}
        status="PENDENTE"
        onGenerate={() => {}}
      />,
    );
    expect(screen.getByRole('status')).toHaveTextContent('Gerando relatório');
    expect(screen.getByRole('button', { name: /aguardando processamento/i })).toBeDisabled();
  });

  it('shows download action when processed', () => {
    const onDownload = vi.fn();
    render(
      <RelatorioCatalogCard
        title="Executivo de Folha"
        description="PDF com KPIs."
        icon={<AssessmentIcon />}
        status="PROCESSADO"
        onGenerate={() => {}}
        onDownload={onDownload}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Baixar PDF Executivo de Folha' }));
    expect(onDownload).toHaveBeenCalled();
  });

  it('shows retry action when error', () => {
    const onRetry = vi.fn();
    render(
      <RelatorioCatalogCard
        title="Executivo de Folha"
        description="PDF com KPIs."
        icon={<AssessmentIcon />}
        status="ERRO"
        erro="Falha ao gerar relatório"
        onGenerate={() => {}}
        onRetry={onRetry}
      />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Falha ao gerar relatório');
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente Executivo de Folha' }));
    expect(onRetry).toHaveBeenCalled();
  });
});
