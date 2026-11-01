#version 100
// A full-frame quad for the TV export (Media3 effect, OpenGL ES 2).
attribute vec4 aFramePosition;
varying vec2 vTexSamplingCoord;

void main() {
    gl_Position = aFramePosition;
    vTexSamplingCoord = aFramePosition.xy * 0.5 + 0.5;
}
