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

package com.jnngl.vanillaminimaps.listener;

import com.jnngl.vanillaminimaps.VanillaMinimaps;
import com.jnngl.vanillaminimaps.map.renderer.world.cache.WorldMapCache;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;
import org.joml.Vector4i;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
public class MinimapBlockListener implements Listener {
  private static final Set<Class<? extends Event>> EVENTS =
      Set.of(BlockBurnEvent.class, BlockExpEvent.class, BlockExplodeEvent.class, BlockFadeEvent.class,
          BlockFertilizeEvent.class, BlockFromToEvent.class, BlockGrowEvent.class, BlockIgniteEvent.class,
          BlockPistonExtendEvent.class, BlockPistonRetractEvent.class, BlockPlaceEvent.class, BlockPhysicsEvent.class,
          BlockRedstoneEvent.class, FluidLevelChangeEvent.class, LeavesDecayEvent.class, MoistureChangeEvent.class,
          SculkBloomEvent.class, SpongeAbsorbEvent.class, TNTPrimeEvent.class, EntityBlockFormEvent.class,
          BlockFormEvent.class, BlockSpreadEvent.class, EntityExplodeEvent.class, EntityChangeBlockEvent.class);

  @Getter
  private final Set<WorldMapCache<?>> registeredCache = new HashSet<>();
  private final VanillaMinimaps plugin;

  public void registerListener(Plugin plugin) {
    EVENTS.forEach(eventClass ->
        Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.HIGH,
            (listener, event) -> onBlockChange(event), plugin));
  }

  public void registerCache(WorldMapCache<?> cache) {
    registeredCache.add(cache);
  }

  public void unregisterCache(WorldMapCache<?> cache) {
    registeredCache.remove(cache);
  }

  public void update(Block block) {
    registeredCache.forEach(cache -> update(block, cache, true));
  }

  protected void updateBlock(Block block, WorldMapCache<?> cache) {
    byte[] data = cache.getCached(block.getWorld(), block.getX() + 64, block.getZ() + 64);
    if (data != null) {
      int offsetX = (block.getX() - 64) % 128;
      int offsetZ = (block.getZ() - 64) % 128;
      if (offsetX < 0) {
        offsetX += 128;
      }
      if (offsetZ < 0) {
        offsetZ += 128;
      }
      cache.getRenderer().updateBlock(block, (127 - offsetZ) * 128 + (127 - offsetX), data);
    }
  }

  protected void update(Block block, WorldMapCache<?> cache, boolean updateNeighbourBlocks) {
    if (block == null) {
      return;
    }

    Vector4i area = new Vector4i(block.getX(), block.getZ(), 1, 1);
    updateBlock(block, cache);
    if (updateNeighbourBlocks) {
      updateBlock(block.getRelative(-1, 0, 0), cache);
      updateBlock(block.getRelative(1, 0, 0), cache);
      updateBlock(block.getRelative(0, 0, -1), cache);
      updateBlock(block.getRelative(0, 0, 1), cache);
      area.add(-1, -1, 2, 2);
    }
    cache.notifyDirtyArea(block.getWorld(), area);
  }

  private void onBlockChange(Event event) {
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      if (event instanceof BlockExplodeEvent explode) {
        explode.blockList().forEach(this::update);
      } else if (event instanceof EntityExplodeEvent explode) {
        explode.blockList().forEach(this::update);
      } else if (event instanceof EntityChangeBlockEvent changeBlock) {
        update(changeBlock.getBlock());
      } else if (event instanceof BlockBurnEvent burn) {
        update(burn.getIgnitingBlock());
      } else if (event instanceof BlockFertilizeEvent fertilize) {
        fertilize.getBlocks().stream()
            .filter(BlockState::isPlaced)
            .map(BlockState::getBlock)
            .forEach(this::update);
      } else if (event instanceof BlockFromToEvent fromTo) {
        update(fromTo.getToBlock());
      } else if (event instanceof BlockIgniteEvent ignite) {
        update(ignite.getIgnitingBlock());
      } else if (event instanceof BlockPistonExtendEvent pistonExtend) {
        pistonExtend.getBlocks().forEach(this::update);
      } else if (event instanceof BlockPistonRetractEvent pistonRetract) {
        pistonRetract.getBlocks().forEach(this::update);
      } else if (event instanceof SpongeAbsorbEvent spongeAbsorb) {
        spongeAbsorb.getBlocks().stream()
            .filter(BlockState::isPlaced)
            .map(BlockState::getBlock)
            .forEach(this::update);
      } else if (event instanceof TNTPrimeEvent tntPrime) {
        update(tntPrime.getPrimingBlock());
      }
      if (event instanceof BlockEvent blockEvent) {
        update(blockEvent.getBlock());
      }
    });
  }
}
