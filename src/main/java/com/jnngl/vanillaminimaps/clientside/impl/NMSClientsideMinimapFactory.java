package com.jnngl.vanillaminimaps.clientside.impl;

import com.jnngl.vanillaminimaps.clientside.ClientsideMinimapFactory;
import com.jnngl.vanillaminimaps.clientside.EntityHandle;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.renderer.MinimapLayerRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public class NMSClientsideMinimapFactory implements ClientsideMinimapFactory {

  private static int MAP_ID_COUNTER = -100000;

  private ItemFrame createLayerFrame(World world, ItemStack item, Direction direction) {
    ItemFrame itemFrame = new ItemFrame(EntityType.ITEM_FRAME, ((CraftWorld) world).getHandle());
    itemFrame.setDirection(direction);
    itemFrame.setItem(item);
    itemFrame.setInvisible(true);

    return itemFrame;
  }

  @Override
  public MinimapLayer createMinimapLayer(World world, MinimapLayerRenderer renderer) {
    int mapId = MAP_ID_COUNTER--;

    ItemStack item = new ItemStack(Items.FILLED_MAP);
    item.getOrCreateTag().putInt("map", mapId);

    ItemFrame upperFrame = createLayerFrame(world, item, Direction.DOWN);
    ItemFrame lowerFrame = createLayerFrame(world, item, Direction.UP);

    return new MinimapLayer(mapId, new EntityHandle<>(upperFrame), new EntityHandle<>(lowerFrame), renderer);
  }

  @Override
  public Minimap createMinimap(Player holder, MinimapLayer primaryLayer, Map<String, SecondaryMinimapLayer> secondaryLayers) {
    Minimap minimap = new Minimap(holder, primaryLayer, new LinkedHashMap<>());
    if (secondaryLayers != null) {
      minimap.secondaryLayers().putAll(secondaryLayers);
    }
    return minimap;
  }
}
