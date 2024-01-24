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

package com.jnngl.vanillaminimaps.command;

import com.jnngl.vanillaminimaps.VanillaMinimaps;
import com.jnngl.vanillaminimaps.clientside.SteerableLockedView;
import com.jnngl.vanillaminimaps.config.Config;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import com.jnngl.vanillaminimaps.map.MinimapScreenPosition;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.fullscreen.FullscreenMinimap;
import com.jnngl.vanillaminimaps.map.icon.MinimapIcon;
import com.jnngl.vanillaminimaps.map.renderer.MinimapIconRenderer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class MinimapCommand extends BrigadierCommand {

  public MinimapCommand(VanillaMinimaps plugin) {
    super(plugin);
  }

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    var positionLiteral = Commands.literal("position");
    for (MinimapScreenPosition position : MinimapScreenPosition.values()) {
      positionLiteral.then(Commands.literal(position.toString().toLowerCase(Locale.ENGLISH))
          .executes(ctx -> position(ctx, position)));
    }

    dispatcher.register(
        Commands.literal("minimap")
            .then(Commands.literal("enable")
                .executes(this::enable))
            .then(Commands.literal("disable")
                .executes(this::disable))
            .then(positionLiteral)
            .then(Commands.literal("marker")
                .then(Commands.literal("add")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("icon", StringArgumentType.string())
                            .suggests(this::suggestIcons)
                            .executes(this::addMarker))))
                .then(Commands.literal("set")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(this::suggestMarkers)
                        .then(Commands.literal("icon")
                            .then(Commands.argument("icon", StringArgumentType.string())
                                .suggests(this::suggestIcons)
                                .executes(this::changeMarkerIcon)))
                        .then(Commands.literal("name")
                            .then(Commands.argument("new_name", StringArgumentType.string())
                                .executes(this::renameMarker)))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(this::suggestMarkers)
                        .executes(this::removeMarker))))
            .then(Commands.literal("fullscreen")
                .executes(this::fullscreen))
    );
  }

  private int enable(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer serverPlayer = ctx.getSource().getPlayerOrException();
    Player player = serverPlayer.getBukkitEntity();

    getPlugin().minimapListener().enableMinimap(player);
    return 1;
  }

  private int disable(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer serverPlayer = ctx.getSource().getPlayerOrException();
    Player player = serverPlayer.getBukkitEntity();

    getPlugin().minimapListener().disableMinimap(player);
    return 1;
  }

  private int position(CommandContext<CommandSourceStack> ctx, MinimapScreenPosition position) throws CommandSyntaxException {
    ServerPlayer serverPlayer = ctx.getSource().getPlayerOrException();
    Player player = serverPlayer.getBukkitEntity();

    Minimap minimap = getPlugin().getPlayerMinimap(player);
    if (minimap == null) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "Minimap is disabled.");
    }

    minimap.screenPosition(position);
    minimap.update(getPlugin());
    return 1;
  }

  private CompletableFuture<Suggestions> suggestIcons(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
    Set<String> keys = getPlugin().iconProvider().genericIconKeys();
    keys.stream()
        .filter(key -> key.startsWith(builder.getRemaining()))
        .forEach(builder::suggest);
    return builder.buildFuture();
  }

  private CompletableFuture<Suggestions> suggestMarkers(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
    ServerPlayer serverPlayer = ctx.getSource().getPlayer();
    if (serverPlayer == null) {
      return builder.buildFuture();
    }

    Player player = serverPlayer.getBukkitEntity();
    Minimap minimap = getPlugin().getPlayerMinimap(player);
    if (minimap == null) {
      return builder.buildFuture();
    }

    minimap.secondaryLayers().forEach((name, value) -> {
      if ("player".equals(name) || "death_point".equals(name)) {
        return;
      }

      if (!name.startsWith(builder.getRemaining())) {
        return;
      }

      String suggestedName = name;
      if (suggestedName.contains(" ")) {
        suggestedName = "\"" + suggestedName + "\"";
      }

      builder.suggest(suggestedName);
    });

    return builder.buildFuture();
  }

  private MinimapIcon minimapIcon(String iconName) throws CommandSyntaxException {
    MinimapIcon icon = null;
    if (!getPlugin().iconProvider().specialIconKeys().contains(iconName)) {
      icon = getPlugin().iconProvider().getIcon(iconName);
    }

    if (icon == null) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "Invalid icon.");
    }

    return icon;
  }

  private void modifyMarker(Player player, String markerName, BiConsumer<Minimap, SecondaryMinimapLayer> consumer) throws CommandSyntaxException {
    Minimap minimap = getPlugin().getPlayerMinimap(player);
    if (minimap == null) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "Minimap is disabled.");
    }

    if ("player".equals(markerName) || "death_point".equals(markerName)) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "This marker cannot be modified.");
    }

    SecondaryMinimapLayer marker = minimap.secondaryLayers().get(markerName);
    if (marker == null) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "There is no such marker.");
    }

    consumer.accept(minimap, marker);
    minimap.update(getPlugin());
  }

  private int changeMarkerIcon(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    String markerName = StringArgumentType.getString(ctx, "name");
    String iconName = StringArgumentType.getString(ctx, "icon");

    ServerPlayer serverPlayer = ctx.getSource().getPlayerOrException();
    Player player = serverPlayer.getBukkitEntity();

    MinimapIcon icon = minimapIcon(iconName);

    modifyMarker(player, markerName, (minimap, marker) ->
        marker.setRenderer(new MinimapIconRenderer(icon)));

    return 1;
  }

  private int renameMarker(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    String markerName = StringArgumentType.getString(ctx, "name");
    String newMarkerName = StringArgumentType.getString(ctx, "new_name");

    ServerPlayer serverPlayer = ctx.getSource().getPlayerOrException();
    Player player = serverPlayer.getBukkitEntity();

    modifyMarker(player, markerName, (minimap, marker) -> {
      if (minimap.secondaryLayers().remove(markerName, marker)) {
        minimap.secondaryLayers().put(newMarkerName, marker);
      }
    });

    return 1;
  }

  private int removeMarker(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    String markerName = StringArgumentType.getString(ctx, "name");

    ServerPlayer serverPlayer = ctx.getSource().getPlayerOrException();
    Player player = serverPlayer.getBukkitEntity();

    Minimap minimap = getPlugin().getPlayerMinimap(player);
    if (minimap == null) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "Minimap is disabled.");
    }

    if ("player".equals(markerName) || "death_point".equals(markerName)) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "This marker cannot be removed.");
    }

    SecondaryMinimapLayer marker = minimap.secondaryLayers().remove(markerName);
    if (marker == null) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "There is no such marker.");
    }

    getPlugin().packetSender().despawnLayer(minimap.holder(), marker.getBaseLayer());
    minimap.update(getPlugin());

    return 1;
  }

  private int addMarker(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    String markerName = StringArgumentType.getString(ctx, "name");
    String iconName = StringArgumentType.getString(ctx, "icon");

    ServerPlayer serverPlayer = ctx.getSource().getPlayerOrException();
    Player player = serverPlayer.getBukkitEntity();

    Minimap minimap = getPlugin().getPlayerMinimap(player);
    if (minimap == null) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "Minimap is disabled.");
    }

    MinimapIcon icon = minimapIcon(iconName);

    if ("player".equals(markerName) || "death_point".equals(markerName)) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "This marker name is unavailable.");
    }

    if (minimap.secondaryLayers().containsKey(markerName)) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "Marker with this name already exists.");
    }

    float depth = 0.05F + minimap.secondaryLayers().size() * 0.01F;
    MinimapLayer iconBaseLayer = getPlugin().clientsideMinimapFactory().createMinimapLayer(player.getWorld(), null);
    SecondaryMinimapLayer iconLayer = new SecondaryMinimapLayer(iconBaseLayer, new MinimapIconRenderer(icon), true,
        Config.instance().markers.customMarkers.stickToBorder, (int) player.getX(), (int) player.getZ(), depth);
    minimap.secondaryLayers().put(markerName, iconLayer);

    getPlugin().packetSender().spawnLayer(player, iconBaseLayer);
    minimap.update(getPlugin());

    return 1;
  }

  private int fullscreen(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer serverPlayer = ctx.getSource().getPlayerOrException();
    Player player = serverPlayer.getBukkitEntity();

    Minimap minimap = getPlugin().getPlayerMinimap(player);
    if (minimap == null) {
      throw new CommandSyntaxException(new CommandExceptionType() {}, () -> "Minimap is disabled.");
    }

    if (getPlugin().getFullscreenMinimap(player) != null) {
      getPlugin().minimapListener().closeFullscreen(player);
      return 1;
    }

    FullscreenMinimap fullscreenMinimap = FullscreenMinimap.create(getPlugin(), minimap);
    SteerableLockedView view = getPlugin().minimapListener().openFullscreen(fullscreenMinimap);

    view.onSneak(v -> getPlugin().minimapListener().closeFullscreen(player));

    return 1;
  }
}
