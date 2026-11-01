// A 2004 camera phone filming 3GP: a tiny sensor with purple and green noise of its own,
// washed-out colour with a yellow cast, crunchy over-sharpened edges, and the 16 x 16
// macroblocks of a codec squeezing video through a phone - flat areas go blotchy, colour
// spreads over whole blocks and their edges show.
uniform sampler2D uSrc;
uniform vec2 uSrcSize;
uniform uint uFrame;
uniform float uBlockiness;
uniform float uNoise;
uniform float uSaturation;
uniform float uContrast;
uniform vec3 uTint;
uniform float uSharpen;

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec2 px = 1.0 / uSrcSize;
    vec2 pixel = vUv * uSrcSize;
    vec3 c = texture(uSrc, vUv).rgb;
    vec3 blur = 0.25 * (
        texture(uSrc, vUv + vec2(px.x, 0.0)).rgb + texture(uSrc, vUv - vec2(px.x, 0.0)).rgb +
        texture(uSrc, vUv + vec2(0.0, px.y)).rgb + texture(uSrc, vUv - vec2(0.0, px.y)).rgb
    );
    c += (c - blur) * uSharpen;

    // Macroblocks: detail inside a block is quantised in coarse steps around the block's
    // mean, and the block's colour spreads over it.
    vec2 origin = floor(pixel / 16.0) * 16.0;
    vec3 mean = vec3(0.0);
    for (int i = 0; i < 4; i++) {
        vec2 o = vec2(float(i & 1), float(i >> 1)) * 8.0 + 4.0;
        mean += texture(uSrc, (origin + o) * px).rgb * 0.25;
    }
    vec3 yiq = rgbToYiq(c);
    vec3 blockYiq = rgbToYiq(mean);
    float quant = mix(0.02, 0.13, uBlockiness);
    yiq.x = blockYiq.x + floor((yiq.x - blockYiq.x) / quant + 0.5) * quant;
    yiq.yz = mix(yiq.yz, blockYiq.yz, 0.75 * uBlockiness);
    c = yiqToRgb(yiq);
    vec2 inBlock = mod(pixel, 16.0);
    float seam = min(step(inBlock.x, 1.0) + step(inBlock.y, 1.0), 1.0);
    c *= 1.0 - 0.07 * uBlockiness * seam;

    // Washed out, yellowish and contrasty.
    c = clamp((c - 0.5) * uContrast + 0.5, 0.0, 1.0);
    c = mix(vec3(luma(c)), c, uSaturation) * uTint;

    // Purple and green sensor noise, strongest in the shadows.
    uint ux = uint(int(pixel.x));
    uint uy = uint(int(pixel.y));
    float n = hash3(ux, uy, uFrame) - 0.5;
    float m = hash3(ux, uy, uFrame + 17u) - 0.5;
    float shadow = 1.0 - smoothstep(0.15, 0.7, luma(c));
    c += (vec3(n) * 0.5 + vec3(m, -m, m)) * uNoise * (0.3 + shadow);

    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
