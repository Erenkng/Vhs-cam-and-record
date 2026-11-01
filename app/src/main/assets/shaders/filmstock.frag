// Film stocks (Gold, Portrait, Cine 800, Red Grain, Redscale, Silver 400, Aerochrome,
// cross-processing, Slide 50). Works the way a scan of the film looks: the stock's colour
// response in linear light, halation where light bounced back off the film base, a soft
// shoulder and an S-curve for the print's contrast, split toning, and grain made of dye
// clouds - coloured, mostly red, strongest in the shadows. A plastic lens can soften the
// corners, and a slide can sit in its mount.
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
uniform float uSoftness;     // plastic lens: blur towards the corners
uniform float uMount;        // slide mount: a dark frame with rounded corners

in vec2 vUv;
out vec4 fragColor;

// Signed distance to a rounded rectangle (negative inside).
float roundedBox(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

// The picture through the lens: a cheap plastic one smears it towards the corners.
vec3 lens(vec2 uv) {
    vec3 c = texture(uSrc, uv).rgb;
    if (uSoftness <= 0.0) return c;
    float aspect = uSrcSize.x / uSrcSize.y;
    float r = length((uv - 0.5) * vec2(aspect, 1.0));
    vec2 px = 1.0 / uSrcSize;
    float blur = uSoftness * smoothstep(0.2, 0.75, r) * 9.0 * uScale;
    c *= 0.4;
    for (int i = 0; i < 6; i++) {
        float a = float(i) * 1.0471976 + 0.3;
        c += texture(uSrc, uv + vec2(cos(a), sin(a)) * px * blur).rgb * 0.1;
    }
    return c;
}

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
    vec3 src = lens(vUv);
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

    // A slide in its mount: the picture's rounded window in dark plastic.
    if (uMount > 0.0) {
        float aspect = uSrcSize.x / uSrcSize.y;
        vec2 p = (vUv - 0.5) * vec2(aspect, 1.0);
        float m = 0.045 * uMount;
        float d = roundedBox(p, vec2(aspect * 0.5 - m, 0.5 - m), 0.03 * uMount + 0.002);
        float window = 1.0 - smoothstep(-0.002, 0.002, d);
        vec3 plastic = vec3(0.03, 0.028, 0.027) + (vnoise2(gl_FragCoord.xy * 0.5, 9u) - 0.5) * 0.012;
        col = mix(plastic, col, window);
    }

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
