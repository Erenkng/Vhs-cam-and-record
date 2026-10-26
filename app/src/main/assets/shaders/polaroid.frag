// Instant film: soft, low-contrast, faded colour with a cool-green/warm cast, bloom,
// vignetting and uneven development near the borders.
uniform sampler2D uSrc;
uniform sampler2D uGlow;
uniform vec2 uSrcSize;
uniform float uScale;        // working height / 1600
uniform float uSeed;
uniform float uSaturation;
uniform float uContrast;
uniform float uFade;
uniform vec3 uTint;
uniform float uSoftness;
uniform float uBloom;
uniform float uChemistry;
uniform float uVignette;

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec2 px = 1.0 / uSrcSize;
    vec3 col = texture(uSrc, vUv).rgb * 0.36;
    for (int i = 0; i < 8; i++) {
        float angle = float(i) * 0.7853982 + 0.39;
        col += texture(uSrc, vUv + vec2(cos(angle), sin(angle)) * px * uSoftness * uScale * 1.5).rgb * 0.08;
    }

    col += texture(uGlow, vUv).rgb * uBloom * 1.4;
    col = (col - 0.5) * uContrast + 0.5;
    col = uFade + col * (1.0 - uFade * 1.3);
    // Instant film rolls highlights off early.
    col = 1.0 - exp(-col * 1.9) * 0.98;
    float gray = luma(col);
    col = mix(vec3(gray), col, uSaturation) * uTint;

    vec2 centred = vUv - 0.5;
    float edge = max(abs(centred.x), abs(centred.y)) * 2.0;
    uint seed = uint(uSeed * 1000.0);
    float chem = smoothstep(0.72, 1.0, edge + (vnoise2(vUv * 7.0, seed) - 0.5) * 0.25);
    col = mix(col, col * vec3(0.92, 1.0, 1.06) * 0.82 + vec3(0.03, 0.02, 0.0), chem * uChemistry);

    col *= vignette(vUv, uVignette);
    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
