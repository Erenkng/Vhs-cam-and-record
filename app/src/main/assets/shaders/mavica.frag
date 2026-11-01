// Sony Mavica, the camera that saved to a floppy disk: a 640 x 480 CCD with a narrow range -
// shadows block up and highlights clip early - a cool cast, edge sharpening, and bright lights
// smearing into vertical streaks and blooming across the sensor. The JPEG blocks are baked in
// when the photo is saved, and the pixels stay sharp when it is scaled up.
uniform sampler2D uSrc;
uniform sampler2D uGlow;
uniform vec2 uSrcSize;
uniform uint uFrame;
uniform float uSaturation;
uniform float uContrast;
uniform float uKnee;
uniform float uSmear;
uniform float uSharpen;
uniform float uNoise;
uniform vec3 uTint;

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec2 px = 1.0 / uSrcSize;
    vec3 c = texture(uSrc, vUv).rgb;
    vec3 blur = 0.25 * (
        texture(uSrc, vUv + vec2(px.x, 0.0)).rgb + texture(uSrc, vUv - vec2(px.x, 0.0)).rgb +
        texture(uSrc, vUv + vec2(0.0, px.y)).rgb + texture(uSrc, vUv - vec2(0.0, px.y)).rgb
    );
    c += (c - blur) * uSharpen;

    // Narrow range: contrast pushes the shadows down, and past the knee the highlights shoot
    // up to white.
    c = clamp((c - 0.5) * uContrast + 0.5, 0.0, 1.0);
    c = min(c, vec3(uKnee)) + max(c - uKnee, 0.0) * 4.0;
    c = clamp(c, 0.0, 1.0);
    c = mix(vec3(luma(c)), c, uSaturation) * uTint;

    // CCD smear: light from the brightest spots bleeds along the whole column.
    float smear = 0.0;
    for (int i = 0; i < 24; i++) {
        float v = (float(i) + 0.5) / 24.0;
        smear += max(luma(texture(uSrc, vec2(vUv.x, v)).rgb) - 0.8, 0.0);
    }
    c += vec3(0.93, 0.95, 1.0) * smear / 24.0 * uSmear * 12.0;
    // And blooms around them.
    c += texture(uGlow, vUv).rgb * uSmear * 0.45;

    uint ux = uint(int(vUv.x * uSrcSize.x));
    uint uy = uint(int(vUv.y * uSrcSize.y));
    c += (hash3(ux, uy, uFrame) - 0.5) * uNoise;

    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
