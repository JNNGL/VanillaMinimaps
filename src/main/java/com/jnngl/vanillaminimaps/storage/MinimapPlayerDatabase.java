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

package com.jnngl.vanillaminimaps.storage;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataPersisterManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.jnngl.vanillaminimaps.config.Config;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import com.jnngl.vanillaminimaps.map.MinimapProvider;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.icon.MinimapIcon;
import com.jnngl.vanillaminimaps.map.marker.MarkerMinimapLayer;
import com.jnngl.vanillaminimaps.map.renderer.MinimapIconRenderer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;

public class MinimapPlayerDatabase {

  private final JdbcPooledConnectionSource connectionSource;
  private final Dao<DatabasePlayerModel, UUID> playerDao;
  private final Dao<DatabaseMarkerModel, Integer> markerDao;

  public MinimapPlayerDatabase(Path databasePath) throws SQLException {
    DataPersisterManager.registerDataPersisters(LocationPersister.getSingleton());
    connectionSource = new JdbcPooledConnectionSource("jdbc:sqlite:" + databasePath.toAbsolutePath());

    TableUtils.createTableIfNotExists(connectionSource, DatabasePlayerModel.class);
    TableUtils.createTableIfNotExists(connectionSource, DatabaseMarkerModel.class);

    playerDao = DaoManager.createDao(connectionSource, DatabasePlayerModel.class);
    markerDao = DaoManager.createDao(connectionSource, DatabaseMarkerModel.class);
  }

  public DatabasePlayerModel queryPlayer(UUID uuid) throws SQLException {
    return playerDao.queryForId(uuid);
  }

  public DatabasePlayerModel queryPlayer(Player player) throws SQLException {
    return queryPlayer(player.getUniqueId());
  }

  public void restore(MinimapProvider provider, Player player) throws Exception {
    provider.minimapListener().disableMinimap(player);

    DatabasePlayerModel data = queryPlayer(player);
    if (data == null) {
      // Load defaults
      if (Config.instance().enabledByDefault) {
        provider.minimapListener().enableMinimap(player);
      }
      return;
    }

    if (!data.isEnabled()) {
      return;
    }

    Minimap minimap = provider.minimapListener().enableMinimap(player);
    minimap.screenPosition(data.getPosition());
    if (data.getDeathLocation() != null) {
      minimap.setDeathPoint(provider, data.getDeathLocation());
    }

    if (data.getMarkers() != null) {
      try (CloseableIterator<DatabaseMarkerModel> markerIterator = data.getMarkers().closeableIterator()) {
        while (markerIterator.hasNext()) {
          DatabaseMarkerModel marker = markerIterator.next();
          MinimapIcon icon = provider.iconProvider().getIcon(marker.getIcon());
          if (icon == null) {
            continue;
          }

          float depth = 0.05F + minimap.secondaryLayers().size() * 0.01F;
          MinimapLayer iconBaseLayer = provider.clientsideMinimapFactory().createMinimapLayer(player.getWorld(), null);
          SecondaryMinimapLayer iconLayer = new MarkerMinimapLayer(iconBaseLayer, new MinimapIconRenderer(icon), true,
              Config.instance().markers.customMarkers.stickToBorder, marker.getLocation().getWorld(),
              marker.getLocation().getBlockX(), marker.getLocation().getBlockZ(), depth);
          minimap.secondaryLayers().put(marker.getName(), iconLayer);

          if (iconLayer.getWorld() == null || player.getWorld().equals(iconLayer.getWorld())) {
            provider.packetSender().spawnLayer(player, iconBaseLayer);
          }
        }
      }
    }

    minimap.update(provider);
  }

  public void save(Minimap minimap) throws Exception {
    Player player = minimap.holder();
    DatabasePlayerModel queriedModel = queryPlayer(player);
    DatabasePlayerModel playerModel = queriedModel != null ? queriedModel : new DatabasePlayerModel();

    if (playerModel.getMarkers() != null) {
      IntList ids = new IntArrayList();
      try (CloseableIterator<DatabaseMarkerModel> markerIterator = playerModel.getMarkers().closeableIterator()) {
        while (markerIterator.hasNext()) {
          DatabaseMarkerModel marker = markerIterator.next();
          ids.add(marker.getId());
        }
      }
      markerDao.deleteIds(ids);
    }

    ForeignCollection<DatabaseMarkerModel> markers = playerDao.getEmptyForeignCollection("markers");
    minimap.secondaryLayers().forEach((name, layer) -> {
      if ("player".equals(name) || "death_point".equals(name)) {
        return;
      }

      if (!(layer instanceof MarkerMinimapLayer marker)) {
        return;
      }

      if (!(marker.getRenderer() instanceof MinimapIconRenderer renderer)) {
        return;
      }

      Location location = new Location(marker.getWorld(), marker.getPositionX(), 0, marker.getPositionZ());
      DatabaseMarkerModel markerModel = new DatabaseMarkerModel(0, name, location, renderer.icon().key(), playerModel);
      markers.add(markerModel);
    });

    playerModel.setHolder(player.getUniqueId());
    playerModel.setEnabled(true);
    playerModel.setPosition(minimap.screenPosition());
    playerModel.setDeathLocation(Config.instance().markers.deathMarker.enabled ? minimap.getDeathPoint() : null);
    playerModel.setMarkers(markers);

    playerDao.createOrUpdate(playerModel);
  }

  public void disableMinimap(Player player) throws SQLException {
    DatabasePlayerModel model = queryPlayer(player);
    if (model == null) {
      model = new DatabasePlayerModel(player.getUniqueId(), false, Config.instance().defaultPosition, null, null);
    }

    model.setEnabled(false);
    playerDao.createOrUpdate(model);
  }

  public void enableMinimap(Player player) throws SQLException {
    DatabasePlayerModel model = queryPlayer(player);
    if (model == null) {
      model = new DatabasePlayerModel(player.getUniqueId(), true, Config.instance().defaultPosition, null, null);
    }

    model.setEnabled(true);
    playerDao.createOrUpdate(model);
  }

  public void close() throws Exception {
    if (connectionSource != null) {
      connectionSource.close();
    }
  }
}
