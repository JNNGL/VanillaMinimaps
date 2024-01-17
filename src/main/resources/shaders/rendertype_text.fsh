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

out vec4 fragColor;

void main() {
    if (minimap == 1.0 || minimap == 2.0) {
        vec2 uvn11 = texCoord2 * 2.0 - 1.0;
        float dist = dot(uvn11, uvn11);
        if (dist < 0.89 || keepEdges == 1.0) {
            fragColor = texture(Sampler0, texCoord1) * vertexColor * ColorModulator;
        } else if (dist < 0.93 && minimap == 1.0) {
            fragColor = vec4(0.1);
            fragColor.a = 1.0;
        } else {
            discard;
        }
        if (minimap == 1.0) {
            fragColor.a = 1.0;
        } else {
            if (fract(texCoord1.x) < 17. / 128. && fract(texCoord1.y) < 3. / 128. ||
                fract(texCoord1.x) >= 127. / 128. || fract(texCoord1.y) >= 127. / 128.) {
                discard;
            }
        }
        if (fragColor.a < 0.1) {
            discard;
        }
        return;
    } else if (minimap > 0.0) {
        discard;
    }

    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}