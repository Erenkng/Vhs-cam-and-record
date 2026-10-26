// Security camera: wide-angle lens with barrel distortion, black and white with hard contrast,
// grainy gain noise, a slow mains hum bar rolling through the picture and dark corners.
// The time stamp is drawn by the OSD layer; the low frame rate comes from frame holding.
uniform sampler2D uSrc;
uniform vec2 uSrcSize;
uniform uint uFrame;
uniform float uTime;
uniform float uContrast;
uniform float uNoise;
uniform float uBarrel;
uniform float uHum;
uniform vec3 uTint;
uniform float uVignette;

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec2 d = vUv - 0.5;
    float aspect = uSrcSize.x / uSrcSize.y;
    vec2 da = vec2(d.x * aspect, d.y);
    float r2 = dot(da, da);
    // Barrel distortion, scaled so the corners still reach the frame edge.
    float k = uBarrel;
    float edge = 1.0 + k * (0.25 * aspect * aspect + 0.25);
    vec2 uv = 0.5 + d * (1.0 + k * r2) / edge;

    float y = luma(texture(uSrc, uv).rgb);
    y = clamp((y - 0.5) * uContrast + 0.5 + 0.03, 0.0, 1.0);

    // Mains hum: a soft darker band drifting slowly upwards.
    float band = fract(vUv.y * 1.0 - uTime * 0.07);
    y *= 1.0 - uHum * smoothstep(0.0, 0.25, band) * (1.0 - smoothstep(0.25, 0.5, band));

    uint ux = uint(int(vUv.x * uSrcSize.x));
    uint uy = uint(int(vUv.y * uSrcSize.y));
    y += (hash3(ux, uy, uFrame) - 0.5) * uNoise;

    y *= vignette(vUv, uVignette);
    fragColor = vec4(clamp(y * uTint, 0.0, 1.0), 1.0);
}
