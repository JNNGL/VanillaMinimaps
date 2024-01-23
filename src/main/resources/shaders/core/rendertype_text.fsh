// Vanilla Minimaps
// https://github.com/JNNGL/VanillaMinimaps

#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform vec2 ScreenSize;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec2 texCoord2;
in float minimap;
in float keepEdges;
in float transition;
in float fullscreenMinimap;

out vec4 fragColor;

#moj_import <minimap/fragment_util.glsl>

void main() {
    #moj_import <minimap/fragment_main.glsl>

    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}