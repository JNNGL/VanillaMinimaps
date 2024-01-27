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

package com.jnngl.vanillaminimaps.map.icon.provider;

import com.jnngl.vanillaminimaps.map.icon.MinimapIcon;

import java.util.Set;
import java.util.stream.Collectors;

public interface MinimapIconProvider {

  MinimapIcon getIcon(String key);

  void registerIcon(String key, MinimapIcon icon);

  Set<String> allKeys();

  Set<String> specialIconKeys();

  default Set<String> genericIconKeys() {
    Set<String> special = specialIconKeys();
    return allKeys().stream()
        .filter(key -> !special.contains(key))
        .collect(Collectors.toSet());
  }
}
