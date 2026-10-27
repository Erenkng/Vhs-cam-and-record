// Super 8 home movie, as a projector or a film scanner shows it: warm, punchy reversal colour,
// coarse grain, exposure that flickers from frame to frame, a picture that weaves in the gate,
// dust and the odd scratch, and the gate's rounded edge with the sprocket hole beside it.
uniform sampler2D uSrc;
uniform vec2 uSrcSize;
uniform float uScale;        // working height / 540
uniform uint uFrame;
uniform float uSaturation;
uniform float uContrast;
uniform vec3 uTint;
uniform float uFade;
uniform float uGrain;
uniform float uGrainSize;
uniform float uFlicker;
uniform float uWeave;
uniform float uDust;
uniform float uScratches;
uniform float uSoftness;
uniform float uVignette;
uniform float uGate;         // 0 picture only .. 1 film edge and sprocket hole in view

in vec2 vUv;
out vec4 fragColor;

// Signed distance to a rounded rectangle (negative inside).
float roundedBox(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

vec3 picture(vec2 uv, vec2 px) {
    vec3 c = texture(uSrc, uv).rgb * 0.4;
    for (int i = 0; i < 6; i++) {
        float a = float(i) * 1.0471976 + 0.5;
        c += texture(uSrc, uv + vec2(cos(a), sin(a)) * px * uSoftness * uScale * 1.6).rgb * 0.1;
    }
    return c;
}

void main() {
    vec2 px = 1.0 / uSrcSize;
    float aspect = uSrcSize.x / uSrcSize.y;
    uint f = uFrame;
    float t = float(f);

    // Positions in frame-height units, centred.
    vec2 p = (vUv - 0.5) * vec2(aspect, 1.0);

    // The film weaves in the gate: a slow sideways drift and a jitter from the pull-down claw.
    vec2 weave = vec2(
        (vnoise(t * 0.23, 11u) - 0.5) * 1.2 + (hash2(f, 13u) - 0.5) * 0.3,
        (hash2(f, 17u) - 0.5) + (vnoise(t * 0.31, 19u) - 0.5) * 0.6
    ) * uWeave * 0.008;
    vec2 film = p - weave;

    // The picture sits right of the sprocket hole, with a thin black margin on the other sides.
    float left = 0.17 * uGate;
    float margin = 0.035 * uGate;
    vec2 halfSize = vec2(aspect * 0.5 - (left + margin) * 0.5, 0.5 - margin);
    vec2 centre = vec2((left - margin) * 0.5, 0.0);
    vec2 inPicture = film - centre;
    // Fill the picture's height and crop the sides, so nothing is squeezed.
    vec2 uv = vec2(inPicture.x / (aspect * 2.0 * halfSize.y), inPicture.y / (2.0 * halfSize.y)) + 0.5;

    vec3 col = picture(uv, px);

    // Reversal film: deep shadows, saturated warm colour, a gentle shoulder.
    col = clamp((col - 0.5) * uContrast + 0.5, 0.0, 1.0);
    col = mix(col, col * col * (3.0 - 2.0 * col), 0.45);
    float gray = luma(col);
    col = mix(vec3(gray), col, uSaturation) * uTint;
    // An old projection print: the blacks have gone a little brown.
    col = mix(col, vec3(0.13, 0.08, 0.05), uFade * (1.0 - smoothstep(0.0, 0.5, gray)));

    // Exposure flickers from frame to frame and is a touch uneven across the frame.
    float flicker = (hash2(f, 5u) - 0.5) * 0.12 + (vnoise(t * 0.5, 7u) - 0.5) * 0.1;
    flicker += (hash2(f, 9u) - 0.5) * 0.08 * uv.x;
    col *= 1.0 + flicker * uFlicker;

    // Coarse grain, strongest in the mid-tones.
    vec2 g = gl_FragCoord.xy / max(uGrainSize * uScale, 0.5);
    float grain = (vnoise2(g, f) - 0.5) * 0.75 + (vnoise2(g * 2.2, f + 31u) - 0.5) * 0.35;
    col += grain * uGrain * (0.45 + 2.2 * gray * (1.0 - gray));

    // Dust on this frame only: dark specks and, now and then, a curled hair.
    for (int i = 0; i < 7; i++) {
        uint id = f * 7u + uint(i);
        if (hash1(id * 5u + 1u) > uDust * 0.8) continue;
        vec2 at = (vec2(hash1(id * 5u + 2u), hash1(id * 5u + 3u)) - 0.5) * vec2(aspect, 1.0);
        float r = mix(0.002, 0.009, hash1(id * 5u + 4u) * hash1(id * 5u + 4u));
        float speck = 1.0 - smoothstep(r * 0.5, r, length(film - at));
        col *= 1.0 - speck * 0.8;
    }
    if (hash2(f, 61u) < uDust * 0.25) {
        vec2 at = (vec2(hash2(f, 62u), hash2(f, 63u)) - 0.5) * vec2(aspect, 1.0) * 0.8;
        vec2 d = film - at;
        float curl = d.y - 0.04 * sin(d.x * 40.0 + hash2(f, 64u) * 6.28);
        float hair = (1.0 - smoothstep(0.0012, 0.0028, abs(curl))) * (1.0 - smoothstep(0.03, 0.06, abs(d.x)));
        col *= 1.0 - hair * 0.7;
    }

    // A scratch along the film that wanders slowly and comes and goes.
    float scratchOn = smoothstep(1.0 - uScratches * 0.55, 1.0, vnoise(t * 0.07, 41u));
    float scratchX = (vnoise(t * 0.02, 43u) - 0.5) * aspect * 0.7 + 0.002 * sin(film.y * 23.0 + t);
    float scratch = 1.0 - smoothstep(0.0008, 0.0022, abs(film.x - scratchX));
    col += scratch * scratchOn * 0.3;

    col *= vignette(uv, uVignette);

    // The projector gate: rounded corners and a soft dark edge around the picture.
    float edge = roundedBox(inPicture, halfSize, 0.06);
    float inside = 1.0 - smoothstep(-0.012, 0.004, edge);
    col *= mix(1.0, inside, min(uGate * 4.0, 1.0));

    // The sprocket hole: lamp light through the film base, left of the picture.
    if (uGate > 0.0) {
        vec2 hole = film - vec2(-aspect * 0.5 + left * 0.5 - 0.005, 0.0);
        float d = roundedBox(hole, vec2(left * 0.24, 0.085 * uGate + 0.01), 0.025 * uGate);
        float lit = 1.0 - smoothstep(-0.006, 0.003, d);
        col = mix(col, vec3(1.0, 0.94, 0.82) * (0.92 + flicker * 0.4), lit * min(uGate * 3.0, 1.0));
    }

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
