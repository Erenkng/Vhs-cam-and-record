// Early-2000s CCD point-and-shoot: punchy saturated colour with a cool cast, hard-clipping
// highlights, lateral chromatic aberration with purple fringes, heavy in-camera sharpening
// and colour noise in the shadows. (JPEG blocking is baked in when the photo is saved.)
uniform sampler2D uSrc;
uniform vec2 uSrcSize;
uniform float uScale;        // working height / 1200
uniform uint uFrame;
uniform float uSaturation;
uniform float uContrast;
uniform float uKnee;
uniform float uFringe;
uniform float uSharpen;
uniform float uNoise;
uniform vec3 uTint;
uniform float uVignette;

in vec2 vUv;
out vec4 fragColor;

vec3 tex(vec2 uv) {
    return texture(uSrc, uv).rgb;
}

void main() {
    vec2 px = 1.0 / uSrcSize;
    vec2 centred = vUv - 0.5;
    vec2 ca = centred * length(centred) * 2.0 * uFringe * uScale * px * 6.0;
    vec3 col = vec3(tex(vUv + ca).r, tex(vUv).g, tex(vUv - ca).b);

    vec3 n = tex(vUv + vec2(px.x, 0.0)) + tex(vUv - vec2(px.x, 0.0)) +
             tex(vUv + vec2(0.0, px.y)) + tex(vUv - vec2(0.0, px.y));
    vec3 blur = n * 0.25;
    col += (col - blur) * uSharpen * 1.6;

    // Purple fringing where a highlight meets something darker.
    float l = luma(col);
    float neighbourMax = max(max(luma(tex(vUv + vec2(px.x * 2.0, 0.0))), luma(tex(vUv - vec2(px.x * 2.0, 0.0)))),
                             max(luma(tex(vUv + vec2(0.0, px.y * 2.0))), luma(tex(vUv - vec2(0.0, px.y * 2.0)))));
    float fringe = smoothstep(0.82, 0.98, neighbourMax) * smoothstep(0.08, 0.35, neighbourMax - l);
    col = mix(col, vec3(0.62, 0.22, 0.95) * max(l, 0.35) * 1.6, fringe * min(uFringe, 2.0) * 0.35);

    col = (col - 0.5) * uContrast + 0.5;
    // Small sensors clip each channel hard, which shifts the hue of bright skies and skin.
    col = col * 1.04 + smoothstep(vec3(uKnee), vec3(1.0), col) * 0.15;
    col = clamp(col, 0.0, 1.0);

    float gray = luma(col);
    col = mix(vec3(gray), col, uSaturation) * uTint;

    uvec2 p = uvec2(gl_FragCoord.xy / max(uScale, 0.25));
    vec3 noise = vec3(hash3(p.x, p.y, uFrame), hash3(p.x, p.y, uFrame + 1u), hash3(p.x, p.y, uFrame + 2u)) - 0.5;
    float shadow = (1.0 - gray) * (1.0 - gray);
    col += noise * uNoise * 2.0 * shadow;

    col *= vignette(vUv, uVignette);
    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
