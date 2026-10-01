package dev.fallingcloud.betterinventory.client.screen;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.data.SettingKey;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.menu.SettingsMenu;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import dev.fallingcloud.betterinventory.shim.PacketDistributor;

public class SettingsScreen extends AbstractContainerScreen<SettingsMenu> {

    private static final Identifier TEXTURE = BetterInventory.id("textures/gui/settings.png");

    private static final int MARGIN = 10;
    private static final int ROW_H = 17;
    private static final int BTN_H = 16;
    private static final int VALUE_W = 54;
    private static final int SHARE_W = 32;
    private static final int LABEL_COLOR = 0x3F3F46;
    private static final int HEADER_COLOR = 0x22222A;
    private static final int MUTED_COLOR = 0x7A7A82;

    private record Row(SettingKey key, String labelKey, Button value, Button share) {}

    private final List<Row> rows = new ArrayList<>();
    private final List<Button> upgradeButtons = new ArrayList<>();
    private final List<UpgradePaint> upgradePaints = new ArrayList<>();

    /** Icon + upgrade kind for a toggle button, painted by the screen. */
    private record UpgradePaint(Button button, ItemStack icon, UpgradeItem.Kind kind) {}
    private List<UpgradeItem.Kind> upgradeKinds = List.of();
    private Button blocksButton;

    public SettingsScreen(SettingsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SettingsMenu.IMAGE_W, SettingsMenu.IMAGE_H);
                
        // Section headers are drawn manually; hide the default labels.
        this.titleLabelX = -10000;
        this.titleLabelY = -10000;
        this.inventoryLabelX = -10000;
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        int x = leftPos + MARGIN;
        int valueX = leftPos + imageWidth - MARGIN - SHARE_W - 2 - VALUE_W;
        int shareX = leftPos + imageWidth - MARGIN - SHARE_W;
        int y = topPos + SettingsMenu.DIV_TOP + 5;

        addRow(SettingKey.GATHER_HUB, "screen.betterinventory.settings.gather", valueX, shareX, y);
        addRow(SettingKey.AUTO_REFILL, "screen.betterinventory.settings.refill", valueX, shareX, y + ROW_H);
        addRow(SettingKey.COMBAT_WEAPON, "screen.betterinventory.settings.combat", valueX, shareX, y + ROW_H * 2);

        blocksButton = addRenderableWidget(Button.builder(CommonComponents.EMPTY,
                b -> minecraft.setScreenAndShow(new BlockChooserScreen(this, menu.tab)))
                .bounds(x, y + ROW_H * 3, imageWidth - MARGIN * 2, BTN_H).build());
        blocksButton.setTooltip(Tooltip.create(Component.translatable("screen.betterinventory.settings.pickaxe2.tip")));

        rebuildUpgradeButtons();
        updateLabels();
    }

    private void addRow(SettingKey key, String labelKey, int valueX, int shareX, int y) {
        Button value = addRenderableWidget(Button.builder(CommonComponents.EMPTY, b -> toggle(key))
                .bounds(valueX, y, VALUE_W, BTN_H).build());
        Button share = addRenderableWidget(Button.builder(CommonComponents.EMPTY, b -> share(key))
                .bounds(shareX, y, SHARE_W, BTN_H).build());
        share.setTooltip(Tooltip.create(Component.translatable("screen.betterinventory.settings.share.tip")));
        rows.add(new Row(key, labelKey, value, share));
    }

    /** One compact icon button per distinct installed upgrade. */
    private void rebuildUpgradeButtons() {
        upgradeButtons.forEach(this::removeWidget);
        upgradeButtons.clear();
        upgradePaints.clear();
        Map<UpgradeItem.Kind, ItemStack> kinds = new LinkedHashMap<>();
        for (int i = 0; i < SettingsMenu.UPGRADE_SLOTS; i++) {
            ItemStack stack = menu.slots.get(SettingsMenu.SLOT_UPGRADES + i).getItem();
            if (stack.getItem() instanceof UpgradeItem upgrade) {
                kinds.putIfAbsent(upgrade.kind(), stack.copy());
            }
        }
        upgradeKinds = List.copyOf(kinds.keySet());

        int count = upgradeKinds.size();
        if (count == 0) {
            return;
        }
        int gap = 2;
        int btnW = Math.min(34, (imageWidth - MARGIN * 2 - gap * (count - 1)) / count);
        int totalW = btnW * count + gap * (count - 1);
        int x = leftPos + (imageWidth - totalW) / 2;
        int y = topPos + SettingsMenu.TOGGLES_Y;
        int i = 0;
        for (UpgradeItem.Kind kind : upgradeKinds) {
            final UpgradeItem.Kind k = kind;
            Button button = Button.builder(CommonComponents.EMPTY, b -> toggleUpgrade(k))
                    .bounds(x + i * (btnW + gap), y, btnW, 20)
                    .tooltip(Tooltip.create(Component.translatable("item.betterinventory.upgrade_" + k.id())
                            .append(" - ")
                            .append(Component.translatable("screen.betterinventory.settings.upgrade.tip"))))
                    .build();
            upgradeButtons.add(addRenderableWidget(button));
            upgradePaints.add(new UpgradePaint(button, kinds.get(kind), k));
            i++;
        }
    }

    private void toggle(SettingKey key) {
        int value = switch (key) {
            case GATHER_HUB -> BetterInventorySettings.gatherHub(minecraft.player, menu.tab) ? 0 : 1;
            case AUTO_REFILL -> BetterInventorySettings.autoRefill(minecraft.player, menu.tab) ? 0 : 1;
            case COMBAT_WEAPON -> BetterInventorySettings.combat(minecraft.player, menu.tab) == TabSettings.CombatPref.SWORD
                    ? 1 : 0;
        };
        BetterInventorySettings.applySetting(minecraft.player, menu.tab, key, value);
        PacketDistributor.sendToServer(new BetterInventoryPayloads.ToggleSetting(menu.tab, key.ordinal(), value));
        updateLabels();
    }

    private void share(SettingKey key) {
        boolean shared = !BetterInventorySettings.isShared(minecraft.player, key);
        BetterInventorySettings.setShared(minecraft.player, key, shared, menu.tab);
        PacketDistributor.sendToServer(new BetterInventoryPayloads.SetShare(key.ordinal(), shared, menu.tab));
        updateLabels();
    }

    private void toggleUpgrade(UpgradeItem.Kind kind) {
        boolean enabled = !BetterInventorySettings.raw(minecraft.player, menu.tab).upgradeEnabled(kind.id());
        BetterInventorySettings.setRaw(minecraft.player, menu.tab,
                BetterInventorySettings.raw(minecraft.player, menu.tab).withUpgradeToggle(kind.id(), enabled));
        PacketDistributor.sendToServer(new BetterInventoryPayloads.ToggleUpgrade(menu.tab, kind.id(), enabled));
    }

    private void updateLabels() {
        var player = minecraft.player;
        for (Row row : rows) {
            Component value = switch (row.key()) {
                case GATHER_HUB -> onOff(BetterInventorySettings.gatherHub(player, menu.tab));
                case AUTO_REFILL -> onOff(BetterInventorySettings.autoRefill(player, menu.tab));
                case COMBAT_WEAPON -> Component.translatable(
                        BetterInventorySettings.combat(player, menu.tab) == TabSettings.CombatPref.SWORD
                                ? "screen.betterinventory.sword" : "screen.betterinventory.axe");
            };
            row.value().setMessage(value);
            row.share().setMessage(Component.translatable(
                    BetterInventorySettings.isShared(player, row.key()) ? "screen.betterinventory.shared" : "screen.betterinventory.per_tab"));
        }
        int blocks = BetterInventorySettings.raw(player, menu.tab).pickaxe2Blocks().size();
        blocksButton.setMessage(Component.translatable("screen.betterinventory.settings.pickaxe2", blocks));
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "screen.betterinventory.on" : "screen.betterinventory.off");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Upgrades can be inserted or pulled while the screen is open.
        List<UpgradeItem.Kind> current = new ArrayList<>();
        for (int i = 0; i < SettingsMenu.UPGRADE_SLOTS; i++) {
            ItemStack stack = menu.slots.get(SettingsMenu.SLOT_UPGRADES + i).getItem();
            if (stack.getItem() instanceof UpgradeItem upgrade && !current.contains(upgrade.kind())) {
                current.add(upgrade.kind());
            }
        }
        if (!current.equals(upgradeKinds)) {
            rebuildUpgradeButtons();
        }
        updateLabels();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        for (UpgradePaint paint : upgradePaints) {
            Button widget = paint.button();
            boolean on = BetterInventorySettings.raw(minecraft.player, menu.tab).upgradeEnabled(paint.kind().id());
            graphics.item(paint.icon(), widget.getX() + 3, widget.getY() + 2);
            if (!on) {
                graphics.fill(widget.getX() + 3, widget.getY() + 2, widget.getX() + 19, widget.getY() + 18, 0x99101014);
            }
            int pip = on ? 0xFF56C25A : 0xFFB4443F;
            int px = widget.getX() + widget.getWidth() - 8;
            graphics.fill(px, widget.getY() + 7, px + 5, widget.getY() + 12, 0xFF1A1A1E);
            graphics.fill(px + 1, widget.getY() + 8, px + 4, widget.getY() + 11, pip);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 512);
        int capacity = menu.upgradeCapacity();
        for (int i = capacity; i < SettingsMenu.UPGRADE_SLOTS; i++) {
            int cx = leftPos + SettingsMenu.UPGRADES_X - 1 + (i % 5) * 18;
            int cy = topPos + SettingsMenu.UPGRADES_Y - 1 + (i / 5) * 18;
            graphics.fill(cx, cy, cx + 18, cy + 18, 0x73101014);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int x = leftPos + MARGIN;
        graphics.text(font, Component.translatable("container.betterinventory.settings"),
                x, topPos + 8, HEADER_COLOR, false);
        Component subtitle = menu.tab == 0
                ? Component.translatable("screen.betterinventory.settings.default_tab")
                : Component.translatable("screen.betterinventory.settings.tab", menu.tab);
        graphics.text(font, subtitle, x, topPos + 18, MUTED_COLOR, false);

        // Row labels, vertically centred against their buttons.
        for (Row row : rows) {
            graphics.text(font, Component.translatable(row.labelKey()),
                    x, row.value().getY() + (BTN_H - 8) / 2, LABEL_COLOR, false);
        }

        // Hint sits on the toggle row, which is empty in exactly these two cases -
        // no header line above the bay, which would collide with its slots.
        Component hint = menu.upgradeCapacity() == 0
                ? Component.translatable("screen.betterinventory.settings.no_backpack")
                : upgradeKinds.isEmpty() ? Component.translatable("screen.betterinventory.settings.no_upgrades") : null;
        if (hint != null) {
            graphics.centeredText(font, hint, leftPos + imageWidth / 2,
                    topPos + SettingsMenu.TOGGLES_Y + 5, MUTED_COLOR);
        }
        graphics.text(font, playerInventoryTitle,
                leftPos + SettingsMenu.INV_X, topPos + SettingsMenu.DIV_BOTTOM + 4, LABEL_COLOR, false);

        extractTooltip(graphics, mouseX, mouseY);
    }
}
