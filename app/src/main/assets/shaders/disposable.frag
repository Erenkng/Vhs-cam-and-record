// Single-use camera: a plastic lens that goes soft and fringed towards the corners, a flash
// that lights what is in the middle and leaves the rest to fall into the dark, and a cheap
// colour negative printed punchy, with greenish shadows, warm highlights and plenty of grain.
uniform sampler2D uSrc;
uniform vec2 uSrcSize;
uniform float uScale;        // working height / 1200
uniform uint uFrame;
uniform float uFlash;
uniform float uSaturation;
uniform float uContrast;
uniform vec3 uShadowTint;
uniform vec3 uHighlightTint;
uniform float uGrain;
uniform float uGrainSize;
uniform float uSoftness;
uniform float uFringe;
uniform float uVignette;

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec2 px = 1.0 / uSrcSize;
    float aspect = uSrcSize.x / uSrcSize.y;
    vec2 centred = vUv - 0.5;
    vec2 p = centred * vec2(aspect, 1.0);
    float r = length(centred) * 1.414;

    // Plastic lens: colours split and the picture softens towards the corners.
    vec2 dir = centred * r * uFringe * 0.012;
    vec3 col = vec3(
        texture(uSrc, vUv + dir).r,
        texture(uSrc, vUv).g,
        texture(uSrc, vUv - dir).b
    );
    float soft = smoothstep(0.3, 1.0, r) * uSoftness;
    if (soft > 0.01) {
        vec3 blur = vec3(0.0);
        for (int i = 0; i < 8; i++) {
            float angle = float(i) * 0.7853982 + 0.3;
            blur += texture(uSrc, vUv + vec2(cos(angle), sin(angle)) * px * uScale * (2.0 + 6.0 * soft)).rgb;
        }
        col = mix(col, blur / 8.0, min(soft * 1.3, 1.0));
    }

    // The flash: lights the middle, the further out the darker. Bright things up close blow out.
    float lit = exp(-dot(p, p) * 2.2);
    col *= mix(1.0 - 0.5 * uFlash, 1.0 + 0.3 * uFlash, lit);

    // Cheap colour negative, printed punchy by the corner shop's machine.
    col = clamp((col - 0.5) * uContrast + 0.5, 0.0, 1.0);
    col = mix(col, col * col * (3.0 - 2.0 * col), 0.5);
    float gray = luma(col);
    col = mix(vec3(gray), col, uSaturation);
    col *= mix(uShadowTint, uHighlightTint, smoothstep(0.15, 0.75, gray));

    col *= vignette(vUv, uVignette);

    vec2 g = gl_FragCoord.xy / max(uGrainSize * uScale, 0.5);
    float grain = (vnoise2(g, uFrame) - 0.5) * 0.7 + (vnoise2(g * 2.3, uFrame + 7u) - 0.5) * 0.3;
    col += grain * uGrain * (0.6 + 0.7 * (1.0 - gray));

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
