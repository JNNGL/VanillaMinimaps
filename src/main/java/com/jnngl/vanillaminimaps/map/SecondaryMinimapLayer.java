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

package com.jnngl.vanillaminimaps.map;

import com.jnngl.vanillaminimaps.map.renderer.SecondaryMinimapLayerRenderer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;

@Getter
@Setter
@AllArgsConstructor
public class SecondaryMinimapLayer {

  private final MinimapLayer baseLayer;
  private SecondaryMinimapLayerRenderer renderer;
  private boolean trackLocation;
  private boolean keepOnEdge;
  private World world;
  private int positionX;
  private int positionZ;
  private float depth;

}
