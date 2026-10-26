// Shared helpers. Sources are concatenated by the loader (app: ShaderLibrary, lab: lab.js),
// which also prepends "#version 300 es" and the input-type defines.

precision highp float;
precision highp int;

const vec3 LUMA = vec3(0.299, 0.587, 0.114);

float luma(vec3 c) {
    return dot(c, LUMA);
}

// NTSC YIQ, the colour space VHS actually records in (luma + colour-under).
vec3 rgbToYiq(vec3 c) {
    return vec3(
        dot(c, vec3(0.299, 0.587, 0.114)),
        dot(c, vec3(0.596, -0.274, -0.322)),
        dot(c, vec3(0.211, -0.523, 0.312))
    );
}

vec3 yiqToRgb(vec3 c) {
    return vec3(
        c.x + 0.956 * c.y + 0.621 * c.z,
        c.x - 0.272 * c.y - 0.647 * c.z,
        c.x - 1.106 * c.y + 1.703 * c.z
    );
}

// PCG hash: integer based, so it behaves the same on every GPU (no sin() hashing).
uint pcg(uint v) {
    uint state = v * 747796405u + 2891336453u;
    uint word = ((state >> ((state >> 28u) + 4u)) ^ state) * 277803737u;
    return (word >> 22u) ^ word;
}

float hash1(uint a) {
    return float(pcg(a)) * (1.0 / 4294967295.0);
}

float hash2(uint a, uint b) {
    return hash1(a ^ pcg(b + 0x9E3779B9u));
}

float hash3(uint a, uint b, uint c) {
    return hash1(a ^ pcg(b ^ pcg(c + 0x7F4A7C15u)));
}

uint ufloor(float x) {
    return uint(int(floor(x)));
}

// Smooth 1D value noise in [0, 1].
float vnoise(float x, uint seed) {
    float i = floor(x);
    float f = fract(x);
    float a = hash2(uint(int(i)), seed);
    float b = hash2(uint(int(i) + 1), seed);
    return mix(a, b, f * f * (3.0 - 2.0 * f));
}

// Smooth 2D value noise in [0, 1].
float vnoise2(vec2 p, uint seed) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    uint x0 = uint(int(i.x));
    uint y0 = uint(int(i.y));
    float a = hash3(x0, y0, seed);
    float b = hash3(x0 + 1u, y0, seed);
    float c = hash3(x0, y0 + 1u, seed);
    float d = hash3(x0 + 1u, y0 + 1u, seed);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

// Composites a premultiplied-alpha layer over an opaque colour.
vec3 over(vec3 base, vec4 layer) {
    return base * (1.0 - layer.a) + layer.rgb;
}

float vignette(vec2 uv, float amount) {
    vec2 d = uv - 0.5;
    return 1.0 - amount * smoothstep(0.08, 0.55, dot(d, d) * 1.6);
}
