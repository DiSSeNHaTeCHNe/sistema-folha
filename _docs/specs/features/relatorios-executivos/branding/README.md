# Identidade visual Techne — assets para Relatórios Executivos

Assets extraídos de [techne.com.br](https://techne.com.br/) em 2026-08-03 para uso nos PDFs e na UI da feature **Relatórios Executivos**.

> **Nota:** estes arquivos são referência de design. Para produção, copiar `logo-techne.png` para `backend/src/main/resources/branding/logo.png` conforme `spec.md` (P2: Configuração de marca).

---

## Arquivos

| Arquivo | Uso sugerido | Dimensões |
| --- | --- | --- |
| `logo-techne.png` | Capa do PDF (versão colorida, fundo transparente) | 309 × 85 px |
| `logo-techne.webp` | Original do site (WebP) | 309 × 85 px |
| `logo-techne-monochrome-white.png` | Rodapé / fundos escuros | 309 × 85 px |
| `logo-techne-monochrome-dark.png` | Rodapé / fundos claros (`#273340`) | 309 × 85 px |
| `favicon.png` | Ícone reduzido / cabeçalho de páginas internas | 32 × 32 px |
| `logo-ergon.svg` | Referência de produto Ergon (contexto folha) | vetorial |
| `logo-lyceum.svg` | Referência de produto Lyceum (não usado nesta feature) | vetorial |

---

## Paleta institucional (site techne.com.br)

A spec original assumia `#1976d2` / `#dc004e` (MUI padrão). O site institucional usa **roxo/azul escuro**:

| Token | Hex | Uso no site |
| --- | --- | --- |
| Primária (accent) | `#7836FC` | Botões, destaques, barras de menu |
| Primária (links/números) | `#3661FC` | KPIs "Números que falam por nós", links footer |
| Accent claro | `#a98bff` | Hover menu, destaque em títulos hero |
| Accent kicker | `#b79aff` | Subtítulo hero ("Simplificar a vida das pessoas") |
| Secundária (índigo) | `#2A2991` | Seção apresentação mobile |
| Secundária (botões) | `#4c57d6` | CTAs, bordas |
| Fundo hero/header escuro | `#0f1524` | Hero e header no topo da página |
| Fundo footer | `#120851` | Rodapé |
| Texto escuro | `#273340` | Menu scrolled, corpo |
| Texto secundário | `#334155` / `#20284e` | Parágrafos |
| Texto hero | `#cdd1dc` | Descrição hero |
| Neutro claro | `#f8fafc` | *(manter da spec — alinhado ao app)* |
| Logo (azul marca) | `#2B428D` | Cor predominante no wordmark TECHNE |

### Recomendação para PDFs executivos

```yaml
relatorios.branding:
  primary-color: "#7836FC"    # capa, cabeçalhos, KPI highlights
  secondary-color: "#3661FC"  # gráficos, links, números
  background-dark: "#0f1524"  # capa alternativa (modo escuro premium)
  text-primary: "#273340"
  text-on-dark: "#ffffff"
```

---

## Tipografia

| Contexto | Fonte | Pesos |
| --- | --- | --- |
| Site institucional (Adobe Typekit) | **New Rubrik** | 300, 400, 700 |
| Site institucional (fallback CSS) | **Poppins** | 300, 400, 700 |
| App Sistema de Folha (MUI) | Roboto / system | — |

Para PDFs no backend Java, usar fallback sans-serif próximo: **Rubik** (Google Fonts, livre) ou **Helvetica/Arial** se embed de fonte não for viável no MVP.

---

## Tagline e textos de referência

- **Tagline:** "Simplificar a vida das pessoas"
- **Hero:** "Otimizamos e automatizamos processos"
- **Rodapé PDF (spec):** "Gerado pelo Sistema de Folha — Techne"
- **Empresa:** Techne Engenharia e Sistemas LTDA

---

## Proveniência

| Asset | URL de origem |
| --- | --- |
| Logo principal | https://techne.com.br/img/Logo-Techne.webp |
| Favicon | https://techne.com.br/img/favicon.png |
| Logo Ergon | https://techne.com.br/img/logo-ergon.svg |
| Logo Lyceum | https://techne.com.br/img/logo-lyceum.svg |
| CSS (cores) | https://techne.com.br/css/style.css |
| Fontes | https://use.typekit.net/gax5syd.css (New Rubrik) |

---

## Checklist spec (Assumptions L47–51)

- [x] Logo principal — PNG transparente, ≥ 300 px largura (`logo-techne.png`)
- [x] Logo monocromático — branco e escuro para rodapé
- [x] Ícone reduzido — `favicon.png`
- [x] Cores institucionais documentadas — **diferem** dos defaults `#1976d2` / `#dc004e` da spec
