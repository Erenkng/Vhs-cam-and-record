// Final pass: scales the processed upright frame onto an output surface (preview, encoder or
// photo), adding the CRT-ish line structure, vignette, crisp VCR on-screen display, the
// preview-only focus peaking, the VCR blue screen and the camcorder fader.
uniform sampler2D uSrc;       // processed frame, upright space
uniform vec2 uSrcSize;
uniform sampler2D uOverlay;   // crisp OSD, upright space, premultiplied alpha
uniform float uOverlayEnabled;
uniform float uOverlayMirror;
uniform float uVignette;
uniform float uScanlines;
uniform sampler2D uPeakSrc;   // clean working frame for focus peaking
uniform float uPeaking;
uniform vec3 uPeakColor;
uniform float uBlueScreen;    // 0..1, the deck's blue "no picture" screen
uniform float uFade;          // 0..1 towards uFadeColor
uniform vec3 uFadeColor;

const vec3 VCR_BLUE = vec3(0.05, 0.12, 0.72);

in vec2 vUv;
in vec2 vTexUv;               // upright uv
out vec4 fragColor;

float peakAt(vec2 uv) {
    vec2 px = 1.0 / uSrcSize;
    float l = luma(texture(uPeakSrc, uv - vec2(px.x, 0.0)).rgb);
    float r = luma(texture(uPeakSrc, uv + vec2(px.x, 0.0)).rgb);
    float d = luma(texture(uPeakSrc, uv - vec2(0.0, px.y)).rgb);
    float u = luma(texture(uPeakSrc, uv + vec2(0.0, px.y)).rgb);
    return length(vec2(r - l, u - d));
}

void main() {
    vec2 uv = vTexUv;
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        fragColor = vec4(uFadeColor * uFade, 1.0);
        return;
    }
    vec3 c = texture(uSrc, uv).rgb;

    float line = uv.y * uSrcSize.y;
    c *= 1.0 - uScanlines * (0.5 + 0.5 * cos(6.2831853 * line));
    c *= vignette(uv, uVignette);

    if (uPeaking > 0.0) {
        float edge = smoothstep(0.12, 0.3, peakAt(uv));
        c = mix(c, uPeakColor, edge * uPeaking);
    }

    c = mix(c, VCR_BLUE, uBlueScreen);

    vec2 overlayUv = vec2(mix(uv.x, 1.0 - uv.x, uOverlayMirror), uv.y);
    c = over(c, texture(uOverlay, overlayUv) * uOverlayEnabled);
    c = mix(c, uFadeColor, uFade);
    fragColor = vec4(c, 1.0);
}
