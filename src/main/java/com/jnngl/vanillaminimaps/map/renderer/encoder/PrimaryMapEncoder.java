package com.jnngl.vanillaminimaps.map.renderer.encoder;

import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class PrimaryMapEncoder {

  public static void encodePrimaryLayer(Minimap minimap, byte[] data) {
    Location location = minimap.holder().getLocation();
    Vector fractional = location.clone().subtract(location.toBlockLocation()).toVector();
    encodePrimaryLayer(fractional.getX(), fractional.getZ(), data);
  }

  public static void encodePrimaryLayer(double fractionalX, double fractionalZ, byte[] data) {
    MapEncoderUtils.markCorners(data);
    MapEncoderUtils.encodeFixedPoint(data, 1, 0, fractionalX);
    MapEncoderUtils.encodeFixedPoint(data, 9, 0, fractionalZ);
    data[128] = (byte) 0;
  }
}
