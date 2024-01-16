package com.jnngl.vanillaminimaps.map.renderer.encoder;

import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import org.bukkit.Location;

public class SecondaryMapEncoder {

  public static void encodeSecondaryLayer(Minimap minimap, SecondaryMinimapLayer layer, byte[] data) {
    Location location = minimap.holder().getLocation();

    int trackedX = layer.getPositionX();
    int trackedZ = layer.getPositionZ();
    double positionX = 0;
    double positionZ = 0;
    if (layer.isTrackLocation()) {
      trackedZ = location.getBlockX() - layer.getPositionX();
      trackedX = location.getBlockZ() - layer.getPositionZ();
      positionX = location.getX() - location.getBlockX();
      positionZ = location.getZ() - location.getBlockZ();
      trackedX += 64;
      trackedZ += 64;
    }

    PrimaryMapEncoder.encodePrimaryLayer(positionX, positionZ, data);
    Location position = new Location(location.getWorld(), layer.getPositionX(), location.getY(), layer.getPositionZ());
    boolean tracked = !layer.isTrackLocation() || location.distanceSquared(position) < 64 * 64;
    if (tracked && trackedX >= 0 && trackedX < 128 && trackedZ >= 0 && trackedZ < 128) {
      MapEncoderUtils.encodeFixedPoint(data, 1, 1, layer.getDepth());
      MapEncoderUtils.encodeFixedPoint(data, 9, 1, trackedX / 128.0);
      data[128 * 2] = (byte) 4;
      MapEncoderUtils.encodeFixedPoint(data, 1, 2, trackedZ / 128.0);
    } else {
      data[128 * 2] = (byte) 0;
    }

    data[128] = (byte) 4;
  }
}
