package dev.fallingcloud.betterinventory;

/**
 * 1.4.1 pixel layout for the overhauled inventory screen.
 */
public final class BetterInventoryLayout {
    public static final int SLOT = 18;
    public static final int GAP_BIG = 8;
    public static final int GAP_SMALL = 4;
    public static final int PAD = 3;
    public static final int CHAR_W = 78;
    public static final int CHAR_H = 130;
    public static final int CHAR_X = 0;
    public static final int LEFT_X = 82;
    public static final int CENTER_X = 132;
    public static final int RIGHT_A_X = 308;
    public static final int STRIP_CENTER_X = 216;
    public static final int TABS_H = 20;
    public static final int TOP_Y = 20;
    public static final int GATHER_ROWS = 5;
    public static final int STRIP_MAIN_DY = 105;
    public static final int STRIP_HOTBAR_DY = 172;
    public static final int STRIP_H = 196;
    public static final int MAIN_Y = 125;
    public static final int HOTBAR_Y = 192;
    public static final int SIDE_BOTTOM_Y = 185;
    public static final int SIDE_TOP_Y = 74;
    public static final int ARMOR_STORE_Y = 20;
    public static final int ARMOR_SWAP_Y = 63;
    public static final int ARMOR_SWAP_H = 10;
    public static final int COMPAT_Y = 20;
    public static final int CHAR_Y = 37;
    public static final int IMAGE_W = 350;
    public static final int IMAGE_H = 216;
    public static final int TRASH_W = 24;
    public static final int TRASH_X = 317;
    public static final int TRASH_Y = 192;
    public static final int TOOLBOX_X = 91;
    public static final int TOOLBOX_Y = 192;
    public static final int SWAP_SIZE = 18;
    public static final int SWAP_X = 30;
    public static final int SWAP_Y = 195;
    public static final int EFFECT_SIZE = 24;
    public static final int EFFECT_PITCH = 27;
    public static final int EFFECT_COLS = 3;
    public static final int EFFECT_X = 0;
    public static final int EFFECT_Y = 9;
    public static final int ZOOM_SIZE = 100;
    public static final int ZOOM_GAP = 16;
    public static final int ZOOM_X = 366;
    public static final int ZOOM_Y = 58;
    public static final int IMAGE_W_ZOOM = 466;
    public static final int VIEWER_MIN_W = 100;
    public static final int SLOT_OFF = 4;
    public static final int TAB_W = 24;
    public static final int TAB_H = 20;
    public static final int TAB_X0 = 134;
    public static final int TAB_SPACING = 24;
    public static final int SETTINGS_SIZE = 20;
    public static final int SETTINGS_X = 280;
    public static final int SETTINGS_Y = 0;
    public static final int CRAFT_TAB_X = 254;
    public static final int CREATIVE_TAB_X = 91;
    public static final int CRAFT_COLS = 3;
    public static final int CRAFT_ROWS = 3;
    public static final int CRAFT_GRID_X = 12;
    public static final int CRAFT_GRID_Y = 12;
    public static final int CRAFT_ARROW_Y = 68;
    public static final int CRAFT_RESULT_X = 30;
    public static final int CRAFT_RESULT_Y = 86;
    public static final int FURNACE_SLOT_X = 30;
    public static final int FURNACE_INPUT_Y = 12;
    public static final int FURNACE_FLAME_X = 31;
    public static final int FURNACE_FLAME_Y = 31;
    public static final int FURNACE_FUEL_Y = 48;
    public static final int FURNACE_ARROW_X = 31;
    public static final int FURNACE_ARROW_Y = 66;
    public static final int FURNACE_ARROW_W = 16;
    public static final int FURNACE_ARROW_H = 18;
    public static final int TEX_W = 512;
    public static final int TEX_H = 256;
    public static final int[] CENTER_STRIP = new int[]{0, 0, 176, 196};
    public static final int[] PANEL_2X2 = new int[]{176, 0, 42, 42};
    public static final int[] PANEL_2X3 = new int[]{175, 50, 43, 60, -1, 0};
    public static final int[] PANEL_1X1 = new int[]{176, 112, 24, 24};
    public static final int SPR_TAB_X = 220;
    public static final int SPR_TAB_Y = 0;
    public static final int SPR_SETTINGS_X = 220;
    public static final int SPR_SETTINGS_Y = 66;
    public static final int SPR_LOCK_X = 220;
    public static final int SPR_LOCK_Y = 112;
    public static final int SPR_TAB_ICON_X = 220;
    public static final int SPR_TAB_ICON_Y = 132;
    public static final int SPR_CRAFT_ICON_X = 220;
    public static final int SPR_CRAFT_ICON_Y = 150;
    public static final int SPR_TAB_LOCK_X = 220;
    public static final int SPR_TAB_LOCK_Y = 168;
    public static final int SPR_CRAFT_X = 432;
    public static final int SPR_CRAFT_Y = 0;
    public static final int SPR_CHAR_X = 352;
    public static final int SPR_CHAR_Y = 0;
    public static final int SPR_FURNACE_X = 256;
    public static final int SPR_FURNACE_Y = 116;
    public static final int SPR_FURNACE_ARROW_X = 336;
    public static final int SPR_FURNACE_ARROW_Y = 116;
    public static final int SPR_ZOOM_X = 250;
    public static final int SPR_ZOOM_Y = 0;

    private BetterInventoryLayout() {}

    public static int leftPos(int screenWidth) {
        int centred = screenWidth / 2 - 216;
        return Math.max(0, Math.min(centred, screenWidth - 350));
    }

    public static boolean zoomFits(int screenWidth, boolean recipeViewer) {
        int right = leftPos(screenWidth) + IMAGE_W_ZOOM;
        return right + (recipeViewer ? VIEWER_MIN_W : 2) <= screenWidth;
    }
}
