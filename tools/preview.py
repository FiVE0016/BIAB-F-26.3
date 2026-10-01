#!/usr/bin/env python3
"""Renders a pixel-accurate preview of the inventory screen by blitting the
generated sprites at exactly the coordinates BetterInventoryScreen uses.

Catches layout/atlas mistakes without launching the game. Run:
    python tools/preview.py            -> tools/preview_curios.png + preview_plain.png
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "betterinventory", "textures", "gui", "inventory.png")
OUT = os.path.join(ROOT, "tools")

# --- mirror of BetterInventoryLayout.java -------------------------------------------
GAP_BIG, GAP_SMALL, PAD = 8, 4, 3
LEFT_X = 0
CENTER_X = 42 + GAP_BIG
RIGHT_A_X = CENTER_X + 168 + GAP_BIG
RIGHT_B_X = RIGHT_A_X + 42 + GAP_SMALL
TABS_H = 20
TOP_Y = TABS_H
MAIN_Y = TOP_Y + 42 + GAP_SMALL
HOTBAR_Y = MAIN_Y + 60 + GAP_BIG
IMAGE_W_CURIOS = RIGHT_B_X + 42
IMAGE_W_PLAIN = RIGHT_A_X + 42
IMAGE_H = HOTBAR_Y + 24
TAB_W, TAB_H = 24, 20
TAB_X0 = CENTER_X + 2
TAB_SPACING = 26
SETTINGS_SIZE = 20
SETTINGS_X = CENTER_X + 168 - SETTINGS_SIZE
ZOOM_SIZE, ZOOM_GAP = 100, 16
ZOOM_X = -(ZOOM_SIZE + ZOOM_GAP)
ZOOM_Y = (IMAGE_H - ZOOM_SIZE) // 2
CHAR_W, CHAR_H, CHAR_GAP = 78, 130, 16
CHAR_Y = (IMAGE_H - CHAR_H) // 2

PANEL_9X3 = (0, 0, 168, 60)
PANEL_9X2 = (0, 68, 168, 42)
PANEL_9X1 = (0, 118, 168, 24)
PANEL_2X2 = (176, 0, 42, 42)
PANEL_2X3 = (176, 50, 42, 60)
SPR_TAB = (220, 0)
SPR_SETTINGS = (220, 66)
SPR_LOCK = (220, 112)
SPR_ZOOM = (250, 0)
SPR_CHAR = (352, 0)


def render(curios):
    tex = Image.open(TEX).convert("RGBA")
    image_w = IMAGE_W_CURIOS if curios else IMAGE_W_PLAIN
    char_x = image_w + CHAR_GAP
    # canvas covers the floating zoom (left) and character panel (right)
    pad_left = ZOOM_SIZE + ZOOM_GAP + 10
    pad_right = CHAR_GAP + CHAR_W + 10
    canvas = Image.new("RGBA", (pad_left + image_w + pad_right, IMAGE_H + 20), (24, 24, 28, 255))
    ox, oy = pad_left, 10

    def blit(sprite, x, y):
        u, v, w, h = sprite
        canvas.alpha_composite(tex.crop((u, v, u + w, v + h)), (ox + x, oy + y))

    blit(PANEL_2X2, LEFT_X, TOP_Y)
    blit(PANEL_2X3, LEFT_X, MAIN_Y)
    blit(PANEL_9X2, CENTER_X, TOP_Y)
    blit(PANEL_9X3, CENTER_X, MAIN_Y)
    blit(PANEL_9X1, CENTER_X, HOTBAR_Y)
    blit(PANEL_2X2, RIGHT_A_X, TOP_Y)
    blit(PANEL_2X3, RIGHT_A_X, MAIN_Y)
    if curios:
        blit(PANEL_2X2, RIGHT_B_X, TOP_Y)
        blit(PANEL_2X3, RIGHT_B_X, MAIN_Y)

    # tabs: 0 selected, 1 unselected(has pack), 2 & 3 disabled
    states = [1, 0, 2, 2]
    for i, st in enumerate(states):
        blit((SPR_TAB[0], SPR_TAB[1] + st * (TAB_H + 1), TAB_W, TAB_H),
             TAB_X0 + i * TAB_SPACING, 0)
    blit((SPR_SETTINGS[0], SPR_SETTINGS[1], SETTINGS_SIZE, SETTINGS_SIZE), SETTINGS_X, 0)

    # dimmed gather hub (no backpack case) shown on the plain variant
    if not curios:
        for r in range(2):
            for c in range(9):
                blit((SPR_LOCK[0], SPR_LOCK[1], 18, 18),
                     CENTER_X + PAD + c * 18, TOP_Y + PAD + r * 18)

    blit((SPR_CHAR[0], SPR_CHAR[1], CHAR_W, CHAR_H), char_x, CHAR_Y)
    canvas.alpha_composite(tex.crop((SPR_ZOOM[0], SPR_ZOOM[1], SPR_ZOOM[0] + ZOOM_SIZE, SPR_ZOOM[1] + ZOOM_SIZE)),
                           (ox + ZOOM_X, oy + ZOOM_Y))

    # measure gaps and annotate
    d = ImageDraw.Draw(canvas)
    checks = []
    checks.append(("left->center", CENTER_X - (LEFT_X + 42), GAP_BIG))
    checks.append(("center->rightA", RIGHT_A_X - (CENTER_X + 168), GAP_BIG))
    checks.append(("main->hotbar", HOTBAR_Y - (MAIN_Y + 60), GAP_BIG))
    checks.append(("top->bottom", MAIN_Y - (TOP_Y + 42), GAP_SMALL))
    if curios:
        checks.append(("rightA->rightB", RIGHT_B_X - (RIGHT_A_X + 42), GAP_SMALL))
    return canvas, checks


def main():
    for curios in (True, False):
        img, checks = render(curios)
        name = "preview_curios.png" if curios else "preview_plain.png"
        img = img.resize((img.width * 2, img.height * 2), Image.NEAREST)
        img.save(os.path.join(OUT, name))
        print(name)
        for label, actual, expected in checks:
            flag = "OK " if actual == expected else "BAD"
            print("  %s %-16s %d (expected %d)" % (flag, label, actual, expected))


if __name__ == "__main__":
    main()
