// Camcorder "trail" effect: each frame leaves a fading ghost of the previous ones.
uniform sampler2D uSrc;      // current working frame
uniform sampler2D uPrevious; // last output of this pass
uniform float uTrail;        // how much of the previous frame survives, 0..1

in vec2 vUv;
out vec4 fragColor;

void main() {
    vec3 current = texture(uSrc, vUv).rgb;
    vec3 previous = texture(uPrevious, vUv).rgb;
    fragColor = vec4(mix(current, max(current, previous), uTrail), 1.0);
}
