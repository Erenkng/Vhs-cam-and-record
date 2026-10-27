// Pass A: camera frame (or photo) -> upright working frame, plus the OSD that real cameras
// burn into the picture before it is degraded (camcorder date, film imprint). Camcorder
// digital effects (night vision, sepia, black and white, mosaic) happen here too, before tape.
#ifdef INPUT_OES
uniform samplerExternalOES uInput;
#else
uniform sampler2D uInput;
#endif

// Same uniform as the vertex shader: upright uv -> input texture uv.
uniform mat4 uUvTransform;
// Texture-space vectors of one working pixel along x and y, for a 4-tap downscale filter.
uniform vec2 uPixelX;
uniform vec2 uPixelY;
uniform vec2 uWorkSize;
// Mosaic block size in working pixels (also used for the blocky digital zoom); <= 1 is off.
uniform float uMosaic;
// 0 none, 1 night vision, 2 sepia, 3 black and white.
uniform float uEffect;
uniform float uNightGain;
uniform sampler2D uOsd;
uniform float uOsdEnabled;
uniform float uOsdMirror;

in vec2 vUv;
in vec2 vTexUv;
out vec4 fragColor;

vec3 camcorderEffect(vec3 c) {
    if (uEffect < 0.5) return c;
    if (uEffect < 1.5) {
        // Infrared night mode: phosphor green with hot highlights, lit by an infrared lamp
        // that falls off towards the corners.
        float y = pow(clamp(luma(c) * uNightGain, 0.0, 1.0), 0.85) * vignette(vUv, 0.55);
        return vec3(0.03, 0.09, 0.04) + vec3(0.32, 1.0, 0.45) * y * 0.92;
    }
    if (uEffect < 2.5) {
        return clamp(vec3(
            dot(c, vec3(0.393, 0.769, 0.189)),
            dot(c, vec3(0.349, 0.686, 0.168)),
            dot(c, vec3(0.272, 0.534, 0.131))
        ), 0.0, 1.0);
    }
    return vec3(luma(c));
}

void main() {
    vec2 texUv = vTexUv;
    vec2 dx = uPixelX * 0.25;
    vec2 dy = uPixelY * 0.25;
    if (uMosaic > 1.0) {
        vec2 block = (floor(vUv * uWorkSize / uMosaic) + 0.5) * uMosaic / uWorkSize;
        texUv = (uUvTransform * vec4(block, 0.0, 1.0)).xy;
        dx *= uMosaic;
        dy *= uMosaic;
    }
    vec3 c = 0.25 * (
        texture(uInput, texUv - dx - dy).rgb +
        texture(uInput, texUv + dx - dy).rgb +
        texture(uInput, texUv - dx + dy).rgb +
        texture(uInput, texUv + dx + dy).rgb
    );
    c = camcorderEffect(c);
    vec2 osdUv = vec2(mix(vUv.x, 1.0 - vUv.x, uOsdMirror), vUv.y);
    vec4 osd = texture(uOsd, osdUv) * uOsdEnabled;
    fragColor = vec4(over(c, osd), 1.0);
}
