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

package com.jnngl.vanillaminimaps.map.renderer.world.provider;

import com.jnngl.vanillaminimaps.map.renderer.world.WorldMinimapRenderer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MinimapWorldRendererProviderImpl implements MinimapWorldRendererProvider {

  private final Map<String, Supplier<WorldMinimapRenderer>> suppliers = new ConcurrentHashMap<>();

  @Override
  public Supplier<WorldMinimapRenderer> supplier(String key) {
    return suppliers.get(key);
  }

  @Override
  public void register(String key, Supplier<WorldMinimapRenderer> renderer) {
    suppliers.put(key, renderer);
  }

  @Override
  public Set<String> keys() {
    return suppliers.keySet();
  }
}
