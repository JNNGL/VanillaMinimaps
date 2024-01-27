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

import com.jnngl.vanillaminimaps.map.fullscreen.FullscreenMinimap;
import com.jnngl.vanillaminimaps.map.fullscreen.FullscreenMinimapLayer;

public class FullscreenMapEncoder {

  // 0  - unsigned byte - magic value
  // 8  - unsigned byte - segment position x
  // 16 - unsigned byte - segment position y
  // 24 - unsigned byte - horizontal segments
  // 32 - unsigned byte - vertical segments
  // 40 - fixed point   - transition
  public static void encodePrimaryLayer(FullscreenMinimap minimap, FullscreenMinimapLayer layer, byte[] data) {
    MapEncoderUtils.encodeUnsigned(data, 0, 0, 178);
    MapEncoderUtils.encodeUnsigned(data, 8, 0, layer.screenX());
    MapEncoderUtils.encodeUnsigned(data, 16, 0, layer.screenY());
    MapEncoderUtils.encodeUnsigned(data, 24, 0, minimap.getWidth());
    MapEncoderUtils.encodeUnsigned(data, 32, 0, minimap.getHeight());
    MapEncoderUtils.encodeFixedPoint(data, 40, 0, minimap.getTransitionState());
  }

  // 0 - unsigned byte - magic value
  // 8 - fixed point   - transition
  public static void encodeBackground(FullscreenMinimap minimap, byte[] data) {
    MapEncoderUtils.encodeUnsigned(data, 0, 0, 109);
    MapEncoderUtils.encodeFixedPoint(data, 8, 0, minimap.getTransitionState());
  }
}
