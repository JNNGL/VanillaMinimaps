package com.jnngl.vanillaminimaps.clientside;

import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import org.bukkit.entity.Player;

public interface MinimapPacketSender {

  void updateLayer(Player viewer, MinimapLayer layer, int x, int y, int width, int height, byte[] data);

  void spawnLayer(Player viewer, MinimapLayer layer);

  void spawnMinimap(Minimap minimap);

  void despawnLayer(Player viewer, MinimapLayer layer);

  void despawnMinimap(Minimap minimap);
}
