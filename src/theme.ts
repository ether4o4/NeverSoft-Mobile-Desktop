/**
 * MVE palette — committed dark look.
 * Red is the identity + action color (wordmark, send, live). Violet carries the
 * body text; purple is the interactive accent. The terminal is always black.
 */
export const theme = {
  bg: '#0a0810',
  surface: '#100c1a',
  surface2: '#171129',
  border: '#241d38',

  text: '#c9b8f0', // readable violet body text
  textDim: '#8a7fb0',
  textFaint: '#5f5680',

  red: '#e5484d', // MVE wordmark + actions + live
  redSoft: 'rgba(229,72,77,0.16)',

  purple: '#a78bfa', // accent: prompt, divider, interactive
  purpleDim: '#7c5cf0',
  purpleSoft: 'rgba(167,139,250,0.14)',

  // terminal — always black regardless of anything
  termBg: '#08060e',
  termText: '#d8ccf5',
  termDim: '#7c729c',
  termPrompt: '#a78bfa',
  termRed: '#f2687f',

  mono: 'monospace',
};

export type Theme = typeof theme;
