// Full-screen quad. vUv spans the render target; vTexUv is vUv mapped through uUvTransform,
// e.g. upright space -> camera texture, or output surface -> upright space.
in vec2 aPosition;

uniform mat4 uUvTransform;

out vec2 vUv;
out vec2 vTexUv;

void main() {
    vUv = aPosition * 0.5 + 0.5;
    vTexUv = (uUvTransform * vec4(vUv, 0.0, 1.0)).xy;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
