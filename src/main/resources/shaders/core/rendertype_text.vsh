// Vanilla Minimaps
// https://github.com/JNNGL/VanillaMinimaps

#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform vec2 ScreenSize;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec2 texCoord1;
out vec2 texCoord2;
out float minimap;
out float keepEdges;
out float transition;
out float fullscreenMinimap;

#moj_import <minimap/vertex_util.glsl>

void main() {
    vec4 vertex = vec4(Position, 1.0);
    vec4 vcolor = Color * texelFetch(Sampler2, UV2 / 16, 0);
    gl_Position = ProjMat * ModelViewMat * vertex;
    vertexDistance = length((ModelViewMat * vertex).xyz);

    #moj_import <minimap/vertex_main.glsl>
}