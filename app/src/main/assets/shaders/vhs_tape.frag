// Pass C (VHS): playback of the tape. Works line by line like the deck does:
// time-base jitter, head-switching noise, tracking errors, band-limited luma with the deck's
// edge enhancement, smeared and noisy chroma, snow and dropouts.
uniform sampler2D uSrc;      // pass A, RGB
uniform sampler2D uChroma;   // pass B, offset-encoded I/Q at quarter width
uniform vec2 uSrcSize;
uniform float uTime;
uniform uint uFrame;

uniform float uLumaBlur;
uniform float uSharpen;
uniform float uChromaNoise;
uniform float uLumaNoise;
uniform float uJitter;
uniform float uTracking;
uniform float uHeadSwitch;
uniform float uDropouts;
uniform float uSaturation;
uniform float uContrast;
uniform float uBlackLift;
uniform vec3 uTint;
// 0..1 burst of extra distortion, e.g. right after recording starts or resumes.
uniform float uGlitch;

in vec2 vUv;
out vec4 fragColor;

float lumaAt(float x, float v) {
    return luma(texture(uSrc, vec2(x / uSrcSize.x, v)).rgb);
}

// Horizontal gaussian low-pass of luma around pixel column x.
float lumaBlur(float x, float v, float sigma) {
    float s = max(sigma, 0.35);
    float acc = 0.0;
    float weightSum = 0.0;
    for (int i = -3; i <= 3; i++) {
        float o = float(i) * s * 0.8;
        float w = exp(-0.5 * (o * o) / (s * s));
        acc += lumaAt(x + o, v) * w;
        weightSum += w;
    }
    return acc / weightSum;
}

float trackingEnvelope(float t) {
    float threshold = 0.95 - 0.55 * uTracking;
    return smoothstep(threshold, threshold + 0.12, vnoise(t * 0.22, 31u));
}

void main() {
    float width = uSrcSize.x;
    float height = uSrcSize.y;
    float lineF = (1.0 - vUv.y) * height;   // 0 at the top of the picture
    uint line = ufloor(lineF);
    float t = uTime;

    // Time-base error: mostly a slow wobble across lines, a little per-line jitter on top,
    // and a bent top of the picture ("flagging").
    float offset = (hash2(line, uFrame) - 0.5) * 0.7 * uJitter;
    offset += (vnoise(lineF * 0.045 + t * 1.7, 17u) - 0.5) * 2.2 * uJitter;
    offset += (1.0 - smoothstep(0.0, 22.0, lineF)) * uJitter * 4.0 * (vnoise(t * 1.3, 5u) - 0.35);

    // Head switching: the last ~13 lines are torn sideways.
    float hsHeight = 0.028 * height;
    float hs = smoothstep(height - hsHeight, height, lineF) * uHeadSwitch;
    offset += hs * (9.0 + 7.0 * hash2(line, uFrame + 3u));

    // Tracking error: a noisy band that slowly rolls down the picture now and then.
    float track = trackingEnvelope(t) * min(1.0, uTracking * 1.5);
    float bandCentre = fract(t * 0.06 + vnoise(t * 0.17, 9u) * 0.3) * (height + 160.0) - 80.0;
    float bandHalf = mix(6.0, 36.0, vnoise(t * 0.4, 77u));
    float band = track * (1.0 - smoothstep(0.0, bandHalf, abs(lineF - bandCentre)));
    offset += band * (hash2(line, uFrame + 11u) - 0.5) * 46.0;

    // Glitch burst: big blocky tears that settle within a few frames.
    offset += uGlitch * (hash2(line / 4u, uFrame + 23u) - 0.5) * 70.0;
    band = max(band, uGlitch * 0.8);

    float x = vUv.x * width + offset;
    bool blanking = x < 0.0 || x > width;

    // Luma: band-limited, then the deck's edge enhancement adds bright/dark halos.
    float yNarrow = lumaBlur(x, vUv.y, uLumaBlur);
    float yWide = lumaBlur(x, vUv.y, uLumaBlur * 2.6 + 1.2);
    float y = yNarrow + uSharpen * (yNarrow - yWide);

    // Chroma: quarter-width texture, bilinearly smeared back up.
    vec2 iq = texture(uChroma, vec2(x / width, vUv.y)).rg * 2.0 - 1.0;
    float chromaNoise = uChromaNoise * (1.0 + 3.0 * band);
    iq += vec2(
        vnoise(x * 0.055 + float(line) * 7.31, uFrame * 2u + 1u) - 0.5,
        vnoise(x * 0.055 + float(line) * 3.17, uFrame * 2u + 2u) - 0.5
    ) * 2.0 * chromaNoise;
    iq *= 1.0 - 0.7 * hs / max(uHeadSwitch, 0.001);

    // Snow, stronger inside the tracking band and the head-switch region.
    float snowAmp = uLumaNoise * (1.0 + 4.0 * band + 2.5 * hs);
    y += (hash3(ufloor(x), line, uFrame) - 0.5) * 2.0 * snowAmp;
    // Horizontal noise streaks inside the band.
    if (band > 0.15 && hash2(line, uFrame + 41u) > 0.72) {
        y = mix(y, 0.35 + 0.7 * hash3(ufloor(x / 5.0), line, uFrame + 43u), 0.75 * band);
    }

    // Dropouts: oxide missing from the tape gives short white comets.
    if (hash2(line, uFrame * 7u + 3u) < uDropouts * 0.0045) {
        float start = hash2(line, uFrame * 7u + 5u) * width;
        float len = 8.0 + 70.0 * hash2(line, uFrame * 7u + 9u);
        float d = x - start;
        if (d > 0.0 && d < len) {
            float a = 1.0 - d / len;
            y = mix(y, 1.05, 0.85 * a);
            iq *= 1.0 - 0.8 * a;
        }
    }

    if (blanking) {
        y = 0.02;
        iq = vec2(0.0);
    }

    // Deck/tape colour: softer contrast, milky blacks, washed-out colour.
    y = (y - 0.5) * uContrast + 0.5;
    y = uBlackLift + y * (1.0 - uBlackLift);
    iq *= uSaturation;
    vec3 rgb = yiqToRgb(vec3(y, iq)) * uTint;
    fragColor = vec4(clamp(rgb, 0.0, 1.0), 1.0);
}
