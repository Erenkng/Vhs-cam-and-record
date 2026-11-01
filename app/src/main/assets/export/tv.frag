#version 100
// The TV export: a clip playing on an old CRT television in a dark room, as a 9:16 frame for
// Reels and TikTok. Landscape clips get a 4:3 TV, upright ones a 3:4 screen like a rotated
// arcade monitor; the picture is fitted inside with black bars. The tube bulges, shows
// scanlines and phosphor stripes, glows, and lights the wall and the table around it.
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform sampler2D uTexSampler;
uniform vec2 uOutputSize;   // pixels
uniform float uAspect;      // the clip's width / height, upright
uniform float uRotation;    // degrees the frame still has to turn clockwise to stand upright
uniform float uTime;        // seconds

varying vec2 vTexSamplingCoord;

const float PI = 3.14159265;

float roundedBox(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

float sq(float x) {
    return x * x;
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

// The clip at upright uv (0..1 from the top left), however the frame is turned.
vec3 clip(vec2 uv) {
    vec2 t = uv;
    if (uRotation > 45.0 && uRotation < 135.0) {
        t = vec2(uv.y, 1.0 - uv.x);
    } else if (uRotation >= 135.0 && uRotation < 225.0) {
        t = vec2(1.0 - uv.x, 1.0 - uv.y);
    } else if (uRotation >= 225.0) {
        t = vec2(1.0 - uv.y, uv.x);
    }
    return texture2D(uTexSampler, vec2(t.x, 1.0 - t.y)).rgb;
}

// The clip as the tube shows it at screen uv: fitted in with black bars around it.
vec3 picture(vec2 uv, float tube) {
    float fit = uAspect / tube;
    if (fit > 1.0) {
        uv.y = (uv.y - 0.5) * fit + 0.5;
    } else {
        uv.x = (uv.x - 0.5) / fit + 0.5;
    }
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return vec3(0.0);
    return clip(uv);
}

void main() {
    vec2 p = vec2(vTexSamplingCoord.x, 1.0 - vTexSamplingCoord.y) * uOutputSize;
    float W = uOutputSize.x;
    float H = uOutputSize.y;
    float u = W / 1080.0;

    // The set: landscape clips get a 4:3 TV, upright ones a 3:4 screen.
    bool upright = uAspect < 1.0;
    float tube = upright ? 3.0 / 4.0 : 4.0 / 3.0;
    vec2 screenHalf = upright ? vec2(390.0, 520.0) * u : vec2(430.0, 322.5) * u;
    vec2 screenCentre = vec2(W * 0.5, H * (upright ? 0.45 : 0.43));
    float sideMargin = (upright ? 58.0 : 50.0) * u;
    float topMargin = 56.0 * u;
    float panel = 170.0 * u;
    vec2 boxMin = screenCentre - screenHalf - vec2(sideMargin, topMargin);
    vec2 boxMax = screenCentre + screenHalf + vec2(sideMargin, panel);
    vec2 boxCentre = (boxMin + boxMax) * 0.5;
    vec2 boxHalf = (boxMax - boxMin) * 0.5;

    // What the screen shows on average, to light the room with.
    vec3 average = (picture(vec2(0.5), tube) * 2.0 + picture(vec2(0.25, 0.3), tube) + picture(vec2(0.75, 0.3), tube)
        + picture(vec2(0.25, 0.7), tube) + picture(vec2(0.75, 0.7), tube)) / 6.0;
    vec3 light = average * 0.8 + vec3(0.06, 0.07, 0.1);

    // The room: a dark wall lit by the screen, and a table the set stands on.
    float tableTop = boxMax.y;
    vec3 col = mix(vec3(0.028, 0.025, 0.04), vec3(0.06, 0.05, 0.07), p.y / H);
    float wallGlow = exp(-sq(length((p - screenCentre) / u) / 620.0));
    col += light * wallGlow * 0.42;
    if (p.y > tableTop) {
        float depth = (p.y - tableTop) / (H - tableTop);
        vec3 table = mix(vec3(0.075, 0.06, 0.055), vec3(0.03, 0.025, 0.025), depth);
        // The screen's light pooling on the table in front of the set.
        float pool = exp(-sq((p.x - screenCentre.x) / (520.0 * u))) * exp(-depth * 4.0);
        col = table + light * pool * 0.35;
        // Its edge catches a little light.
        col += vec3(0.05) * exp(-(p.y - tableTop) / (3.0 * u));
    }
    // A soft shadow under the set.
    float shadow = roundedBox(p - boxCentre - vec2(0.0, 18.0 * u), boxHalf, 60.0 * u);
    col *= 1.0 - 0.6 * (1.0 - smoothstep(0.0, 70.0 * u, shadow));

    // The cabinet: dark plastic, lit from above, a rim of light along the top edge.
    float box = roundedBox(p - boxCentre, boxHalf, 64.0 * u);
    if (box < 0.0) {
        float y = (p.y - boxMin.y) / (boxMax.y - boxMin.y);
        vec3 plastic = mix(vec3(0.15, 0.145, 0.165), vec3(0.075, 0.07, 0.085), y);
        plastic += light * 0.1 * (1.0 - y);
        plastic *= 1.0 - 0.35 * smoothstep(-26.0 * u, 0.0, box);
        plastic += vec3(0.2) * (1.0 - smoothstep(0.0, 5.0 * u, abs(box + 6.0 * u))) * smoothstep(0.35, 0.0, y);

        // The control panel under the screen: a speaker grille, two knobs and the power light.
        float panelTop = screenCentre.y + screenHalf.y + 34.0 * u;
        float panelBottom = boxMax.y - 34.0 * u;
        float panelMid = (panelTop + panelBottom) * 0.5;
        if (p.y > panelTop && p.y < panelBottom && p.x > boxMin.x + 64.0 * u && p.x < screenCentre.x + 60.0 * u) {
            float slot = fract((p.y - panelTop) / (13.0 * u));
            plastic *= mix(1.0, 0.35, smoothstep(0.35, 0.45, slot) * (1.0 - smoothstep(0.75, 0.85, slot)));
        }
        for (int i = 0; i < 2; i++) {
            vec2 knob = vec2(boxMax.x - (110.0 + float(i) * 104.0) * u, panelMid);
            float d = length(p - knob);
            float r = 34.0 * u;
            if (d < r + 4.0 * u) {
                vec3 metal = mix(vec3(0.26, 0.25, 0.28), vec3(0.08, 0.08, 0.09), clamp((p.y - knob.y + r) / (2.0 * r), 0.0, 1.0));
                float notch = 1.0 - smoothstep(2.0 * u, 4.0 * u, abs(p.x - knob.x)) * step(knob.y - r, p.y) * step(p.y, knob.y - r * 0.35);
                metal *= mix(0.4, 1.0, notch);
                plastic = mix(plastic * 0.5, metal, 1.0 - smoothstep(r - 1.5 * u, r, d));
            }
        }
        vec2 led = vec2(boxMax.x - 300.0 * u, panelMid);
        float ledD = length(p - led);
        plastic += vec3(1.0, 0.16, 0.12) * (1.0 - smoothstep(6.0 * u, 8.0 * u, ledD));
        plastic += vec3(1.0, 0.2, 0.15) * 0.25 * exp(-ledD / (12.0 * u));

        col = mix(col, plastic, 1.0 - smoothstep(-1.5 * u, 0.0, box));
    }

    // The bezel's recess around the glass.
    vec2 s = p - screenCentre;
    float bezel = roundedBox(s, screenHalf + 22.0 * u, 70.0 * u);
    if (bezel < 0.0) {
        col = mix(col, vec3(0.03, 0.028, 0.035) + light * 0.03, 1.0 - smoothstep(-1.5 * u, 0.0, bezel));
    }

    // The tube: the picture bulges with the glass, darker towards its rounded corners.
    float glass = roundedBox(s, screenHalf, 56.0 * u);
    if (glass < 1.5 * u) {
        vec2 sp = s / screenHalf;
        vec2 bent = sp * (1.0 + 0.055 * dot(sp, sp));
        vec2 uv = bent * 0.5 + 0.5;
        vec3 tv = picture(uv, tube);
        // Bright parts bloom a little into their surroundings.
        vec2 px = vec2(2.5 / (screenHalf.x * 2.0), 2.5 / (screenHalf.y * 2.0));
        vec3 glow = picture(uv + vec2(px.x, 0.0), tube) + picture(uv - vec2(px.x, 0.0), tube)
            + picture(uv + vec2(0.0, px.y), tube) + picture(uv - vec2(0.0, px.y), tube);
        tv = tv * 0.82 + glow * 0.045 + max(glow * 0.25 - 0.55, 0.0) * 0.35;

        // Scanlines, the phosphor stripes, and a faint bar rolling down the screen.
        float lines = 0.5 + 0.5 * cos(uv.y * 260.0 * 2.0 * PI);
        tv *= mix(1.0, lines, 0.3);
        float stripe = mod(floor(p.x / max(u, 0.5)), 3.0);
        vec3 mask = vec3(stripe < 0.5 ? 1.12 : 0.82, stripe > 0.5 && stripe < 1.5 ? 1.12 : 0.82, stripe > 1.5 ? 1.12 : 0.82);
        tv *= mask * 1.22;
        float roll = fract(uTime * 0.11);
        tv *= 1.0 + 0.05 * exp(-sq((uv.y - roll) / 0.05));
        tv += (hash(floor(p / (2.0 * u)) + fract(uTime) * 61.0) - 0.5) * 0.035;
        tv *= 0.985 + 0.015 * sin(uTime * 47.0);

        // The glass: darker into the corners, a glare top left and a soft sheen across it.
        tv *= 1.0 - 0.45 * pow(clamp(length(sp) / 1.42, 0.0, 1.0), 2.4);
        tv += vec3(0.85, 0.9, 1.0) * 0.07 * exp(-sq(length((sp - vec2(-0.52, -0.6)) * vec2(1.0, 1.6)) / 0.45));
        tv += vec3(1.0) * 0.025 * (1.0 - smoothstep(0.0, 0.5, abs(sp.x + sp.y * 0.8 + 0.6)));

        col = mix(col, clamp(tv, 0.0, 1.0), 1.0 - smoothstep(-1.5 * u, 1.5 * u, glass));
    }

    // A slight vignette over the whole frame, like a phone filming the set in a dark room.
    vec2 v = vTexSamplingCoord - 0.5;
    col *= 1.0 - 0.35 * dot(v, v) * 2.0;
    gl_FragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
