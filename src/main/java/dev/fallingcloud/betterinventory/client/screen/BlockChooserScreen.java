package dev.fallingcloud.betterinventory.client.screen;

import dev.fallingcloud.betterinventory.inv.BetterInventorySettings;
import dev.fallingcloud.betterinventory.net.BetterInventoryPayloads;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import dev.fallingcloud.betterinventory.shim.PacketDistributor;

/**
 * Search-and-pick popup for the per-tab "use pickaxe 2 on these blocks" list.
 * A plain screen layered over the settings menu screen; the menu stays open.
 */
public class BlockChooserScreen extends Screen {
    private static final int PANEL_W = 220;
    private static final int PANEL_H = 190;
    private static final int ROW_H = 20;
    private static final int VISIBLE_ROWS = 6;

    private final SettingsScreen parent;
    private final int tab;
    private final Set<Identifier> selected = new LinkedHashSet<>();
    private final List<Block> allBlocks = new ArrayList<>();
    private List<Block> filtered = List.of();
    private EditBox search;
    private int scroll;
    private int panelLeft;
    private int panelTop;

    public BlockChooserScreen(SettingsScreen parent, int tab) {
        super(Component.translatable("screen.betterinventory.chooser.title"));
        this.parent = parent;
        this.tab = tab;
    }

    @Override
    protected void init() {
        panelLeft = (width - PANEL_W) / 2;
        panelTop = (height - PANEL_H) / 2;

        if (allBlocks.isEmpty()) {
            for (Block block : BuiltInRegistries.BLOCK) {
                if (block == Blocks.AIR || block.asItem() == net.minecraft.world.item.Items.AIR) {
                    continue;
                }
                allBlocks.add(block);
            }
            selected.addAll(BetterInventorySettings.raw(minecraft.player, tab).pickaxe2Blocks());
        }

        search = new EditBox(font, panelLeft + 10, panelTop + 22, PANEL_W - 20, 16,
                Component.translatable("screen.betterinventory.chooser.search"));
        search.setResponder(text -> {
            refilter();
            scroll = 0;
        });
        addRenderableWidget(search);
        setInitialFocus(search);

        addRenderableWidget(Button.builder(Component.translatable("screen.betterinventory.chooser.done"), b -> done())
                .bounds(panelLeft + 10, panelTop + PANEL_H - 24, 96, 18).build());
        addRenderableWidget(Button.builder(CommonComponentsCancel(), b -> back())
                .bounds(panelLeft + PANEL_W - 106, panelTop + PANEL_H - 24, 96, 18).build());

        refilter();
    }

    private static Component CommonComponentsCancel() {
        return Component.translatable("gui.cancel");
    }

    private void refilter() {
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
        List<Block> result = new ArrayList<>();
        for (Block block : allBlocks) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (query.isEmpty()
                    || block.getName().getString().toLowerCase(Locale.ROOT).contains(query)
                    || id.toString().contains(query)) {
                result.add(block);
            }
        }
        // Selected entries float to the top so the current list is easy to review.
        result.sort((a, b) -> {
            boolean sa = selected.contains(BuiltInRegistries.BLOCK.getKey(a));
            boolean sb = selected.contains(BuiltInRegistries.BLOCK.getKey(b));
            if (sa != sb) {
                return sa ? -1 : 1;
            }
            return a.getName().getString().compareToIgnoreCase(b.getName().getString());
        });
        filtered = result;
    }

    private void done() {
        BetterInventorySettings.setRaw(minecraft.player, tab,
                BetterInventorySettings.raw(minecraft.player, tab).withPickaxe2Blocks(List.copyOf(selected)));
        PacketDistributor.sendToServer(new BetterInventoryPayloads.SetPickaxeList(tab, List.copyOf(selected)));
        back();
    }

    private void back() {
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void onClose() {
        back();
    }

    private int listTop() {
        return panelTop + 44;
    }

    /**
     * Panel and rows are drawn here, not in render(). Screen#render calls
     * renderBackground FIRST, and renderBlurredBackground blurs the whole render
     * target - anything drawn before super.render() gets blurred with the world
     * (which is why only the widgets stayed sharp). Drawing after the background
     * but before the widgets keeps this content crisp and correctly layered.
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics, mouseX, mouseY);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(panelLeft - 1, panelTop - 1, panelLeft + PANEL_W + 1, panelTop + PANEL_H + 1, 0xFF000000);
        graphics.fill(panelLeft, panelTop, panelLeft + PANEL_W, panelTop + PANEL_H, 0xFFC6C6C6);
        graphics.text(font, title, panelLeft + 10, panelTop + 8, 0x404040, false);

        int top = listTop();
        int max = Math.min(filtered.size(), scroll + VISIBLE_ROWS);
        for (int i = scroll; i < max; i++) {
            Block block = filtered.get(i);
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            int rowY = top + (i - scroll) * ROW_H;
            boolean hovered = mouseX >= panelLeft + 8 && mouseX < panelLeft + PANEL_W - 8
                    && mouseY >= rowY && mouseY < rowY + ROW_H;
            boolean isSelected = selected.contains(id);
            graphics.fill(panelLeft + 8, rowY, panelLeft + PANEL_W - 8, rowY + ROW_H - 2,
                    hovered ? 0xFFA0A0A0 : 0xFF909090);
            ItemStack icon = new ItemStack(block.asItem());
            graphics.fakeItem(icon, panelLeft + 11, rowY + 1);
            graphics.text(font, block.getName(), panelLeft + 32, rowY + 5,
                    isSelected ? 0x226622 : 0x303030, false);
            // checkbox
            int boxX = panelLeft + PANEL_W - 24;
            graphics.fill(boxX, rowY + 3, boxX + 12, rowY + 15, 0xFF555555);
            graphics.fill(boxX + 1, rowY + 4, boxX + 11, rowY + 14, isSelected ? 0xFF3D8E3D : 0xFFB0B0B0);
        }
        if (filtered.isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.betterinventory.chooser.none"),
                    panelLeft + PANEL_W / 2, top + 30, 0xFFFFFF);
        }
        int count = selected.size();
        graphics.text(font, Component.translatable("screen.betterinventory.chooser.count", count),
                panelLeft + 10, panelTop + PANEL_H - 38, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        return onMouseClick(event, doubleClick);
    }

    /** Body of the old mouseClicked, now driven by the 26.3 event object. */
    private boolean onMouseClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if (button == 1) {
            int top = listTop();
            int max = Math.min(filtered.size(), scroll + VISIBLE_ROWS);
            for (int i = scroll; i < max; i++) {
                int rowY = top + (i - scroll) * ROW_H;
                if (mouseX >= panelLeft + 8 && mouseX < panelLeft + PANEL_W - 8
                        && mouseY >= rowY && mouseY < rowY + ROW_H - 2) {
                    Identifier id = BuiltInRegistries.BLOCK.getKey(filtered.get(i));
                    if (!selected.remove(id)) {
                        selected.add(id);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        scroll = Mth.clamp(scroll - (int) Math.signum(deltaY), 0, Math.max(0, filtered.size() - VISIBLE_ROWS));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
