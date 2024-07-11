fullscreenMinimap = 0.0;
transition = 0.0;
minimap = 0.0;
keepEdges = 0.0;
sx = 0.0;
sy = 0.0;
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
        sx = float(segmentX) / float(xSegments - 1);
        sy = float(segmentY) / float(ySegments - 1);
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
