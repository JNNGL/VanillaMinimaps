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

import com.google.common.collect.ImmutableSet;
import com.jnngl.vanillaminimaps.map.icon.MinimapIcon;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class BuiltinMinimapIconProvider implements MinimapIconProvider {

  private static final Set<String> SPECIAL_KEYS = Set.of("player", "death", "offscreen_player");

  private final Map<String, MinimapIcon> cache = new ConcurrentHashMap<>();
  private final Path lookupDirectory;
  private Set<String> loadedKeys;

  public BuiltinMinimapIconProvider(Path lookupDirectory) {
    this.lookupDirectory = lookupDirectory;
    try {
      loadKeys();
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public MinimapIcon getIcon(String key) {
    MinimapIcon cached = cache.get(key);
    if (cached != null) {
      return cached;
    } else {
      MinimapIcon icon = lookupIconSilent(key);
      if (icon != null) {
        cache.put(key, icon);
        return icon;
      } else {
        return null;
      }
    }
  }

  public MinimapIcon lookupIconSilent(String key) {
    try {
      return lookupIcon(key);
    } catch (IOException e) {
      return null;
    }
  }

  public MinimapIcon lookupIcon(String key) throws IOException {
    Path path = lookupDirectory.resolve(key + ".png");
    BufferedImage image = null;
    if (Files.exists(path)) {
      image = ImageIO.read(Files.newInputStream(path));
    } else {
      try (InputStream resourceInput = BuiltinMinimapIconProvider.class.getResourceAsStream("/minimap/" + key + ".png")) {
        if (resourceInput != null) {
          image = ImageIO.read(resourceInput);
        }
      }
    }

    if (image != null) {
      return MinimapIcon.fromBufferedImage(image);
    } else {
      return null;
    }
  }

  @Override
  public void registerIcon(String key, MinimapIcon icon) {
    cache.put(key, icon);
  }

  protected void loadKeys(Set<String> keys, Path lookupPath) throws IOException {
    if (Files.exists(lookupPath) && Files.isDirectory(lookupPath)) {
      try (Stream<Path> icons = Files.list(lookupPath)) {
        icons.filter(Files::isRegularFile)
            .filter(path -> FilenameUtils.getExtension(path.toString()).equals("png"))
            .map(Path::getFileName)
            .map(Path::toString)
            .map(FilenameUtils::getBaseName)
            .forEach(keys::add);
      }
    }
  }

  protected void loadClasspathKeys(Set<String> destination, String directory) throws IOException, URISyntaxException {
    URL url = BuiltinMinimapIconProvider.class.getResource(directory);
    if (url != null) {
      if (url.getProtocol().equals("file")) {
        Path lookupPath = Paths.get(url.toURI());
        loadKeys(destination, lookupPath);
      } else if (url.getProtocol().equals("jar")) {
        String dirname = directory;
        if (dirname.startsWith("/")) {
          dirname = dirname.substring(1);
        }
        if (!dirname.endsWith("/")) {
          dirname += "/";
        }
        String path = url.getPath();
        String jarPath = path.substring(5, path.indexOf("!"));
        try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
          Enumeration<JarEntry> entries = jar.entries();
          while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith(dirname) && !dirname.equals(name) &&
                FilenameUtils.getExtension(name).equals("png")) {
              destination.add(FilenameUtils.getBaseName(name));
            }
          }
        }
      }
    }
  }

  protected void loadKeys() throws IOException, URISyntaxException {
    Set<String> keys = new HashSet<>();
    loadKeys(keys, lookupDirectory);
    loadClasspathKeys(keys, "/minimap");
    loadedKeys = ImmutableSet.copyOf(keys);
  }

  @Override
  public Set<String> allKeys() {
    return loadedKeys;
  }

  @Override
  public Set<String> specialIconKeys() {
    Set<String> keys = allKeys();
    return SPECIAL_KEYS.stream()
        .filter(keys::contains)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<String> genericIconKeys() {
    Set<String> keys = allKeys();
    return keys.stream()
        .filter(key -> !SPECIAL_KEYS.contains(key))
        .collect(Collectors.toSet());
  }
}
