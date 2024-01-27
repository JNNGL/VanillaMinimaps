package com.jnngl.vanillaminimaps.config;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import net.elytrium.serializer.LoadResult;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class BlockConfig extends YamlSerializable {

  private static final BlockConfig INSTANCE = new BlockConfig();

  public static BlockConfig instance() {
    return INSTANCE;
  }

  BlockConfig() {
  }

  @Comment({
      @CommentValue("Custom blockstate colors."),
      @CommentValue("Example: \"minecraft:note_block[instrument=harp,note=0,powered=false]\": \"minecraft:obsidian\"")
  })
  public Map<String, String> blockOverrides = Map.of();

  @Getter
  private transient Map<BlockState, MapColor> resolvedOverrides;

  @Override
  public LoadResult reload(Path path) {
    LoadResult result = super.reload(path);
    if (blockOverrides != null) {
      resolvedOverrides = new HashMap<>();
      blockOverrides.forEach((key, value) -> {
        try {
          var lookup = BuiltInRegistries.BLOCK.asLookup();
          BlockStateParser.BlockResult block = BlockStateParser.parseForBlock(lookup, new StringReader(key), false);
          MapColor color;
          try {
            color = MapColor.byId(Integer.parseInt(value));
          } catch (NumberFormatException e) {
            BlockStateParser.BlockResult mapping = BlockStateParser.parseForBlock(lookup, new StringReader(value), false);
            color = mapping.blockState().getMapColor(null, null);
          }
          resolvedOverrides.put(block.blockState(), color);
        } catch (CommandSyntaxException e) {
          throw new RuntimeException(e);
        }
      });
    } else {
      resolvedOverrides = Map.of();
    }

    return result;
  }
}
