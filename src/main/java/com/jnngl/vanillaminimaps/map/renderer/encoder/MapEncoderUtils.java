/*
 *  Copyright (C) 2024  JNNGL
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jnngl.vanillaminimaps.map.renderer.encoder;

public class MapEncoderUtils {

  public static void encodeUnsigned(byte[] image, int offsetX, int offsetY, int value) {
    for (int bit = 0; bit < 8; bit++) {
      boolean set = (value & (1 << bit)) != 0;
      image[offsetY * 128 + offsetX + bit] = (byte) (set ? 4 : 0);
    }
  }

  public static void encodeFixedPoint(byte[] image, int offsetX, int offsetY, double value) {
    if (value < 0) {
      value = 1 - value;
    }
    int fixedPoint = (int) (Math.abs(value) * 255.0);
    fixedPoint = Math.min(fixedPoint, 255);

    encodeUnsigned(image, offsetX, offsetY, fixedPoint);
  }

  public static void markCorners(byte[] image) {
    image[0] = (byte) 158;
    image[127] = (byte) 158;
    image[128 * 127] = (byte) 158;
    image[128 * 127 + 127] = (byte) 158;
  }
}
