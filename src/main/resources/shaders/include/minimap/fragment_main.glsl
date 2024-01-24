#define BORDER_COLOR1 vec4(vec3(40. / 255.), 1.)
#define BORDER_COLOR2 vec4(vec3(70. / 255.), 1.)

if (minimap == 1.0 || minimap == 2.0) {
    vec2 uvn11 = texCoord2 * 2.0 - 1.0;
    float dist = dot(uvn11, uvn11);
    if (dist < 0.87 || keepEdges == 1.0) {
        vec4 color = texture(Sampler0, texCoord1);
        remapColor(color);
        fragColor = color * vertexColor * ColorModulator;
    } else if (dist < 0.93 && minimap == 1.0) {
        fragColor = vec4(17. / 255.);
        if (dist > 0.89 && dist < 0.92) {
            fragColor = vec4(40. / 255.);
            if (dist > 0.9) {
                fragColor = vec4(70. / 255.);
            }
        }
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

if (fullscreenMinimap == 1.0) {
    ivec2 iuv = ivec2(texCoord0 * 128.);
    iuv.y = 127 - iuv.y;
    vec2 uv = texCoord0;
    uv.y *= 127. / 128.;
    uv.y += 1. / 128.;
    vec4 color = texture(Sampler0, uv);
    remapColor(color);
    float fsx = fract(sx);
    float fsy = fract(sy);
    if (fsx == 0 && fsy == 0) {
        if (sx == 1) iuv.y = 127 - iuv.y;
        if (sy == 1) iuv.x = 127 - iuv.x;
        if (iuv.x == 0 || iuv.y == 0) color = BORDER_COLOR1;
        else if (iuv.x == 1 || iuv.y == 1) color = BORDER_COLOR2;
        else if (iuv.x == 2 || iuv.y == 2) color = BORDER_COLOR1;
    } else if (fsx == 0 || fsy == 0) {
        int d = 0;
        if (fsx == 0) {
            if (sx == 1) iuv.y = 127 - iuv.y;
            d = iuv.y;
        } else {
            if (sy == 1) iuv.x = 127 - iuv.x;
            d = iuv.x;
        }
        if (d == 0) color = BORDER_COLOR1;
        else if (d == 1) color = BORDER_COLOR2;
        else if (d == 2) color = BORDER_COLOR1;
    }
    fragColor = color * vertexColor * ColorModulator;
    fragColor.a *= transition;
    return;
} else if (fullscreenMinimap == 2.0) {
    fragColor = vec4(0, 0, 0, 0.5 * transition);
    return;
}