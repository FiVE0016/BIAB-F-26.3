package dev.fallingcloud.betterinventory.platform;

import dev.fallingcloud.betterinventory.compat.TrinketsCompat;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Fabric binding for the shared {@link Platform} seam. */
public class FabricPlatform implements Platform {
    @Override
    public String loaderName() {
        return "fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public PlayerData playerData(Player player) {
        return new AttachmentData(player);
    }

    @Override
    public void sendToServer(Object payload) {
        if (payload instanceof CustomPacketPayload typed) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(typed);
        }
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object payload) {
        if (payload instanceof CustomPacketPayload typed) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, typed);
        }
    }

    @Override
    public AccessorySlots accessories(Player player) {
        return TrinketsCompat.equipped(player);
    }

    private record AttachmentData(Player player) implements PlayerData {
        @Override
        public CompoundTag read(HolderLookup.Provider registries) {
            return ModAttachments.get(player).serializeNBT(registries);
        }

        @Override
        public void write(CompoundTag tag, HolderLookup.Provider registries) {
            ModAttachments.get(player).deserializeNBT(registries, tag);
        }

        @Override
        public void setDirty() {
            PlayerLoadout loadout = ModAttachments.get(player);
            if (loadout != null) {
                loadout.dirty = true;
            }
        }
    }
}
