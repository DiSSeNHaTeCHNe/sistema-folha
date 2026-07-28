/** Formata valor monetário serializado (string decimal ou number da API) apenas para exibição. */
export function formatMoneyDisplay(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  const normalized = typeof value === 'string' ? value.replace(',', '.') : String(value);
  const parsed = Number.parseFloat(normalized);

  if (Number.isNaN(parsed)) {
    return '-';
  }

  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(parsed);
}
