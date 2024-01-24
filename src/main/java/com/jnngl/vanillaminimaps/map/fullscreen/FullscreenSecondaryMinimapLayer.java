package com.jnngl.vanillaminimaps.map.fullscreen;

import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;

public record FullscreenSecondaryMinimapLayer(SecondaryMinimapLayer base, int chunkX, int chunkZ, int screenX, int screenY) {
}