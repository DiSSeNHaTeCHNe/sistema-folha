/**
 * Techne brand tokens for Workspace v2 (WKS2-35).
 * Source: gen_mockups_workspace.py palette.
 */
export const colors = {
  navy: '#20284E',
  violet: '#7836FC',
  page: '#EFF2F7',
  line: '#DCE2EE',
  card: '#FFFFFF',
  text: '#111111',
  soft: '#5B6478',
  ok: '#0F6E56',
  okSoft: '#E1F5EE',
  warn: '#8A5200',
  warnSoft: '#FFF1DE',
  danger: '#A32D2D',
  dangerSoft: '#FCEBEB',
  info: '#2A2991',
  infoSoft: '#ECEDFB',
  ai: '#7836FC',
  aiSoft: '#F3EBFF',
} as const;

export type ChipVariant = 'info' | 'ok' | 'warn' | 'danger' | 'ai';

export const chipVariants = {
  info: {
    color: colors.info,
    backgroundColor: colors.infoSoft,
    borderColor: colors.line,
  },
  ok: {
    color: colors.ok,
    backgroundColor: colors.okSoft,
    borderColor: colors.line,
  },
  warn: {
    color: colors.warn,
    backgroundColor: colors.warnSoft,
    borderColor: colors.line,
  },
  danger: {
    color: colors.danger,
    backgroundColor: colors.dangerSoft,
    borderColor: colors.line,
  },
  ai: {
    color: colors.ai,
    backgroundColor: colors.aiSoft,
    borderColor: colors.violet,
  },
} as const satisfies Record<ChipVariant, { color: string; backgroundColor: string; borderColor: string }>;

export type BannerVariant = 'info' | 'warn' | 'danger' | 'ai';

export const bannerVariants = {
  info: {
    color: colors.info,
    backgroundColor: colors.infoSoft,
    borderColor: colors.line,
    role: 'status' as const,
  },
  warn: {
    color: colors.warn,
    backgroundColor: colors.warnSoft,
    borderColor: colors.line,
    role: 'status' as const,
  },
  danger: {
    color: colors.danger,
    backgroundColor: colors.dangerSoft,
    borderColor: colors.line,
    role: 'alert' as const,
  },
  ai: {
    color: colors.ai,
    backgroundColor: colors.aiSoft,
    borderColor: colors.violet,
    role: 'status' as const,
  },
} as const satisfies Record<
  BannerVariant,
  { color: string; backgroundColor: string; borderColor: string; role: 'alert' | 'status' }
>;
