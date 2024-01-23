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