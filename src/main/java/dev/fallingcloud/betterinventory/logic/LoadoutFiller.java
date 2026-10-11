package dev.fallingcloud.betterinventory.logic;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.fallingcloud.betterinventory.inv.ComponentBackedHandler;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import dev.fallingcloud.betterinventory.registry.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class LoadoutFiller {
   private LoadoutFiller() {
   }

   private static LoadoutFiller.Entry e(Item item, int min, int max) {
      return new LoadoutFiller.Entry(item, min, max);
   }

   private static List<LoadoutFiller.Entry> grid(int stage) {
      return switch (stage) {
         case 1 -> List.of(
            e(Items.OAK_LOG, 5, 10),
            e(Items.OAK_PLANKS, 7, 14),
            e(Items.STICK, 3, 7),
            e(Items.COBBLESTONE, 9, 19),
            e(Items.DIRT, 4, 12),
            e(Items.SAND, 3, 8),
            e(Items.GRAVEL, 2, 7),
            e(Items.FLINT, 1, 3),
            e(Items.COAL, 2, 6),
            e(Items.RAW_IRON, 1, 4),
            e(Items.CHEST, 1, 3),
            e(Items.FURNACE, 1, 2),
            e(Items.WHEAT_SEEDS, 2, 5),
            e(Items.OAK_SAPLING, 2, 8),
            e(Items.APPLE, 2, 7),
            e(Items.COOKED_COD, 1, 3),
            e(Items.STRING, 1, 3),
            e(Items.BONE, 1, 4),
            e(Items.ROTTEN_FLESH, 2, 6),
            e(Items.WOODEN_HOE, 1, 1)
         );
         case 2 -> List.of(
            e(Items.IRON_INGOT, 4, 10),
            e(Items.RAW_IRON, 4, 14),
            e(Items.RAW_COPPER, 6, 16),
            e(Items.COAL, 7, 19),
            e(Items.COBBLESTONE, 14, 32),
            e(Items.COBBLED_DEEPSLATE, 12, 28),
            e(Items.OAK_PLANKS, 9, 22),
            e(Items.OAK_LOG, 6, 14),
            e(Items.SAND, 6, 17),
            e(Items.GRAVEL, 4, 12),
            e(Items.DIRT, 8, 22),
            e(Items.FLINT, 2, 6),
            e(Items.LAPIS_LAZULI, 2, 8),
            e(Items.REDSTONE, 4, 13),
            e(Items.CHEST, 1, 4),
            e(Items.FURNACE, 1, 3),
            e(Items.STRING, 2, 6),
            e(Items.BONE, 2, 8),
            e(Items.ROTTEN_FLESH, 4, 10),
            e(Items.WHEAT_SEEDS, 3, 9),
            e(Items.BUCKET, 1, 2),
            e(Items.APPLE, 2, 4)
         );
         case 3 -> List.of(
            e(Items.DIAMOND, 4, 15),
            e(Items.GOLD_INGOT, 8, 24),
            e(Items.IRON_INGOT, 17, 43),
            e(Items.REDSTONE, 20, 52),
            e(Items.LAPIS_LAZULI, 13, 34),
            e(Items.QUARTZ, 15, 41),
            e(Items.OBSIDIAN, 6, 18),
            e(Items.COAL, 27, 62),
            e(Items.STONE_BRICKS, 38, 85),
            e(Items.COBBLED_DEEPSLATE, 34, 77),
            e(Items.OAK_PLANKS, 29, 67),
            e(Items.GLASS, 18, 46),
            e(Items.NETHERRACK, 24, 59),
            e(Items.SAND, 21, 50),
            e(Items.RAW_GOLD, 7, 21),
            e(Items.RAW_COPPER, 18, 48),
            e(Items.ENDER_PEARL, 2, 8),
            e(Items.BLAZE_ROD, 2, 7),
            e(Items.CHEST, 2, 5),
            e(Items.BUCKET, 1, 2),
            e(Items.STRING, 7, 21),
            e(Items.BONE, 8, 25),
            e(Items.ROTTEN_FLESH, 11, 32),
            e(Items.FLINT, 7, 20)
         );
         case 4 -> List.of(
            e(Items.NETHERITE_SCRAP, 2, 6),
            e(Items.DIAMOND, 39, 95),
            e(Items.EMERALD, 25, 74),
            e(Items.GOLD_INGOT, 49, 120),
            e(Items.IRON_INGOT, 81, 180),
            e(Items.REDSTONE, 92, 204),
            e(Items.QUARTZ, 67, 155),
            e(Items.OBSIDIAN, 32, 84),
            e(Items.DEEPSLATE_TILES, 109, 225),
            e(Items.STONE_BRICKS, 120, 225),
            e(Items.GLASS, 74, 165),
            e(Items.SMOOTH_QUARTZ, 95, 197),
            e(Items.FIREWORK_ROCKET, 81, 215),
            e(Items.SHULKER_BOX, 1, 3),
            e(Items.ENDER_PEARL, 18, 49),
            e(Items.BLAZE_POWDER, 25, 74),
            e(Items.EXPERIENCE_BOTTLE, 39, 109),
            e(Items.GOLDEN_APPLE, 2, 7),
            e(Items.AMETHYST_SHARD, 21, 63),
            e(Items.ECHO_SHARD, 2, 6),
            e(Items.ANCIENT_DEBRIS, 1, 4),
            e(Items.CHORUS_FRUIT, 28, 84),
            e(Items.ENCHANTED_BOOK, 1, 3),
            e(Items.NETHERRACK, 67, 155)
         );
         default -> List.of(
            e(Items.NETHERITE_INGOT, 24, 72),
            e(Items.NETHERITE_SCRAP, 32, 96),
            e(Items.DIAMOND, 216, 464),
            e(Items.EMERALD, 152, 352),
            e(Items.ANCIENT_DEBRIS, 2, 7),
            e(Items.ECHO_SHARD, 40, 112),
            e(Items.SHULKER_SHELL, 56, 168),
            e(Items.AMETHYST_SHARD, 104, 272),
            e(Items.FIREWORK_ROCKET, 328, 512),
            e(Items.SHULKER_BOX, 2, 5),
            e(Items.ENDER_PEARL, 72, 192),
            e(Items.TOTEM_OF_UNDYING, 1, 3),
            e(Items.ENCHANTED_GOLDEN_APPLE, 1, 4),
            e(Items.EXPERIENCE_BOTTLE, 216, 488),
            e(Items.DRAGON_BREATH, 2, 7),
            e(Items.END_CRYSTAL, 1, 5),
            e(Items.SMOOTH_QUARTZ, 296, 512),
            e(Items.DEEPSLATE_TILES, 328, 512),
            e(Items.GLASS, 232, 464),
            e(Items.CONCRETE.pick(DyeColor.WHITE), 264, 488),
            e(Items.BLAZE_POWDER, 88, 232),
            e(Items.GLOWSTONE_DUST, 112, 288),
            e(Items.CHORUS_FRUIT, 136, 328),
            e(Items.WITHER_SKELETON_SKULL, 1, 3),
            e(Items.ENCHANTED_BOOK, 2, 5),
            e(Items.NETHERITE_BLOCK, 1, 3)
         );
      };
   }

   private static List<LoadoutFiller.Entry> gather(int stage) {
      return switch (stage) {
         case 1 -> List.of(
            e(Items.ANDESITE, 4, 12),
            e(Items.DIORITE, 3, 10),
            e(Items.GRANITE, 2, 9),
            e(Items.COBBLED_DEEPSLATE, 2, 8),
            e(Items.CLAY_BALL, 1, 4),
            e(Items.SUGAR_CANE, 1, 3),
            e(Items.POPPY, 2, 6),
            e(Items.DANDELION, 2, 5)
         );
         case 2 -> List.of(
            e(Items.ANDESITE, 8, 26),
            e(Items.DIORITE, 6, 21),
            e(Items.GRANITE, 6, 18),
            e(Items.TUFF, 4, 16),
            e(Items.DEEPSLATE, 10, 29),
            e(Items.CLAY_BALL, 2, 8),
            e(Items.GUNPOWDER, 2, 6),
            e(Items.SPIDER_EYE, 2, 7),
            e(Items.OAK_SAPLING, 2, 4),
            e(Items.SUGAR_CANE, 2, 6)
         );
         case 3 -> List.of(
            e(Items.ANDESITE, 29, 81),
            e(Items.DIORITE, 24, 66),
            e(Items.GRANITE, 20, 57),
            e(Items.TUFF, 17, 50),
            e(Items.DEEPSLATE, 41, 90),
            e(Items.COBBLESTONE, 46, 90),
            e(Items.GRAVEL, 18, 52),
            e(Items.DIRT, 27, 64),
            e(Items.CLAY_BALL, 8, 27),
            e(Items.GUNPOWDER, 6, 18),
            e(Items.SPIDER_EYE, 4, 13),
            e(Items.MAGMA_CREAM, 2, 7)
         );
         case 4 -> List.of(
            e(Items.DEEPSLATE, 130, 225),
            e(Items.COBBLESTONE, 144, 225),
            e(Items.ANDESITE, 84, 201),
            e(Items.TUFF, 60, 155),
            e(Items.BLACKSTONE, 74, 172),
            e(Items.END_STONE, 56, 144),
            e(Items.GRAVEL, 49, 134),
            e(Items.DIRT, 77, 180),
            e(Items.MAGMA_CREAM, 11, 39),
            e(Items.GHAST_TEAR, 2, 6),
            e(Items.SOUL_SAND, 46, 120),
            e(Items.GLOWSTONE_DUST, 32, 92)
         );
         default -> List.of(
            e(Items.DEEPSLATE, 352, 512),
            e(Items.COBBLESTONE, 376, 512),
            e(Items.BLACKSTONE, 232, 488),
            e(Items.END_STONE, 192, 424),
            e(Items.BASALT, 168, 376),
            e(Items.SOUL_SAND, 136, 328),
            e(Items.MAGMA_BLOCK, 104, 272),
            e(Items.OBSIDIAN, 88, 232),
            e(Items.GRAVEL, 128, 312),
            e(Items.NETHERRACK, 264, 512),
            e(Items.GHAST_TEAR, 24, 72),
            e(Items.MAGMA_CREAM, 40, 128)
         );
      };
   }

   private static List<LoadoutFiller.Entry> blocks(int stage) {
      return switch (stage) {
         case 1 -> List.of(
            e(Items.OAK_PLANKS, 14, 19),
            e(Items.COBBLESTONE, 16, 19),
            e(Items.DIRT, 10, 19),
            e(Items.SAND, 8, 17),
            e(Items.OAK_LOG, 6, 13),
            e(Items.GRAVEL, 5, 12),
            e(Items.OAK_SLAB, 4, 9),
            e(Items.OAK_STAIRS, 3, 8),
            e(Items.COARSE_DIRT, 6, 14),
            e(Items.OAK_FENCE, 3, 9),
            e(Items.OAK_DOOR, 1, 3),
            e(Items.OAK_TRAPDOOR, 1, 4),
            e(Items.CRAFTING_TABLE, 1, 3),
            e(Items.TORCH, 8, 18),
            e(Items.CHEST, 2, 7),
            e(Items.LADDER, 3, 8)
         );
         case 2 -> List.of(
            e(Items.COBBLESTONE, 28, 32),
            e(Items.STONE, 22, 32),
            e(Items.OAK_PLANKS, 26, 32),
            e(Items.COBBLED_DEEPSLATE, 20, 32),
            e(Items.SAND, 16, 30),
            e(Items.GLASS, 8, 21),
            e(Items.SANDSTONE, 10, 24),
            e(Items.OAK_STAIRS, 7, 18),
            e(Items.OAK_SLAB, 10, 22),
            e(Items.DIRT, 20, 32),
            e(Items.SMOOTH_STONE, 14, 29),
            e(Items.STONE_BRICKS, 12, 26),
            e(Items.COBBLESTONE_STAIRS, 8, 20),
            e(Items.STONE_SLAB, 10, 24),
            e(Items.OAK_FENCE, 6, 16),
            e(Items.TORCH, 16, 32),
            e(Items.GLASS_PANE, 7, 18),
            e(Items.OAK_DOOR, 2, 6),
            e(Items.LADDER, 6, 14),
            e(Items.CHEST, 2, 4)
         );
         case 3 -> List.of(
            e(Items.STONE_BRICKS, 69, 90),
            e(Items.DEEPSLATE_BRICKS, 57, 90),
            e(Items.STONE, 74, 90),
            e(Items.OAK_PLANKS, 66, 90),
            e(Items.GLASS, 41, 81),
            e(Items.SANDSTONE, 34, 71),
            e(Items.TERRACOTTA, 25, 60),
            e(Items.SMOOTH_STONE, 38, 78),
            e(Items.OAK_STAIRS, 29, 66),
            e(Items.OAK_SLAB, 36, 74),
            e(Items.SAND, 48, 87),
            e(Items.COBBLESTONE, 62, 90),
            e(Items.POLISHED_DEEPSLATE, 48, 90),
            e(Items.SMOOTH_STONE_SLAB, 38, 80),
            e(Items.STONE_BRICK_STAIRS, 32, 71),
            e(Items.GLASS_PANE, 27, 62),
            e(Items.BRICKS, 29, 66),
            e(Items.TORCH, 57, 90),
            e(Items.OAK_FENCE, 24, 55),
            e(Items.CHISELED_STONE_BRICKS, 18, 43),
            e(Items.LANTERN, 15, 38),
            e(Items.CHEST, 6, 17),
            e(Items.OAK_DOOR, 7, 20),
            e(Items.LADDER, 20, 50)
         );
         case 4 -> List.of(
            e(Items.DEEPSLATE_TILES, 201, 225),
            e(Items.SMOOTH_QUARTZ, 180, 225),
            e(Items.STONE_BRICKS, 190, 225),
            e(Items.CONCRETE.pick(DyeColor.WHITE), 165, 225),
            e(Items.GLASS, 137, 225),
            e(Items.POLISHED_DEEPSLATE, 151, 225),
            e(Items.COPPER_BLOCK.weathering().unaffected(), 60, 137),
            e(Items.TERRACOTTA, 95, 197),
            e(Items.SANDSTONE, 109, 215),
            e(Items.OAK_PLANKS, 155, 225),
            e(Items.SEA_LANTERN, 46, 116),
            e(Items.AMETHYST_BLOCK, 39, 102),
            e(Items.POLISHED_BLACKSTONE, 120, 225),
            e(Items.END_STONE_BRICKS, 102, 215),
            e(Items.PURPUR_BLOCK, 84, 187),
            e(Items.BRICKS, 95, 201),
            e(Items.GLASS_PANE, 81, 180),
            e(Items.PRISMARINE, 67, 155),
            e(Items.LANTERN, 60, 144),
            e(Items.CHISELED_DEEPSLATE, 74, 165),
            e(Items.STONE_BRICK_STAIRS, 92, 197),
            e(Items.SMOOTH_STONE_SLAB, 109, 218),
            e(Items.CHEST, 18, 49),
            e(Items.OAK_DOOR, 21, 60)
         );
         default -> List.of(
            e(Items.SMOOTH_QUARTZ, 488, 512),
            e(Items.DEEPSLATE_TILES, 464, 512),
            e(Items.CONCRETE.pick(DyeColor.WHITE), 456, 512),
            e(Items.POLISHED_BLACKSTONE, 408, 512),
            e(Items.SEA_LANTERN, 272, 512),
            e(Items.GLASS, 376, 512),
            e(Items.AMETHYST_BLOCK, 216, 464),
            e(Items.COPPER_BLOCK.weathering().unaffected(), 192, 424),
            e(Items.PURPUR_BLOCK, 248, 488),
            e(Items.END_STONE_BRICKS, 296, 512),
            e(Items.STONE_BRICKS, 392, 512),
            e(Items.TERRACOTTA, 264, 496),
            e(Items.PRISMARINE_BRICKS, 272, 512),
            e(Items.DARK_PRISMARINE, 232, 488),
            e(Items.BLACKSTONE, 248, 512),
            e(Items.CHISELED_DEEPSLATE, 216, 456),
            e(Items.BRICKS, 192, 424),
            e(Items.GLASS_PANE, 208, 448),
            e(Items.LANTERN, 168, 376),
            e(Items.OBSIDIAN, 136, 328),
            e(Items.CRYING_OBSIDIAN, 72, 192),
            e(Items.STONE_BRICK_STAIRS, 232, 472),
            e(Items.SMOOTH_STONE_SLAB, 264, 512),
            e(Items.CHEST, 48, 136)
         );
      };
   }

   private static List<LoadoutFiller.Entry> mats(int stage) {
      return switch (stage) {
         case 1 -> List.of(
            e(Items.COAL, 5, 13),
            e(Items.RAW_IRON, 3, 9),
            e(Items.STICK, 7, 18),
            e(Items.STRING, 2, 6),
            e(Items.FLINT, 2, 5),
            e(Items.BONE, 2, 7),
            e(Items.GUNPOWDER, 1, 3),
            e(Items.LEATHER, 1, 4),
            e(Items.CLAY_BALL, 3, 7),
            e(Items.SUGAR_CANE, 2, 6),
            e(Items.PAPER, 2, 5),
            e(Items.BOOK, 1, 3),
            e(Items.WHEAT_SEEDS, 4, 10),
            e(Items.OAK_SAPLING, 2, 6),
            e(Items.EGG, 1, 4),
            e(Items.FEATHER, 2, 7)
         );
         case 2 -> List.of(
            e(Items.IRON_INGOT, 10, 24),
            e(Items.COAL, 14, 30),
            e(Items.RAW_COPPER, 9, 22),
            e(Items.LAPIS_LAZULI, 6, 14),
            e(Items.REDSTONE, 8, 20),
            e(Items.STICK, 16, 32),
            e(Items.STRING, 4, 12),
            e(Items.LEATHER, 4, 10),
            e(Items.FLINT, 4, 11),
            e(Items.GUNPOWDER, 2, 7),
            e(Items.COPPER_INGOT, 7, 17),
            e(Items.CLAY_BALL, 6, 14),
            e(Items.PAPER, 4, 12),
            e(Items.BOOK, 2, 8),
            e(Items.EGG, 3, 8),
            e(Items.FEATHER, 4, 13),
            e(Items.WHEAT_SEEDS, 8, 22),
            e(Items.SUGAR_CANE, 6, 14),
            e(Items.BAMBOO, 6, 17),
            e(Items.COAL_BLOCK, 2, 6)
         );
         case 3 -> List.of(
            e(Items.DIAMOND, 13, 34),
            e(Items.GOLD_INGOT, 24, 55),
            e(Items.IRON_INGOT, 43, 90),
            e(Items.REDSTONE, 38, 81),
            e(Items.LAPIS_LAZULI, 27, 62),
            e(Items.QUARTZ, 32, 71),
            e(Items.OBSIDIAN, 15, 41),
            e(Items.COAL, 50, 90),
            e(Items.BLAZE_ROD, 8, 24),
            e(Items.ENDER_PEARL, 7, 20),
            e(Items.STRING, 18, 46),
            e(Items.LEATHER, 15, 38),
            e(Items.COPPER_INGOT, 29, 66),
            e(Items.AMETHYST_SHARD, 13, 34),
            e(Items.PAPER, 24, 57),
            e(Items.BOOK, 15, 38),
            e(Items.GLOWSTONE_DUST, 18, 46),
            e(Items.MAGMA_CREAM, 7, 21),
            e(Items.SLIME_BALL, 10, 29),
            e(Items.HONEYCOMB, 8, 25),
            e(Items.CLAY_BALL, 20, 50),
            e(Items.BAMBOO, 29, 66),
            e(Items.FEATHER, 18, 48),
            e(Items.COAL_BLOCK, 7, 24)
         );
         case 4 -> List.of(
            e(Items.NETHERITE_SCRAP, 14, 39),
            e(Items.DIAMOND, 95, 204),
            e(Items.EMERALD, 67, 155),
            e(Items.GOLD_INGOT, 120, 225),
            e(Items.IRON_INGOT, 165, 225),
            e(Items.REDSTONE, 144, 225),
            e(Items.QUARTZ, 130, 225),
            e(Items.OBSIDIAN, 74, 165),
            e(Items.AMETHYST_SHARD, 60, 137),
            e(Items.ECHO_SHARD, 18, 49),
            e(Items.BLAZE_ROD, 39, 95),
            e(Items.ENDER_PEARL, 32, 81),
            e(Items.COPPER_INGOT, 109, 225),
            e(Items.GLOWSTONE_DUST, 74, 165),
            e(Items.SLIME_BALL, 46, 120),
            e(Items.HONEYCOMB, 39, 102),
            e(Items.PAPER, 84, 187),
            e(Items.BOOK, 60, 137),
            e(Items.PRISMARINE_SHARD, 49, 127),
            e(Items.NAUTILUS_SHELL, 11, 32),
            e(Items.PHANTOM_MEMBRANE, 18, 56),
            e(Items.GOLD_BLOCK, 11, 39),
            e(Items.IRON_BLOCK, 18, 60),
            e(Items.COAL_BLOCK, 25, 74)
         );
         default -> List.of(
            e(Items.NETHERITE_INGOT, 40, 112),
            e(Items.DIAMOND_BLOCK, 24, 72),
            e(Items.EMERALD_BLOCK, 2, 8),
            e(Items.IRON_BLOCK, 56, 168),
            e(Items.GOLD_BLOCK, 40, 136),
            e(Items.DIAMOND, 352, 512),
            e(Items.EMERALD, 248, 488),
            e(Items.REDSTONE, 408, 512),
            e(Items.QUARTZ, 376, 512),
            e(Items.ECHO_SHARD, 56, 152),
            e(Items.SHULKER_SHELL, 88, 216),
            e(Items.ANCIENT_DEBRIS, 24, 88),
            e(Items.COPPER_INGOT, 328, 512),
            e(Items.GLOWSTONE_DUST, 216, 464),
            e(Items.SLIME_BALL, 152, 352),
            e(Items.HONEYCOMB, 136, 312),
            e(Items.PAPER, 248, 512),
            e(Items.BOOK, 168, 376),
            e(Items.PRISMARINE_SHARD, 184, 408),
            e(Items.NAUTILUS_SHELL, 40, 112),
            e(Items.PHANTOM_MEMBRANE, 72, 192),
            e(Items.COAL_BLOCK, 88, 232),
            e(Items.COPPER_BLOCK.weathering().unaffected(), 56, 168),
            e(Items.GOLD_INGOT, 296, 512)
         );
      };
   }

   private static List<LoadoutFiller.Entry> food(int stage) {
      return switch (stage) {
         case 1 -> List.of(
            e(Items.BREAD, 4, 10),
            e(Items.APPLE, 2, 6),
            e(Items.CARROT, 3, 8),
            e(Items.POTATO, 3, 7),
            e(Items.COOKED_COD, 2, 6),
            e(Items.COOKED_PORKCHOP, 2, 5),
            e(Items.WHEAT, 4, 11),
            e(Items.MELON_SLICE, 2, 4),
            e(Items.COOKED_CHICKEN, 3, 7),
            e(Items.COOKED_MUTTON, 2, 6),
            e(Items.BEETROOT, 2, 7),
            e(Items.DRIED_KELP, 3, 9),
            e(Items.PUMPKIN_PIE, 1, 3),
            e(Items.COOKIE, 2, 5),
            e(Items.MILK_BUCKET, 1, 1),
            e(Items.SWEET_BERRIES, 2, 6)
         );
         case 2 -> List.of(
            e(Items.COOKED_PORKCHOP, 8, 20),
            e(Items.BREAD, 7, 17),
            e(Items.COOKED_BEEF, 6, 14),
            e(Items.CARROT, 8, 19),
            e(Items.POTATO, 6, 16),
            e(Items.BAKED_POTATO, 6, 14),
            e(Items.APPLE, 4, 10),
            e(Items.WHEAT, 10, 26),
            e(Items.MELON_SLICE, 3, 8),
            e(Items.SWEET_BERRIES, 2, 7),
            e(Items.COOKED_CHICKEN, 6, 16),
            e(Items.COOKED_MUTTON, 6, 14),
            e(Items.BEETROOT, 6, 14),
            e(Items.DRIED_KELP, 8, 20),
            e(Items.PUMPKIN_PIE, 2, 7),
            e(Items.COOKIE, 4, 12),
            e(Items.MILK_BUCKET, 1, 2),
            e(Items.GOLDEN_APPLE, 1, 3),
            e(Items.BEETROOT_SOUP, 1, 3),
            e(Items.MUSHROOM_STEW, 1, 3)
         );
         case 3 -> List.of(
            e(Items.COOKED_BEEF, 34, 77),
            e(Items.GOLDEN_CARROT, 13, 32),
            e(Items.BREAD, 27, 62),
            e(Items.COOKED_PORKCHOP, 24, 55),
            e(Items.BAKED_POTATO, 29, 66),
            e(Items.CARROT, 25, 59),
            e(Items.WHEAT, 38, 85),
            e(Items.NETHER_WART, 18, 48),
            e(Items.GLASS_BOTTLE, 10, 27),
            e(Items.SUGAR, 15, 39),
            e(Items.MELON_SLICE, 13, 34),
            e(Items.APPLE, 17, 43),
            e(Items.COOKED_CHICKEN, 24, 55),
            e(Items.COOKED_MUTTON, 20, 48),
            e(Items.BEETROOT, 22, 53),
            e(Items.DRIED_KELP, 29, 66),
            e(Items.PUMPKIN_PIE, 10, 27),
            e(Items.COOKIE, 18, 43),
            e(Items.MILK_BUCKET, 1, 3),
            e(Items.HONEY_BOTTLE, 6, 17),
            e(Items.BEETROOT_SOUP, 1, 4),
            e(Items.RABBIT_STEW, 1, 3),
            e(Items.SPIDER_EYE, 7, 22),
            e(Items.BROWN_MUSHROOM, 13, 38)
         );
         case 4 -> List.of(
            e(Items.GOLDEN_CARROT, 95, 215),
            e(Items.COOKED_BEEF, 109, 225),
            e(Items.GOLDEN_APPLE, 21, 60),
            e(Items.BREAD, 84, 187),
            e(Items.BAKED_POTATO, 92, 201),
            e(Items.POTION, 2, 5),
            e(Items.SPLASH_POTION, 2, 4),
            e(Items.NETHER_WART, 74, 165),
            e(Items.GLASS_BOTTLE, 46, 120),
            e(Items.SUGAR, 60, 144),
            e(Items.GLISTERING_MELON_SLICE, 32, 84),
            e(Items.FERMENTED_SPIDER_EYE, 14, 46),
            e(Items.COOKED_CHICKEN, 74, 165),
            e(Items.COOKED_MUTTON, 67, 155),
            e(Items.DRIED_KELP, 95, 204),
            e(Items.PUMPKIN_PIE, 39, 95),
            e(Items.COOKIE, 60, 144),
            e(Items.HONEY_BOTTLE, 25, 67),
            e(Items.MILK_BUCKET, 1, 4),
            e(Items.RABBIT_STEW, 1, 5),
            e(Items.BEETROOT, 74, 165),
            e(Items.BROWN_MUSHROOM, 46, 120),
            e(Items.SPIDER_EYE, 32, 84),
            e(Items.SUGAR_CANE, 60, 144)
         );
         default -> List.of(
            e(Items.ENCHANTED_GOLDEN_APPLE, 2, 6),
            e(Items.GOLDEN_APPLE, 88, 232),
            e(Items.GOLDEN_CARROT, 272, 512),
            e(Items.COOKED_BEEF, 328, 512),
            e(Items.POTION, 3, 7),
            e(Items.SPLASH_POTION, 2, 6),
            e(Items.LINGERING_POTION, 1, 4),
            e(Items.NETHER_WART, 216, 464),
            e(Items.GLASS_BOTTLE, 136, 352),
            e(Items.SUGAR, 168, 408),
            e(Items.GLISTERING_MELON_SLICE, 104, 272),
            e(Items.BLAZE_POWDER, 152, 376),
            e(Items.COOKED_CHICKEN, 216, 464),
            e(Items.COOKED_MUTTON, 192, 424),
            e(Items.DRIED_KELP, 272, 512),
            e(Items.PUMPKIN_PIE, 136, 312),
            e(Items.COOKIE, 168, 376),
            e(Items.HONEY_BOTTLE, 88, 216),
            e(Items.MILK_BUCKET, 2, 5),
            e(Items.RABBIT_STEW, 1, 6),
            e(Items.BEETROOT, 208, 448),
            e(Items.BROWN_MUSHROOM, 136, 328),
            e(Items.SPIDER_EYE, 104, 272),
            e(Items.SUGAR_CANE, 192, 424)
         );
      };
   }

   private static List<LoadoutFiller.Entry> spares(int stage) {
      return switch (stage) {
         case 1 -> List.of(
            e(Items.WOODEN_PICKAXE, 1, 1),
            e(Items.STONE_AXE, 1, 1),
            e(Items.TORCH, 7, 16),
            e(Items.CHEST, 2, 6),
            e(Items.CRAFTING_TABLE, 1, 3),
            e(Items.FURNACE, 1, 3),
            e(Items.LADDER, 2, 7),
            e(Items.BOWL, 1, 3),
            e(Items.WOODEN_SHOVEL, 1, 1),
            e(Items.WOODEN_AXE, 1, 1),
            e(Items.BOW, 1, 1),
            e(Items.ARROW, 4, 11),
            e(Items.FISHING_ROD, 1, 1),
            e(Items.OAK_SIGN, 1, 3),
            e(Items.OAK_BOAT, 1, 1),
            e(Items.FLINT_AND_STEEL, 1, 1)
         );
         case 2 -> List.of(
            e(Items.STONE_PICKAXE, 1, 1),
            e(Items.IRON_SHOVEL, 1, 1),
            e(Items.TORCH, 17, 32),
            e(Items.BOW, 1, 1),
            e(Items.ARROW, 8, 22),
            e(Items.CHEST, 3, 8),
            e(Items.LADDER, 6, 16),
            e(Items.BUCKET, 1, 3),
            e(Items.CRAFTING_TABLE, 1, 2),
            e(Items.FISHING_ROD, 1, 1),
            e(Items.IRON_AXE, 1, 1),
            e(Items.SHIELD, 1, 1),
            e(Items.FLINT_AND_STEEL, 1, 1),
            e(Items.OAK_BOAT, 1, 1),
            e(Items.OAK_SIGN, 2, 6),
            e(Items.SHEARS, 1, 1),
            e(Items.COMPASS, 1, 1),
            e(Items.CLOCK, 1, 1),
            e(Items.NAME_TAG, 1, 2),
            e(Items.LEAD, 1, 3)
         );
         case 3 -> List.of(
            e(Items.IRON_PICKAXE, 1, 1),
            e(Items.DIAMOND_AXE, 1, 1),
            e(Items.TORCH, 62, 90),
            e(Items.BOW, 1, 1),
            e(Items.ARROW, 38, 85),
            e(Items.ENDER_CHEST, 1, 1),
            e(Items.ANVIL, 1, 2),
            e(Items.CHEST, 6, 15),
            e(Items.LADDER, 24, 57),
            e(Items.TNT, 4, 15),
            e(Items.FLINT_AND_STEEL, 1, 1),
            e(Items.SPYGLASS, 1, 1),
            e(Items.IRON_AXE, 1, 1),
            e(Items.SHIELD, 1, 1),
            e(Items.SHEARS, 1, 1),
            e(Items.OAK_BOAT, 1, 1),
            e(Items.OAK_SIGN, 8, 24),
            e(Items.COMPASS, 1, 2),
            e(Items.CLOCK, 1, 1),
            e(Items.NAME_TAG, 1, 3),
            e(Items.LEAD, 2, 6),
            e(Items.BUCKET, 1, 3),
            e(Items.MINECART, 1, 4),
            e(Items.POWERED_RAIL, 7, 24)
         );
         case 4 -> List.of(
            e(Items.DIAMOND_PICKAXE, 1, 1),
            e(Items.NETHERITE_SWORD, 1, 1),
            e(Items.ELYTRA, 1, 1),
            e(Items.FIREWORK_ROCKET, 155, 225),
            e(Items.TOTEM_OF_UNDYING, 1, 3),
            e(Items.SHULKER_BOX, 2, 5),
            e(Items.ENDER_CHEST, 1, 2),
            e(Items.ANVIL, 1, 3),
            e(Items.LANTERN, 95, 204),
            e(Items.ARROW, 120, 225),
            e(Items.CROSSBOW, 1, 1),
            e(Items.EXPERIENCE_BOTTLE, 74, 180),
            e(Items.DIAMOND_SHOVEL, 1, 1),
            e(Items.SHIELD, 1, 2),
            e(Items.SHEARS, 1, 1),
            e(Items.OAK_BOAT, 1, 1),
            e(Items.COMPASS, 1, 2),
            e(Items.CLOCK, 1, 2),
            e(Items.NAME_TAG, 2, 5),
            e(Items.LEAD, 3, 8),
            e(Items.BUCKET, 2, 5),
            e(Items.MINECART, 2, 6),
            e(Items.POWERED_RAIL, 32, 92),
            e(Items.SPYGLASS, 1, 1)
         );
         default -> List.of(
            e(Items.NETHERITE_PICKAXE, 1, 1),
            e(Items.NETHERITE_AXE, 1, 1),
            e(Items.TRIDENT, 1, 1),
            e(Items.ELYTRA, 1, 1),
            e(Items.FIREWORK_ROCKET, 456, 512),
            e(Items.TOTEM_OF_UNDYING, 2, 4),
            e(Items.SHULKER_BOX, 3, 7),
            e(Items.BEACON, 1, 2),
            e(Items.ENDER_CHEST, 1, 3),
            e(Items.LANTERN, 312, 512),
            e(Items.ARROW, 376, 512),
            e(Items.CROSSBOW, 1, 1),
            e(Items.NETHERITE_SHOVEL, 1, 1),
            e(Items.SHIELD, 2, 3),
            e(Items.SHEARS, 1, 2),
            e(Items.OAK_BOAT, 1, 2),
            e(Items.COMPASS, 2, 4),
            e(Items.CLOCK, 1, 3),
            e(Items.NAME_TAG, 3, 7),
            e(Items.LEAD, 32, 88),
            e(Items.BUCKET, 3, 7),
            e(Items.MINECART, 3, 8),
            e(Items.POWERED_RAIL, 112, 296),
            e(Items.SPYGLASS, 1, 2)
         );
      };
   }

   private static List<LoadoutFiller.Entry> prettyGrid(int stage, LoadoutFiller.Preset preset) {
      return switch (preset) {
         case BUILDING -> {
            switch (stage) {
               case 1:
                  yield List.of(
                     e(Items.OAK_PLANKS, 12, 19),
                     e(Items.COBBLESTONE, 11, 19),
                     e(Items.OAK_STAIRS, 8, 17),
                     e(Items.OAK_SLAB, 9, 18),
                     e(Items.GLASS, 7, 16),
                     e(Items.GLASS_PANE, 6, 14),
                     e(Items.FLOWER_POT, 8, 17),
                     e(Items.COBBLESTONE_WALL, 6, 13),
                     e(Items.OAK_FENCE, 4, 10),
                     e(Items.OAK_DOOR, 2, 4),
                     e(Items.OAK_TRAPDOOR, 2, 5),
                     e(Items.SCAFFOLDING, 5, 12),
                     e(Items.LADDER, 4, 10),
                     e(Items.OAK_LOG, 6, 14),
                     e(Items.CRAFTING_TABLE, 7, 15),
                     e(Items.CHEST, 1, 4)
                  );
               case 2:
                  yield List.of(
                     e(Items.COBBLESTONE, 20, 32),
                     e(Items.STONE, 18, 32),
                     e(Items.STONE_BRICK_STAIRS, 14, 29),
                     e(Items.STONE_SLAB, 16, 30),
                     e(Items.GLASS, 12, 26),
                     e(Items.GLASS_PANE, 10, 24),
                     e(Items.FLOWER_POT, 13, 28),
                     e(Items.COBBLESTONE_WALL, 10, 22),
                     e(Items.OAK_FENCE, 7, 17),
                     e(Items.OAK_DOOR, 2, 7),
                     e(Items.OAK_TRAPDOOR, 3, 8),
                     e(Items.SCAFFOLDING, 8, 20),
                     e(Items.LADDER, 6, 16),
                     e(Items.OAK_LOG, 10, 24),
                     e(Items.OAK_PLANKS, 17, 32),
                     e(Items.SMOOTH_STONE, 12, 26),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.CHEST, 1, 4)
                  );
               case 3:
                  yield List.of(
                     e(Items.STONE_BRICKS, 57, 90),
                     e(Items.POLISHED_DEEPSLATE, 52, 90),
                     e(Items.STONE_BRICK_STAIRS, 38, 81),
                     e(Items.SMOOTH_STONE_SLAB, 43, 85),
                     e(Items.GLASS, 34, 74),
                     e(Items.GLASS_PANE, 29, 66),
                     e(Items.TERRACOTTA, 36, 78),
                     e(Items.STONE_BRICK_WALL, 27, 62),
                     e(Items.OAK_FENCE, 20, 48),
                     e(Items.OAK_DOOR, 7, 20),
                     e(Items.OAK_TRAPDOOR, 8, 24),
                     e(Items.SCAFFOLDING, 24, 57),
                     e(Items.LADDER, 18, 46),
                     e(Items.OAK_LOG, 29, 66),
                     e(Items.OAK_PLANKS, 48, 90),
                     e(Items.CHISELED_STONE_BRICKS, 32, 71),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.ENDER_CHEST, 1, 4)
                  );
               case 4:
                  yield List.of(
                     e(Items.DEEPSLATE_TILES, 144, 225),
                     e(Items.POLISHED_BLACKSTONE, 130, 225),
                     e(Items.STONE_BRICK_STAIRS, 95, 204),
                     e(Items.SMOOTH_STONE_SLAB, 109, 215),
                     e(Items.GLASS, 84, 187),
                     e(Items.GLASS_PANE, 74, 165),
                     e(Items.CONCRETE.pick(DyeColor.WHITE), 92, 197),
                     e(Items.DEEPSLATE_BRICK_WALL, 67, 155),
                     e(Items.OAK_FENCE, 49, 120),
                     e(Items.OAK_DOOR, 18, 49),
                     e(Items.OAK_TRAPDOOR, 21, 60),
                     e(Items.SCAFFOLDING, 60, 144),
                     e(Items.LADDER, 46, 116),
                     e(Items.OAK_LOG, 74, 165),
                     e(Items.OAK_PLANKS, 120, 225),
                     e(Items.CHISELED_DEEPSLATE, 81, 180),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.SHULKER_BOX, 1, 4)
                  );
               default:
                  yield List.of(
                     e(Items.SMOOTH_QUARTZ, 328, 512),
                     e(Items.POLISHED_BLACKSTONE, 296, 512),
                     e(Items.STONE_BRICK_STAIRS, 216, 464),
                     e(Items.SMOOTH_STONE_SLAB, 248, 488),
                     e(Items.GLASS, 192, 424),
                     e(Items.GLASS_PANE, 168, 376),
                     e(Items.CONCRETE.pick(DyeColor.WHITE), 208, 448),
                     e(Items.DEEPSLATE_BRICK_WALL, 152, 352),
                     e(Items.OAK_FENCE, 112, 272),
                     e(Items.OAK_DOOR, 40, 112),
                     e(Items.OAK_TRAPDOOR, 48, 136),
                     e(Items.SCAFFOLDING, 136, 328),
                     e(Items.LADDER, 104, 264),
                     e(Items.OAK_LOG, 168, 376),
                     e(Items.OAK_PLANKS, 272, 512),
                     e(Items.PURPUR_BLOCK, 184, 408),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.SHULKER_BOX, 1, 4)
                  );
            }
         }
         case COMBAT -> {
            switch (stage) {
               case 1:
                  yield List.of(
                     e(Items.ARROW, 10, 19),
                     e(Items.SPECTRAL_ARROW, 3, 8),
                     e(Items.GOLDEN_APPLE, 2, 4),
                     e(Items.POTION, 2, 5),
                     e(Items.SPLASH_POTION, 1, 4),
                     e(Items.MILK_BUCKET, 1, 1),
                     e(Items.ENDER_PEARL, 1, 3),
                     e(Items.COBWEB, 1, 3),
                     e(Items.OAK_PLANKS, 8, 17),
                     e(Items.OAK_LOG, 5, 12),
                     e(Items.CHEST, 1, 3),
                     e(Items.TIPPED_ARROW, 2, 6),
                     e(Items.COAL, 3, 7),
                     e(Items.IRON_INGOT, 3, 9),
                     e(Items.BONE, 2, 6)
                  );
               case 2:
                  yield List.of(
                     e(Items.ARROW, 17, 32),
                     e(Items.SPECTRAL_ARROW, 6, 14),
                     e(Items.GOLDEN_APPLE, 2, 7),
                     e(Items.POTION, 2, 5),
                     e(Items.SPLASH_POTION, 1, 4),
                     e(Items.MILK_BUCKET, 1, 1),
                     e(Items.ENDER_PEARL, 2, 6),
                     e(Items.COBWEB, 2, 4),
                     e(Items.COBBLESTONE, 14, 29),
                     e(Items.OAK_LOG, 8, 20),
                     e(Items.OAK_PLANKS, 12, 26),
                     e(Items.CHEST, 1, 3),
                     e(Items.TIPPED_ARROW, 4, 10),
                     e(Items.LAPIS_LAZULI, 4, 12),
                     e(Items.IRON_INGOT, 6, 14),
                     e(Items.BONE, 4, 10)
                  );
               case 3:
                  yield List.of(
                     e(Items.ARROW, 48, 90),
                     e(Items.SPECTRAL_ARROW, 15, 38),
                     e(Items.GOLDEN_APPLE, 7, 20),
                     e(Items.POTION, 2, 5),
                     e(Items.SPLASH_POTION, 1, 4),
                     e(Items.MILK_BUCKET, 1, 1),
                     e(Items.ENDER_PEARL, 6, 15),
                     e(Items.COBWEB, 4, 13),
                     e(Items.STONE_BRICKS, 38, 81),
                     e(Items.OAK_LOG, 24, 55),
                     e(Items.OAK_PLANKS, 34, 74),
                     e(Items.ENDER_CHEST, 1, 3),
                     e(Items.TIPPED_ARROW, 10, 27),
                     e(Items.DIAMOND, 13, 34),
                     e(Items.GOLD_INGOT, 15, 41),
                     e(Items.BONE, 10, 29)
                  );
               case 4:
                  yield List.of(
                     e(Items.ARROW, 120, 225),
                     e(Items.SPECTRAL_ARROW, 39, 95),
                     e(Items.GOLDEN_APPLE, 18, 49),
                     e(Items.POTION, 2, 5),
                     e(Items.SPLASH_POTION, 1, 4),
                     e(Items.MILK_BUCKET, 1, 1),
                     e(Items.ENDER_PEARL, 14, 39),
                     e(Items.COBWEB, 11, 32),
                     e(Items.DEEPSLATE_TILES, 95, 204),
                     e(Items.OAK_LOG, 60, 137),
                     e(Items.OAK_PLANKS, 84, 187),
                     e(Items.SHULKER_BOX, 1, 3),
                     e(Items.TIPPED_ARROW, 25, 67),
                     e(Items.DIAMOND, 32, 84),
                     e(Items.NETHERITE_SCRAP, 39, 102),
                     e(Items.BONE, 25, 74)
                  );
               default:
                  yield List.of(
                     e(Items.ARROW, 272, 512),
                     e(Items.SPECTRAL_ARROW, 88, 216),
                     e(Items.GOLDEN_APPLE, 40, 112),
                     e(Items.POTION, 2, 5),
                     e(Items.SPLASH_POTION, 1, 4),
                     e(Items.MILK_BUCKET, 1, 1),
                     e(Items.ENDER_PEARL, 32, 88),
                     e(Items.COBWEB, 24, 72),
                     e(Items.SMOOTH_QUARTZ, 216, 464),
                     e(Items.OAK_LOG, 136, 312),
                     e(Items.OAK_PLANKS, 192, 424),
                     e(Items.SHULKER_BOX, 1, 3),
                     e(Items.TIPPED_ARROW, 56, 152),
                     e(Items.DIAMOND_BLOCK, 72, 192),
                     e(Items.NETHERITE_INGOT, 88, 232),
                     e(Items.BONE, 56, 168)
                  );
            }
         }
         case MINING -> {
            switch (stage) {
               case 1:
                  yield List.of(
                     e(Items.TORCH, 13, 19),
                     e(Items.LADDER, 6, 14),
                     e(Items.RAIL, 5, 12),
                     e(Items.POWERED_RAIL, 2, 6),
                     e(Items.MINECART, 1, 4),
                     e(Items.TNT, 1, 4),
                     e(Items.COAL, 5, 12),
                     e(Items.IRON_INGOT, 4, 10),
                     e(Items.GOLD_INGOT, 6, 13),
                     e(Items.REDSTONE, 9, 19),
                     e(Items.LAPIS_LAZULI, 6, 14),
                     e(Items.EMERALD, 3, 9),
                     e(Items.QUARTZ, 7, 16),
                     e(Items.OAK_PLANKS, 8, 17),
                     e(Items.CHEST, 1, 3),
                     e(Items.RAW_IRON, 2, 6)
                  );
               case 2:
                  yield List.of(
                     e(Items.TORCH, 22, 32),
                     e(Items.LADDER, 10, 24),
                     e(Items.RAIL, 8, 20),
                     e(Items.POWERED_RAIL, 3, 10),
                     e(Items.MINECART, 1, 4),
                     e(Items.TNT, 2, 6),
                     e(Items.LAPIS_LAZULI, 8, 20),
                     e(Items.IRON_INGOT, 7, 17),
                     e(Items.GOLD_INGOT, 10, 22),
                     e(Items.REDSTONE, 16, 31),
                     e(Items.COAL, 17, 32),
                     e(Items.EMERALD, 6, 14),
                     e(Items.QUARTZ, 12, 26),
                     e(Items.COBBLESTONE, 14, 28),
                     e(Items.CHEST, 1, 3),
                     e(Items.RAW_COPPER, 4, 10)
                  );
               case 3:
                  yield List.of(
                     e(Items.TORCH, 62, 90),
                     e(Items.LADDER, 29, 66),
                     e(Items.RAIL, 24, 57),
                     e(Items.POWERED_RAIL, 8, 27),
                     e(Items.MINECART, 1, 4),
                     e(Items.TNT, 6, 18),
                     e(Items.DIAMOND, 24, 55),
                     e(Items.GOLD_INGOT, 20, 48),
                     e(Items.IRON_INGOT, 38, 81),
                     e(Items.REDSTONE, 43, 87),
                     e(Items.LAPIS_LAZULI, 29, 66),
                     e(Items.COAL, 48, 90),
                     e(Items.EMERALD, 15, 41),
                     e(Items.QUARTZ, 34, 74),
                     e(Items.STONE_BRICKS, 38, 80),
                     e(Items.ENDER_CHEST, 1, 3),
                     e(Items.RAW_GOLD, 10, 29)
                  );
               case 4:
                  yield List.of(
                     e(Items.LANTERN, 155, 225),
                     e(Items.LADDER, 74, 165),
                     e(Items.RAIL, 60, 144),
                     e(Items.POWERED_RAIL, 21, 67),
                     e(Items.MINECART, 1, 4),
                     e(Items.TNT, 14, 46),
                     e(Items.DIAMOND, 60, 137),
                     e(Items.NETHERITE_SCRAP, 49, 120),
                     e(Items.IRON_INGOT, 95, 204),
                     e(Items.GOLD_INGOT, 67, 155),
                     e(Items.REDSTONE, 109, 218),
                     e(Items.LAPIS_LAZULI, 74, 165),
                     e(Items.COAL, 120, 225),
                     e(Items.EMERALD, 39, 102),
                     e(Items.QUARTZ, 84, 187),
                     e(Items.DEEPSLATE_TILES, 95, 201),
                     e(Items.SHULKER_BOX, 1, 3),
                     e(Items.ANCIENT_DEBRIS, 25, 74)
                  );
               default:
                  yield List.of(
                     e(Items.LANTERN, 352, 512),
                     e(Items.LADDER, 168, 376),
                     e(Items.RAIL, 136, 328),
                     e(Items.POWERED_RAIL, 48, 152),
                     e(Items.MINECART, 1, 4),
                     e(Items.TNT, 32, 104),
                     e(Items.DIAMOND_BLOCK, 136, 312),
                     e(Items.NETHERITE_INGOT, 112, 272),
                     e(Items.IRON_INGOT, 216, 464),
                     e(Items.GOLD_INGOT, 152, 352),
                     e(Items.REDSTONE, 248, 496),
                     e(Items.LAPIS_LAZULI, 168, 376),
                     e(Items.COAL, 272, 512),
                     e(Items.EMERALD, 88, 232),
                     e(Items.QUARTZ, 192, 424),
                     e(Items.SMOOTH_QUARTZ, 216, 456),
                     e(Items.SHULKER_BOX, 1, 3),
                     e(Items.ANCIENT_DEBRIS, 56, 168)
                  );
            }
         }
         case FARMING -> {
            switch (stage) {
               case 1:
                  yield List.of(
                     e(Items.WHEAT_SEEDS, 10, 19),
                     e(Items.BEETROOT_SEEDS, 6, 14),
                     e(Items.PUMPKIN_SEEDS, 5, 12),
                     e(Items.MELON_SEEDS, 4, 10),
                     e(Items.BONE_MEAL, 9, 19),
                     e(Items.WHEAT, 8, 17),
                     e(Items.CARROT, 7, 16),
                     e(Items.POTATO, 6, 14),
                     e(Items.BEETROOT, 6, 13),
                     e(Items.HAY_BLOCK, 2, 4),
                     e(Items.OAK_SAPLING, 4, 10),
                     e(Items.SHEARS, 1, 1),
                     e(Items.LEAD, 2, 7),
                     e(Items.NAME_TAG, 1, 4),
                     e(Items.EGG, 2, 6),
                     e(Items.SUGAR_CANE, 5, 12),
                     e(Items.BAMBOO, 4, 11),
                     e(Items.COMPOSTER, 1, 2)
                  );
               case 2:
                  yield List.of(
                     e(Items.WHEAT_SEEDS, 17, 32),
                     e(Items.BEETROOT_SEEDS, 10, 24),
                     e(Items.PUMPKIN_SEEDS, 8, 20),
                     e(Items.MELON_SEEDS, 7, 17),
                     e(Items.BONE_MEAL, 16, 32),
                     e(Items.WHEAT, 14, 29),
                     e(Items.CARROT, 12, 26),
                     e(Items.POTATO, 10, 24),
                     e(Items.BEETROOT, 10, 22),
                     e(Items.HAY_BLOCK, 2, 8),
                     e(Items.OAK_SAPLING, 6, 16),
                     e(Items.SHEARS, 1, 1),
                     e(Items.LEAD, 2, 7),
                     e(Items.NAME_TAG, 1, 4),
                     e(Items.EGG, 4, 10),
                     e(Items.SUGAR_CANE, 8, 20),
                     e(Items.BAMBOO, 7, 18),
                     e(Items.COMPOSTER, 1, 2)
                  );
               case 3:
                  yield List.of(
                     e(Items.WHEAT_SEEDS, 48, 90),
                     e(Items.BEETROOT_SEEDS, 29, 66),
                     e(Items.PUMPKIN_SEEDS, 24, 55),
                     e(Items.MELON_SEEDS, 20, 48),
                     e(Items.BONE_MEAL, 43, 90),
                     e(Items.WHEAT, 38, 81),
                     e(Items.CARROT, 34, 74),
                     e(Items.POTATO, 29, 66),
                     e(Items.BEETROOT, 27, 62),
                     e(Items.HAY_BLOCK, 7, 21),
                     e(Items.OAK_SAPLING, 18, 46),
                     e(Items.SHEARS, 1, 1),
                     e(Items.LEAD, 2, 7),
                     e(Items.NAME_TAG, 1, 4),
                     e(Items.EGG, 11, 29),
                     e(Items.SUGAR_CANE, 24, 57),
                     e(Items.BAMBOO, 20, 50),
                     e(Items.COMPOSTER, 1, 2)
                  );
               case 4:
                  yield List.of(
                     e(Items.WHEAT_SEEDS, 120, 225),
                     e(Items.BEETROOT_SEEDS, 74, 165),
                     e(Items.PUMPKIN_SEEDS, 60, 137),
                     e(Items.MELON_SEEDS, 49, 120),
                     e(Items.BONE_MEAL, 109, 225),
                     e(Items.WHEAT, 95, 204),
                     e(Items.CARROT, 84, 187),
                     e(Items.POTATO, 74, 165),
                     e(Items.BEETROOT, 67, 155),
                     e(Items.HAY_BLOCK, 18, 53),
                     e(Items.OAK_SAPLING, 46, 116),
                     e(Items.SHEARS, 1, 1),
                     e(Items.LEAD, 2, 7),
                     e(Items.NAME_TAG, 1, 4),
                     e(Items.EGG, 28, 74),
                     e(Items.SUGAR_CANE, 60, 144),
                     e(Items.BAMBOO, 49, 127),
                     e(Items.COMPOSTER, 1, 2)
                  );
               default:
                  yield List.of(
                     e(Items.WHEAT_SEEDS, 272, 512),
                     e(Items.BEETROOT_SEEDS, 168, 376),
                     e(Items.PUMPKIN_SEEDS, 136, 312),
                     e(Items.MELON_SEEDS, 112, 272),
                     e(Items.BONE_MEAL, 248, 512),
                     e(Items.WHEAT, 216, 464),
                     e(Items.CARROT, 192, 424),
                     e(Items.POTATO, 168, 376),
                     e(Items.BEETROOT, 152, 352),
                     e(Items.HAY_BLOCK, 40, 120),
                     e(Items.OAK_SAPLING, 104, 264),
                     e(Items.SHEARS, 1, 1),
                     e(Items.LEAD, 2, 7),
                     e(Items.NAME_TAG, 1, 4),
                     e(Items.EGG, 64, 168),
                     e(Items.SUGAR_CANE, 136, 328),
                     e(Items.BAMBOO, 112, 288),
                     e(Items.COMPOSTER, 1, 2)
                  );
            }
         }
         case EXPLORING -> {
            switch (stage) {
               case 1:
                  yield List.of(
                     e(Items.MAP, 1, 3),
                     e(Items.COMPASS, 1, 2),
                     e(Items.CLOCK, 1, 2),
                     e(Items.SPYGLASS, 1, 1),
                     e(Items.ENDER_PEARL, 2, 4),
                     e(Items.OAK_PLANKS, 8, 17),
                     e(Items.LADDER, 5, 12),
                     e(Items.OAK_LOG, 6, 14),
                     e(Items.LEAD, 2, 6),
                     e(Items.NAME_TAG, 1, 3),
                     e(Items.FLINT_AND_STEEL, 1, 1),
                     e(Items.FISHING_ROD, 1, 1),
                     e(Items.OAK_SIGN, 1, 4),
                     e(Items.OAK_FENCE, 3, 9),
                     e(Items.CHEST, 1, 3),
                     e(Items.APPLE, 1, 5),
                     e(Items.COBWEB, 2, 7)
                  );
               case 2:
                  yield List.of(
                     e(Items.MAP, 2, 4),
                     e(Items.COMPASS, 1, 2),
                     e(Items.CLOCK, 1, 2),
                     e(Items.SPYGLASS, 1, 1),
                     e(Items.ENDER_PEARL, 3, 8),
                     e(Items.COBBLESTONE, 14, 29),
                     e(Items.OAK_PLANKS, 17, 32),
                     e(Items.LADDER, 8, 20),
                     e(Items.OAK_LOG, 10, 24),
                     e(Items.LEAD, 2, 6),
                     e(Items.NAME_TAG, 1, 3),
                     e(Items.FLINT_AND_STEEL, 1, 1),
                     e(Items.FISHING_ROD, 1, 1),
                     e(Items.OAK_SIGN, 2, 6),
                     e(Items.OAK_FENCE, 6, 14),
                     e(Items.CHEST, 1, 3),
                     e(Items.GOLDEN_APPLE, 1, 5),
                     e(Items.COBWEB, 2, 7)
                  );
               case 3:
                  yield List.of(
                     e(Items.MAP, 4, 13),
                     e(Items.COMPASS, 1, 2),
                     e(Items.CLOCK, 1, 2),
                     e(Items.SPYGLASS, 1, 1),
                     e(Items.ENDER_PEARL, 8, 21),
                     e(Items.STONE_BRICKS, 38, 81),
                     e(Items.OAK_PLANKS, 48, 90),
                     e(Items.LADDER, 24, 57),
                     e(Items.OAK_LOG, 29, 66),
                     e(Items.LEAD, 2, 6),
                     e(Items.NAME_TAG, 1, 3),
                     e(Items.FLINT_AND_STEEL, 1, 1),
                     e(Items.FISHING_ROD, 1, 1),
                     e(Items.OAK_SIGN, 6, 17),
                     e(Items.OAK_FENCE, 15, 41),
                     e(Items.ENDER_CHEST, 1, 3),
                     e(Items.GOLDEN_APPLE, 1, 5),
                     e(Items.COBWEB, 2, 7)
                  );
               case 4:
                  yield List.of(
                     e(Items.MAP, 11, 32),
                     e(Items.COMPASS, 1, 2),
                     e(Items.CLOCK, 1, 2),
                     e(Items.SPYGLASS, 1, 1),
                     e(Items.ENDER_PEARL, 21, 53),
                     e(Items.DEEPSLATE_TILES, 95, 204),
                     e(Items.OAK_PLANKS, 120, 225),
                     e(Items.LADDER, 60, 144),
                     e(Items.OAK_LOG, 74, 165),
                     e(Items.LEAD, 2, 6),
                     e(Items.NAME_TAG, 1, 3),
                     e(Items.FLINT_AND_STEEL, 1, 1),
                     e(Items.FISHING_ROD, 1, 1),
                     e(Items.OAK_SIGN, 14, 42),
                     e(Items.OAK_FENCE, 39, 102),
                     e(Items.SHULKER_BOX, 1, 3),
                     e(Items.ENCHANTED_GOLDEN_APPLE, 1, 5),
                     e(Items.COBWEB, 2, 7)
                  );
               default:
                  yield List.of(
                     e(Items.MAP, 24, 72),
                     e(Items.COMPASS, 1, 2),
                     e(Items.CLOCK, 1, 2),
                     e(Items.SPYGLASS, 1, 1),
                     e(Items.ENDER_PEARL, 48, 120),
                     e(Items.SMOOTH_QUARTZ, 216, 464),
                     e(Items.OAK_PLANKS, 272, 512),
                     e(Items.LADDER, 136, 328),
                     e(Items.OAK_LOG, 168, 376),
                     e(Items.LEAD, 2, 6),
                     e(Items.NAME_TAG, 1, 3),
                     e(Items.FLINT_AND_STEEL, 1, 1),
                     e(Items.FISHING_ROD, 1, 1),
                     e(Items.OAK_SIGN, 32, 96),
                     e(Items.OAK_FENCE, 88, 232),
                     e(Items.SHULKER_BOX, 1, 3),
                     e(Items.ENCHANTED_GOLDEN_APPLE, 1, 5),
                     e(Items.COBWEB, 2, 7)
                  );
            }
         }
         default -> {
            switch (stage) {
               case 1:
                  yield List.of(
                     e(Items.OAK_PLANKS, 10, 19),
                     e(Items.COBBLESTONE, 8, 17),
                     e(Items.GLASS, 6, 14),
                     e(Items.OAK_LOG, 7, 16),
                     e(Items.OAK_STAIRS, 5, 12),
                     e(Items.OAK_SLAB, 6, 13),
                     e(Items.LADDER, 4, 10),
                     e(Items.COAL, 3, 9),
                     e(Items.IRON_INGOT, 4, 10),
                     e(Items.GOLD_INGOT, 4, 11),
                     e(Items.REDSTONE, 7, 16),
                     e(Items.CHEST, 1, 4),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.WATER_BUCKET, 1, 1),
                     e(Items.ARROW, 6, 14)
                  );
               case 2:
                  yield List.of(
                     e(Items.COBBLESTONE, 17, 32),
                     e(Items.STONE, 14, 29),
                     e(Items.GLASS, 10, 24),
                     e(Items.OAK_LOG, 12, 26),
                     e(Items.OAK_PLANKS, 16, 31),
                     e(Items.STONE_BRICK_STAIRS, 8, 20),
                     e(Items.STONE_SLAB, 10, 22),
                     e(Items.LADDER, 7, 17),
                     e(Items.LAPIS_LAZULI, 6, 14),
                     e(Items.IRON_INGOT, 6, 16),
                     e(Items.GOLD_INGOT, 7, 18),
                     e(Items.REDSTONE, 12, 26),
                     e(Items.COAL, 14, 29),
                     e(Items.CHEST, 1, 4),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.WATER_BUCKET, 1, 1),
                     e(Items.ARROW, 10, 24)
                  );
               case 3:
                  yield List.of(
                     e(Items.STONE_BRICKS, 48, 90),
                     e(Items.POLISHED_DEEPSLATE, 38, 81),
                     e(Items.GLASS, 29, 66),
                     e(Items.OAK_LOG, 34, 74),
                     e(Items.OAK_PLANKS, 43, 87),
                     e(Items.STONE_BRICK_STAIRS, 24, 57),
                     e(Items.SMOOTH_STONE_SLAB, 27, 62),
                     e(Items.LADDER, 20, 48),
                     e(Items.DIAMOND, 15, 41),
                     e(Items.GOLD_INGOT, 18, 46),
                     e(Items.IRON_INGOT, 29, 66),
                     e(Items.REDSTONE, 34, 74),
                     e(Items.COAL, 38, 81),
                     e(Items.ENDER_CHEST, 1, 4),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.WATER_BUCKET, 1, 1),
                     e(Items.ARROW, 29, 66)
                  );
               case 4:
                  yield List.of(
                     e(Items.DEEPSLATE_TILES, 120, 225),
                     e(Items.POLISHED_BLACKSTONE, 95, 204),
                     e(Items.GLASS, 74, 165),
                     e(Items.OAK_LOG, 84, 187),
                     e(Items.OAK_PLANKS, 109, 218),
                     e(Items.STONE_BRICK_STAIRS, 60, 144),
                     e(Items.SMOOTH_STONE_SLAB, 67, 155),
                     e(Items.LADDER, 49, 120),
                     e(Items.DIAMOND, 39, 102),
                     e(Items.NETHERITE_SCRAP, 46, 116),
                     e(Items.IRON_INGOT, 74, 165),
                     e(Items.GOLD_INGOT, 49, 127),
                     e(Items.REDSTONE, 84, 187),
                     e(Items.COAL, 95, 204),
                     e(Items.SHULKER_BOX, 1, 4),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.WATER_BUCKET, 1, 1),
                     e(Items.ARROW, 74, 165)
                  );
               default:
                  yield List.of(
                     e(Items.SMOOTH_QUARTZ, 272, 512),
                     e(Items.POLISHED_BLACKSTONE, 216, 464),
                     e(Items.GLASS, 168, 376),
                     e(Items.OAK_LOG, 192, 424),
                     e(Items.OAK_PLANKS, 248, 496),
                     e(Items.STONE_BRICK_STAIRS, 136, 328),
                     e(Items.SMOOTH_STONE_SLAB, 152, 352),
                     e(Items.LADDER, 112, 272),
                     e(Items.DIAMOND_BLOCK, 88, 232),
                     e(Items.NETHERITE_INGOT, 104, 264),
                     e(Items.IRON_INGOT, 168, 376),
                     e(Items.GOLD_INGOT, 112, 288),
                     e(Items.REDSTONE, 192, 424),
                     e(Items.COAL, 216, 464),
                     e(Items.SHULKER_BOX, 1, 4),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.WATER_BUCKET, 1, 1),
                     e(Items.ARROW, 168, 376)
                  );
            }
         }
      };
   }

   private static List<LoadoutFiller.Entry> prettyGather(int stage) {
      return switch (stage) {
         case 1 -> List.of(
            e(Items.RAW_IRON, 3, 9),
            e(Items.RAW_COPPER, 7, 16),
            e(Items.RAW_GOLD, 5, 12),
            e(Items.COAL, 10, 19),
            e(Items.AMETHYST_SHARD, 3, 9),
            e(Items.CLAY_BALL, 4, 10),
            e(Items.OAK_LOG, 6, 14),
            e(Items.SUGAR_CANE, 5, 12),
            e(Items.BAMBOO, 6, 13),
            e(Items.HONEYCOMB, 2, 6)
         );
         case 2 -> List.of(
            e(Items.RAW_COPPER, 6, 16),
            e(Items.RAW_IRON, 14, 29),
            e(Items.RAW_GOLD, 8, 20),
            e(Items.COAL, 17, 32),
            e(Items.LAPIS_LAZULI, 7, 17),
            e(Items.AMETHYST_SHARD, 6, 14),
            e(Items.CLAY_BALL, 6, 16),
            e(Items.OAK_LOG, 10, 24),
            e(Items.SUGAR_CANE, 8, 20),
            e(Items.BAMBOO, 10, 22),
            e(Items.HONEYCOMB, 4, 10)
         );
         case 3 -> List.of(
            e(Items.RAW_GOLD, 15, 43),
            e(Items.RAW_IRON, 38, 81),
            e(Items.RAW_COPPER, 34, 74),
            e(Items.COAL, 48, 90),
            e(Items.DIAMOND, 20, 48),
            e(Items.AMETHYST_SHARD, 15, 41),
            e(Items.CLAY_BALL, 18, 46),
            e(Items.OAK_LOG, 29, 66),
            e(Items.SUGAR_CANE, 24, 57),
            e(Items.BAMBOO, 27, 62),
            e(Items.HONEYCOMB, 10, 29)
         );
         case 4 -> List.of(
            e(Items.ANCIENT_DEBRIS, 39, 109),
            e(Items.RAW_IRON, 95, 204),
            e(Items.RAW_COPPER, 84, 187),
            e(Items.RAW_GOLD, 60, 137),
            e(Items.COAL, 120, 225),
            e(Items.DIAMOND, 49, 120),
            e(Items.AMETHYST_SHARD, 39, 102),
            e(Items.CLAY_BALL, 46, 116),
            e(Items.OAK_LOG, 74, 165),
            e(Items.SUGAR_CANE, 60, 144),
            e(Items.BAMBOO, 67, 155),
            e(Items.HONEYCOMB, 25, 74)
         );
         default -> List.of(
            e(Items.ANCIENT_DEBRIS, 88, 248),
            e(Items.RAW_IRON, 216, 464),
            e(Items.RAW_COPPER, 192, 424),
            e(Items.RAW_GOLD, 136, 312),
            e(Items.COAL, 272, 512),
            e(Items.DIAMOND_BLOCK, 112, 272),
            e(Items.AMETHYST_SHARD, 88, 232),
            e(Items.CLAY_BALL, 104, 264),
            e(Items.OAK_LOG, 168, 376),
            e(Items.SUGAR_CANE, 136, 328),
            e(Items.BAMBOO, 152, 352),
            e(Items.HONEYCOMB, 56, 168)
         );
      };
   }

   private static List<LoadoutFiller.Entry> kitBuilding(int stage) {
      return switch (stage) {
         case 1 -> List.of(
            e(Items.OAK_PLANKS, 14, 19),
            e(Items.COBBLESTONE, 13, 19),
            e(Items.CRAFTING_TABLE, 8, 17),
            e(Items.OAK_STAIRS, 10, 19),
            e(Items.OAK_SLAB, 11, 19),
            e(Items.COBBLESTONE_WALL, 7, 15),
            e(Items.FLOWER_POT, 9, 18),
            e(Items.GLASS, 9, 19),
            e(Items.GLASS_PANE, 8, 16),
            e(Items.OAK_LOG, 8, 17),
            e(Items.OAK_FENCE, 5, 12),
            e(Items.OAK_DOOR, 2, 5),
            e(Items.OAK_TRAPDOOR, 2, 6),
            e(Items.SCAFFOLDING, 6, 14),
            e(Items.LADDER, 5, 12),
            e(Items.TORCH, 4, 10),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.CHEST, 2, 6)
         );
         case 2 -> List.of(
            e(Items.COBBLESTONE, 24, 32),
            e(Items.STONE, 22, 32),
            e(Items.STONE_BRICKS, 20, 32),
            e(Items.SMOOTH_STONE, 14, 29),
            e(Items.STONE_BRICK_STAIRS, 17, 32),
            e(Items.STONE_SLAB, 18, 32),
            e(Items.COBBLESTONE_WALL, 12, 26),
            e(Items.FLOWER_POT, 14, 30),
            e(Items.GLASS, 16, 32),
            e(Items.GLASS_PANE, 13, 28),
            e(Items.OAK_LOG, 14, 29),
            e(Items.OAK_PLANKS, 22, 32),
            e(Items.OAK_FENCE, 8, 20),
            e(Items.OAK_DOOR, 3, 8),
            e(Items.OAK_TRAPDOOR, 4, 10),
            e(Items.SCAFFOLDING, 10, 24),
            e(Items.LADDER, 8, 20),
            e(Items.LANTERN, 6, 17),
            e(Items.TORCH, 17, 32),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.CHEST, 2, 6),
            e(Items.CRAFTING_TABLE, 1, 3)
         );
         case 3 -> List.of(
            e(Items.STONE_BRICKS, 67, 90),
            e(Items.POLISHED_DEEPSLATE, 62, 90),
            e(Items.DEEPSLATE_BRICKS, 57, 90),
            e(Items.CHISELED_STONE_BRICKS, 38, 81),
            e(Items.STONE_BRICK_STAIRS, 48, 90),
            e(Items.SMOOTH_STONE_SLAB, 52, 90),
            e(Items.STONE_BRICK_WALL, 34, 71),
            e(Items.TERRACOTTA, 41, 85),
            e(Items.GLASS, 43, 90),
            e(Items.GLASS_PANE, 36, 77),
            e(Items.OAK_LOG, 38, 81),
            e(Items.OAK_PLANKS, 62, 90),
            e(Items.OAK_FENCE, 24, 55),
            e(Items.OAK_DOOR, 8, 24),
            e(Items.OAK_TRAPDOOR, 10, 27),
            e(Items.SCAFFOLDING, 29, 66),
            e(Items.LADDER, 24, 57),
            e(Items.LANTERN, 18, 48),
            e(Items.TORCH, 48, 90),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.ENDER_CHEST, 2, 6),
            e(Items.CRAFTING_TABLE, 1, 3)
         );
         case 4 -> List.of(
            e(Items.DEEPSLATE_TILES, 169, 225),
            e(Items.POLISHED_BLACKSTONE, 155, 225),
            e(Items.END_STONE_BRICKS, 144, 225),
            e(Items.CHISELED_DEEPSLATE, 95, 204),
            e(Items.STONE_BRICK_STAIRS, 120, 225),
            e(Items.SMOOTH_STONE_SLAB, 130, 225),
            e(Items.DEEPSLATE_BRICK_WALL, 84, 180),
            e(Items.CONCRETE.pick(DyeColor.WHITE), 102, 215),
            e(Items.GLASS, 109, 225),
            e(Items.GLASS_PANE, 92, 194),
            e(Items.OAK_LOG, 95, 204),
            e(Items.OAK_PLANKS, 155, 225),
            e(Items.OAK_FENCE, 60, 137),
            e(Items.OAK_DOOR, 21, 60),
            e(Items.OAK_TRAPDOOR, 25, 67),
            e(Items.SCAFFOLDING, 74, 165),
            e(Items.LADDER, 60, 144),
            e(Items.SEA_LANTERN, 46, 120),
            e(Items.LANTERN, 120, 225),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.SHULKER_BOX, 2, 6),
            e(Items.CRAFTING_TABLE, 1, 3)
         );
         default -> List.of(
            e(Items.SMOOTH_QUARTZ, 384, 512),
            e(Items.POLISHED_BLACKSTONE, 352, 512),
            e(Items.PRISMARINE_BRICKS, 328, 512),
            e(Items.PURPUR_BLOCK, 216, 464),
            e(Items.STONE_BRICK_STAIRS, 272, 512),
            e(Items.SMOOTH_STONE_SLAB, 296, 512),
            e(Items.DEEPSLATE_BRICK_WALL, 192, 408),
            e(Items.CONCRETE.pick(DyeColor.WHITE), 232, 488),
            e(Items.GLASS, 248, 512),
            e(Items.GLASS_PANE, 208, 440),
            e(Items.OAK_LOG, 216, 464),
            e(Items.OAK_PLANKS, 352, 512),
            e(Items.OAK_FENCE, 136, 312),
            e(Items.OAK_DOOR, 48, 136),
            e(Items.OAK_TRAPDOOR, 56, 152),
            e(Items.SCAFFOLDING, 168, 376),
            e(Items.LADDER, 136, 328),
            e(Items.SEA_LANTERN, 104, 272),
            e(Items.LANTERN, 272, 512),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.SHULKER_BOX, 2, 6),
            e(Items.CRAFTING_TABLE, 1, 3)
         );
      };
   }

   private static List<LoadoutFiller.Entry> kitCombat(int stage) {
      return switch (stage) {
         case 1 -> List.of(
            e(Items.STONE_SWORD, 1, 1),
            e(Items.STONE_AXE, 1, 1),
            e(Items.BOW, 1, 1),
            e(Items.SHIELD, 1, 2),
            e(Items.ARROW, 14, 19),
            e(Items.SPECTRAL_ARROW, 4, 10),
            e(Items.APPLE, 1, 3),
            e(Items.GOLDEN_APPLE, 2, 6),
            e(Items.POTION, 3, 7),
            e(Items.SPLASH_POTION, 2, 5),
            e(Items.MILK_BUCKET, 1, 2),
            e(Items.BREAD, 8, 18),
            e(Items.ENDER_PEARL, 2, 4),
            e(Items.OAK_PLANKS, 12, 19),
            e(Items.TORCH, 7, 16),
            e(Items.COBWEB, 1, 4),
            e(Items.TIPPED_ARROW, 3, 8),
            e(Items.CHEST, 1, 4)
         );
         case 2 -> List.of(
            e(Items.IRON_SWORD, 1, 1),
            e(Items.IRON_AXE, 1, 1),
            e(Items.BOW, 1, 1),
            e(Items.SHIELD, 1, 2),
            e(Items.ARROW, 24, 32),
            e(Items.SPECTRAL_ARROW, 7, 17),
            e(Items.GOLDEN_APPLE, 2, 4),
            e(Items.POTION, 3, 7),
            e(Items.SPLASH_POTION, 2, 5),
            e(Items.MILK_BUCKET, 1, 2),
            e(Items.COOKED_PORKCHOP, 14, 30),
            e(Items.ENDER_PEARL, 3, 8),
            e(Items.COBBLESTONE, 20, 32),
            e(Items.TORCH, 12, 26),
            e(Items.COBWEB, 2, 6),
            e(Items.TIPPED_ARROW, 4, 14),
            e(Items.CHEST, 1, 4)
         );
         case 3 -> List.of(
            e(Items.DIAMOND_SWORD, 1, 1),
            e(Items.DIAMOND_AXE, 1, 1),
            e(Items.CROSSBOW, 1, 1),
            e(Items.SHIELD, 1, 2),
            e(Items.ARROW, 66, 90),
            e(Items.SPECTRAL_ARROW, 20, 48),
            e(Items.GOLDEN_APPLE, 4, 13),
            e(Items.POTION, 3, 7),
            e(Items.SPLASH_POTION, 2, 5),
            e(Items.MILK_BUCKET, 1, 2),
            e(Items.COOKED_BEEF, 38, 85),
            e(Items.ENDER_PEARL, 8, 21),
            e(Items.STONE_BRICKS, 57, 90),
            e(Items.TORCH, 34, 74),
            e(Items.COBWEB, 6, 17),
            e(Items.BOW, 1, 1),
            e(Items.TIPPED_ARROW, 13, 38),
            e(Items.ENDER_CHEST, 1, 4)
         );
         case 4 -> List.of(
            e(Items.NETHERITE_SWORD, 1, 1),
            e(Items.DIAMOND_AXE, 1, 1),
            e(Items.CROSSBOW, 1, 1),
            e(Items.SHIELD, 1, 2),
            e(Items.ARROW, 165, 225),
            e(Items.SPECTRAL_ARROW, 49, 120),
            e(Items.ENCHANTED_GOLDEN_APPLE, 11, 32),
            e(Items.GOLDEN_APPLE, 25, 74),
            e(Items.POTION, 3, 7),
            e(Items.SPLASH_POTION, 2, 5),
            e(Items.MILK_BUCKET, 1, 2),
            e(Items.GOLDEN_CARROT, 95, 215),
            e(Items.ENDER_PEARL, 21, 53),
            e(Items.DEEPSLATE_TILES, 144, 225),
            e(Items.LANTERN, 84, 187),
            e(Items.COBWEB, 14, 42),
            e(Items.BOW, 1, 1),
            e(Items.TIPPED_ARROW, 32, 95),
            e(Items.SHULKER_BOX, 1, 4)
         );
         default -> List.of(
            e(Items.NETHERITE_SWORD, 1, 1),
            e(Items.NETHERITE_AXE, 1, 1),
            e(Items.CROSSBOW, 1, 1),
            e(Items.SHIELD, 1, 2),
            e(Items.ARROW, 376, 512),
            e(Items.SPECTRAL_ARROW, 112, 272),
            e(Items.ENCHANTED_GOLDEN_APPLE, 24, 72),
            e(Items.GOLDEN_APPLE, 56, 168),
            e(Items.POTION, 3, 7),
            e(Items.SPLASH_POTION, 2, 5),
            e(Items.MILK_BUCKET, 1, 2),
            e(Items.GOLDEN_CARROT, 216, 488),
            e(Items.ENDER_PEARL, 48, 120),
            e(Items.SMOOTH_QUARTZ, 328, 512),
            e(Items.LANTERN, 192, 424),
            e(Items.COBWEB, 32, 96),
            e(Items.BOW, 1, 1),
            e(Items.TIPPED_ARROW, 72, 216),
            e(Items.SHULKER_BOX, 1, 4)
         );
      };
   }

   private static List<LoadoutFiller.Entry> kitMining(int stage) {
      return switch (stage) {
         case 1 -> List.of(
            e(Items.STONE_PICKAXE, 1, 1),
            e(Items.STONE_SHOVEL, 1, 1),
            e(Items.TORCH, 15, 19),
            e(Items.LADDER, 8, 17),
            e(Items.RAIL, 7, 16),
            e(Items.POWERED_RAIL, 3, 8),
            e(Items.MINECART, 2, 6),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.BREAD, 7, 16),
            e(Items.CHEST, 2, 5),
            e(Items.TNT, 2, 5),
            e(Items.COAL, 8, 17),
            e(Items.IRON_INGOT, 6, 14),
            e(Items.RAW_IRON, 3, 9),
            e(Items.GOLD_INGOT, 7, 15),
            e(Items.REDSTONE, 12, 19),
            e(Items.LAPIS_LAZULI, 8, 17),
            e(Items.EMERALD, 5, 12),
            e(Items.QUARTZ, 9, 19)
         );
         case 2 -> List.of(
            e(Items.IRON_PICKAXE, 1, 1),
            e(Items.IRON_SHOVEL, 1, 1),
            e(Items.TORCH, 26, 32),
            e(Items.LANTERN, 10, 24),
            e(Items.LADDER, 14, 29),
            e(Items.RAIL, 12, 26),
            e(Items.POWERED_RAIL, 4, 13),
            e(Items.MINECART, 2, 6),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.COOKED_PORKCHOP, 12, 26),
            e(Items.CHEST, 2, 5),
            e(Items.TNT, 2, 8),
            e(Items.LAPIS_LAZULI, 14, 29),
            e(Items.IRON_INGOT, 10, 24),
            e(Items.RAW_COPPER, 6, 14),
            e(Items.GOLD_INGOT, 12, 26),
            e(Items.REDSTONE, 20, 32),
            e(Items.COAL, 22, 32),
            e(Items.EMERALD, 8, 20),
            e(Items.QUARTZ, 16, 31)
         );
         case 3 -> List.of(
            e(Items.DIAMOND_PICKAXE, 1, 1),
            e(Items.IRON_SHOVEL, 1, 1),
            e(Items.TORCH, 71, 90),
            e(Items.LANTERN, 29, 66),
            e(Items.LADDER, 38, 81),
            e(Items.RAIL, 34, 74),
            e(Items.POWERED_RAIL, 13, 36),
            e(Items.MINECART, 2, 6),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.COOKED_BEEF, 34, 74),
            e(Items.ENDER_CHEST, 2, 5),
            e(Items.TNT, 7, 24),
            e(Items.DIAMOND, 38, 81),
            e(Items.GOLD_INGOT, 29, 66),
            e(Items.RAW_GOLD, 15, 41),
            e(Items.IRON_INGOT, 48, 90),
            e(Items.REDSTONE, 57, 90),
            e(Items.LAPIS_LAZULI, 38, 80),
            e(Items.COAL, 62, 90),
            e(Items.EMERALD, 24, 55),
            e(Items.QUARTZ, 43, 87)
         );
         case 4 -> List.of(
            e(Items.NETHERITE_PICKAXE, 1, 1),
            e(Items.DIAMOND_SHOVEL, 1, 1),
            e(Items.LANTERN, 180, 225),
            e(Items.SEA_LANTERN, 74, 165),
            e(Items.LADDER, 95, 204),
            e(Items.RAIL, 84, 187),
            e(Items.POWERED_RAIL, 32, 92),
            e(Items.MINECART, 2, 6),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.GOLDEN_CARROT, 84, 187),
            e(Items.SHULKER_BOX, 2, 5),
            e(Items.TNT, 18, 60),
            e(Items.DIAMOND, 95, 204),
            e(Items.NETHERITE_SCRAP, 74, 165),
            e(Items.ANCIENT_DEBRIS, 39, 102),
            e(Items.IRON_INGOT, 120, 225),
            e(Items.GOLD_INGOT, 84, 180),
            e(Items.REDSTONE, 144, 225),
            e(Items.LAPIS_LAZULI, 95, 201),
            e(Items.COAL, 155, 225),
            e(Items.EMERALD, 60, 137),
            e(Items.QUARTZ, 109, 218)
         );
         default -> List.of(
            e(Items.NETHERITE_PICKAXE, 1, 1),
            e(Items.NETHERITE_SHOVEL, 1, 1),
            e(Items.LANTERN, 408, 512),
            e(Items.SEA_LANTERN, 168, 376),
            e(Items.LADDER, 216, 464),
            e(Items.RAIL, 192, 424),
            e(Items.POWERED_RAIL, 72, 208),
            e(Items.MINECART, 2, 6),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.GOLDEN_CARROT, 192, 424),
            e(Items.SHULKER_BOX, 2, 5),
            e(Items.TNT, 40, 136),
            e(Items.DIAMOND_BLOCK, 216, 464),
            e(Items.NETHERITE_INGOT, 168, 376),
            e(Items.ANCIENT_DEBRIS, 88, 232),
            e(Items.IRON_INGOT, 272, 512),
            e(Items.GOLD_INGOT, 192, 408),
            e(Items.REDSTONE, 328, 512),
            e(Items.LAPIS_LAZULI, 216, 456),
            e(Items.COAL, 352, 512),
            e(Items.EMERALD, 136, 312),
            e(Items.QUARTZ, 248, 496)
         );
      };
   }

   private static List<LoadoutFiller.Entry> kitFarming(int stage) {
      return switch (stage) {
         case 1 -> List.of(
            e(Items.WOODEN_HOE, 1, 1),
            e(Items.WHEAT_SEEDS, 14, 19),
            e(Items.BEETROOT_SEEDS, 8, 17),
            e(Items.PUMPKIN_SEEDS, 6, 14),
            e(Items.MELON_SEEDS, 6, 13),
            e(Items.BONE_MEAL, 13, 19),
            e(Items.WATER_BUCKET, 1, 2),
            e(Items.COMPOSTER, 1, 3),
            e(Items.WHEAT, 12, 19),
            e(Items.CARROT, 10, 19),
            e(Items.POTATO, 9, 19),
            e(Items.BEETROOT, 8, 17),
            e(Items.HAY_BLOCK, 2, 6),
            e(Items.OAK_SAPLING, 5, 12),
            e(Items.SHEARS, 1, 1),
            e(Items.LEAD, 1, 3),
            e(Items.NAME_TAG, 2, 6),
            e(Items.EGG, 3, 8),
            e(Items.SUGAR_CANE, 7, 16),
            e(Items.BAMBOO, 6, 14),
            e(Items.COOKED_COD, 6, 14),
            e(Items.PUMPKIN, 2, 5)
         );
         case 2 -> List.of(
            e(Items.STONE_HOE, 1, 1),
            e(Items.WHEAT_SEEDS, 24, 32),
            e(Items.BEETROOT_SEEDS, 14, 29),
            e(Items.PUMPKIN_SEEDS, 10, 24),
            e(Items.MELON_SEEDS, 10, 22),
            e(Items.BONE_MEAL, 22, 32),
            e(Items.WATER_BUCKET, 1, 2),
            e(Items.COMPOSTER, 1, 3),
            e(Items.WHEAT, 20, 32),
            e(Items.CARROT, 17, 32),
            e(Items.POTATO, 16, 31),
            e(Items.BEETROOT, 14, 28),
            e(Items.HAY_BLOCK, 4, 10),
            e(Items.OAK_SAPLING, 8, 20),
            e(Items.SHEARS, 1, 1),
            e(Items.LEAD, 2, 4),
            e(Items.NAME_TAG, 2, 6),
            e(Items.EGG, 6, 14),
            e(Items.SUGAR_CANE, 12, 26),
            e(Items.BAMBOO, 10, 24),
            e(Items.BAKED_POTATO, 10, 24),
            e(Items.PUMPKIN, 2, 8)
         );
         case 3 -> List.of(
            e(Items.IRON_HOE, 1, 1),
            e(Items.WHEAT_SEEDS, 66, 90),
            e(Items.BEETROOT_SEEDS, 38, 81),
            e(Items.PUMPKIN_SEEDS, 29, 66),
            e(Items.MELON_SEEDS, 27, 62),
            e(Items.BONE_MEAL, 62, 90),
            e(Items.WATER_BUCKET, 1, 2),
            e(Items.COMPOSTER, 1, 3),
            e(Items.WHEAT, 57, 90),
            e(Items.CARROT, 48, 90),
            e(Items.POTATO, 43, 87),
            e(Items.BEETROOT, 38, 80),
            e(Items.HAY_BLOCK, 10, 29),
            e(Items.OAK_SAPLING, 24, 55),
            e(Items.SHEARS, 1, 1),
            e(Items.LEAD, 4, 13),
            e(Items.NAME_TAG, 2, 6),
            e(Items.EGG, 15, 38),
            e(Items.SUGAR_CANE, 34, 74),
            e(Items.BAMBOO, 29, 66),
            e(Items.GOLDEN_CARROT, 29, 66),
            e(Items.PUMPKIN, 7, 24)
         );
         case 4 -> List.of(
            e(Items.DIAMOND_HOE, 1, 1),
            e(Items.WHEAT_SEEDS, 165, 225),
            e(Items.BEETROOT_SEEDS, 95, 204),
            e(Items.PUMPKIN_SEEDS, 74, 165),
            e(Items.MELON_SEEDS, 67, 155),
            e(Items.BONE_MEAL, 155, 225),
            e(Items.WATER_BUCKET, 1, 2),
            e(Items.COMPOSTER, 1, 3),
            e(Items.WHEAT, 144, 225),
            e(Items.CARROT, 120, 225),
            e(Items.POTATO, 109, 218),
            e(Items.BEETROOT, 95, 201),
            e(Items.HAY_BLOCK, 25, 74),
            e(Items.OAK_SAPLING, 60, 137),
            e(Items.SHEARS, 1, 1),
            e(Items.LEAD, 11, 32),
            e(Items.NAME_TAG, 2, 6),
            e(Items.EGG, 39, 95),
            e(Items.SUGAR_CANE, 84, 187),
            e(Items.BAMBOO, 74, 165),
            e(Items.COOKED_BEEF, 74, 165),
            e(Items.PUMPKIN, 18, 60)
         );
         default -> List.of(
            e(Items.NETHERITE_HOE, 1, 1),
            e(Items.WHEAT_SEEDS, 376, 512),
            e(Items.BEETROOT_SEEDS, 216, 464),
            e(Items.PUMPKIN_SEEDS, 168, 376),
            e(Items.MELON_SEEDS, 152, 352),
            e(Items.BONE_MEAL, 352, 512),
            e(Items.WATER_BUCKET, 1, 2),
            e(Items.COMPOSTER, 1, 3),
            e(Items.WHEAT, 328, 512),
            e(Items.CARROT, 272, 512),
            e(Items.POTATO, 248, 496),
            e(Items.BEETROOT, 216, 456),
            e(Items.HAY_BLOCK, 56, 168),
            e(Items.OAK_SAPLING, 136, 312),
            e(Items.SHEARS, 1, 1),
            e(Items.LEAD, 24, 72),
            e(Items.NAME_TAG, 2, 6),
            e(Items.EGG, 88, 216),
            e(Items.SUGAR_CANE, 192, 424),
            e(Items.BAMBOO, 168, 376),
            e(Items.COOKED_BEEF, 168, 376),
            e(Items.PUMPKIN, 40, 136)
         );
      };
   }

   private static List<LoadoutFiller.Entry> kitExploring(int stage) {
      return switch (stage) {
         case 1 -> List.of(
            e(Items.OAK_BOAT, 1, 2),
            e(Items.BED.pick(DyeColor.RED), 1, 3),
            e(Items.MAP, 1, 3),
            e(Items.COMPASS, 1, 3),
            e(Items.CLOCK, 1, 2),
            e(Items.SPYGLASS, 1, 2),
            e(Items.TORCH, 13, 19),
            e(Items.BREAD, 8, 17),
            e(Items.APPLE, 2, 7),
            e(Items.ENDER_PEARL, 2, 6),
            e(Items.CHEST, 2, 5),
            e(Items.OAK_PLANKS, 10, 19),
            e(Items.LADDER, 6, 14),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.LEAD, 2, 7),
            e(Items.NAME_TAG, 1, 4),
            e(Items.FLINT_AND_STEEL, 1, 1),
            e(Items.FISHING_ROD, 1, 1),
            e(Items.OAK_SIGN, 2, 4),
            e(Items.OAK_FENCE, 4, 10)
         );
         case 2 -> List.of(
            e(Items.OAK_BOAT, 1, 2),
            e(Items.BED.pick(DyeColor.RED), 1, 3),
            e(Items.MAP, 2, 6),
            e(Items.COMPASS, 1, 3),
            e(Items.CLOCK, 1, 2),
            e(Items.SPYGLASS, 1, 2),
            e(Items.TORCH, 22, 32),
            e(Items.COOKED_PORKCHOP, 14, 29),
            e(Items.GOLDEN_APPLE, 2, 7),
            e(Items.ENDER_PEARL, 4, 10),
            e(Items.CHEST, 2, 5),
            e(Items.COBBLESTONE, 17, 32),
            e(Items.LADDER, 10, 24),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.OAK_PLANKS, 20, 32),
            e(Items.LEAD, 2, 7),
            e(Items.NAME_TAG, 1, 4),
            e(Items.FLINT_AND_STEEL, 1, 1),
            e(Items.FISHING_ROD, 1, 1),
            e(Items.OAK_SIGN, 2, 8),
            e(Items.OAK_FENCE, 6, 16)
         );
         case 3 -> List.of(
            e(Items.OAK_BOAT, 1, 2),
            e(Items.BED.pick(DyeColor.RED), 1, 3),
            e(Items.MAP, 6, 15),
            e(Items.COMPASS, 1, 3),
            e(Items.CLOCK, 1, 2),
            e(Items.SPYGLASS, 1, 2),
            e(Items.TORCH, 62, 90),
            e(Items.COOKED_BEEF, 38, 81),
            e(Items.GOLDEN_APPLE, 2, 7),
            e(Items.ENDER_PEARL, 11, 27),
            e(Items.ENDER_CHEST, 2, 5),
            e(Items.STONE_BRICKS, 48, 90),
            e(Items.LADDER, 29, 66),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.OAK_PLANKS, 57, 90),
            e(Items.LEAD, 2, 7),
            e(Items.NAME_TAG, 1, 4),
            e(Items.FLINT_AND_STEEL, 1, 1),
            e(Items.FISHING_ROD, 1, 1),
            e(Items.OAK_SIGN, 7, 21),
            e(Items.OAK_FENCE, 18, 46)
         );
         case 4 -> List.of(
            e(Items.OAK_BOAT, 1, 2),
            e(Items.BED.pick(DyeColor.RED), 1, 3),
            e(Items.MAP, 14, 39),
            e(Items.COMPASS, 1, 3),
            e(Items.CLOCK, 1, 2),
            e(Items.SPYGLASS, 1, 2),
            e(Items.LANTERN, 155, 225),
            e(Items.GOLDEN_CARROT, 95, 204),
            e(Items.ENCHANTED_GOLDEN_APPLE, 2, 7),
            e(Items.ENDER_PEARL, 28, 67),
            e(Items.SHULKER_BOX, 2, 5),
            e(Items.DEEPSLATE_TILES, 120, 225),
            e(Items.LADDER, 74, 165),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.OAK_PLANKS, 144, 225),
            e(Items.LEAD, 2, 7),
            e(Items.NAME_TAG, 1, 4),
            e(Items.FLINT_AND_STEEL, 1, 1),
            e(Items.FISHING_ROD, 1, 1),
            e(Items.OAK_SIGN, 18, 53),
            e(Items.OAK_FENCE, 46, 116),
            e(Items.TORCH, 130, 225)
         );
         default -> List.of(
            e(Items.OAK_BOAT, 1, 2),
            e(Items.BED.pick(DyeColor.RED), 1, 3),
            e(Items.MAP, 32, 88),
            e(Items.COMPASS, 1, 3),
            e(Items.CLOCK, 1, 2),
            e(Items.SPYGLASS, 1, 2),
            e(Items.LANTERN, 352, 512),
            e(Items.GOLDEN_CARROT, 216, 464),
            e(Items.ENCHANTED_GOLDEN_APPLE, 2, 7),
            e(Items.ENDER_PEARL, 64, 152),
            e(Items.SHULKER_BOX, 2, 5),
            e(Items.SMOOTH_QUARTZ, 272, 512),
            e(Items.LADDER, 168, 376),
            e(Items.WATER_BUCKET, 1, 1),
            e(Items.OAK_PLANKS, 328, 512),
            e(Items.LEAD, 2, 7),
            e(Items.NAME_TAG, 1, 4),
            e(Items.FLINT_AND_STEEL, 1, 1),
            e(Items.FISHING_ROD, 1, 1),
            e(Items.OAK_SIGN, 40, 120),
            e(Items.OAK_FENCE, 104, 264),
            e(Items.TORCH, 296, 512)
         );
      };
   }

   private static List<LoadoutFiller.Entry> hotbar(int stage, LoadoutFiller.Preset preset) {
      return switch (preset) {
         case BUILDING -> {
            switch (stage) {
               case 1:
                  yield List.of(
                     e(Items.OAK_PLANKS, 12, 19),
                     e(Items.DIRT, 10, 19),
                     e(Items.COBBLESTONE, 8, 17),
                     e(Items.OAK_STAIRS, 5, 12),
                     e(Items.OAK_SLAB, 6, 13),
                     e(Items.SAND, 3, 8),
                     e(Items.SCAFFOLDING, 3, 11),
                     e(Items.LADDER, 2, 8),
                     e(Items.GLASS_PANE, 4, 10)
                  );
               case 2:
                  yield List.of(
                     e(Items.COBBLESTONE, 20, 32),
                     e(Items.STONE, 16, 32),
                     e(Items.STONE_BRICKS, 14, 29),
                     e(Items.OAK_STAIRS, 8, 20),
                     e(Items.OAK_SLAB, 10, 22),
                     e(Items.GLASS, 4, 17),
                     e(Items.SCAFFOLDING, 6, 18),
                     e(Items.LADDER, 4, 13),
                     e(Items.GLASS_PANE, 6, 16)
                  );
               case 3:
                  yield List.of(
                     e(Items.STONE_BRICKS, 57, 90),
                     e(Items.COBBLED_DEEPSLATE, 46, 90),
                     e(Items.DEEPSLATE_BRICKS, 38, 81),
                     e(Items.OAK_STAIRS, 24, 57),
                     e(Items.OAK_SLAB, 27, 62),
                     e(Items.GLASS, 13, 48),
                     e(Items.SCAFFOLDING, 15, 52),
                     e(Items.LADDER, 10, 36),
                     e(Items.GLASS_PANE, 18, 46)
                  );
               case 4:
                  yield List.of(
                     e(Items.DEEPSLATE_TILES, 144, 225),
                     e(Items.STONE_BRICKS, 116, 225),
                     e(Items.POLISHED_DEEPSLATE, 95, 204),
                     e(Items.OAK_STAIRS, 60, 144),
                     e(Items.OAK_SLAB, 67, 155),
                     e(Items.GLASS, 32, 120),
                     e(Items.SCAFFOLDING, 39, 130),
                     e(Items.LADDER, 25, 92),
                     e(Items.FIREWORK_ROCKET, 81, 215)
                  );
               default:
                  yield List.of(
                     e(Items.SMOOTH_QUARTZ, 328, 512),
                     e(Items.DEEPSLATE_TILES, 264, 512),
                     e(Items.POLISHED_BLACKSTONE, 216, 464),
                     e(Items.OAK_STAIRS, 136, 328),
                     e(Items.OAK_SLAB, 152, 352),
                     e(Items.GLASS, 72, 272),
                     e(Items.SCAFFOLDING, 88, 296),
                     e(Items.LADDER, 56, 208),
                     e(Items.FIREWORK_ROCKET, 184, 488)
                  );
            }
         }
         case COMBAT -> {
            switch (stage) {
               case 1:
                  yield List.of(
                     e(Items.BOW, 1, 1),
                     e(Items.ARROW, 6, 18),
                     e(Items.APPLE, 1, 3),
                     e(Items.EGG, 1, 4),
                     e(Items.COBWEB, 2, 7),
                     e(Items.OAK_PLANKS, 6, 15),
                     e(Items.COBBLESTONE, 5, 13),
                     e(Items.CHEST, 1, 3),
                     e(Items.SPECTRAL_ARROW, 2, 6)
                  );
               case 2:
                  yield List.of(
                     e(Items.BOW, 1, 1),
                     e(Items.ARROW, 10, 30),
                     e(Items.APPLE, 2, 4),
                     e(Items.EGG, 2, 6),
                     e(Items.COBWEB, 2, 7),
                     e(Items.COBBLESTONE, 10, 26),
                     e(Items.STONE_BRICKS, 8, 22),
                     e(Items.CHEST, 1, 3),
                     e(Items.SPECTRAL_ARROW, 4, 10)
                  );
               case 3:
                  yield List.of(
                     e(Items.CROSSBOW, 1, 1),
                     e(Items.ARROW, 27, 85),
                     e(Items.GOLDEN_APPLE, 1, 5),
                     e(Items.ENDER_PEARL, 2, 8),
                     e(Items.COBWEB, 2, 7),
                     e(Items.STONE_BRICKS, 29, 71),
                     e(Items.DEEPSLATE_BRICKS, 24, 62),
                     e(Items.ENDER_CHEST, 1, 1),
                     e(Items.SPECTRAL_ARROW, 10, 27)
                  );
               case 4:
                  yield List.of(
                     e(Items.CROSSBOW, 1, 1),
                     e(Items.ARROW, 67, 215),
                     e(Items.GOLDEN_APPLE, 1, 5),
                     e(Items.ENDER_PEARL, 2, 8),
                     e(Items.COBWEB, 2, 7),
                     e(Items.DEEPSLATE_TILES, 74, 180),
                     e(Items.POLISHED_DEEPSLATE, 60, 155),
                     e(Items.ENDER_CHEST, 1, 1),
                     e(Items.FIREWORK_ROCKET, 81, 215)
                  );
               default:
                  yield List.of(
                     e(Items.CROSSBOW, 1, 1),
                     e(Items.ARROW, 152, 488),
                     e(Items.GOLDEN_APPLE, 1, 5),
                     e(Items.ENDER_PEARL, 2, 8),
                     e(Items.COBWEB, 2, 7),
                     e(Items.SMOOTH_QUARTZ, 168, 408),
                     e(Items.POLISHED_BLACKSTONE, 136, 352),
                     e(Items.ENDER_CHEST, 1, 1),
                     e(Items.FIREWORK_ROCKET, 184, 488)
                  );
            }
         }
         case MINING -> {
            switch (stage) {
               case 1:
                  yield List.of(
                     e(Items.DIRT, 8, 19),
                     e(Items.OAK_PLANKS, 6, 14),
                     e(Items.LADDER, 3, 9),
                     e(Items.RAIL, 2, 7),
                     e(Items.POWERED_RAIL, 1, 3),
                     e(Items.MINECART, 1, 3),
                     e(Items.CHEST, 1, 3),
                     e(Items.COBBLESTONE, 5, 13),
                     e(Items.OAK_LOG, 3, 8)
                  );
               case 2:
                  yield List.of(
                     e(Items.STONE, 14, 32),
                     e(Items.COBBLESTONE, 10, 24),
                     e(Items.LADDER, 4, 16),
                     e(Items.RAIL, 3, 12),
                     e(Items.POWERED_RAIL, 2, 6),
                     e(Items.MINECART, 1, 3),
                     e(Items.CHEST, 1, 3),
                     e(Items.STONE_BRICKS, 8, 22),
                     e(Items.OAK_LOG, 4, 14)
                  );
               case 3:
                  yield List.of(
                     e(Items.COBBLED_DEEPSLATE, 38, 90),
                     e(Items.STONE_BRICKS, 29, 66),
                     e(Items.LADDER, 13, 43),
                     e(Items.RAIL, 8, 34),
                     e(Items.POWERED_RAIL, 4, 15),
                     e(Items.MINECART, 1, 3),
                     e(Items.ENDER_CHEST, 1, 1),
                     e(Items.TNT, 3, 13),
                     e(Items.OAK_LOG, 13, 38)
                  );
               case 4:
                  yield List.of(
                     e(Items.STONE_BRICKS, 95, 225),
                     e(Items.DEEPSLATE_TILES, 74, 165),
                     e(Items.LADDER, 32, 109),
                     e(Items.RAIL, 21, 84),
                     e(Items.POWERED_RAIL, 11, 39),
                     e(Items.MINECART, 1, 3),
                     e(Items.ENDER_CHEST, 1, 1),
                     e(Items.TNT, 7, 32),
                     e(Items.FIREWORK_ROCKET, 81, 215)
                  );
               default:
                  yield List.of(
                     e(Items.DEEPSLATE_TILES, 216, 512),
                     e(Items.SMOOTH_QUARTZ, 168, 376),
                     e(Items.LADDER, 72, 248),
                     e(Items.RAIL, 48, 192),
                     e(Items.POWERED_RAIL, 24, 88),
                     e(Items.MINECART, 1, 3),
                     e(Items.ENDER_CHEST, 1, 1),
                     e(Items.TNT, 16, 72),
                     e(Items.FIREWORK_ROCKET, 184, 488)
                  );
            }
         }
         case FARMING -> {
            switch (stage) {
               case 1:
                  yield List.of(
                     e(Items.WHEAT_SEEDS, 6, 18),
                     e(Items.BEETROOT_SEEDS, 3, 10),
                     e(Items.PUMPKIN_SEEDS, 3, 8),
                     e(Items.CARROT, 3, 10),
                     e(Items.POTATO, 3, 9),
                     e(Items.BONE_MEAL, 5, 15),
                     e(Items.COMPOSTER, 1, 2),
                     e(Items.HAY_BLOCK, 1, 4),
                     e(Items.OAK_SAPLING, 2, 5)
                  );
               case 2:
                  yield List.of(
                     e(Items.WHEAT_SEEDS, 10, 30),
                     e(Items.BEETROOT_SEEDS, 6, 17),
                     e(Items.PUMPKIN_SEEDS, 4, 14),
                     e(Items.CARROT, 6, 17),
                     e(Items.POTATO, 4, 14),
                     e(Items.BONE_MEAL, 8, 26),
                     e(Items.COMPOSTER, 1, 2),
                     e(Items.HAY_BLOCK, 2, 7),
                     e(Items.OAK_SAPLING, 2, 8)
                  );
               case 3:
                  yield List.of(
                     e(Items.WHEAT_SEEDS, 29, 85),
                     e(Items.BEETROOT_SEEDS, 15, 48),
                     e(Items.PUMPKIN_SEEDS, 13, 38),
                     e(Items.CARROT, 15, 48),
                     e(Items.POTATO, 13, 41),
                     e(Items.BONE_MEAL, 24, 71),
                     e(Items.COMPOSTER, 1, 2),
                     e(Items.HAY_BLOCK, 4, 20),
                     e(Items.OAK_SAPLING, 7, 24)
                  );
               case 4:
                  yield List.of(
                     e(Items.WHEAT_SEEDS, 74, 215),
                     e(Items.BEETROOT_SEEDS, 39, 120),
                     e(Items.PUMPKIN_SEEDS, 32, 95),
                     e(Items.CARROT, 39, 120),
                     e(Items.POTATO, 32, 102),
                     e(Items.BONE_MEAL, 60, 180),
                     e(Items.COMPOSTER, 1, 2),
                     e(Items.HAY_BLOCK, 11, 49),
                     e(Items.FIREWORK_ROCKET, 81, 215)
                  );
               default:
                  yield List.of(
                     e(Items.WHEAT_SEEDS, 168, 488),
                     e(Items.BEETROOT_SEEDS, 88, 272),
                     e(Items.PUMPKIN_SEEDS, 72, 216),
                     e(Items.CARROT, 88, 272),
                     e(Items.POTATO, 72, 232),
                     e(Items.BONE_MEAL, 136, 408),
                     e(Items.COMPOSTER, 1, 2),
                     e(Items.HAY_BLOCK, 24, 112),
                     e(Items.FIREWORK_ROCKET, 184, 488)
                  );
            }
         }
         case EXPLORING -> {
            switch (stage) {
               case 1:
                  yield List.of(
                     e(Items.OAK_BOAT, 1, 1),
                     e(Items.BED.pick(DyeColor.RED), 1, 1),
                     e(Items.MAP, 1, 4),
                     e(Items.COMPASS, 1, 1),
                     e(Items.CLOCK, 1, 1),
                     e(Items.OAK_PLANKS, 6, 14),
                     e(Items.COBBLESTONE, 4, 11),
                     e(Items.CHEST, 1, 3),
                     e(Items.LADDER, 2, 7)
                  );
               case 2:
                  yield List.of(
                     e(Items.OAK_BOAT, 1, 1),
                     e(Items.BED.pick(DyeColor.RED), 1, 1),
                     e(Items.MAP, 1, 4),
                     e(Items.COMPASS, 1, 1),
                     e(Items.CLOCK, 1, 1),
                     e(Items.COBBLESTONE, 10, 24),
                     e(Items.STONE_BRICKS, 7, 18),
                     e(Items.CHEST, 1, 3),
                     e(Items.LADDER, 4, 12)
                  );
               case 3:
                  yield List.of(
                     e(Items.OAK_BOAT, 1, 1),
                     e(Items.BED.pick(DyeColor.RED), 1, 1),
                     e(Items.MAP, 1, 4),
                     e(Items.SPYGLASS, 1, 1),
                     e(Items.ENDER_PEARL, 2, 8),
                     e(Items.STONE_BRICKS, 27, 66),
                     e(Items.DEEPSLATE_BRICKS, 20, 52),
                     e(Items.ENDER_CHEST, 1, 1),
                     e(Items.LADDER, 10, 34)
                  );
               case 4:
                  yield List.of(
                     e(Items.OAK_BOAT, 1, 1),
                     e(Items.BED.pick(DyeColor.RED), 1, 1),
                     e(Items.MAP, 1, 4),
                     e(Items.SPYGLASS, 1, 1),
                     e(Items.ENDER_PEARL, 2, 8),
                     e(Items.DEEPSLATE_TILES, 67, 165),
                     e(Items.POLISHED_DEEPSLATE, 49, 130),
                     e(Items.ENDER_CHEST, 1, 1),
                     e(Items.FIREWORK_ROCKET, 81, 215)
                  );
               default:
                  yield List.of(
                     e(Items.OAK_BOAT, 1, 1),
                     e(Items.BED.pick(DyeColor.RED), 1, 1),
                     e(Items.MAP, 1, 4),
                     e(Items.SPYGLASS, 1, 1),
                     e(Items.ENDER_PEARL, 2, 8),
                     e(Items.SMOOTH_QUARTZ, 152, 376),
                     e(Items.POLISHED_BLACKSTONE, 112, 296),
                     e(Items.ENDER_CHEST, 1, 1),
                     e(Items.FIREWORK_ROCKET, 184, 488)
                  );
            }
         }
         default -> {
            switch (stage) {
               case 1:
                  yield List.of(
                     e(Items.OAK_PLANKS, 8, 19),
                     e(Items.DIRT, 6, 15),
                     e(Items.COBBLESTONE, 5, 13),
                     e(Items.SAND, 3, 8),
                     e(Items.LADDER, 2, 8),
                     e(Items.CHEST, 1, 3),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.FURNACE, 1, 3),
                     e(Items.OAK_LOG, 3, 9)
                  );
               case 2:
                  yield List.of(
                     e(Items.COBBLESTONE, 14, 32),
                     e(Items.STONE, 10, 26),
                     e(Items.STONE_BRICKS, 8, 22),
                     e(Items.GLASS, 4, 17),
                     e(Items.LADDER, 4, 13),
                     e(Items.CHEST, 1, 3),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.FURNACE, 1, 3),
                     e(Items.OAK_LOG, 6, 14)
                  );
               case 3:
                  yield List.of(
                     e(Items.STONE_BRICKS, 38, 90),
                     e(Items.COBBLED_DEEPSLATE, 29, 71),
                     e(Items.DEEPSLATE_BRICKS, 24, 62),
                     e(Items.GLASS, 13, 48),
                     e(Items.LADDER, 10, 36),
                     e(Items.ENDER_CHEST, 1, 1),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.FURNACE, 1, 3),
                     e(Items.OAK_LOG, 15, 41)
                  );
               case 4:
                  yield List.of(
                     e(Items.DEEPSLATE_TILES, 95, 225),
                     e(Items.STONE_BRICKS, 74, 180),
                     e(Items.POLISHED_DEEPSLATE, 60, 155),
                     e(Items.GLASS, 32, 120),
                     e(Items.LADDER, 25, 92),
                     e(Items.ENDER_CHEST, 1, 1),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.FURNACE, 1, 3),
                     e(Items.FIREWORK_ROCKET, 81, 215)
                  );
               default:
                  yield List.of(
                     e(Items.SMOOTH_QUARTZ, 216, 512),
                     e(Items.DEEPSLATE_TILES, 168, 408),
                     e(Items.POLISHED_BLACKSTONE, 136, 352),
                     e(Items.GLASS, 72, 272),
                     e(Items.LADDER, 56, 208),
                     e(Items.ENDER_CHEST, 1, 1),
                     e(Items.CRAFTING_TABLE, 1, 2),
                     e(Items.FURNACE, 1, 3),
                     e(Items.FIREWORK_ROCKET, 184, 488)
                  );
            }
         }
      };
   }

   private static List<LoadoutFiller.Entry> offhand(int stage, LoadoutFiller.Preset preset) {
      return switch (preset) {
         case BUILDING -> {
            switch (stage) {
               case 1:
                  yield List.of(e(Items.SHIELD, 1, 1), e(Items.TORCH, 5, 15), e(Items.BREAD, 2, 6), e(Items.WATER_BUCKET, 1, 1));
               case 2:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.TORCH, 8, 26), e(Items.COOKED_PORKCHOP, 4, 10), e(Items.WATER_BUCKET, 1, 1)
                  );
               case 3:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.TORCH, 22, 71), e(Items.COOKED_BEEF, 10, 29), e(Items.WATER_BUCKET, 1, 1)
                  );
               case 4:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.LANTERN, 56, 180), e(Items.GOLDEN_CARROT, 25, 74), e(Items.TOTEM_OF_UNDYING, 1, 1)
                  );
               default:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.LANTERN, 128, 408), e(Items.COOKED_BEEF, 56, 168), e(Items.TOTEM_OF_UNDYING, 1, 1)
                  );
            }
         }
         case COMBAT -> {
            switch (stage) {
               case 1:
                  yield List.of(e(Items.SHIELD, 1, 1), e(Items.TORCH, 5, 15), e(Items.MILK_BUCKET, 1, 1), e(Items.WATER_BUCKET, 1, 1));
               case 2:
                  yield List.of(e(Items.SHIELD, 1, 1), e(Items.TORCH, 8, 26), e(Items.MILK_BUCKET, 1, 1), e(Items.WATER_BUCKET, 1, 1));
               case 3:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.TORCH, 24, 71), e(Items.MILK_BUCKET, 1, 1), e(Items.WATER_BUCKET, 1, 1)
                  );
               case 4:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.LANTERN, 60, 180), e(Items.ENCHANTED_GOLDEN_APPLE, 1, 3), e(Items.TOTEM_OF_UNDYING, 1, 1)
                  );
               default:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.LANTERN, 136, 408), e(Items.ENCHANTED_GOLDEN_APPLE, 1, 3), e(Items.TOTEM_OF_UNDYING, 1, 1)
                  );
            }
         }
         case MINING -> {
            switch (stage) {
               case 1:
                  yield List.of(e(Items.SHIELD, 1, 1), e(Items.TORCH, 5, 15), e(Items.BREAD, 2, 6), e(Items.WATER_BUCKET, 1, 1));
               case 2:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.TORCH, 8, 26), e(Items.COOKED_PORKCHOP, 4, 10), e(Items.WATER_BUCKET, 1, 1)
                  );
               case 3:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.TORCH, 22, 71), e(Items.COOKED_BEEF, 10, 29), e(Items.WATER_BUCKET, 1, 1)
                  );
               case 4:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.LANTERN, 56, 180), e(Items.GOLDEN_CARROT, 25, 74), e(Items.TOTEM_OF_UNDYING, 1, 1)
                  );
               default:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.LANTERN, 128, 408), e(Items.COOKED_BEEF, 56, 168), e(Items.TOTEM_OF_UNDYING, 1, 1)
                  );
            }
         }
         case FARMING -> {
            switch (stage) {
               case 1:
                  yield List.of(e(Items.SHIELD, 1, 1), e(Items.TORCH, 5, 15), e(Items.BREAD, 2, 6), e(Items.WATER_BUCKET, 1, 1));
               case 2:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.TORCH, 8, 26), e(Items.COOKED_PORKCHOP, 4, 10), e(Items.WATER_BUCKET, 1, 1)
                  );
               case 3:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.TORCH, 22, 71), e(Items.COOKED_BEEF, 10, 29), e(Items.WATER_BUCKET, 1, 1)
                  );
               case 4:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.LANTERN, 56, 180), e(Items.GOLDEN_CARROT, 25, 74), e(Items.TOTEM_OF_UNDYING, 1, 1)
                  );
               default:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.LANTERN, 128, 408), e(Items.COOKED_BEEF, 56, 168), e(Items.TOTEM_OF_UNDYING, 1, 1)
                  );
            }
         }
         case EXPLORING -> {
            switch (stage) {
               case 1:
                  yield List.of(e(Items.SHIELD, 1, 1), e(Items.TORCH, 5, 15), e(Items.BREAD, 2, 6), e(Items.WATER_BUCKET, 1, 1));
               case 2:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.TORCH, 8, 26), e(Items.COOKED_PORKCHOP, 4, 10), e(Items.WATER_BUCKET, 1, 1)
                  );
               case 3:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.TORCH, 22, 71), e(Items.COOKED_BEEF, 10, 29), e(Items.WATER_BUCKET, 1, 1)
                  );
               case 4:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.LANTERN, 56, 180), e(Items.GOLDEN_CARROT, 25, 74), e(Items.TOTEM_OF_UNDYING, 1, 1)
                  );
               default:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.LANTERN, 128, 408), e(Items.COOKED_BEEF, 56, 168), e(Items.TOTEM_OF_UNDYING, 1, 1)
                  );
            }
         }
         default -> {
            switch (stage) {
               case 1:
                  yield List.of(e(Items.SHIELD, 1, 1), e(Items.TORCH, 5, 15), e(Items.BREAD, 2, 6), e(Items.WATER_BUCKET, 1, 1));
               case 2:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.TORCH, 8, 26), e(Items.COOKED_PORKCHOP, 4, 10), e(Items.WATER_BUCKET, 1, 1)
                  );
               case 3:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.TORCH, 22, 71), e(Items.COOKED_BEEF, 10, 29), e(Items.WATER_BUCKET, 1, 1)
                  );
               case 4:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.LANTERN, 56, 180), e(Items.GOLDEN_CARROT, 25, 74), e(Items.TOTEM_OF_UNDYING, 1, 1)
                  );
               default:
                  yield List.of(
                     e(Items.SHIELD, 1, 1), e(Items.LANTERN, 128, 408), e(Items.COOKED_BEEF, 56, 168), e(Items.TOTEM_OF_UNDYING, 1, 1)
                  );
            }
         }
      };
   }

   private static List<List<LoadoutFiller.Entry>> tabOrder(int stage, LoadoutFiller.Preset preset, LoadoutFiller.Style style) {
      if (style == LoadoutFiller.Style.PRETTY) {
         return switch (preset) {
            case BUILDING -> List.of(kitBuilding(stage), kitMining(stage), kitExploring(stage), kitCombat(stage));
            case COMBAT -> List.of(kitCombat(stage), kitMining(stage), kitBuilding(stage), kitExploring(stage));
            case MINING -> List.of(kitMining(stage), kitBuilding(stage), kitCombat(stage), kitExploring(stage));
            case FARMING -> List.of(kitFarming(stage), kitBuilding(stage), kitExploring(stage), kitCombat(stage));
            case EXPLORING -> List.of(kitExploring(stage), kitCombat(stage), kitMining(stage), kitBuilding(stage));
            default -> List.of(kitBuilding(stage), kitCombat(stage), kitMining(stage), kitFarming(stage));
         };
      } else {
         List<LoadoutFiller.Entry> blocks = blocks(stage);
         List<LoadoutFiller.Entry> mats = mats(stage);
         List<LoadoutFiller.Entry> food = food(stage);
         List<LoadoutFiller.Entry> spares = spares(stage);

         return switch (preset) {
            case BUILDING -> List.of(blocks, mats, spares, food);
            case COMBAT -> List.of(spares, food, mats, blocks);
            case MINING -> List.of(mats, blocks, spares, food);
            case FARMING -> List.of(food, mats, blocks, spares);
            case EXPLORING -> List.of(food, spares, blocks, mats);
            default -> List.of(blocks, mats, food, spares);
         };
      }
   }

   private static Item[] rack(int stage) {
      return switch (stage) {
         case 1 -> new Item[]{
            Items.WOODEN_HOE, Items.WOODEN_SHOVEL, Items.STONE_PICKAXE, Items.WOODEN_PICKAXE, Items.STONE_AXE, Items.STONE_SWORD
         };
         case 2 -> new Item[]{
            Items.STONE_HOE, Items.IRON_SHOVEL, Items.IRON_PICKAXE, Items.STONE_PICKAXE, Items.IRON_AXE, Items.IRON_SWORD
         };
         case 3 -> new Item[]{
            Items.IRON_HOE, Items.IRON_SHOVEL, Items.DIAMOND_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SWORD
         };
         case 4 -> new Item[]{
            Items.DIAMOND_HOE, Items.DIAMOND_SHOVEL, Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.NETHERITE_SWORD
         };
         default -> new Item[]{
            Items.NETHERITE_HOE, Items.NETHERITE_SHOVEL, Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SWORD
         };
      };
   }

   private static Item[] armour(int stage) {
      return switch (stage) {
         case 1 -> new Item[]{Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, null, Items.LEATHER_BOOTS};
         case 2 -> new Item[]{Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.IRON_BOOTS};
         case 3 -> new Item[]{Items.IRON_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.IRON_BOOTS};
         case 4 -> new Item[]{Items.DIAMOND_HELMET, Items.NETHERITE_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS};
         default -> new Item[]{Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS};
      };
   }

   private static Item[] fullArmour(int stage) {
      return switch (stage) {
         case 1 -> new Item[]{Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS};
         case 2 -> new Item[]{Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS};
         case 3 -> new Item[]{Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS};
         default -> new Item[]{Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS};
      };
   }

   private static ItemStack stackUpgrade(int stage) {
      int tier = switch (stage) {
         case 1, 2 -> 0;
         case 3 -> 2;
         case 4 -> 4;
         default -> 6;
      };
      return tier == 0 ? ItemStack.EMPTY : new ItemStack((ItemLike)ModItems.STACK_UPGRADES[tier - 1].get());
   }

   private static ItemStack craftingUpgrade(int stage) {
      return stage <= 2
         ? ItemStack.EMPTY
         : new ItemStack(stage >= 4 ? (ItemLike)ModItems.ADVANCED_CRAFTING_UPGRADE.get() : (ItemLike)ModItems.CRAFTING_UPGRADE.get());
   }

   private static List<ItemStack> upgrades(int stage) {
      List<ItemStack> out = new ArrayList<>();
      if (stage >= 2) {
         out.add(upgrade("upgrade_magnet", stage >= 4 ? "_3" : ""));
      }

      if (stage >= 3) {
         out.add(upgrade("upgrade_pickup", stage >= 5 ? "_2" : ""));
      }

      if (stage >= 4) {
         out.add(upgrade("upgrade_feeding", stage >= 5 ? "_3" : ""));
      }

      return out;
   }

   private static ItemStack upgrade(String base, String suffix) {
      ItemStack fallback = ItemStack.EMPTY;

      for (Item item : ModItems.all()) {
         String id = BuiltInRegistries.ITEM.getKey(item).getPath();
         if (id.equals(base + suffix)) {
            return new ItemStack(item);
         }

         if (id.equals(base)) {
            fallback = new ItemStack(item);
         }
      }

      return fallback;
   }

   private static int enchantCost(int stage, LoadoutFiller.Style style, RandomSource rng) {
      int base = switch (stage) {
         case 1 -> 0;
         case 2 -> 8;
         case 3 -> 18;
         case 4 -> 27;
         default -> 34;
      };
      if (base == 0) {
         return 0;
      } else {
         return style == LoadoutFiller.Style.PRETTY ? base + 4 : base - rng.nextInt(6);
      }
   }

   private static ItemStack dress(ItemStack stack, int stage, LoadoutFiller.Style style, RandomSource rng, RegistryAccess registries) {
      if (stack.isEmpty()) {
         return stack;
      }

      int cost = enchantCost(stage, style, rng);
      if (cost > 0 && stack.isEnchantable()) {
         stack = EnchantmentHelper.enchantItem(rng, stack, cost, registries, Optional.empty());
      }

      if (style == LoadoutFiller.Style.REALISTIC && stack.isDamageableItem() && rng.nextInt(4) > 0) {
         stack.setDamageValue(stack.getMaxDamage() * (10 + rng.nextInt(45)) / 100);
      }

      return stack;
   }

   private static ItemStack roll(LoadoutFiller.Entry entry, RandomSource rng, int cap) {
      ItemStack stack = new ItemStack(entry.item());
      int n = entry.max() > entry.min() ? entry.min() + rng.nextInt(entry.max() - entry.min() + 1) : entry.min();
      int limit = Math.min(stack.getMaxStackSize(), StackUpgradeService.scaledCap(stack, cap));
      stack.setCount(Math.max(1, Math.min(limit, n)));
      return stack;
   }

   private static List<LoadoutFiller.Entry> without(List<LoadoutFiller.Entry> list, Set<Item> taken) {
      List<LoadoutFiller.Entry> out = new ArrayList<>();

      for (LoadoutFiller.Entry entry : list) {
         if (!taken.contains(entry.item())) {
            out.add(entry);
         }
      }

      return out;
   }

   public static LiteralArgumentBuilder<CommandSourceStack> build() {
      RequiredArgumentBuilder<CommandSourceStack, Integer> stage = (RequiredArgumentBuilder<CommandSourceStack, Integer>)Commands.argument(
            "stage", IntegerArgumentType.integer(1, 5)
         )
         .executes(
            ctx -> run((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger(ctx, "stage"), LoadoutFiller.Preset.GENERAL, LoadoutFiller.Style.REALISTIC)
         );

      for (LoadoutFiller.Style style : LoadoutFiller.Style.values()) {
         stage.then(
            Commands.literal(style.name().toLowerCase(Locale.ROOT))
               .executes(ctx -> run((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger(ctx, "stage"), LoadoutFiller.Preset.GENERAL, style))
         );
      }

      for (LoadoutFiller.Preset preset : LoadoutFiller.Preset.values()) {
         LiteralArgumentBuilder<CommandSourceStack> node = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal(preset.name().toLowerCase(Locale.ROOT))
            .executes(ctx -> run((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger(ctx, "stage"), preset, LoadoutFiller.Style.REALISTIC));

         for (LoadoutFiller.Style style : LoadoutFiller.Style.values()) {
            node.then(
               Commands.literal(style.name().toLowerCase(Locale.ROOT))
                  .executes(ctx -> run((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger(ctx, "stage"), preset, style))
            );
         }

         stage.then(node);
      }

      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("betterinventory")
               .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)))
            .then(Commands.literal("fill").then(stage)))
         .then(
            ((LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("rules").requires(src -> Boolean.getBoolean("betterinventory.selftest")))
               .executes(context -> SelfTestFeatures.rulesScene(((CommandSourceStack)context.getSource()).getPlayerOrException()))
         );
   }

   private static int run(CommandSourceStack source, int stage, LoadoutFiller.Preset preset, LoadoutFiller.Style style) {
      ServerPlayer player;
      try {
         player = source.getPlayerOrException();
      } catch (Exception ex) {
         source.sendFailure(Component.literal("Run this as a player."));
         return 0;
      }

      apply(player, stage, preset, style);
      source.sendSuccess(
         () -> Component.literal(
            "Filled inventory: stage " + stage + ", " + preset.name().toLowerCase(Locale.ROOT) + ", " + style.name().toLowerCase(Locale.ROOT)
         ),
         true
      );
      return 1;
   }

   public static void apply(ServerPlayer player, int stage, LoadoutFiller.Preset preset, LoadoutFiller.Style style) {
      PlayerLoadout loadout = PlayerLoadout.of(player);
      RandomSource rng = RandomSource.create();
      RegistryAccess registries = player.level().registryAccess();
      player.getInventory().clearContent();
      ItemStack pack = new ItemStack((ItemLike)ModItems.BACKPACKS[Math.min(5, Math.max(1, stage)) - 1].get());
      loadout.backpacks.setStackInSlot(0, pack);
      loadout.personal.setStackInSlot(0, stackUpgrade(stage));
      loadout.personal.setStackInSlot(1, craftingUpgrade(stage));
      List<ItemStack> ups = upgrades(stage);

      for (int i = 0; i < 3; i++) {
         loadout.packUpgrades.setStackInSlot(i, i < ups.size() ? ups.get(i) : ItemStack.EMPTY);
      }

      Item[] rack = rack(stage);

      for (int i = 0; i < rack.length; i++) {
         loadout.tools.setStackInSlot(i, dress(new ItemStack(rack[i]), stage, style, rng, registries));
      }

      Item[] armour = style == LoadoutFiller.Style.PRETTY ? fullArmour(stage) : armour(stage);
      EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

      for (int i = 0; i < armour.length; i++) {
         player.setItemSlot(slots[i], armour[i] == null ? ItemStack.EMPTY : dress(new ItemStack(armour[i]), stage, style, rng, registries));
      }

      if (stage >= 5 && style == LoadoutFiller.Style.REALISTIC) {
         player.setItemSlot(EquipmentSlot.CHEST, dress(new ItemStack(Items.ELYTRA), stage, style, rng, registries));
      }

      int cap = loadout.storageCap();
      List<LoadoutFiller.Entry> bar = hotbar(stage, preset);
      List<LoadoutFiller.Entry> off = offhand(stage, preset);
      Set<Item> onScreen = new HashSet<>();
      Set<Item> hotbarItems = new HashSet<>();

      for (LoadoutFiller.Entry entry : bar) {
         onScreen.add(entry.item());
         hotbarItems.add(entry.item());
      }

      for (LoadoutFiller.Entry entry : off) {
         onScreen.add(entry.item());
      }

      place(player.getInventory()::setItem, 0, 9, bar, stage, style, rng, registries, cap);

      for (int i = 0; i < loadout.offhandStore.getSlots(); i++) {
         loadout.offhandStore.setStackInSlot(i, i < off.size() ? dress(roll(off.get(i), rng, cap), stage, style, rng, registries) : ItemStack.EMPTY);
      }

      List<LoadoutFiller.Entry> grid = without(style == LoadoutFiller.Style.PRETTY ? prettyGrid(stage, preset) : grid(stage), onScreen);

      for (LoadoutFiller.Entry entry : grid) {
         onScreen.add(entry.item());
      }

      place(player.getInventory()::setItem, 9, 27, grid, stage, style, rng, registries, cap);
      int hub = loadout.gather.getSlots();
      place(
         loadout.gather::setStackInSlot,
         0,
         hub,
         without(style == LoadoutFiller.Style.PRETTY ? prettyGather(stage) : gather(stage), onScreen),
         stage,
         style,
         rng,
         registries,
         cap
      );
      ComponentBackedHandler pages = new ComponentBackedHandler(
         loadout.backpacks.getStackInSlot(0), ModComponents.BACKPACK_CONTENTS.get(), 288, loadout::storageCap
      );
      List<List<LoadoutFiller.Entry>> order = tabOrder(stage, preset, style);

      for (int tab = 1; tab <= 4; tab++) {
         place(pages::setStackInSlot, PlayerLoadout.pageBase(tab), 72, without(order.get(tab - 1), hotbarItems), stage, style, rng, registries, cap);
      }

      loadout.dirty = true;
      player.inventoryMenu.broadcastChanges();
   }

   private static void place(
      LoadoutFiller.SlotSink sink,
      int base,
      int slots,
      List<LoadoutFiller.Entry> entries,
      int stage,
      LoadoutFiller.Style style,
      RandomSource rng,
      RegistryAccess registries,
      int cap
   ) {
      List<LoadoutFiller.Entry> ordered = new ArrayList<>(entries);
      if (style == LoadoutFiller.Style.REALISTIC) {
         Collections.shuffle(ordered, new Random(rng.nextLong()));
      }

      int index = 0;

      for (int slot = 0; slot < slots; slot++) {
         if (index >= ordered.size()) {
            sink.set(base + slot, ItemStack.EMPTY);
         } else {
            boolean slack = slots - slot > ordered.size() - index;
            if (style == LoadoutFiller.Style.REALISTIC && slack && rng.nextInt(7) == 0) {
               sink.set(base + slot, ItemStack.EMPTY);
            } else {
               sink.set(base + slot, dress(roll(ordered.get(index++), rng, cap), stage, style, rng, registries));
            }
         }
      }
   }

   private record Entry(Item item, int min, int max) {
   }

   public enum Preset {
      GENERAL,
      BUILDING,
      COMBAT,
      MINING,
      FARMING,
      EXPLORING;
   }

   private interface SlotSink {
      void set(int var1, ItemStack var2);
   }

   public enum Style {
      REALISTIC,
      PRETTY;
   }
}
