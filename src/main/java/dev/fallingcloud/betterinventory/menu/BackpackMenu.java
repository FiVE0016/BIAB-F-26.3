package dev.fallingcloud.betterinventory.menu;

import dev.fallingcloud.betterinventory.block.BackpackBlockEntity;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.ComponentBackedHandler;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.item.BackpackItem;
import dev.fallingcloud.betterinventory.menu.slots.LockedHandlerSlot;
import dev.fallingcloud.betterinventory.registry.ModAttachments;
import dev.fallingcloud.betterinventory.registry.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** The standalone backpack GUI (right click in hand or on a placed backpack). */
public class BackpackMenu extends AbstractContainerMenu {
    public static final int STORAGE_SIZE = TabStorage.SIZE;
    public static final int SLOT_STORAGE = 0;   // 45: 0-26 main, 27-44 gather
    public static final int SLOT_PLAYER = 45;   // 27 player inv
    public static final int SLOT_HOTBAR = 72;   // 9 hotbar
    public static final int SLOT_END = 81;

    public static final int IMAGE_W = 176;
    public static final int IMAGE_H = 212;
    public static final int GATHER_Y = 18;
    public static final int MAIN_Y = 58;
    public static final int PLAYER_Y = 126;
    public static final int HOTBAR_Y = 188;

    public interface Access {
        ItemStack stack();

        boolean stillValid(Player player);

        default void markChanged() {}
    }

    public record HandAccess(Player player, InteractionHand hand) implements Access {
        @Override
        public ItemStack stack() {
            return player.getItemInHand(hand);
        }

        @Override
        public boolean stillValid(Player player) {
            return stack().getItem() instanceof BackpackItem;
        }
    }

    public record BlockAccess(BackpackBlockEntity be) implements Access {
        @Override
        public ItemStack stack() {
            return be.stored();
        }

        @Override
        public boolean stillValid(Player player) {
            if (be.isRemoved() || !(be.stored().getItem() instanceof BackpackItem)) {
                return false;
            }
            Vec3 center = Vec3.atCenterOf(be.getBlockPos());
            return player.position().distanceToSqr(center) <= 64.0;
        }

        @Override
        public void markChanged() {
            be.setChanged();
        }
    }

    public final Player player;
    public final Access access;
    public final ComponentBackedHandler storageHandler;
    private final int guardedHotbarSlot;

    public BackpackMenu(int id, Inventory inventory, Access access) {
        super(dev.fallingcloud.betterinventory.registry.ModMenus.BACKPACK, id);
        this.player = inventory.player;
        this.access = access;
        PlayerLoadout loadout = ModAttachments.get(player);
        ItemStack host = access.stack().getItem() instanceof BackpackItem
                ? access.stack()
                : new ItemStack(dev.fallingcloud.betterinventory.registry.ModItems.BACKPACKS[0].get());
        this.storageHandler = new ComponentBackedHandler(host, ModComponents.BACKPACK_CONTENTS.get(), STORAGE_SIZE, loadout::storageCap);
        this.storageHandler.setChangeListener(access::markChanged);
        this.guardedHotbarSlot = access instanceof HandAccess handAccess && handAccess.hand() == InteractionHand.MAIN_HAND
                ? inventory.getSelectedSlot()
                : -1;

        // 0-44 storage: gather hub rows on top, main grid below.
        for (int i = 0; i < STORAGE_SIZE; i++) {
            int x;
            int y;
            if (i < TabStorage.GATHER_START) {
                x = 8 + (i % 9) * 18;
                y = MAIN_Y + (i / 9) * 18;
            } else {
                int j = i - TabStorage.GATHER_START;
                x = 8 + (j % 9) * 18;
                y = GATHER_Y + (j / 9) * 18;
            }
            addSlot(new LockedHandlerSlot(storageHandler, i, x, y,
                    () -> BackpackItem.settings(storageHandler.host()),
                    loadout::storageCap,
                    storageHandler::host));
        }
        // 45-71 player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, PLAYER_Y + row * 18));
            }
        }
        // 72-80 hotbar
        for (int i = 0; i < 9; i++) {
            final int hotbarIndex = i;
            addSlot(new Slot(inventory, i, 8 + i * 18, HOTBAR_Y) {
                @Override
                public boolean mayPickup(Player p) {
                    return hotbarIndex != guardedHotbarSlot && super.mayPickup(p);
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return hotbarIndex != guardedHotbarSlot;
                }
            });
        }
    }

    /** Screen-opening data for the backpack menu, sent through the menu type's codec. */
    public record OpenData(int kind, BlockPos pos) {
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenData> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, OpenData::kind,
                BlockPos.STREAM_CODEC, OpenData::pos,
                OpenData::new);

        public static OpenData hand(InteractionHand hand) {
            return new OpenData(hand == InteractionHand.OFF_HAND ? 1 : 0, BlockPos.ZERO);
        }

        public static OpenData block(BlockPos pos) {
            return new OpenData(2, pos);
        }
    }

    /** Client-side factory used by {@link net.fabricmc.fabric.api.menu.v1.ExtendedMenuType}. */
    public static BackpackMenu create(int id, Inventory inventory, OpenData data) {
        if (data.kind() == 2) {
            if (inventory.player.level().getBlockEntity(data.pos()) instanceof BackpackBlockEntity be) {
                return new BackpackMenu(id, inventory, new BlockAccess(be));
            }
            return new BackpackMenu(id, inventory, new HandAccess(inventory.player, InteractionHand.MAIN_HAND));
        }
        InteractionHand hand = data.kind() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return new BackpackMenu(id, inventory, new HandAccess(inventory.player, hand));
    }

    /** Legacy reader kept for reference; unused now that data travels as {@link OpenData}. */
    @SuppressWarnings("unused")
    public static BackpackMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        byte kind = buf.readByte();
        if (kind == 2) {
            BlockPos pos = buf.readBlockPos();
            if (inventory.player.level().getBlockEntity(pos) instanceof BackpackBlockEntity be) {
                return new BackpackMenu(id, inventory, new BlockAccess(be));
            }
            return new BackpackMenu(id, inventory, new HandAccess(inventory.player, InteractionHand.MAIN_HAND));
        }
        InteractionHand hand = kind == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return new BackpackMenu(id, inventory, new HandAccess(inventory.player, hand));
    }

    public static void openHand(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BackpackItem)) {
            return;
        }
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return stack.getHoverName();
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new BackpackMenu(id, inv, new HandAccess(p, hand));
            }
        };
        player.openMenu(MenuData.wrap(provider, () -> OpenData.hand(hand)));
    }

    public static void openBlock(ServerPlayer player, BackpackBlockEntity be) {
        if (!(be.stored().getItem() instanceof BackpackItem)) {
            return;
        }
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return be.stored().getHoverName();
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new BackpackMenu(id, inv, new BlockAccess(be));
            }
        };
        player.openMenu(MenuData.wrap(provider, () -> OpenData.block(be.getBlockPos())));
    }

    @Override
    public boolean stillValid(Player player) {
        return access.stillValid(player);
    }

    /** Feeds the stack into any storage slot reserved for that exact item. */
    private boolean moveToLockedSlots(ItemStack stack, int sourceIndex) {
        TabSettings settings = BackpackItem.settings(storageHandler.host());
        if (stack.isEmpty() || settings.locks().isEmpty()) {
            return false;
        }
        boolean moved = false;
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = SLOT_STORAGE; i < SLOT_PLAYER && !stack.isEmpty(); i++) {
                if (i == sourceIndex) {
                    continue;
                }
                Slot slot = slots.get(i);
                if (settings.lockFor(slot.index).filter(stack::is).isEmpty()) {
                    continue;
                }
                if (slot.hasItem() == (pass == 1)) {
                    continue;
                }
                int before = stack.getCount();
                slot.safeInsert(stack);
                if (stack.getCount() != before) {
                    moved = true;
                }
            }
        }
        return moved;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        // A slot locked to this item wins over the normal routing, from anywhere.
        boolean moved = moveToLockedSlots(stack, index);
        if (!moved && !stack.isEmpty()) {
            if (index < SLOT_PLAYER) { // storage -> player
                moved = moveItemStackTo(stack, SLOT_PLAYER, SLOT_END, true);
            } else if (index < SLOT_HOTBAR) { // player inv -> main grid, then gather
                moved = moveItemStackTo(stack, SLOT_STORAGE, TabStorage.GATHER_START, false)
                        || moveItemStackTo(stack, TabStorage.GATHER_START, STORAGE_SIZE, false);
            } else { // hotbar -> main grid only (never the gather hub)
                moved = moveItemStackTo(stack, SLOT_STORAGE, TabStorage.GATHER_START, false);
            }
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }
}
