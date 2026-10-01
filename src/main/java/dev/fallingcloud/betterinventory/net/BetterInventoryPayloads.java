package dev.fallingcloud.betterinventory.net;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.data.SettingKey;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.logic.SyncService;
import dev.fallingcloud.betterinventory.logic.TabService;
import dev.fallingcloud.betterinventory.menu.BackpackMenu;
import dev.fallingcloud.betterinventory.menu.BetterInventoryMenu;
import dev.fallingcloud.betterinventory.menu.SettingsMenu;
import net.minecraft.world.item.ItemStack;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import dev.fallingcloud.betterinventory.shim.RegisterPayloadHandlersEvent;
import dev.fallingcloud.betterinventory.shim.PayloadRegistrar;

public final class BetterInventoryPayloads {
    private BetterInventoryPayloads() {}

    // ------------------------------------------------------------------ C2S

    public record OpenLoadout() implements CustomPacketPayload {
        public static final Type<OpenLoadout> TYPE = new Type<>(BetterInventory.id("open_loadout"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenLoadout> CODEC = StreamCodec.unit(new OpenLoadout());

        @Override
        public Type<OpenLoadout> type() {
            return TYPE;
        }
    }

    public record OpenSettings(int tab) implements CustomPacketPayload {
        public static final Type<OpenSettings> TYPE = new Type<>(BetterInventory.id("open_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenSettings> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenSettings::tab, OpenSettings::new);

        @Override
        public Type<OpenSettings> type() {
            return TYPE;
        }
    }

    public record SwitchTab(int tab) implements CustomPacketPayload {
        public static final Type<SwitchTab> TYPE = new Type<>(BetterInventory.id("switch_tab"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SwitchTab> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, SwitchTab::tab, SwitchTab::new);

        @Override
        public Type<SwitchTab> type() {
            return TYPE;
        }
    }

    /** Toggles the crafting panel that replaces the character view. */
    public record ToggleCrafting() implements CustomPacketPayload {
        public static final Type<ToggleCrafting> TYPE = new Type<>(BetterInventory.id("toggle_crafting"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleCrafting> CODEC =
                StreamCodec.unit(new ToggleCrafting());

        @Override
        public Type<ToggleCrafting> type() {
            return TYPE;
        }
    }

    public record SelectOffhand(int index) implements CustomPacketPayload {
        public static final Type<SelectOffhand> TYPE = new Type<>(BetterInventory.id("select_offhand"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectOffhand> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, SelectOffhand::index, SelectOffhand::new);

        @Override
        public Type<SelectOffhand> type() {
            return TYPE;
        }
    }

    public record ToggleSetting(int tab, int key, int value) implements CustomPacketPayload {
        public static final Type<ToggleSetting> TYPE = new Type<>(BetterInventory.id("toggle_setting"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleSetting> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ToggleSetting::tab,
                ByteBufCodecs.VAR_INT, ToggleSetting::key,
                ByteBufCodecs.VAR_INT, ToggleSetting::value,
                ToggleSetting::new);

        @Override
        public Type<ToggleSetting> type() {
            return TYPE;
        }
    }

    public record ToggleUpgrade(int tab, String upgradeId, boolean enabled) implements CustomPacketPayload {
        public static final Type<ToggleUpgrade> TYPE = new Type<>(BetterInventory.id("toggle_upgrade"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleUpgrade> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ToggleUpgrade::tab,
                ByteBufCodecs.STRING_UTF8, ToggleUpgrade::upgradeId,
                ByteBufCodecs.BOOL, ToggleUpgrade::enabled,
                ToggleUpgrade::new);

        @Override
        public Type<ToggleUpgrade> type() {
            return TYPE;
        }
    }

    public record SetShare(int key, boolean shared, int sourceTab) implements CustomPacketPayload {
        public static final Type<SetShare> TYPE = new Type<>(BetterInventory.id("set_share"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetShare> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SetShare::key,
                ByteBufCodecs.BOOL, SetShare::shared,
                ByteBufCodecs.VAR_INT, SetShare::sourceTab,
                SetShare::new);

        @Override
        public Type<SetShare> type() {
            return TYPE;
        }
    }

    public record SetLock(int tab, int slot, Optional<Identifier> item) implements CustomPacketPayload {
        public static final Type<SetLock> TYPE = new Type<>(BetterInventory.id("set_lock"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetLock> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SetLock::tab,
                ByteBufCodecs.VAR_INT, SetLock::slot,
                ByteBufCodecs.optional(Identifier.STREAM_CODEC), SetLock::item,
                SetLock::new);

        @Override
        public Type<SetLock> type() {
            return TYPE;
        }
    }

    public record SetPickaxeList(int tab, List<Identifier> blocks) implements CustomPacketPayload {
        public static final Type<SetPickaxeList> TYPE = new Type<>(BetterInventory.id("set_pickaxe_list"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetPickaxeList> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SetPickaxeList::tab,
                Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(1024)), SetPickaxeList::blocks,
                SetPickaxeList::new);

        @Override
        public Type<SetPickaxeList> type() {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------ S2C

    public record SyncLoadout(CompoundTag data) implements CustomPacketPayload {
        public static final Type<SyncLoadout> TYPE = new Type<>(BetterInventory.id("sync_loadout"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncLoadout> CODEC =
                StreamCodec.composite(ByteBufCodecs.TRUSTED_COMPOUND_TAG, SyncLoadout::data, SyncLoadout::new);

        @Override
        public Type<SyncLoadout> type() {
            return TYPE;
        }
    }

    // ------------------------------------------------------------------ registration

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(OpenLoadout.TYPE, OpenLoadout.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                BetterInventoryMenu.open(player);
            }
        });

        registrar.playToServer(OpenSettings.TYPE, OpenSettings.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                int tab = clampTab(payload.tab());
                SettingsMenu.open(player, tab);
            }
        });

        registrar.playToServer(SwitchTab.TYPE, SwitchTab.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                // Physically swaps the tab's contents into the live inventory.
                TabService.switchTo(player, clampTab(payload.tab()));
            }
        });

        registrar.playToServer(ToggleCrafting.TYPE, ToggleCrafting.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                PlayerLoadout loadout = ModAttachments.get(player);
                loadout.craftingOpen = !loadout.craftingOpen;
                // Closing hands the grid back rather than leaving items in a panel
                // the player can no longer see.
                if (!loadout.craftingOpen
                        && player.containerMenu instanceof BetterInventoryMenu menu) {
                    menu.returnCraftingGrid(player);
                }
                loadout.dirty = true;
            }
        });

        registrar.playToServer(SelectOffhand.TYPE, SelectOffhand.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                PlayerLoadout loadout = ModAttachments.get(player);
                int index = Math.floorMod(payload.index(), 4);
                if (index != loadout.activeOffhand) {
                    loadout.activeOffhand = index;
                    // Mirror the newly selected stored item into the real offhand slot.
                    player.getInventory().setItem(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND, loadout.offhandStore.getStackInSlot(index));
                    loadout.dirty = true;
                }
            }
        });

        registrar.playToServer(ToggleSetting.TYPE, ToggleSetting.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                BetterInventorySettings.applySetting(player, clampTab(payload.tab()), SettingKey.byId(payload.key()), payload.value());
            }
        });

        registrar.playToServer(ToggleUpgrade.TYPE, ToggleUpgrade.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                int tab = clampTab(payload.tab());
                if (payload.upgradeId().length() <= 32) {
                    TabSettings settings = BetterInventorySettings.raw(player, tab);
                    BetterInventorySettings.setRaw(player, tab, settings.withUpgradeToggle(payload.upgradeId(), payload.enabled()));
                }
            }
        });

        registrar.playToServer(SetShare.TYPE, SetShare.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                BetterInventorySettings.setShared(player, SettingKey.byId(payload.key()), payload.shared(), clampTab(payload.sourceTab()));
            }
        });

        registrar.playToServer(SetLock.TYPE, SetLock.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                int slot = payload.slot();
                if (slot < 0 || slot >= 45) {
                    return;
                }
                Optional<Identifier> item = payload.item()
                        .filter(rl -> BuiltInRegistries.ITEM.getOptional(rl).isPresent());
                if (payload.item().isPresent() && item.isEmpty()) {
                    return;
                }
                if (payload.tab() == -1) {
                    // Lock inside the standalone backpack GUI (held or placed backpack).
                    if (player.containerMenu instanceof BackpackMenu backpackMenu) {
                        ItemStack host = backpackMenu.storageHandler.host();
                        if (host.getItem() instanceof BackpackItem) {
                            BackpackItem.setSettings(host, BackpackItem.settings(host).withLock(slot, item));
                            backpackMenu.access.markChanged();
                        }
                    }
                    return;
                }
                int tab = clampTab(payload.tab());
                TabSettings settings = BetterInventorySettings.raw(player, tab);
                BetterInventorySettings.setRaw(player, tab, settings.withLock(slot, item));
            }
        });

        registrar.playToServer(SetPickaxeList.TYPE, SetPickaxeList.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                int tab = clampTab(payload.tab());
                List<Identifier> blocks = payload.blocks().stream()
                        .filter(rl -> BuiltInRegistries.BLOCK.getOptional(rl).isPresent())
                        .distinct()
                        .limit(1024)
                        .toList();
                TabSettings settings = BetterInventorySettings.raw(player, tab);
                BetterInventorySettings.setRaw(player, tab, settings.withPickaxe2Blocks(blocks));
            }
        });

        registrar.playToClient(SyncLoadout.TYPE, SyncLoadout.CODEC, (payload, context) -> {
            SyncService.applyClientSync(context.player(), payload.data());
        });
    }

    private static int clampTab(int tab) {
        return Math.floorMod(tab, PlayerLoadout.TAB_COUNT);
    }
}
