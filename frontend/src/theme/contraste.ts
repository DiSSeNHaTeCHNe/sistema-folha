function parseHex(hex: string): [number, number, number] {
  const normalized = hex.replace('#', '').trim();
  if (normalized.length === 3) {
    const r = parseInt(normalized[0] + normalized[0], 16);
    const g = parseInt(normalized[1] + normalized[1], 16);
    const b = parseInt(normalized[2] + normalized[2], 16);
    return [r, g, b];
  }
  const r = parseInt(normalized.slice(0, 2), 16);
  const g = parseInt(normalized.slice(2, 4), 16);
  const b = parseInt(normalized.slice(4, 6), 16);
  return [r, g, b];
}

function rgbToHex([r, g, b]: [number, number, number]): string {
  return `#${[r, g, b].map((c) => c.toString(16).padStart(2, '0')).join('')}`;
}

function blend(
  fg: [number, number, number],
  alpha: number,
  bg: [number, number, number],
): [number, number, number] {
  return [
    Math.round(fg[0] * alpha + bg[0] * (1 - alpha)),
    Math.round(fg[1] * alpha + bg[1] * (1 - alpha)),
    Math.round(fg[2] * alpha + bg[2] * (1 - alpha)),
  ];
}

function resolveColor(color: string, backdrop = '#ffffff'): [number, number, number] {
  const rgbaMatch = color.match(
    /rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)(?:\s*,\s*([\d.]+))?\s*\)/,
  );
  if (rgbaMatch) {
    const rgb: [number, number, number] = [
      Number(rgbaMatch[1]),
      Number(rgbaMatch[2]),
      Number(rgbaMatch[3]),
    ];
    const alpha = rgbaMatch[4] !== undefined ? Number(rgbaMatch[4]) : 1;
    if (alpha < 1) {
      return blend(rgb, alpha, parseHex(backdrop));
    }
    return rgb;
  }
  return parseHex(color);
}

function luminanciaRelativaRgb([r, g, b]: [number, number, number]): number {
  const canal = [r, g, b].map((valor) => {
    const srgb = valor / 255;
    return srgb <= 0.03928 ? srgb / 12.92 : ((srgb + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * canal[0] + 0.7152 * canal[1] + 0.0722 * canal[2];
}

/** WCAG 2.1 contrast ratio between foreground and background colors. */
export function razaoContraste(fg: string, bg: string): number {
  const bgRgb = resolveColor(bg);
  const bgHex = rgbToHex(bgRgb);
  const fgRgb = resolveColor(fg, bgHex);

  const l1 = luminanciaRelativaRgb(fgRgb);
  const l2 = luminanciaRelativaRgb(bgRgb);
  const lighter = Math.max(l1, l2);
  const darker = Math.min(l1, l2);
  return (lighter + 0.05) / (darker + 0.05);
}

export const RAZAO_MINIMA_AA = 4.5;
