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

import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapScreenPosition;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class SecondaryMapEncoder {

  public static void encodeSecondaryLayer(Minimap minimap, SecondaryMinimapLayer layer, byte[] data) {
    Location location = minimap.holder().getLocation();

    int trackedX = layer.getPositionX();
    int trackedZ = layer.getPositionZ();
    double positionX = 0;
    double positionZ = 0;
    if (layer.isTrackLocation()) {
      trackedZ = location.getBlockX() - layer.getPositionX(); // (Not a typo)
      trackedX = location.getBlockZ() - layer.getPositionZ();
      positionX = location.getX() - location.getBlockX();
      positionZ = location.getZ() - location.getBlockZ();
      if (layer.isKeepOnEdge()) {
        Vector direction = new Vector(trackedX, 0, trackedZ);
        if (direction.lengthSquared() > 60 * 60) {
          direction.normalize();
          direction.multiply(60);
          trackedX = direction.getBlockX();
          trackedZ = direction.getBlockZ();
          positionX = direction.getZ() - trackedZ;
          positionZ = direction.getX() - trackedX;
        }
      }
      trackedX += 64;
      trackedZ += 64;
    }

    PrimaryMapEncoder.encodePrimaryLayer(minimap.screenPosition() == MinimapScreenPosition.RIGHT, positionX, positionZ, data);
    Location position = new Location(location.getWorld(), layer.getPositionX(), location.getY(), layer.getPositionZ());
    boolean tracked = !layer.isTrackLocation() || layer.isKeepOnEdge() || location.distanceSquared(position) < 64 * 64;
    if (tracked && trackedX >= 0 && trackedX < 128 && trackedZ >= 0 && trackedZ < 128) {
      MapEncoderUtils.encodeFixedPoint(data, 1, 1, layer.getDepth());
      MapEncoderUtils.encodeFixedPoint(data, 9, 1, trackedX / 128.0);
      data[128 * 2] = (byte) 4;
      MapEncoderUtils.encodeFixedPoint(data, 1, 2, trackedZ / 128.0);
      data[128 * 2 + 9] = layer.isKeepOnEdge() ? (byte) 4 : (byte) 0;
    } else {
      data[128 * 2] = (byte) 0;
    }

    data[128] = (byte) 4;
  }
}
