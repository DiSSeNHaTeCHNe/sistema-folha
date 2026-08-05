import { FormControl, InputLabel, MenuItem, Select, type SelectChangeEvent } from '@mui/material';
import { useEffect, useState } from 'react';
import { resumoFolhaPagamentoService } from '../../services/resumoFolhaPagamentoService';

const MAIS_RECENTE_VALUE = '';

interface CompetenciaSelectorProps {
  value: string | null;
  onChange: (competencia: string | null) => void;
}

function formatCompetenciaLabel(competencia: string): string {
  const [year, month] = competencia.split('-');
  const monthNames = [
    'Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun',
    'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez',
  ];
  const monthIndex = Number(month) - 1;
  return `${monthNames[monthIndex] ?? month}/${year}`;
}

function toCompetenciaKey(competenciaInicio: string): string {
  return competenciaInicio.slice(0, 7);
}

export function CompetenciaSelector({ value, onChange }: CompetenciaSelectorProps) {
  const [opcoes, setOpcoes] = useState<string[]>([]);

  useEffect(() => {
    let cancelled = false;
    async function carregar() {
      try {
        const resumos = await resumoFolhaPagamentoService.listarMaisRecentes();
        if (cancelled) {
          return;
        }
        const unique = [...new Set(resumos.map((r) => toCompetenciaKey(r.competenciaInicio)))];
        setOpcoes(unique.sort((a, b) => b.localeCompare(a)));
      } catch {
        if (!cancelled) {
          setOpcoes([]);
        }
      }
    }
    void carregar();
    return () => {
      cancelled = true;
    };
  }, []);

  const handleChange = (event: SelectChangeEvent<string>) => {
    const selected = event.target.value;
    onChange(selected === MAIS_RECENTE_VALUE ? null : selected);
  };

  return (
    <FormControl size="small" sx={{ minWidth: 200 }}>
      <InputLabel id="competencia-global-label">Competência</InputLabel>
      <Select
        labelId="competencia-global-label"
        id="competencia-global"
        value={value ?? MAIS_RECENTE_VALUE}
        label="Competência"
        displayEmpty
        renderValue={(selected) => {
          if (!selected || selected === MAIS_RECENTE_VALUE) {
            return 'Mais recente';
          }
          return formatCompetenciaLabel(selected);
        }}
        onChange={handleChange}
        aria-label="Seletor global de competência"
      >
        <MenuItem value={MAIS_RECENTE_VALUE}>Mais recente</MenuItem>
        {opcoes.map((competencia) => (
          <MenuItem key={competencia} value={competencia}>
            {formatCompetenciaLabel(competencia)}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
