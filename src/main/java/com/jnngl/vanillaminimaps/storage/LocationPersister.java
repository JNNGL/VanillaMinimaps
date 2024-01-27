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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.SQLException;
import java.util.UUID;

public class LocationPersister extends StringType {

  private static final Gson GSON = new Gson();
  private static final LocationPersister INSTANCE = new LocationPersister();

  public static LocationPersister getSingleton() {
    return INSTANCE;
  }

  protected LocationPersister() {
    super(SqlType.STRING, new Class<?>[]{Location.class});
  }

  @Override
  public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
    if (javaObject == null) {
      return null;
    }

    Location location = (Location) javaObject;
    JsonObject object = new JsonObject();
    object.addProperty("world", location.getWorld().getUID().toString());
    object.addProperty("x", location.x());
    object.addProperty("y", location.y());
    object.addProperty("z", location.z());
    return object.toString();
  }

  @Override
  public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
    if (sqlArg == null) {
      return null;
    }

    JsonObject object = GSON.fromJson((String) sqlArg, JsonObject.class);
    World world = Bukkit.getWorld(UUID.fromString(object.get("world").getAsString()));
    double x = object.get("x").getAsDouble();
    double y = object.get("y").getAsDouble();
    double z = object.get("z").getAsDouble();
    return new Location(world, x, y, z);
  }
}
