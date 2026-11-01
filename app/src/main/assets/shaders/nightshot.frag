// A 90s camcorder's infrared night mode: the infrared picture in phosphor green, lit by the
// camera's own lamp so the middle is brightest and the edges fall into the dark, lights
// blooming, coarse gain noise and a ghost of the picture trailing beside it. In video the
// renderer also blends in the previous frames, so moving things leave trails.
uniform sampler2D uSrc;
uniform sampler2D uGlow;
uniform vec2 uSrcSize;
uniform float uScale;        // working height / 480
uniform uint uFrame;
uniform float uGain;
uniform float uGlowAmount;
uniform float uGhost;
uniform float uNoise;
uniform float uSpot;

in vec2 vUv;
out vec4 fragColor;

// Infrared: foliage and skin come out bright, red counts most and blue hardly at all.
float infrared(vec3 c) {
    return dot(c, vec3(0.5, 0.42, 0.08));
}

void main() {
    vec2 px = 1.0 / uSrcSize;
    float aspect = uSrcSize.x / uSrcSize.y;
    float ir = infrared(texture(uSrc, vUv).rgb);
    // The ghost: the picture again, faintly, a little to the right.
    float ghost = infrared(texture(uSrc, vUv - vec2(px.x * 10.0 * uScale, 0.0)).rgb);
    ir = ir * (1.0 - 0.3 * uGhost) + ghost * 0.45 * uGhost;

    // The lamp lights the middle.
    vec2 d = (vUv - 0.5) * vec2(aspect, 1.0);
    float lamp = mix(1.0, 0.3 + 0.95 * exp(-dot(d, d) * 3.5), uSpot);
    // The gain lifts everything, and bright parts saturate softly rather than clip.
    float y = 1.0 - exp(-ir * uGain * lamp * 1.8);
    y += luma(texture(uGlow, vUv).rgb) * uGlowAmount * 0.6;

    // Gain noise, a little coarse.
    vec2 cell = floor(vUv * uSrcSize / max(1.5 * uScale, 1.0));
    float n = hash3(uint(int(cell.x)), uint(int(cell.y)), uFrame) - 0.5;
    y += n * uNoise * (1.2 - min(y, 1.0) * 0.6);
    y = clamp(y, 0.0, 1.0);

    // Phosphor green, the brightest parts going pale.
    vec3 col = vec3(0.30, 0.95, 0.40) * y;
    col = mix(col, vec3(0.86, 1.0, 0.86), smoothstep(0.75, 1.0, y) * 0.8);
    col += vec3(0.0, 0.025, 0.01);
    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
