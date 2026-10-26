// Mirrors the defaults in core/src/main/kotlin/.../look (VhsPresets, PhotoLookPresets) so the
// lab renders what the app renders. Keep the two in sync when tuning.

export const VHS = {
  SP: {
    lumaBlur: 1.1, sharpen: 0.55, chromaBlur: 5.0, chromaShift: 2.0,
    chromaNoise: 0.035, lumaNoise: 0.035, jitter: 0.5, tracking: 0.12,
    headSwitch: 0.8, dropouts: 0.25, saturation: 0.92, contrast: 0.92,
    blackLift: 0.045, tint: [1.0, 0.985, 0.97], vignette: 0.25, scanlines: 0.10,
  },
  LP: {
    lumaBlur: 1.45, sharpen: 0.65, chromaBlur: 6.5, chromaShift: 2.8,
    chromaNoise: 0.06, lumaNoise: 0.06, jitter: 0.85, tracking: 0.25,
    headSwitch: 1.0, dropouts: 0.5, saturation: 0.86, contrast: 0.9,
    blackLift: 0.06, tint: [1.0, 0.98, 0.95], vignette: 0.28, scanlines: 0.12,
  },
  EP: {
    lumaBlur: 1.9, sharpen: 0.75, chromaBlur: 8.5, chromaShift: 3.8,
    chromaNoise: 0.09, lumaNoise: 0.09, jitter: 1.3, tracking: 0.45,
    headSwitch: 1.2, dropouts: 0.9, saturation: 0.8, contrast: 0.88,
    blackLift: 0.075, tint: [1.0, 0.975, 0.93], vignette: 0.3, scanlines: 0.14,
  },
};

export const CCD = {
  saturation: 1.22, contrast: 1.1, highlightKnee: 0.82, fringe: 1.6, sharpen: 0.7,
  noise: 0.05, tint: [0.97, 1.0, 1.06], vignette: 0.12, jpegQuality: 42,
};

export const FILM = {
  saturation: 0.92, contrast: 0.96, fade: 0.07, warm: [1.06, 1.0, 0.88], grain: 0.085,
  grainSize: 1.6, halation: 0.38, lightLeak: 0.55, vignette: 0.38, cornerSoftness: 0.5,
};

export const POLAROID = {
  saturation: 0.74, contrast: 0.84, fade: 0.12, tint: [1.02, 1.03, 0.94], softness: 1.4,
  bloom: 0.25, chemistry: 0.4, vignette: 0.32,
};
