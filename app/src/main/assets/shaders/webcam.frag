// Mid-2000s USB webcam: QVGA sensor with too much gain, clipped highlights, a cold cast,
// smeary 4:2:0 colour, noisy shadows and the 8 x 8 blocks of cheap motion-JPEG compression.
uniform sampler2D uSrc;
uniform vec2 uSrcSize;
uniform uint uFrame;
uniform float uExposure;
uniform float uContrast;
uniform float uSaturation;
uniform vec3 uTint;
uniform float uNoise;
uniform float uBlockiness;
uniform float uVignette;

in vec2 vUv;
out vec4 fragColor;

vec3 at(vec2 uv) {
    return texture(uSrc, uv).rgb;
}

void main() {
    vec2 px = 1.0 / uSrcSize;
    vec2 pixel = vUv * uSrcSize;

    // Colour is stored at half resolution (2 x 2 blocks), luma at full resolution.
    vec2 chromaUv = (floor(pixel / 2.0) * 2.0 + 1.0) * px;
    vec3 chromaSrc = 0.25 * (
        at(chromaUv + vec2(-0.5, -0.5) * px) + at(chromaUv + vec2(0.5, -0.5) * px) +
        at(chromaUv + vec2(-0.5, 0.5) * px) + at(chromaUv + vec2(0.5, 0.5) * px)
    );
    vec3 yiq = vec3(luma(at(vUv)), rgbToYiq(chromaSrc).yz);

    // Motion-JPEG: flat areas collapse towards the mean of their 8 x 8 block.
    vec2 blockOrigin = floor(pixel / 8.0) * 8.0;
    float blockMean = 0.0;
    float blockMin = 1.0;
    float blockMax = 0.0;
    for (int i = 0; i < 4; i++) {
        vec2 o = vec2(float(i & 1), float(i >> 1)) * 4.0 + 2.0;
        float l = luma(at((blockOrigin + o) * px));
        blockMean += l * 0.25;
        blockMin = min(blockMin, l);
        blockMax = max(blockMax, l);
    }
    float flatness = 1.0 - smoothstep(0.04, 0.2, blockMax - blockMin);
    yiq.x = mix(yiq.x, blockMean, uBlockiness * flatness * 0.85);
    yiq.yz *= 1.0 - 0.35 * uBlockiness * flatness;

    vec3 c = yiqToRgb(vec3(yiq.x, yiq.yz * uSaturation));
    c *= uTint;

    // Auto-exposure that never quite copes: lifted mids, hard clipping.
    c = (c * uExposure - 0.5) * uContrast + 0.5;
    c = clamp(c, 0.0, 1.0);

    // Sensor noise, mostly in the shadows, a little coloured.
    uint ux = uint(int(pixel.x));
    uint uy = uint(int(pixel.y));
    float n = hash3(ux, uy, uFrame) - 0.5;
    vec3 cn = vec3(hash3(ux, uy, uFrame + 7u), hash3(ux, uy, uFrame + 13u), hash3(ux, uy, uFrame + 29u)) - 0.5;
    float shadow = 1.0 - smoothstep(0.1, 0.6, luma(c));
    c += (n * 0.8 + cn * 0.5) * uNoise * (0.4 + shadow);

    c *= vignette(vUv, uVignette);
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
