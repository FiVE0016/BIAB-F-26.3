package dev.fallingcloud.betterinventory;

/**
 * Pixel layout for the overhauled inventory screen. Mirrored by tools/generate.py —
 * keep both in sync. All "big" gaps (side clusters to center, main grid to hotbar)
 * share one size, all "small" gaps (top row to bottom row, right column pair) share
 * another, per the design sketch.
 *
 * <p>The background is drawn as individual panel sprites rather than one image, so
 * the curio column can be omitted entirely when Curios is not installed.
 */
public final class BetterInventoryLayout {
    private BetterInventoryLayout() {}

    public static final int SLOT = 18;
    public static final int GAP_BIG = 8;
    public static final int GAP_SMALL = 4;
    public static final int PAD = 3;

    public static final int LEFT_X = 0;
    public static final int CENTER_X = 42 + GAP_BIG;            // 50
    public static final int RIGHT_A_X = CENTER_X + 168 + GAP_BIG; // 226
    public static final int RIGHT_B_X = RIGHT_A_X + 42 + GAP_SMALL; // 272

    public static final int TABS_H = 20;
    public static final int TOP_Y = TABS_H;                     // 20
    public static final int MAIN_Y = TOP_Y + 42 + GAP_SMALL;    // 66
    public static final int HOTBAR_Y = MAIN_Y + 60 + GAP_BIG;   // 134

    /** Width with and without the curio column. */
    public static final int IMAGE_W_CURIOS = RIGHT_B_X + 42;    // 314
    public static final int IMAGE_W_PLAIN = RIGHT_A_X + 42;     // 268
    public static final int IMAGE_H = HOTBAR_Y + 24;            // 158

    /** Offset from a panel origin to the first slot's item position. */
    public static final int SLOT_OFF = PAD + 1;                 // 4

    // Tabs row: four backpack tabs + settings button.
    public static final int TAB_W = 24;
    public static final int TAB_H = 20;
    public static final int TAB_X0 = CENTER_X + 2;
    /** Flush spacing, so the five inventory tabs plus the crafting tab clear the gear. */
    public static final int TAB_SPACING = 24;
    public static final int SETTINGS_SIZE = 20;
    public static final int SETTINGS_X = CENTER_X + 168 - SETTINGS_SIZE;
    public static final int SETTINGS_Y = 0;
    /** Crafting tab: sits after the inventory tabs; toggles the right-hand panel. */
    public static final int CRAFT_TAB_X = TAB_X0 + 5 * TAB_SPACING;   // 172

    // Floating side panels (drawn outside the main image bounds).
    public static final int ZOOM_SIZE = 100;
    public static final int ZOOM_GAP = 16;
    public static final int ZOOM_X = -(ZOOM_SIZE + ZOOM_GAP);   // relative to leftPos
    public static final int ZOOM_Y = (IMAGE_H - ZOOM_SIZE) / 2; // vertically centered
    public static final int CHAR_W = 78;
    public static final int CHAR_H = 130;
    public static final int CHAR_GAP = 16;
    public static final int CHAR_Y = (IMAGE_H - CHAR_H) / 2;    // vertically centered

    // Vertical crafting panel, drawn in place of the character view and sharing its
    // 78x130 footprint. Offsets are relative to the panel origin (charX, CHAR_Y).
    /**
     * The grid is always laid out at its full 3x3 extent; how much of it is usable
     * depends on the installed crafting upgrade (2x2 / 2x3 / 3x3). Slot positions are
     * final once built, so locked cells are dimmed in place rather than re-centred -
     * which also shows what the next upgrade unlocks.
     */
    public static final int CRAFT_COLS = 3;
    public static final int CRAFT_ROWS = 3;
    public static final int CRAFT_GRID_X = (CHAR_W - CRAFT_COLS * SLOT) / 2;  // 21
    public static final int CRAFT_GRID_Y = 12;
    public static final int CRAFT_ARROW_Y = 68;
    public static final int CRAFT_RESULT_X = (CHAR_W - SLOT) / 2;             // 30
    public static final int CRAFT_RESULT_Y = 86;

    public static int charX(boolean curios) {
        return (curios ? IMAGE_W_CURIOS : IMAGE_W_PLAIN) + CHAR_GAP;
    }

    public static int imageWidth(boolean curios) {
        return curios ? IMAGE_W_CURIOS : IMAGE_W_PLAIN;
    }

    // ---------------------------------------------------------------- texture atlas
    public static final int TEX_W = 512;
    public static final int TEX_H = 256;

    /** Panel sprites: {u, v, w, h}. A panel is cols*18 + PAD*2 by rows*18 + PAD*2. */
    public static final int[] PANEL_9X3 = {0, 0, 168, 60};
    public static final int[] PANEL_9X2 = {0, 68, 168, 42};
    public static final int[] PANEL_9X1 = {0, 118, 168, 24};
    public static final int[] PANEL_2X2 = {176, 0, 42, 42};
    public static final int[] PANEL_2X3 = {176, 50, 42, 60};

    /** Tab states, stacked (pitch TAB_H+1): unselected / selected / disabled. */
    public static final int SPR_TAB_X = 220;
    public static final int SPR_TAB_Y = 0;
    /** Settings button: normal, hover below (pitch SETTINGS_SIZE+1). */
    public static final int SPR_SETTINGS_X = 220;
    public static final int SPR_SETTINGS_Y = 66;
    /** 18x18 translucent dim overlay for unavailable slots. */
    public static final int SPR_LOCK_X = 220;
    public static final int SPR_LOCK_Y = 112;
    /** 16x16 grid glyph marking the default (player inventory) tab. */
    public static final int SPR_TAB_ICON_X = 220;
    public static final int SPR_TAB_ICON_Y = 132;
    /** 16x16 glyph marking the crafting tab. */
    public static final int SPR_CRAFT_ICON_X = 220;
    public static final int SPR_CRAFT_ICON_Y = 150;
    /** 78x130 vertical crafting panel, same footprint as the character panel. */
    public static final int SPR_CRAFT_X = 432;
    public static final int SPR_CRAFT_Y = 0;
    public static final int SPR_ZOOM_X = 250;
    public static final int SPR_ZOOM_Y = 0;
    public static final int SPR_CHAR_X = 352;
    public static final int SPR_CHAR_Y = 0;
}
