package dev.fallingcloud.betterinventory.client.screen;

import dev.fallingcloud.betterinventory.BetterInventory;
import dev.fallingcloud.betterinventory.data.SettingKey;
import dev.fallingcloud.betterinventory.data.TabSettings;
import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.item.UpgradeItem;
import dev.fallingcloud.betterinventory.menu.SettingsMenu;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import dev.fallingcloud.betterinventory.net.Net;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.4.1 settings screen: three on/off toggle rows, the pickaxe-2 block
 * chooser, the pack ability-upgrade buttons and the back arrow.
 *
 * <p>26.3 adaptations: Button is builder/subclass based (its constructor is
 * protected), so the custom upgrade button overrides
 * {@code extractWidgetRenderState} instead of renderWidget, tooltips are
 * {@code Tooltip.create}, and drawText/centeredText/blit replace the 1.21.1
 * draw calls.
 */
public class SettingsScreen extends AbstractContainerScreen<SettingsMenu> {
    private static final Identifier TEXTURE = BetterInventory.id("textures/gui/settings.png");
    private static final int MARGIN = 10;
    private static final int ROW_H = 17;
    private static final int BTN_H = 16;
    // 26.3: GuiGraphicsExtractor.text 在 ARGB.alpha(color)==0 时直接 return（不画），
    // 必须带上不透明 alpha；1.21.1 会自动补，所以 1.4.1 的无 alpha 常量到这边会"消失"。
    private static final int LABEL_COLOR = 0xFF3F3F46;
    private static final int HEADER_COLOR = 0xFF222AAA;
    private static final int MUTED_COLOR = 0xFF7A7A82;
    private final List<Row> rows = new ArrayList<>();
    private final List<Button> upgradeButtons = new ArrayList<>();
    private final List<UpgradePaint> upgradePaints = new ArrayList<>();

    /** Icon + upgrade kind for a toggle button, painted by the screen. */
    private record UpgradePaint(Button button, ItemStack icon, UpgradeItem.Kind kind) {}
    private List<UpgradeItem.Kind> upgradeKinds = List.of();
    private Button blocksButton;

    public SettingsScreen(SettingsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SettingsMenu.IMAGE_W, SettingsMenu.IMAGE_H);
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
        int valueW = 88;
        int valueX = leftPos + imageWidth - MARGIN - valueW;
        int y = topPos + SettingsMenu.DIV_TOP + 5;
        addRow(SettingKey.GATHER_HUB, "screen.betterinventory.settings.gather", valueX, valueW, y);
        addRow(SettingKey.AUTO_REFILL, "screen.betterinventory.settings.refill", valueX, valueW, y + ROW_H);
        addRow(SettingKey.COMBAT_WEAPON, "screen.betterinventory.settings.combat", valueX, valueW, y + 2 * ROW_H);
        blocksButton = addRenderableWidget(Button.builder(Component.empty(),
                        b -> minecraft.gui.setScreen(new BlockChooserScreen(this, menu.tab)))
                .bounds(x, y + 3 * ROW_H, imageWidth - 2 * MARGIN, BTN_H).build());
        blocksButton.setTooltip(Tooltip.create(Component.translatable("screen.betterinventory.settings.pickaxe2.tip")));
        rebuildUpgradeButtons();
        updateLabels();
        addRenderableWidget(Button.builder(Component.literal("←"),
                        b -> Net.sendToServer(new BetterInventoryPayloads.OpenLoadout()))
                .bounds(leftPos + imageWidth - MARGIN - 16, topPos + 4, 16, 14).build());
    }

    private void addRow(SettingKey key, String labelKey, int valueX, int valueW, int y) {
        Button value = addRenderableWidget(Button.builder(Component.empty(), b -> toggle(key))
                .bounds(valueX, y, valueW, BTN_H).build());
        rows.add(new Row(key, labelKey, value));
    }

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
        if (count != 0) {
            int gap = 2;
            int btnW = Math.min(34, (imageWidth - 2 * MARGIN - gap * (count - 1)) / count);
            int totalW = btnW * count + gap * (count - 1);
            int x = leftPos + (imageWidth - totalW) / 2;
            int y = topPos + SettingsMenu.TOGGLES_Y;
            int i = 0;
            for (UpgradeItem.Kind kind : upgradeKinds) {
                UpgradeItem.Kind k = kind;
                Button button = Button.builder(Component.empty(), b -> toggleUpgrade(k))
                        .bounds(x + i * (btnW + gap), y, btnW, 20)
                        .tooltip(Tooltip.create(
                                Component.translatable("item.betterinventory.upgrade_" + k.id())
                                        .append(" - ")
                                        .append(Component.translatable("screen.betterinventory.settings.upgrade.tip"))))
                        .build();
                upgradeButtons.add(addRenderableWidget(button));
                upgradePaints.add(new UpgradePaint(button, kinds.get(kind), k));
                i++;
            }
        }
    }

    private void toggle(SettingKey key) {
        int value = switch (key) {
            case GATHER_HUB -> BetterInventorySettings.gatherHub(minecraft.player, menu.tab) ? 0 : 1;
            case AUTO_REFILL -> BetterInventorySettings.autoRefill(minecraft.player, menu.tab) ? 0 : 1;
            case COMBAT_WEAPON -> BetterInventorySettings.combat(minecraft.player, menu.tab) == TabSettings.CombatPref.SWORD ? 1 : 0;
        };
        BetterInventorySettings.applySetting(minecraft.player, menu.tab, key, value);
        Net.sendToServer(new BetterInventoryPayloads.ToggleSetting(menu.tab, key.ordinal(), value));
        updateLabels();
    }

    private void toggleUpgrade(UpgradeItem.Kind kind) {
        boolean enabled = !BetterInventorySettings.upgradeEnabled(minecraft.player, kind.id());
        BetterInventorySettings.setUpgradeEnabled(minecraft.player, kind.id(), enabled);
        Net.sendToServer(new BetterInventoryPayloads.ToggleUpgrade(0, kind.id(), enabled));
    }

    private void updateLabels() {
        LocalPlayer player = minecraft.player;
        for (Row row : rows) {
            Component value = switch (row.key()) {
                case GATHER_HUB -> onOff(BetterInventorySettings.gatherHub(player, menu.tab));
                case AUTO_REFILL -> onOff(BetterInventorySettings.autoRefill(player, menu.tab));
                case COMBAT_WEAPON -> Component.translatable(
                        BetterInventorySettings.combat(player, menu.tab) == TabSettings.CombatPref.SWORD
                                ? "screen.betterinventory.sword"
                                : "screen.betterinventory.axe");
            };
            row.value().setMessage(value);
        }
        int blocks = BetterInventorySettings.raw(player, menu.tab).pickaxe2Blocks().size();
        blocksButton.setMessage(Component.translatable("screen.betterinventory.settings.pickaxe2", blocks));
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "screen.betterinventory.on" : "screen.betterinventory.off");
    }

    @Override
    public void containerTick() {
        super.containerTick();
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
        // Upgrade toggle icons are painted here: 26.3's AbstractButton finalizes
        // its widget render state, so a screen-side paint replaces the 1.4.1
        // Button subclass' renderWidget override.
        for (UpgradePaint paint : upgradePaints) {
            Button widget = paint.button();
            boolean on = BetterInventorySettings.upgradeEnabled(minecraft.player, paint.kind().id());
            if (on) {
                graphics.item(paint.icon(), widget.getX() + 3, widget.getY() + 2);
            } else {
                graphics.blit(RenderPipelines.GUI_TEXTURED,
                        BetterInventory.id("textures/gui/upgrade_off/" + paint.kind().id() + ".png"),
                        widget.getX() + 3, widget.getY() + 2, 0, 0, 16, 16, 16, 16);
            }
            int pip = on ? -11091366 : -4963265;
            int px = widget.getX() + widget.getWidth() - 8;
            graphics.fill(px, widget.getY() + 7, px + 5, widget.getY() + 12, -15066594);
            graphics.fill(px + 1, widget.getY() + 8, px + 4, widget.getY() + 11, pip);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 512);
        if (!menu.loadout.upgradesUnlocked()) {
            int cx = leftPos + SettingsMenu.UPGRADES_X - 1;
            int cy = topPos + SettingsMenu.UPGRADES_Y - 1;
            graphics.fill(cx, cy, cx + 54, cy + 18, -15263973);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int x = leftPos + MARGIN;
        graphics.text(font, Component.translatable("container.betterinventory.settings"), x, topPos + 8, HEADER_COLOR, false);
        Component subtitle = menu.tab == 0
                ? Component.translatable("screen.betterinventory.settings.default_tab")
                : Component.translatable("screen.betterinventory.settings.tab", menu.tab);
        graphics.text(font, subtitle, x, topPos + 18, MUTED_COLOR, false);
        for (Row row : rows) {
            graphics.text(font, Component.translatable(row.labelKey()), x, row.value().getY() + 4, LABEL_COLOR, false);
        }
        Component hint = !menu.loadout.upgradesUnlocked()
                ? Component.translatable("screen.betterinventory.settings.no_backpack")
                : (upgradeKinds.isEmpty() ? Component.translatable("screen.betterinventory.settings.no_upgrades") : null);
        if (hint != null) {
            graphics.centeredText(font, hint, leftPos + imageWidth / 2, topPos + SettingsMenu.TOGGLES_Y + 5, MUTED_COLOR);
        }
        graphics.text(font, playerInventoryTitle, leftPos + SettingsMenu.INV_X, topPos + SettingsMenu.DIV_BOTTOM + 4, LABEL_COLOR, false);
        extractTooltip(graphics, mouseX, mouseY);
    }

    private record Row(SettingKey key, String labelKey, Button value) {
    }
}
