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

package com.jnngl.vanillaminimaps.map.icon;

import com.jnngl.mapcolor.ColorMatcher;
import com.jnngl.mapcolor.matchers.CachedColorMatcher;
import com.jnngl.mapcolor.palette.Palette;

import java.awt.*;
import java.awt.image.BufferedImage;

public record MinimapIcon(byte[] data, int width, int height) {

  private static final ThreadLocal<ColorMatcher> COLOR_MATCHER = ThreadLocal.withInitial(() -> new CachedColorMatcher(Palette.NEWEST_PALETTE) {

    @Override
    public byte matchColor(Color color) {
      if (color.getAlpha() < 128) {
        return (byte) 0;
      }
      return super.matchColor(color);
    }

    @Override
    public byte matchColor(int rgb) {
      return this.matchColor(new Color(rgb, true));
    }
  });

  public static MinimapIcon fromBufferedImage(BufferedImage image) {
    ColorMatcher matcher = COLOR_MATCHER.get();
    byte[] data = matcher.matchImage(image);
    return new MinimapIcon(data, image.getWidth(), image.getHeight());
  }
}
