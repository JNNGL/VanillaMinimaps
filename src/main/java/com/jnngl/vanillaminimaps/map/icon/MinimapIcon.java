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
