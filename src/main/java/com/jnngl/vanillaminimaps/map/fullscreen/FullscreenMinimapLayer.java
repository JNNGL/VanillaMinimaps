package com.jnngl.vanillaminimaps.map.fullscreen;

import com.jnngl.vanillaminimaps.map.MinimapLayer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

public record FullscreenMinimapLayer(MinimapLayer base, int chunkX, int chunkZ, int screenX, int screenY) {

}
