package com.jnngl.vanillaminimaps.map;

import com.jnngl.vanillaminimaps.clientside.EntityHandle;
import com.jnngl.vanillaminimaps.map.renderer.MinimapLayerRenderer;

public record MinimapLayer(int mapId, EntityHandle<?> lowerFrame, EntityHandle<?> upperFrame, MinimapLayerRenderer renderer) {

}
