// Pass B (VHS): colour-under. The chroma of a VHS recording only resolves ~40 lines across
// the picture and lags behind luma, so it is low-passed hard, delayed to the right and
// stored at a quarter of the working width as offset-encoded I/Q.
uniform sampler2D uSrc;
uniform vec2 uSrcSize;
uniform float uChromaBlur;
uniform float uChromaShift;

in vec2 vUv;
out vec4 fragColor;

void main() {
    float sigma = max(uChromaBlur, 0.6);
    float x = vUv.x * uSrcSize.x - uChromaShift;
    float lineStep = 1.0 / uSrcSize.y;
    vec2 acc = vec2(0.0);
    float weightSum = 0.0;
    for (int i = -7; i <= 7; i++) {
        float o = float(i) * sigma * 0.4;
        float w = exp(-0.5 * (o * o) / (sigma * sigma));
        float u = (x + o) / uSrcSize.x;
        // Average two lines too: VHS colour is vertically smeared by the comb filter.
        vec3 a = texture(uSrc, vec2(u, vUv.y)).rgb;
        vec3 b = texture(uSrc, vec2(u, vUv.y + lineStep)).rgb;
        acc += (rgbToYiq(a).yz + rgbToYiq(b).yz) * 0.5 * w;
        weightSum += w;
    }
    vec2 iq = acc / weightSum;
    fragColor = vec4(iq * 0.5 + 0.5, 0.0, 1.0);
}
