// Vanilla Minimaps
// https://github.com/JNNGL/VanillaMinimaps

#version 150

#moj_import <fog.glsl>
#moj_import <light.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform vec2 ScreenSize;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec2 texCoord1;
out vec2 texCoord2;
out vec4 normal;
out float minimap;
out float keepEdges;
out float transition;
out float fullscreenMinimap;
out float sx, sy;

#moj_import <minimap/vertex_util.glsl>

void main() {
    vec4 vertex = vec4(Position, 1.0);
    vec4 vcolor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    gl_Position = ProjMat * ModelViewMat * vertex;
    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);

    #moj_import <minimap/vertex_main.glsl>
}
