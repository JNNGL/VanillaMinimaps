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

package com.jnngl.vanillaminimaps.map.renderer.world.cache;

import it.unimi.dsi.fastutil.longs.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.World;
import org.joml.Vector4i;
import org.joml.Vector4ic;

import java.util.*;
import java.util.function.Consumer;

@Getter
@AllArgsConstructor
public class WorldMapCache<R extends CacheableWorldMinimapRenderer> {

  private static final List<UUID> WORLD_SET = new ArrayList<>();

  private final Long2ObjectMap<byte[]> cache = new Long2ObjectOpenHashMap<>();
  private final Long2ObjectMap<Set<UUID>> refCounts = new Long2ObjectOpenHashMap<>();
  private final Map<UUID, LongSet> viewers = new HashMap<>();
  private final Map<UUID, Consumer<Vector4ic>> callbacks = new HashMap<>();
  private final R renderer;

  public static long getKey(World world, int x, int z) {
    int worldId = WORLD_SET.indexOf(world.getUID());
    if (worldId == -1) {
      worldId = WORLD_SET.size();
      WORLD_SET.add(world.getUID());
    }
    return (long) worldId << 52 | ((long) (z < 0 ? 1 : 0) << 51) | (long) (Math.abs(z) >> 7) << 26 | ((long) (x < 0 ? 1 : 0) << 25) | (Math.abs(x) >> 7);
  }

  public byte[] getCached(World world, int x, int z) {
    int alignedX = (x >> 7) << 7;
    int alignedZ = (z >> 7) << 7;
    long key = getKey(world, alignedX, alignedZ);
    return cache.get(key);
  }

  public byte[] get(World world, int x, int z) {
    int alignedX = (x >> 7) << 7;
    int alignedZ = (z >> 7) << 7;
    long key = getKey(world, alignedX, alignedZ);
    return cache.computeIfAbsent(key, k -> {
      byte[] data = new byte[128 * 128];
      renderer.renderFully(world, alignedX - 64, alignedZ - 64, data);
      return data;
    });
  }

  public void invalidate(World world, int x, int z) {
    cache.remove(getKey(world, x, z));
  }

  protected void addViewer(long key, UUID viewer) {
    viewers.computeIfAbsent(viewer, k -> new LongOpenHashSet()).add(key);
    refCounts.computeIfAbsent(key, k -> new HashSet<>()).add(viewer);
  }

  public void addViewer(World world, int x, int z, UUID viewer) {
    addViewer(getKey(world, x, z), viewer);
  }

  public void setCallback(UUID viewer, Consumer<Vector4ic> callback) {
    callbacks.put(viewer, callback);
  }

  protected void removeViewer(long key, UUID viewer) {
    LongSet tracked = viewers.get(viewer);
    if (tracked != null) {
      tracked.remove(key);
    }

    Set<UUID> viewers = refCounts.get(key);
    if (viewers == null) {
      return;
    }

    viewers.remove(viewer);
    if (viewers.isEmpty()) {
      refCounts.remove(key);
      cache.remove(key);
    }
  }

  public void removeViewer(World world, int x, int z, UUID viewer) {
    removeViewer(getKey(world, x, z), viewer);
  }

  public void releaseViewer(UUID viewer) {
    callbacks.remove(viewer);
    LongSet tracked = viewers.remove(viewer);
    if (tracked == null) {
      return;
    }

    tracked.forEach(key -> removeViewer(key, viewer));
  }

  public void setViewerChunks(UUID viewer, LongCollection keys) {
    LongSet tracked = viewers.get(viewer);
    viewers.put(viewer, new LongOpenHashSet());
    keys.forEach(key -> addViewer(key, viewer));
    if (tracked != null) {
      tracked.forEach(key -> {
        if (!keys.contains(key)) {
          removeViewer(key, viewer);
        }
      });
    }
  }

  public void notifyDirtyArea(UUID viewer, Vector4i area) {
    Consumer<Vector4ic> callback = callbacks.get(viewer);
    if (callback != null) {
      callback.accept(area);
    }
  }

  public void notifyDirtyArea(World world, Vector4i area) {
    int startX = (area.x() >> 7) << 7;
    int startZ = (area.y() >> 7) << 7;
    int endX = ((area.x() + area.z()) >> 7) << 7;
    int endZ = ((area.y() + area.w()) >> 7) << 7;
    Set<UUID> allViewers = new HashSet<>();
    for (int x = startX; x <= endX; x += 128) {
      for (int z = startZ; z <= endZ; z += 128) {
        Set<UUID> viewers = refCounts.get(getKey(world, x, z));
        if (viewers != null) {
          allViewers.addAll(viewers);
        }
      }
    }
    allViewers.forEach(viewer -> notifyDirtyArea(viewer, area));
  }
}
