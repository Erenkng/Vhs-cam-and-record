// A late-90s MiniDV camcorder with a fisheye on the front, the skate video look: the picture
// bulges out from the middle and the lens hood rounds off the corners, interlaced fields comb
// along edges, and digital video's colour is crisp and a little over-sharpened. The timecode
// and the date come from the OSD overlay.
uniform sampler2D uSrc;
uniform vec2 uSrcSize;
uniform float uScale;        // working height / 480
uniform uint uFrame;
uniform float uTime;
uniform float uFisheye;
uniform float uHood;
uniform float uComb;         // how far the second field is out, in pixels
uniform float uSharpen;
uniform float uSaturation;
uniform float uContrast;
uniform float uNoise;
uniform float uVignette;

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec2 px = 1.0 / uSrcSize;
    float aspect = uSrcSize.x / uSrcSize.y;
    // Positions in frame-height units, centred.
    vec2 p = (vUv - 0.5) * vec2(aspect, 1.0);
    float r2 = dot(p, p);

    // Fisheye: the further out, the more of the scene is squeezed in. The middle of the top
    // and bottom edges stays put; the corners reach past the lens's picture.
    float k = uFisheye * 0.9;
    vec2 q = p * (1.0 + k * r2) / (1.0 + k * 0.25);

    // Interlacing: every other line is the next field, a moment later and a little aside.
    float field = mod(floor(vUv.y * uSrcSize.y), 2.0);
    q.x += field * uComb * uScale * px.y * (0.6 + 0.4 * sin(uTime * 2.7));
    vec2 uv = q / vec2(aspect, 1.0) + 0.5;

    // DV's edge enhancement.
    vec3 c = texture(uSrc, uv).rgb;
    vec3 blur = 0.25 * (
        texture(uSrc, uv + vec2(px.x, 0.0)).rgb + texture(uSrc, uv - vec2(px.x, 0.0)).rgb +
        texture(uSrc, uv + vec2(0.0, px.y)).rgb + texture(uSrc, uv - vec2(0.0, px.y)).rgb
    );
    c += (c - blur) * uSharpen * 1.5;

    c = clamp((c - 0.5) * uContrast + 0.5, 0.0, 1.0);
    c = mix(vec3(luma(c)), c, uSaturation);

    uint ux = uint(int(vUv.x * uSrcSize.x));
    uint uy = uint(int(vUv.y * uSrcSize.y));
    c += (hash3(ux, uy, uFrame) - 0.5) * uNoise;
    c *= vignette(vUv, uVignette);

    // Past the edge of the lens's picture it is black.
    vec2 outside = max(-uv, uv - 1.0);
    c *= 1.0 - smoothstep(0.0, 0.004, max(outside.x, outside.y));

    // The hood cuts into the corners as a dark round frame.
    float rHood = mix(0.9, 0.68, uHood);
    c *= 1.0 - 0.97 * smoothstep(rHood - 0.05, rHood + 0.03, sqrt(r2));

    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
