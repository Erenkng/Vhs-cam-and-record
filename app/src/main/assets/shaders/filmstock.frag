// Colour film stocks (Gold, Portrait, Cine 800, Red grain, Redscale). Works the way a scan of
// the negative looks: the stock's colour response in linear light, red halation where light
// bounced back off the film base, a soft shoulder and an S-curve for the print's contrast,
// split toning, and grain made of dye clouds - coloured, mostly red, strongest in the shadows.
uniform sampler2D uSrc;
uniform sampler2D uGlow;
uniform vec2 uSrcSize;
uniform float uScale;        // working height / 1600
uniform uint uFrame;
uniform float uExposure;     // stops
uniform float uContrast;
uniform float uFade;
uniform mat3 uMatrix;        // colour response, linear light
uniform float uSaturation;
uniform vec3 uShadowTint;
uniform vec3 uHighlightTint;
uniform float uHalation;
uniform vec3 uHalationTint;
uniform float uGrain;
uniform float uGrainSize;
uniform float uRedGrain;
uniform float uVignette;

in vec2 vUv;
out vec4 fragColor;

// Highlights roll off instead of clipping: linear light above 0.75 approaches 1.05.
vec3 shoulder(vec3 linear) {
    vec3 over = max(linear - 0.75, 0.0);
    return min(linear, vec3(0.75)) + over / (1.0 + over / 0.3);
}

// Contrast as an S-curve pivoting on the middle: 1 leaves the picture alone.
vec3 sCurve(vec3 x, float contrast) {
    vec3 a = pow(clamp(x, 0.0, 1.0), vec3(contrast));
    vec3 b = pow(clamp(1.0 - x, 0.0, 1.0), vec3(contrast));
    return a / max(a + b, vec3(1e-5));
}

void main() {
    vec3 src = texture(uSrc, vUv).rgb;
    vec3 linear = pow(src, vec3(2.2)) * exp2(uExposure);
    linear = max(uMatrix * linear, 0.0);

    // Halation: bright light goes through the emulsion, reflects off the base and comes back
    // as a red-orange glow around highlights.
    vec3 glow = texture(uGlow, vUv).rgb;
    linear += pow(glow, vec3(2.2)) * uHalationTint * uHalation * 2.4;

    vec3 col = sCurve(pow(shoulder(linear), vec3(1.0 / 2.2)), uContrast);

    float gray = luma(col);
    col = mix(vec3(gray), col, uSaturation);
    col *= mix(uShadowTint, uHighlightTint, smoothstep(0.1, 0.8, gray));
    col = uFade + col * (1.0 - uFade);
    col *= vignette(vUv, uVignette);

    // Grain: a fine silver-like part in luma, and dye clouds that are larger and coloured.
    vec2 g = gl_FragCoord.xy / max(uGrainSize * uScale, 0.5);
    float fine = (vnoise2(g, uFrame) - 0.5) * 0.7 + (vnoise2(g * 2.3, uFrame + 7u) - 0.5) * 0.3;
    vec2 c = g * 0.5;
    vec3 dye = vec3(
        vnoise2(c, uFrame + 101u),
        vnoise2(c + 17.3, uFrame + 211u),
        vnoise2(c + 41.7, uFrame + 307u)
    );
    // Red dye clouds dominate, and their densest clumps show as red specks.
    vec3 clouds = (dye - 0.5) * vec3(1.0, 0.35, 0.55);
    clouds.r += max(dye.r - 0.6, 0.0) * 2.2;
    float density = 0.35 + 1.6 * gray * (1.0 - gray) + 0.35 * (1.0 - gray);
    col += (vec3(fine) * (1.0 - 0.5 * uRedGrain) + clouds * 1.7 * uRedGrain) * uGrain * density;

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
