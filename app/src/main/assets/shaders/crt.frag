// The picture on a curved CRT television: the glass bulges so the picture bends towards the
// tube's rounded corners inside a black bezel, the red, green and blue phosphor stripes and
// the scanlines show up close, the brightness flickers with the refresh, lights bloom into
// the glass and the room's light glares softly off it.
uniform sampler2D uSrc;
uniform sampler2D uGlow;
uniform vec2 uSrcSize;
uniform float uScale;        // working height / 720
uniform uint uFrame;
uniform float uTime;
uniform float uCurvature;
uniform float uMask;
uniform float uScanlines;
uniform float uFlicker;
uniform float uGlowAmount;
uniform float uSaturation;
uniform float uGlare;
uniform float uVignette;

in vec2 vUv;
out vec4 fragColor;

// Signed distance to a rounded rectangle (negative inside).
float roundedBox(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    float aspect = uSrcSize.x / uSrcSize.y;
    // The glass bends the picture: each edge bows the more the further along it you go, and
    // the whole picture sits a little inside the bezel.
    vec2 s = vUv * 2.0 - 1.0;
    s *= 1.0 + (s.yx * s.yx) * vec2(0.1, 0.14) * uCurvature;
    s *= 1.0 + 0.07 * uCurvature;
    vec2 uv = s * 0.5 + 0.5;

    vec3 c = texture(uSrc, uv).rgb;
    c += texture(uGlow, uv).rgb * uGlowAmount * 0.6;
    c = mix(vec3(luma(c)), c, uSaturation);
    // The glass lifts the blacks a little.
    c = c * 0.94 + 0.025;

    // Scanlines follow the bent picture, 240 to the screen.
    float scan = 0.5 + 0.5 * cos(uv.y * 240.0 * 6.2831853);
    c *= 1.0 - uScanlines * 0.45 * scan;
    // Phosphor stripes: red, green and blue side by side.
    float stripe = floor(mod(gl_FragCoord.x / max(1.5 * uScale, 1.0), 3.0));
    vec3 mask = vec3(1.0 - 0.45 * uMask);
    if (stripe < 1.0) mask.r = 1.0 + 0.2 * uMask;
    else if (stripe < 2.0) mask.g = 1.0 + 0.2 * uMask;
    else mask.b = 1.0 + 0.2 * uMask;
    c *= mask;

    // Refresh flicker and a faint band rolling down the screen.
    float roll = 0.5 + 0.5 * sin(uv.y * 5.0 + uTime * 7.0);
    c *= 1.0 - uFlicker * (0.05 * hash2(uFrame, 3u) + 0.07 * roll);
    c *= vignette(uv, uVignette);

    // The tube's rounded edge, then the bezel around it.
    vec2 e = s * vec2(aspect, 1.0) * 0.5;
    float edge = roundedBox(e, vec2(aspect * 0.5, 0.5), 0.08);
    float inside = 1.0 - smoothstep(-0.006, 0.0, edge);
    c = mix(vec3(0.015), c, inside);

    // Light from the room glancing off the glass, top left.
    vec2 g = (vUv - vec2(0.3, 0.78)) * vec2(aspect * 1.6, 3.2);
    c += vec3(0.9, 0.95, 1.0) * exp(-dot(g, g)) * 0.16 * uGlare * inside;

    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
