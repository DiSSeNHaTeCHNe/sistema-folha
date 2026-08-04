import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'

export default tseslint.config(
  { ignores: ['dist'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': [
        'warn',
        { allowConstantExport: true },
      ],
    },
  },
  {
    files: ['src/pages/**/*.{ts,tsx}', 'src/components/**/*.{ts,tsx}'],
    // SPEC_DEVIATION: design.md escopa a regra a src/pages/** e src/components/**
    // sem enumerar exclusoes; aqui src/pages/DashboardCustomizavel/** fica isento.
    // Reason: a PoC esta em .gitignore e nao e roteada — a spec a lista em
    // "Out of Scope" e fora do grep rastreado. Sem esta isenção o lint acusaria 6
    // erros em arquivos que a feature nao pode alterar nem versionar.
    ignores: ['src/pages/DashboardCustomizavel/**'],
    rules: {
      'no-restricted-syntax': [
        'error',
        {
          selector:
            'Literal[value=/#[0-9a-fA-F]{3,8}\\b|rgba?\\(|hsla?\\(/]',
          message:
            'Cor fixa proibida fora de src/theme/. Use o token do tema (palette.*) ou useTheme().',
        },
        {
          selector: "JSXAttribute[name.name='fontWeight']",
          message:
            'fontWeight em prop vence o tema. Defina o peso em theme.typography.* (ver _docs/specs/features/temas-fidelidade-visual/design.md).',
        },
      ],
    },
  },
)
