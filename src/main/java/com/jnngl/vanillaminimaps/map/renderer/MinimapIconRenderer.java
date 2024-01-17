package com.jnngl.vanillaminimaps.map.renderer;

import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.icon.MinimapIcon;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public record MinimapIconRenderer(MinimapIcon icon) implements SecondaryMinimapLayerRenderer {

  @Override
  public void render(Minimap minimap, SecondaryMinimapLayer layer, byte[] data) {
    Location location = minimap.holder().getLocation();
    int trackedX = layer.getPositionX();
    int trackedZ = layer.getPositionZ();
    if (layer.isTrackLocation()) {
      trackedZ = location.getBlockX() - layer.getPositionX();
      trackedX = location.getBlockZ() - layer.getPositionZ();
      if (layer.isKeepOnEdge()) {
        Vector direction = new Vector(trackedX, 0, trackedZ);
        if (direction.lengthSquared() > 60 * 60) {
          direction.normalize().multiply(60);
          trackedX = direction.getBlockX();
          trackedZ = direction.getBlockZ();
        }
      }
      trackedX += 64;
      trackedZ += 64;
    }

    int offsetX = -icon.width() / 2;
    int offsetY = -icon.height() / 2;
    for (int y = 0; y < icon.height(); y++) {
      int globalY = y + offsetY + trackedZ;
      if (globalY < 0) {
        continue;
      }

      if (globalY >= 128) {
        break;
      }

      for (int x = 0; x < icon.width(); x++) {
        int globalX = x + offsetX + trackedX;
        if (globalX < 0) {
          continue;
        }

        if (globalX >= 128) {
          break;
        }

        data[globalX * 128 + globalY] = icon.data()[y * icon.width() + x];
      }
    }
  }
}
