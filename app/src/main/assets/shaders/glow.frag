// Highlight glow at reduced resolution: halation for film (red bleed in the emulsion),
// bloom for instant film.
uniform sampler2D uSrc;
uniform vec2 uSrcSize;
uniform float uThreshold;
uniform float uRadius;        // in source pixels

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec2 px = 1.0 / uSrcSize;
    vec3 acc = vec3(0.0);
    float weightSum = 0.0;
    // Two rings of taps plus the centre: a cheap wide blur.
    for (int i = 0; i < 17; i++) {
        float fi = float(i);
        float ring = i == 0 ? 0.0 : (i <= 8 ? 0.5 : 1.0);
        float angle = fi * 2.3999632;   // golden angle
        vec2 o = vec2(cos(angle), sin(angle)) * ring * uRadius * px;
        vec3 c = texture(uSrc, vUv + o).rgb;
        float w = 1.0 - ring * 0.5;
        acc += max(c - uThreshold, 0.0) * w;
        weightSum += w;
    }
    fragColor = vec4(acc / weightSum / max(1.0 - uThreshold, 0.05), 1.0);
}
