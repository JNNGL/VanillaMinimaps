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
    vec2 uv = texCoord2;
    uv.x = 1.0 - uv.x;
    uv.y *= 127. / 128.;
    uv.y += 1. / 128.;
    vec4 color = texture(Sampler0, uv);
    remapColor(color);
    fragColor = color * vertexColor * ColorModulator;
    fragColor.a *= transition;
    return;
} else if (fullscreenMinimap == 2.0) {
    fragColor = vec4(0, 0, 0, 0.5 * transition);
    return;
}