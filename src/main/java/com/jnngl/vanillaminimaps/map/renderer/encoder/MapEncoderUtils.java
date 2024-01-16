package com.jnngl.vanillaminimaps.map.renderer.encoder;

public class MapEncoderUtils {

  public static void encodeFixedPoint(byte[] image, int offsetX, int offsetY, double value) {
    if (value < 0) {
      value = 1 - value;
    }
    int fixedPoint = (int) (Math.abs(value) * 255.0);
    fixedPoint = Math.min(fixedPoint, 255);

    for (int bit = 0; bit < 8; bit++) {
      boolean set = (fixedPoint & (1 << bit)) != 0;
      image[offsetY * 128 + offsetX + bit] = (byte) (set ? 4 : 0);
    }
  }

  public static void markCorners(byte[] image) {
    image[0] = (byte) 158;
    image[127] = (byte) 158;
    image[128 * 127] = (byte) 158;
    image[128 * 127 + 127] = (byte) 158;
  }
}
