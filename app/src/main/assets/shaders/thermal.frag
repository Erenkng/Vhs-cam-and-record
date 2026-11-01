// A camera that prints on till roll: one-bit dots from a thermal print head, laid out in an
// ordered dither, dark grey rather than black where the paper is printed, warm white paper
// with its fibres, and faint streaks where the head ran a little cool. 384 dots across, like
// a 58 mm receipt printer; the receipt around it is added when the photo is saved.
uniform sampler2D uSrc;
uniform vec2 uSrcSize;
uniform float uContrast;
uniform float uBrightness;
uniform float uDither;
uniform float uPaper;
uniform float uFade;

in vec2 vUv;
out vec4 fragColor;

// Ordered dither thresholds in [0, 1): 2 x 2, 4 x 4 and 8 x 8 Bayer matrices.
float bayer2(vec2 a) {
    a = floor(a);
    return fract(dot(a, vec2(0.5, a.y * 0.75)));
}

float bayer4(vec2 a) {
    return bayer2(0.5 * a) * 0.25 + bayer2(a);
}

float bayer8(vec2 a) {
    return bayer4(0.5 * a) * 0.25 + bayer2(a);
}

void main() {
    vec2 px = 1.0 / uSrcSize;
    vec2 pixel = vUv * uSrcSize;
    float y = luma(texture(uSrc, vUv).rgb);
    // A little sharpening, so detail survives as dots.
    float around = 0.25 * (
        luma(texture(uSrc, vUv + vec2(px.x, 0.0)).rgb) + luma(texture(uSrc, vUv - vec2(px.x, 0.0)).rgb) +
        luma(texture(uSrc, vUv + vec2(0.0, px.y)).rgb) + luma(texture(uSrc, vUv - vec2(0.0, px.y)).rgb)
    );
    y += (y - around) * 0.8;
    y = clamp((y - 0.5) * uContrast + 0.5 + uBrightness, 0.0, 1.0);

    float threshold = mix(0.5, bayer8(pixel), uDither);
    float printed = step(y, threshold);

    // Warm white paper with fibres; the print dark grey and a bit uneven along the head.
    float fibre = vnoise2(pixel * vec2(0.6, 0.15), 3u) - 0.5;
    vec3 paper = vec3(0.96, 0.95, 0.91) * (1.0 + fibre * 0.06 * uPaper);
    float streak = vnoise(pixel.x * 0.35, 7u) - 0.5;
    float ink = clamp(1.0 - uFade - streak * 0.25 * uPaper, 0.0, 1.0);
    vec3 col = mix(paper, mix(paper, vec3(0.12, 0.12, 0.14), ink), printed);

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
