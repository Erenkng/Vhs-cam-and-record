// Pass A: camera frame (or photo) -> upright working frame, plus the OSD that real cameras
// burn into the picture before it is degraded (camcorder date, film imprint).
#ifdef INPUT_OES
uniform samplerExternalOES uInput;
#else
uniform sampler2D uInput;
#endif

// Texture-space vectors of one working pixel along x and y, for a 4-tap downscale filter.
uniform vec2 uPixelX;
uniform vec2 uPixelY;
uniform sampler2D uOsd;
uniform float uOsdEnabled;
uniform float uOsdMirror;

in vec2 vUv;
in vec2 vTexUv;
out vec4 fragColor;

void main() {
    vec2 dx = uPixelX * 0.25;
    vec2 dy = uPixelY * 0.25;
    vec3 c = 0.25 * (
        texture(uInput, vTexUv - dx - dy).rgb +
        texture(uInput, vTexUv + dx - dy).rgb +
        texture(uInput, vTexUv - dx + dy).rgb +
        texture(uInput, vTexUv + dx + dy).rgb
    );
    vec2 osdUv = vec2(mix(vUv.x, 1.0 - vUv.x, uOsdMirror), vUv.y);
    vec4 osd = texture(uOsd, osdUv) * uOsdEnabled;
    fragColor = vec4(over(c, osd), 1.0);
}
