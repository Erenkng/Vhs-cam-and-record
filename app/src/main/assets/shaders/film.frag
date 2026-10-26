// Disposable 35 mm camera: warm, slightly faded colour negative look with halation around
// highlights, soft plastic-lens corners, vignetting, a light leak and grain.
uniform sampler2D uSrc;
uniform sampler2D uGlow;
uniform vec2 uSrcSize;
uniform float uScale;        // working height / 1600
uniform uint uFrame;
uniform float uSaturation;
uniform float uContrast;
uniform float uFade;
uniform vec3 uWarm;
uniform float uGrain;
uniform float uGrainSize;
uniform float uHalation;
uniform float uLeak;
uniform float uLeakSeed;
uniform float uVignette;
uniform float uCornerSoftness;

in vec2 vUv;
out vec4 fragColor;

vec3 lightLeak(vec2 uv, float seed) {
    float side = fract(seed * 7.13);
    float along = side < 0.5 ? uv.x : 1.0 - uv.x;
    if (fract(seed * 3.31) > 0.7) {
        along = side < 0.5 ? uv.y : 1.0 - uv.y;
    }
    float across = side < 0.5 ? uv.y : uv.x;
    float wobble = 0.08 * sin(across * 5.0 + seed * 10.0) + 0.05 * sin(across * 13.0 + seed * 3.0);
    float falloff = mix(2.8, 6.0, fract(seed * 5.77));
    float a = exp(-(along + wobble) * falloff);
    vec3 warm = vec3(1.0, 0.42, 0.08);
    vec3 pink = vec3(1.0, 0.18, 0.32);
    return mix(warm, pink, fract(seed * 11.3)) * a;
}

vec3 filmCurve(vec3 c, float contrast) {
    // Gentle S-curve that rolls highlights off instead of clipping them.
    vec3 s = c * c * (3.0 - 2.0 * c);
    return mix(c, s, clamp(contrast - 0.6, 0.0, 1.0));
}

void main() {
    vec2 px = 1.0 / uSrcSize;
    vec2 centred = vUv - 0.5;
    float r = length(centred) * 1.414;

    vec3 col = texture(uSrc, vUv).rgb;
    float soft = smoothstep(0.35, 1.0, r) * uCornerSoftness;
    if (soft > 0.01) {
        vec3 blur = vec3(0.0);
        for (int i = 0; i < 8; i++) {
            float angle = float(i) * 0.7853982;
            blur += texture(uSrc, vUv + vec2(cos(angle), sin(angle)) * px * 3.0 * uScale * (1.0 + 3.0 * soft)).rgb;
        }
        col = mix(col, blur / 8.0, min(soft * 1.4, 1.0));
    }

    vec3 glow = texture(uGlow, vUv).rgb;
    col += glow * vec3(1.0, 0.32, 0.12) * uHalation * 1.6;

    col = filmCurve(clamp(col, 0.0, 1.0), uContrast);
    col = uFade + col * (1.0 - uFade * 1.5);
    col *= uWarm;
    float gray = luma(col);
    col = mix(vec3(gray), col, uSaturation);

    col *= vignette(vUv, uVignette);

    // Light leaks fog the film before development, so they sit on top of the vignette.
    vec3 leak = lightLeak(vUv, uLeakSeed) * uLeak * 1.7;
    col = 1.0 - (1.0 - col) * (1.0 - clamp(leak, 0.0, 1.0));

    vec2 g = gl_FragCoord.xy / max(uGrainSize * uScale, 0.5);
    float grain = (vnoise2(g, uFrame) - 0.5) * 0.7 + (vnoise2(g * 2.3, uFrame + 7u) - 0.5) * 0.3;
    col += grain * uGrain * (0.55 + 0.6 * (1.0 - gray));

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
