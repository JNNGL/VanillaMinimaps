package com.jnngl.vanillaminimaps.clientside;

import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;

public abstract class AbstractMinimapPacketSender implements MinimapPacketSender {

  @Override
  public void spawnMinimap(Minimap minimap) {
    spawnLayer(minimap.holder(), minimap.primaryLayer());
    for (SecondaryMinimapLayer secondary : minimap.secondaryLayers().values()) {
      spawnLayer(minimap.holder(), secondary.getBaseLayer());
    }
  }

  @Override
  public void despawnMinimap(Minimap minimap) {
    despawnLayer(minimap.holder(), minimap.primaryLayer());
    for (SecondaryMinimapLayer secondary : minimap.secondaryLayers().values()) {
      despawnLayer(minimap.holder(), secondary.getBaseLayer());
    }
  }
}
