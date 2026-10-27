// 8-bit handheld camera (Game Boy Camera style). The working frame is already tiny (about
// 128 x 112 pixels); this pass adds the camera's strong edge enhancement, quantises to four
// shades with ordered dithering and maps them to the green LCD palette. The present pass
// magnifies it with nearest-neighbour sampling so every pixel stays a crisp square.
uniform sampler2D uSrc;
uniform vec2 uSrcSize;
uniform float uContrast;
uniform float uBrightness;
uniform float uEdge;
uniform float uDither;
uniform vec3 uShade0;   // darkest
uniform vec3 uShade1;
uniform vec3 uShade2;
uniform vec3 uShade3;   // lightest

in vec2 vUv;
out vec4 fragColor;

const float BAYER4[16] = float[16](
    0.0, 8.0, 2.0, 10.0,
    12.0, 4.0, 14.0, 6.0,
    3.0, 11.0, 1.0, 9.0,
    15.0, 7.0, 13.0, 5.0
);

float lumaAt(vec2 uv) {
    return luma(texture(uSrc, uv).rgb);
}

void main() {
    vec2 px = 1.0 / uSrcSize;
    float c = lumaAt(vUv);
    float around = 0.25 * (
        lumaAt(vUv + vec2(px.x, 0.0)) + lumaAt(vUv - vec2(px.x, 0.0)) +
        lumaAt(vUv + vec2(0.0, px.y)) + lumaAt(vUv - vec2(0.0, px.y))
    );
    c += uEdge * (c - around) * 2.0;
    c = (c - 0.5) * uContrast + 0.5 + uBrightness;

    ivec2 p = ivec2(floor(vUv * uSrcSize));
    float threshold = (BAYER4[(p.y & 3) * 4 + (p.x & 3)] + 0.5) / 16.0 - 0.5;
    float level = clamp(floor(clamp(c, 0.0, 1.0) * 3.0 + 0.5 + threshold * uDither), 0.0, 3.0);

    vec3 shade = level < 0.5 ? uShade0 : level < 1.5 ? uShade1 : level < 2.5 ? uShade2 : uShade3;
    fragColor = vec4(shade, 1.0);
}
