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

import com.jnngl.vanillaminimaps.VanillaMinimaps;
import com.jnngl.vanillaminimaps.clientside.ClientsideMinimapFactory;
import com.jnngl.vanillaminimaps.clientside.MinimapPacketSender;
import com.jnngl.vanillaminimaps.clientside.SteerableViewFactory;
import com.jnngl.vanillaminimaps.listener.MinimapBlockListener;
import com.jnngl.vanillaminimaps.listener.MinimapListener;
import com.jnngl.vanillaminimaps.map.fullscreen.FullscreenMinimap;
import com.jnngl.vanillaminimaps.map.icon.provider.MinimapIconProvider;
import com.jnngl.vanillaminimaps.map.renderer.world.WorldMinimapRenderer;
import com.jnngl.vanillaminimaps.map.renderer.world.provider.MinimapWorldRendererProvider;
import com.jnngl.vanillaminimaps.storage.MinimapPlayerDatabase;
import org.bukkit.entity.Player;

public interface MinimapProvider {

  ClientsideMinimapFactory clientsideMinimapFactory();

  MinimapPacketSender packetSender();

  WorldMinimapRenderer worldRenderer();

  MinimapIconProvider iconProvider();

  MinimapWorldRendererProvider worldRendererProvider();

  MinimapListener minimapListener();

  MinimapBlockListener minimapBlockListener();

  SteerableViewFactory steerableViewFactory();

  MinimapPlayerDatabase playerDataStorage();

  Minimap getPlayerMinimap(Player player);

  FullscreenMinimap getFullscreenMinimap(Player player);

  static MinimapProvider get() {
    return VanillaMinimaps.get();
  }
}
