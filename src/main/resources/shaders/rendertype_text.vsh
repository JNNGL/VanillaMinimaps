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
uniform float GameTime;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec2 texCoord1;
out vec2 texCoord2;
out float minimap;
out float keepEdges;
out float transition;
out float fullscreenMinimap;

mat2 mat2_rotate_z(float radians) {
    return mat2(
        cos(radians), -sin(radians),
        sin(radians), cos(radians)
    );
}

int decodeUnsigned(int offsetX, int offsetY) {
    float texel = 1. / 128.;
    float yOffTexel = float(offsetY) * texel;

    int power = 1;
    int value = 0;
    for (int i = 0; i < 8; i++) {
        int x = offsetX + i;
        bool set = sign(length(texture(Sampler0, vec2(float(x) * texel, yOffTexel)).xyz)) > 0;
        if (set) value += power;
        power *= 2;
    }

    return value;
}

float decodeFixedPoint(int offsetX, int offsetY) {
    return float(decodeUnsigned(offsetX, offsetY)) / 255.0;
}

void main() {
    vec4 vertex = vec4(Position, 1.0);
    vec4 vcolor = Color * texelFetch(Sampler2, UV2 / 16, 0);
    gl_Position = ProjMat * ModelViewMat * vertex;
    vertexDistance = length((ModelViewMat * vertex).xyz);

    fullscreenMinimap = 0.0;
    transition = 0.0;
    minimap = 0.0;
    keepEdges = 0.0;
    vec2 uv = UV0;
    vec2 uv2 = vec2(0.0);

    if (texture(Sampler0, uv).xyz == vec3(112. / 255., 108. / 255., 138. / 255.)) {
        int vertexId = gl_VertexID % 4;
        float ratio = ScreenSize.x / ScreenSize.y;
        float vratio = ScreenSize.y / ScreenSize.x;
        if (vratio < ratio) ratio = 1;
        else vratio = 1;
        switch (vertexId) {
            case 0: { gl_Position = vec4(-1 + 0.04 * vratio, 1 - 0.04 * ratio, 0, 1); uv2 = vec2(0, 1); break; }
            case 1: { gl_Position = vec4(-1 + 0.04 * vratio, 1 - 0.70 * ratio, 0, 1); uv2 = vec2(0, 0); break; }
            case 2: { gl_Position = vec4(-1 + 0.70 * vratio, 1 - 0.70 * ratio, 0, 1); uv2 = vec2(1, 0); break; }
            case 3: { gl_Position = vec4(-1 + 0.70 * vratio, 1 - 0.04 * ratio, 0, 1); uv2 = vec2(1, 1); break; }
        }

        vec3 local = IViewRotMat * vec3(1, 0, 0);
        float yaw = -atan(local.x, local.z);

        float vx = decodeFixedPoint(1, 0);
        float vz = decodeFixedPoint(9, 0);

        uv -= vec2(0.5);
        uv = uv * mat2_rotate_z(mod(yaw + 180, 360) - 180);
        uv += vec2(0.5);

        minimap = 1.0;
        vcolor = Color;

        float texel = 1. / 128.;
        uv += vec2(texel) * vec2(-vx, -vz);

        if (sign(length(texture(Sampler0, vec2(texel * 17, 0.)).xyz)) > 0) {
            gl_Position.x += 2.0 - 0.74 * vratio;
        }

        bool isMarker = sign(length(texture(Sampler0, vec2(0., texel)).xyz)) > 0;
        if (isMarker) {
            float depth = decodeFixedPoint(1, 1);
            gl_Position.z -= depth;

            float rPointX = decodeFixedPoint(9, 1);
            float rPointZ = decodeFixedPoint(1, 2);
            vec2 rPoint = vec2(rPointZ, rPointX);
            uv -= rPoint;
            uv = uv * mat2_rotate_z(mod(-yaw + 180, 360) - 180);
            uv += rPoint;

            if (sign(length(texture(Sampler0, vec2(0., texel * 2)).xyz)) == 0) {
                vcolor = vec4(0.0);
            }

            bool keep = sign(length(texture(Sampler0, vec2(texel * 9., texel * 2)).xyz)) > 0;
            keepEdges = keep ? 1.0 : 0.0;
            minimap = 2.0;
        }
    } else {
        int fullscreenMagic = decodeUnsigned(0, 0);
        if (fullscreenMagic == 178) {
            int segmentX = decodeUnsigned(8, 0);
            int segmentY = decodeUnsigned(16, 0);
            int xSegments = decodeUnsigned(24, 0);
            int ySegments = decodeUnsigned(32, 0);
            transition = decodeFixedPoint(40, 0);
            int vertexId = gl_VertexID % 4;
            float ratio = ScreenSize.x / ScreenSize.y;
            float vratio = ScreenSize.y / ScreenSize.x;
            if (vratio < ratio) ratio = 1;
            else vratio = 1;
            float left = -float(xSegments) * 0.5 * vratio * 0.64;
            float top = float(ySegments) * 0.5 * ratio * 0.64 - (1.0 - transition);
            switch (vertexId) {
                case 0: { gl_Position = vec4(left + 0.64 * segmentX * vratio,       top - 0.64 * segmentY * ratio,       -0.5, 1); uv2 = vec2(0, 1); break; }
                case 1: { gl_Position = vec4(left + 0.64 * segmentX * vratio,       top - 0.64 * (segmentY + 1) * ratio, -0.5, 1); uv2 = vec2(0, 0); break; }
                case 2: { gl_Position = vec4(left + 0.64 * (segmentX + 1) * vratio, top - 0.64 * (segmentY + 1) * ratio, -0.5, 1); uv2 = vec2(1, 0); break; }
                case 3: { gl_Position = vec4(left + 0.64 * (segmentX + 1) * vratio, top - 0.64 * segmentY * ratio,       -0.5, 1); uv2 = vec2(1, 1); break; }
            }
            vcolor = Color;
            fullscreenMinimap = 1.0;
        } else if (fullscreenMagic == 109) {
            transition = decodeFixedPoint(8, 0);
            int vertexId = gl_VertexID % 4;
            switch (vertexId) {
                case 0: { gl_Position = vec4(-1, 1, -0.4, 1); uv2 = vec2(0, 1); break; }
                case 1: { gl_Position = vec4(-1, -1, -0.4, 1); uv2 = vec2(0, 0); break; }
                case 2: { gl_Position = vec4(1, -1, -0.4, 1); uv2 = vec2(1, 0); break; }
                case 3: { gl_Position = vec4(1, 1, -0.4, 1); uv2 = vec2(1, 1); break; }
            }
            vcolor = Color;
            fullscreenMinimap = 2.0;
        }
    }

    texCoord0 = UV0;
    texCoord1 = uv;
    texCoord2 = uv2;
    vertexColor = vcolor;
}
