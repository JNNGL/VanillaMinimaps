package com.jnngl.vanillaminimaps.map;

import org.bukkit.entity.Player;

import java.util.LinkedHashMap;

public record Minimap(Player holder, MinimapLayer primaryLayer, LinkedHashMap<String, SecondaryMinimapLayer> secondaryLayers) {
}
