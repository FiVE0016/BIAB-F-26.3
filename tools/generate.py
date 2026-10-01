#!/usr/bin/env python3
"""Generates every texture and data/asset JSON for BetterInventory.

Layout constants mirror BetterInventoryLayout.java - keep in sync.
Run from the repo root:  python tools/generate.py
"""
import json
import math
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODID = "betterinventory"
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", MODID)
DATA = os.path.join(ROOT, "src", "main", "resources", "data")

# ---------------------------------------------------------------- layout mirror
SLOT = 18
GAP_BIG = 8
GAP_SMALL = 4
PAD = 3
LEFT_X = 0
CENTER_X = 42 + GAP_BIG            # 50
RIGHT_A_X = CENTER_X + 168 + GAP_BIG   # 226
RIGHT_B_X = RIGHT_A_X + 42 + GAP_SMALL # 272
TABS_H = 20
TOP_Y = TABS_H                     # 20
MAIN_Y = TOP_Y + 42 + GAP_SMALL    # 66
HOTBAR_Y = MAIN_Y + 60 + GAP_BIG   # 134
IMAGE_W = RIGHT_B_X + 42           # 314
IMAGE_H = HOTBAR_Y + 24            # 158
TAB_W, TAB_H = 24, 20
TAB_X0 = CENTER_X + 2
TAB_SPACING = 26
SETTINGS_SIZE = 20
SPR_TAB = (220, 0)
SPR_SETTINGS = (220, 66)
SPR_LOCK = (220, 112)
SPR_TAB_ICON = (220, 132)
SPR_CRAFT_ICON = (220, 150)
SPR_ZOOM = (250, 0)
SPR_CHAR = (352, 0)
SPR_CRAFT = (432, 0)
ZOOM_SIZE = 100
CHAR_W, CHAR_H = 78, 130
# Vertical crafting panel, sharing the character panel's footprint.
CRAFT_COLS, CRAFT_ROWS = 3, 3
CRAFT_GRID_X = (CHAR_W - CRAFT_COLS * 18) // 2
CRAFT_GRID_Y = 12
CRAFT_ARROW_Y = 68
CRAFT_RESULT_X = (CHAR_W - 18) // 2
CRAFT_RESULT_Y = 86
# Panel sprites: (u, v, cols, rows)
PANEL_SPRITES = [
    ((0, 0), 9, 3),
    ((0, 68), 9, 2),
    ((0, 118), 9, 1),
    ((176, 0), 2, 2),
    ((176, 50), 2, 3),
]

# ---------------------------------------------------------------- palette
BG = (198, 198, 198, 255)
BEVEL_LIGHT = (255, 255, 255, 255)
BEVEL_DARK = (85, 85, 85, 255)
OUTLINE = (20, 20, 20, 255)
SLOT_BG = (139, 139, 139, 255)
SLOT_DARK = (55, 55, 55, 255)
SLOT_LIGHT = (255, 255, 255, 255)


def ensure(path):
    os.makedirs(path, exist_ok=True)
    return path


def save(img, *parts):
    path = os.path.join(*parts)
    ensure(os.path.dirname(path))
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


def write_json(obj, *parts):
    path = os.path.join(*parts)
    ensure(os.path.dirname(path))
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")
    print("wrote", os.path.relpath(path, ROOT))


# ---------------------------------------------------------------- draw helpers

def panel(draw, x, y, w, h):
    """Vanilla-style raised panel: black outline, light TL bevel, dark BR bevel."""
    draw.rectangle([x, y, x + w - 1, y + h - 1], fill=BG)
    # outline with soft corners
    draw.rectangle([x + 1, y, x + w - 2, y], fill=OUTLINE)
    draw.rectangle([x + 1, y + h - 1, x + w - 2, y + h - 1], fill=OUTLINE)
    draw.rectangle([x, y + 1, x, y + h - 2], fill=OUTLINE)
    draw.rectangle([x + w - 1, y + 1, x + w - 1, y + h - 2], fill=OUTLINE)
    # bevels
    draw.rectangle([x + 1, y + 1, x + w - 3, y + 2], fill=BEVEL_LIGHT)
    draw.rectangle([x + 1, y + 1, x + 2, y + h - 3], fill=BEVEL_LIGHT)
    draw.rectangle([x + 2, y + h - 3, x + w - 2, y + h - 2], fill=BEVEL_DARK)
    draw.rectangle([x + w - 3, y + 2, x + w - 2, y + h - 2], fill=BEVEL_DARK)


def slot_cell(draw, x, y):
    """Classic inset 18x18 slot."""
    draw.rectangle([x, y, x + 17, y + 17], fill=SLOT_BG)
    draw.rectangle([x, y, x + 16, y], fill=SLOT_DARK)
    draw.rectangle([x, y, x, y + 16], fill=SLOT_DARK)
    draw.rectangle([x + 1, y + 17, x + 17, y + 17], fill=SLOT_LIGHT)
    draw.rectangle([x + 17, y + 1, x + 17, y + 17], fill=SLOT_LIGHT)
    draw.point((x + 17, y), fill=SLOT_BG)
    draw.point((x, y + 17), fill=SLOT_BG)


def slot_grid(draw, px, py, cols, rows):
    for r in range(rows):
        for c in range(cols):
            slot_cell(draw, px + PAD + c * 18, py + PAD + r * 18)


def panel_with_grid(draw, x, y, cols, rows):
    panel(draw, x, y, cols * 18 + PAD * 2, rows * 18 + PAD * 2)
    slot_grid(draw, x, y, cols, rows)


# ---------------------------------------------------------------- gui textures

def tab_sprite(d, tx, ty, mode):
    """Creative-menu style tab: rounded top, open bottom edge when selected."""
    base = {
        "normal": (198, 198, 198, 255),
        "selected": (222, 222, 222, 255),
        "disabled": (150, 150, 150, 255),
    }[mode]
    light = tuple(min(255, v + 33) for v in base[:3]) + (255,)
    dark = tuple(max(0, v - 60) for v in base[:3]) + (255,)
    w, h = TAB_W, TAB_H
    # body
    d.rectangle([tx + 1, ty + 1, tx + w - 2, ty + h - 1], fill=base)
    # black outline with clipped top corners
    d.rectangle([tx + 2, ty, tx + w - 3, ty], fill=OUTLINE)
    d.point((tx + 1, ty + 1), fill=OUTLINE)
    d.point((tx + w - 2, ty + 1), fill=OUTLINE)
    d.rectangle([tx, ty + 2, tx, ty + h - 1], fill=OUTLINE)
    d.rectangle([tx + w - 1, ty + 2, tx + w - 1, ty + h - 1], fill=OUTLINE)
    # highlight / shade bevels
    d.rectangle([tx + 2, ty + 1, tx + w - 3, ty + 1], fill=light)
    d.rectangle([tx + 1, ty + 2, tx + 1, ty + h - 1], fill=light)
    d.rectangle([tx + w - 2, ty + 2, tx + w - 2, ty + h - 1], fill=dark)
    if mode != "selected":
        # unselected tabs are closed off at the bottom and sit slightly recessed
        d.rectangle([tx + 1, ty + h - 1, tx + w - 2, ty + h - 1], fill=OUTLINE)
        d.rectangle([tx + 1, ty + h - 2, tx + w - 2, ty + h - 2], fill=dark)
    if mode == "disabled":
        # hatched dim wash so the slot still reads as a tab, just unavailable
        for yy in range(ty + 2, ty + h - 1):
            for xx in range(tx + 2, tx + w - 2):
                if (xx + yy) % 2 == 0:
                    d.point((xx, yy), fill=(120, 120, 120, 255))


# Hand-authored 14x14 cog. Explicit pixel art reads far better at this size than
# anything drawn from circles - '#' is the cog body, '.' is transparent.
GEAR_MAP = [
    ".....####.....",
    ".....####.....",
    "..##########..",
    ".############.",
    ".####....####.",
    "###........###",
    "###........###",
    "###........###",
    "###........###",
    ".####....####.",
    ".############.",
    "..##########..",
    ".....####.....",
    ".....####.....",
]
assert all(len(r) == 14 for r in GEAR_MAP), "gear rows must all be 14 wide"


def gear_button(d, gx, gy, hover):
    """A vanilla-looking square button with a crisp cog glyph."""
    s = SETTINGS_SIZE
    base = (150, 150, 150, 255) if not hover else (170, 178, 202, 255)
    light = tuple(min(255, v + 55) for v in base[:3]) + (255,)
    dark = tuple(max(0, v - 65) for v in base[:3]) + (255,)
    d.rectangle([gx, gy, gx + s - 1, gy + s - 1], fill=base)
    d.rectangle([gx + 1, gy, gx + s - 2, gy], fill=OUTLINE)
    d.rectangle([gx + 1, gy + s - 1, gx + s - 2, gy + s - 1], fill=OUTLINE)
    d.rectangle([gx, gy + 1, gx, gy + s - 2], fill=OUTLINE)
    d.rectangle([gx + s - 1, gy + 1, gx + s - 1, gy + s - 2], fill=OUTLINE)
    d.rectangle([gx + 1, gy + 1, gx + s - 2, gy + 1], fill=light)
    d.rectangle([gx + 1, gy + 1, gx + 1, gy + s - 2], fill=light)
    d.rectangle([gx + 1, gy + s - 2, gx + s - 2, gy + s - 2], fill=dark)
    d.rectangle([gx + s - 2, gy + 1, gx + s - 2, gy + s - 2], fill=dark)
    ink = (48, 48, 52, 255)
    shade = (96, 96, 102, 255)
    ox, oy = gx + (s - 14) // 2, gy + (s - 14) // 2
    for y, row in enumerate(GEAR_MAP):
        for x, ch in enumerate(row):
            if ch == "#":
                d.point((ox + x, oy + y), fill=ink)
    # top-edge highlight so the cog reads as raised rather than a flat blob
    for y, row in enumerate(GEAR_MAP):
        if y == 0:
            continue
        for x, ch in enumerate(row):
            if ch == "#" and GEAR_MAP[y - 1][x] == ".":
                d.point((ox + x, oy + y), fill=shade)


def gen_inventory():
    img = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Panel sprites, drawn once each and blitted per-position by the screen so the
    # curio column can be skipped entirely when Curios is absent.
    for (u, v), cols, rows in PANEL_SPRITES:
        panel_with_grid(d, u, v, cols, rows)

    for i, mode in enumerate(("normal", "selected", "disabled")):
        tab_sprite(d, SPR_TAB[0], SPR_TAB[1] + i * (TAB_H + 1), mode)

    for j in range(2):
        gear_button(d, SPR_SETTINGS[0], SPR_SETTINGS[1] + j * (SETTINGS_SIZE + 1), j == 1)

    # Dim overlay for unavailable slots: light enough that the slot texture reads
    # through it instead of turning into a black hole.
    lx, ly = SPR_LOCK
    d.rectangle([lx, ly, lx + 17, ly + 17], fill=(28, 28, 32, 96))

    # Icon for the default (player inventory) tab: a little 3x3 slot grid.
    ix, iy = SPR_TAB_ICON
    for gy in range(3):
        for gx in range(3):
            x0, y0 = ix + 1 + gx * 5, iy + 1 + gy * 5
            d.rectangle([x0, y0, x0 + 3, y0 + 3], fill=(72, 72, 78, 255))
            d.rectangle([x0 + 1, y0 + 1, x0 + 3, y0 + 3], fill=(148, 148, 156, 255))

    # zoom circle
    zx, zy = SPR_ZOOM
    d.ellipse([zx, zy, zx + ZOOM_SIZE - 1, zy + ZOOM_SIZE - 1], fill=OUTLINE)
    d.ellipse([zx + 1, zy + 1, zx + ZOOM_SIZE - 2, zy + ZOOM_SIZE - 2], fill=BEVEL_LIGHT)
    d.ellipse([zx + 3, zy + 3, zx + ZOOM_SIZE - 4, zy + ZOOM_SIZE - 4], fill=BG)
    d.ellipse([zx + 5, zy + 5, zx + ZOOM_SIZE - 6, zy + ZOOM_SIZE - 6], fill=BEVEL_DARK)
    d.ellipse([zx + 6, zy + 6, zx + ZOOM_SIZE - 7, zy + ZOOM_SIZE - 7], fill=SLOT_BG)

    # crafting tab icon: a 2x3 grid glyph with a result pip, matching the panel
    kx, ky = SPR_CRAFT_ICON
    for gy in range(3):
        for gx in range(2):
            x0, y0 = kx + 2 + gx * 5, ky + 1 + gy * 5
            d.rectangle([x0, y0, x0 + 3, y0 + 3], fill=(72, 72, 78, 255))
            d.rectangle([x0 + 1, y0 + 1, x0 + 3, y0 + 3], fill=(168, 140, 96, 255))
    d.rectangle([kx + 12, ky + 6, kx + 14, ky + 9], fill=(96, 84, 60, 255))

    # vertical crafting panel: 2x3 grid, arrow, result slot
    qx, qy = SPR_CRAFT
    panel(d, qx, qy, CHAR_W, CHAR_H)
    for gy in range(CRAFT_ROWS):
        for gx in range(CRAFT_COLS):
            slot_cell(d, qx + CRAFT_GRID_X - 1 + gx * 18, qy + CRAFT_GRID_Y - 1 + gy * 18)
    # downward arrow between grid and result
    ax = qx + CHAR_W // 2
    ay = qy + CRAFT_ARROW_Y
    d.rectangle([ax - 2, ay, ax + 1, ay + 7], fill=BEVEL_DARK)
    for i in range(5):
        d.rectangle([ax - 5 + i, ay + 7 + i, ax + 4 - i, ay + 7 + i], fill=BEVEL_DARK)
    slot_cell(d, qx + CRAFT_RESULT_X - 1, qy + CRAFT_RESULT_Y - 1)

    # character panel
    cx0, cy0 = SPR_CHAR
    panel(d, cx0, cy0, CHAR_W, CHAR_H)
    # recessed viewport: dark enough for the player model to read against, light
    # enough that it does not look like a black hole cut into the GUI
    d.rectangle([cx0 + PAD, cy0 + PAD, cx0 + CHAR_W - PAD - 1, cy0 + CHAR_H - PAD - 1], fill=(88, 90, 104, 255))
    d.rectangle([cx0 + PAD, cy0 + PAD, cx0 + CHAR_W - PAD - 1, cy0 + PAD], fill=SLOT_DARK)
    d.rectangle([cx0 + PAD, cy0 + PAD, cx0 + PAD, cy0 + CHAR_H - PAD - 1], fill=SLOT_DARK)
    d.rectangle([cx0 + PAD, cy0 + CHAR_H - PAD - 1, cx0 + CHAR_W - PAD - 1, cy0 + CHAR_H - PAD - 1], fill=SLOT_LIGHT)
    d.rectangle([cx0 + CHAR_W - PAD - 1, cy0 + PAD, cx0 + CHAR_W - PAD - 1, cy0 + CHAR_H - PAD - 1], fill=SLOT_LIGHT)

    save(img, ASSETS, "textures", "gui", "inventory.png")


def gen_backpack_gui():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    W, H = 176, 212
    GATHER_Y, MAIN_Y2, PLAYER_Y, HOTBAR_Y2 = 18, 58, 126, 188
    panel(d, 0, 0, W, H)
    for r in range(2):
        for c in range(9):
            slot_cell(d, 7 + c * 18, GATHER_Y - 1 + r * 18)
    for r in range(3):
        for c in range(9):
            slot_cell(d, 7 + c * 18, MAIN_Y2 - 1 + r * 18)
    for r in range(3):
        for c in range(9):
            slot_cell(d, 7 + c * 18, PLAYER_Y - 1 + r * 18)
    for c in range(9):
        slot_cell(d, 7 + c * 18, HOTBAR_Y2 - 1)
    # divider between gather hub and main grid
    d.rectangle([7, GATHER_Y + 36 + 2, 7 + 161, GATHER_Y + 36 + 2], fill=BEVEL_DARK)
    d.rectangle([7, GATHER_Y + 36 + 3, 7 + 161, GATHER_Y + 36 + 3], fill=BEVEL_LIGHT)
    save(img, ASSETS, "textures", "gui", "backpack.png")


def inset_frame(d, x, y, w, h):
    """Sunken area used to group a section, matching vanilla's recessed panels."""
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=(174, 174, 174, 255))
    d.rectangle([x, y, x + w - 1, y], fill=BEVEL_DARK)
    d.rectangle([x, y, x, y + h - 1], fill=BEVEL_DARK)
    d.rectangle([x + 1, y + h - 1, x + w - 1, y + h - 1], fill=BEVEL_LIGHT)
    d.rectangle([x + w - 1, y + 1, x + w - 1, y + h - 1], fill=BEVEL_LIGHT)


def divider(d, x, y, w):
    d.rectangle([x, y, x + w - 1, y], fill=BEVEL_DARK)
    d.rectangle([x, y + 1, x + w - 1, y + 1], fill=BEVEL_LIGHT)


# Settings screen layout - mirrored by SettingsMenu.java / SettingsScreen.java.
SET_W, SET_H = 190, 264
SET_UP_X, SET_UP_Y = 50, 107
SET_PLAYER_Y, SET_HOTBAR_Y = 182, 240
SET_INV_X = 14
SET_DIV_TOP = 27
SET_DIV_MID = 102
SET_DIV_BOTTOM = 168


def gen_settings_gui():
    img = Image.new("RGBA", (256, 512), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    panel(d, 0, 0, SET_W, SET_H)
    divider(d, 8, SET_DIV_TOP, SET_W - 16)
    # framed upgrade bay
    inset_frame(d, SET_UP_X - 4, SET_UP_Y - 4, 5 * 18 + 8, 2 * 18 + 8)
    for r in range(2):
        for c in range(5):
            slot_cell(d, SET_UP_X + c * 18, SET_UP_Y + r * 18)
    divider(d, 8, SET_DIV_MID, SET_W - 16)
    divider(d, 8, SET_DIV_BOTTOM, SET_W - 16)
    for r in range(3):
        for c in range(9):
            slot_cell(d, SET_INV_X + c * 18, SET_PLAYER_Y - 1 + r * 18)
    for c in range(9):
        slot_cell(d, SET_INV_X + c * 18, SET_HOTBAR_Y - 1)
    save(img, ASSETS, "textures", "gui", "settings.png")


# hud.png sprite layout - mirrored by BetterInventoryHud.java.
HUD_TEX = 64
HUD_CELL = 22                    # (0,0) idle cell, (0,24) engaged cell
HUD_SEL_W, HUD_SEL_H = 24, 23    # (24,0) selection ring


def hotbar_cell(d, x, y, border):
    """A 22x22 cell at vanilla hotbar proportions: 1px light border over a dark
    translucent interior, with the same soft inner shading. Drawn here rather than
    sliced from the vanilla hud/hotbar sprite so the slot can be tinted for the
    slide/fade animation, which a plain blit honours reliably."""
    s = HUD_CELL
    d.rectangle([x, y, x + s - 1, y + s - 1], fill=(0, 0, 0, 160))
    for edge in (
        [x, y, x + s - 1, y],
        [x, y + s - 1, x + s - 1, y + s - 1],
        [x, y, x, y + s - 1],
        [x + s - 1, y, x + s - 1, y + s - 1],
    ):
        d.rectangle(edge, fill=border)
    # inner shading: lighter top/left, darker bottom/right
    d.rectangle([x + 1, y + 1, x + s - 2, y + 1], fill=(255, 255, 255, 40))
    d.rectangle([x + 1, y + 1, x + 1, y + s - 2], fill=(255, 255, 255, 40))
    d.rectangle([x + 1, y + s - 2, x + s - 2, y + s - 2], fill=(0, 0, 0, 90))
    d.rectangle([x + s - 2, y + 1, x + s - 2, y + s - 2], fill=(0, 0, 0, 90))


def gen_hud():
    img = Image.new("RGBA", (HUD_TEX, HUD_TEX), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    hotbar_cell(d, 0, 0, (150, 150, 150, 255))     # idle
    hotbar_cell(d, 0, 24, (208, 208, 208, 255))    # tool engaged
    # selection ring: 2px near-white frame, like vanilla hotbar_selection
    sx, sy = 24, 0
    ring = (255, 255, 255, 235)
    for t in range(2):
        d.rectangle([sx + t, sy + t, sx + HUD_SEL_W - 1 - t, sy + t], fill=ring)
        d.rectangle([sx + t, sy + HUD_SEL_H - 1 - t, sx + HUD_SEL_W - 1 - t, sy + HUD_SEL_H - 1 - t], fill=ring)
        d.rectangle([sx + t, sy + t, sx + t, sy + HUD_SEL_H - 1 - t], fill=ring)
        d.rectangle([sx + HUD_SEL_W - 1 - t, sy + t, sx + HUD_SEL_W - 1 - t, sy + HUD_SEL_H - 1 - t], fill=ring)
    save(img, ASSETS, "textures", "gui", "hud.png")


# ---------------------------------------------------------------- item art

def from_map(rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), palette[ch])
    return img


BACKPACK_MAP = [
    "................",
    "....o......o....",
    "...o.o....o.o...",
    "...o..oooo..o...",
    "..obbbbbbbbbbo..",
    "..obBBBBBBBBbo..",
    ".obBBBBBBBBBBbo.",
    ".obffffffffffbo.",
    ".obffffffffffbo.",
    ".obfFmmFfffffbo.",
    ".obBBmmBBBBBBbo.",
    ".obBBBBBBBBBBbo.",
    ".obBBBBBBBBBBbo.",
    "..obBBBBBBBBbo..",
    "...obbbbbbbbo...",
    "................",
]

BACKPACK_TIERS = {
    1: {"B": (146, 98, 57), "b": (100, 64, 35), "f": (120, 78, 44), "F": (170, 120, 74), "m": (196, 164, 90), "o": (52, 34, 20)},
    2: {"B": (150, 150, 156), "b": (98, 98, 106), "f": (120, 120, 128), "F": (176, 176, 184), "m": (222, 222, 228), "o": (48, 48, 54)},
    3: {"B": (222, 176, 62), "b": (160, 118, 32), "f": (196, 148, 44), "F": (244, 208, 108), "m": (255, 236, 168), "o": (92, 62, 18)},
    4: {"B": (92, 216, 212), "b": (44, 150, 150), "f": (66, 184, 182), "F": (150, 240, 238), "m": (222, 255, 254), "o": (20, 84, 84)},
    5: {"B": (86, 74, 88), "b": (52, 44, 54), "f": (68, 58, 70), "F": (120, 104, 122), "m": (216, 130, 74), "o": (26, 22, 28)},
}

STACK_MAP = [
    "................",
    ".......aa.......",
    "......aAAa......",
    ".....aAAAAa.....",
    "....aAAAAAAa....",
    "....ooaAAaoo....",
    "......aAAa......",
    "......aAAa......",
    "...cccccccccc...",
    "...cCCCCCCCCc...",
    "...cccccccccc...",
    "...cCCCCCCCCc...",
    "...cccccccccc...",
    "...cCCCCCCCCc...",
    "...cccccccccc...",
    "................",
]

TIER_COLORS = [
    (176, 176, 176), (230, 230, 230), (240, 200, 90), (90, 205, 120),
    (90, 200, 230), (190, 120, 235), (240, 110, 90),
]


def stack_palette(tier):
    accent = TIER_COLORS[tier]
    dark = tuple(max(0, v - 90) for v in accent)
    return {
        "a": dark + (255,),
        "A": accent + (255,),
        "o": (0, 0, 0, 0),
        "c": (70, 70, 76, 255),
        "C": (128, 128, 136, 255),
    }


MAGNET_MAP = [
    "................",
    "................",
    "...rrr....rrr...",
    "..rRRr....rRRr..",
    "..rRRr....rRRr..",
    "..wWWw....wWWw..",
    "..wWWw....wWWw..",
    "..rRRr....rRRr..",
    "..rRRr....rRRr..",
    "..rRRrr..rrRRr..",
    "..rRRRrrrrRRRr..",
    "...rRRRRRRRRr...",
    "....rrRRRRrr....",
    "......rrrr......",
    "................",
    "................",
]

FEEDING_MAP = [
    "................",
    "................",
    "......bbbb......",
    ".....bTTTTb.....",
    "....bTTTTTTb....",
    "....bTTTTTTb....",
    "....bTTTTTb.....",
    ".....bTTTb..gg..",
    "......bTb...gg..",
    ".......bW.gggggg",
    ".......WW.gggggg",
    "......WW....gg..",
    ".....WW.....gg..",
    "....WW..........",
    "................",
    "................",
]

REFILL_MAP = [
    "................",
    "....aaaaaaa.....",
    "...aa......a....",
    "..aa........a...",
    "..aa............",
    "..aa.......XX...",
    "..aa......XXXX..",
    "..........XXXX..",
    "..XXXX..........",
    "..XXXX......aa..",
    "...XX.......aa..",
    "............aa..",
    "...a........aa..",
    "....a......aa...",
    ".....aaaaaaa....",
    "................",
]

VOID_MAP = [
    "................",
    "..pppppppppppp..",
    ".pPPPPPPPPPPPPp.",
    ".pPddddddddddPp.",
    ".pPdDDDDDDDDdPp.",
    ".pPdDkkkkkkDdPp.",
    ".pPdDkKKKKkDdPp.",
    ".pPdDkKkKKkDdPp.",
    ".pPdDkKKkKkDdPp.",
    ".pPdDkKKKKkDdPp.",
    ".pPdDkkkkkkDdPp.",
    ".pPdDDDDDDDDdPp.",
    ".pPddddddddddPp.",
    ".pPPPPPPPPPPPPp.",
    "..pppppppppppp..",
    "................",
]

CRAFT_UPGRADE_MAP = [
    "................",
    "..wwwwwwwwwwww..",
    "..wTTTTTTTTTTw..",
    "..wT.T..T.T..w..",
    "..wTTTTTTTTTTw..",
    "..wT.T..T.T..w..",
    "..wTTTTTTTTTTw..",
    "..wT.T..T.T..w..",
    "..wTTTTTTTTTTw..",
    "..wwwwwwwwwwww..",
    "....gg....gg....",
    "...gggg..gggg...",
    "....gg....gg....",
    "................",
    "................",
    "................",
]

ADV_CRAFT_UPGRADE_MAP = [
    "................",
    ".wwwwwwwwwwwwww.",
    ".wTTTTTTTTTTTTw.",
    ".wT.T..T.T..T.w.",
    ".wTTTTTTTTTTTTw.",
    ".wT.T..T.T..T.w.",
    ".wTTTTTTTTTTTTw.",
    ".wT.T..T.T..T.w.",
    ".wTTTTTTTTTTTTw.",
    ".wwwwwwwwwwwwww.",
    "..gg..gg..gg....",
    ".gggg.gggg.gggg.",
    "..gg..gg..gg....",
    "................",
    "................",
    "................",
]
assert all(len(r) == 16 for r in ADV_CRAFT_UPGRADE_MAP), "adv crafting rows must be 16 wide"
assert all(len(r) == 16 for r in CRAFT_UPGRADE_MAP), "crafting rows must be 16 wide"

PICKUP_MAP = [
    "................",
    ".......gg.......",
    ".......gg.......",
    ".......gg.......",
    ".....gggggg.....",
    "......gggg......",
    ".......gg.......",
    "..obbbbbbbbbbo..",
    ".obBBBBBBBBBBbo.",
    ".obBBBBBBBBBBbo.",
    ".obBBBBBBBBBBbo.",
    ".obBBBBBBBBBBbo.",
    ".obBBBBBBBBBBbo.",
    "..obBBBBBBBBo...",
    "...oobbbbbboo...",
    "................",
]

COMMON = {
    "r": (150, 30, 30, 255), "R": (222, 70, 70, 255),
    "w": (150, 150, 150, 255), "W": (230, 230, 230, 255),
    "b": (100, 64, 35, 255), "B": (146, 98, 57, 255),
    "T": (196, 140, 84, 255),
    "g": (74, 190, 84, 255),
    "a": (86, 156, 222, 255), "X": (86, 156, 222, 255),
    "p": (60, 24, 78, 255), "P": (130, 62, 168, 255),
    "d": (36, 14, 48, 255), "D": (20, 8, 26, 255),
    "k": (10, 4, 14, 255), "K": (168, 108, 220, 255),
    "o": (52, 34, 20, 255),
}


def gen_items():
    for level, pal in BACKPACK_TIERS.items():
        pal_rgba = {k: v + (255,) for k, v in pal.items()}
        save(from_map(BACKPACK_MAP, pal_rgba), ASSETS, "textures", "item", f"backpack_{level}.png")
    for tier in range(7):
        save(from_map(STACK_MAP, stack_palette(tier)), ASSETS, "textures", "item", f"stack_upgrade_{tier + 1}.png")
    save(from_map(MAGNET_MAP, COMMON), ASSETS, "textures", "item", "upgrade_magnet.png")
    save(from_map(FEEDING_MAP, COMMON), ASSETS, "textures", "item", "upgrade_feeding.png")
    save(from_map(REFILL_MAP, COMMON), ASSETS, "textures", "item", "upgrade_refill.png")
    save(from_map(VOID_MAP, COMMON), ASSETS, "textures", "item", "upgrade_void.png")
    save(from_map(PICKUP_MAP, COMMON), ASSETS, "textures", "item", "upgrade_pickup.png")
    save(from_map(CRAFT_UPGRADE_MAP, COMMON), ASSETS, "textures", "item", "crafting_upgrade.png")
    # advanced tier: same frame, wider grid + a brighter accent
    adv = dict(COMMON)
    adv["g"] = (222, 196, 96, 255)
    save(from_map(ADV_CRAFT_UPGRADE_MAP, adv), ASSETS, "textures", "item", "advanced_crafting_upgrade.png")


def silhouette(rows):
    pal = {"#": (139, 139, 139, 255), "x": (100, 100, 100, 255)}
    return from_map(rows, pal)


EMPTY_BACKPACK = [
    "................",
    "....x......x....",
    "...x.x....x.x...",
    "...x..xxxx..x...",
    "..xxxxxxxxxxxx..",
    "..x##########x..",
    ".x############x.",
    ".x#xxxxxxxxxx#x.",
    ".x############x.",
    ".x############x.",
    ".x############x.",
    ".x############x.",
    ".x############x.",
    "..x##########x..",
    "...xxxxxxxxxx...",
    "................",
]

EMPTY_UPGRADE = [
    "................",
    "..xxxxxxxxxxxx..",
    "..x##########x..",
    "..x####xx####x..",
    "..x###xxxx###x..",
    "..x##xxxxxx##x..",
    "..x#xxx##xxx#x..",
    "..x####xx####x..",
    "..x####xx####x..",
    "..x####xx####x..",
    "..x####xx####x..",
    "..x##########x..",
    "..xxxxxxxxxxxx..",
    "................",
    "................",
    "................",
]

EMPTY_STACK = [
    "................",
    "......xx........",
    ".....xxxx.......",
    "....xxxxxx......",
    "......xx........",
    "......xx........",
    "..xxxxxxxxxx....",
    "..x########x....",
    "..xxxxxxxxxx....",
    "..x########x....",
    "..xxxxxxxxxx....",
    "..x########x....",
    "..xxxxxxxxxx....",
    "................",
    "................",
    "................",
]

EMPTY_OFFHAND = [
    "................",
    "..xxxxxxxxxxx...",
    "..x#########x...",
    "..x#########x...",
    "..x####x####x...",
    "..x###xxx###x...",
    "..x####x####x...",
    "..x####x####x...",
    "..x#########x...",
    "...x#######x....",
    "...x#######x....",
    "....x#####x.....",
    ".....x###x......",
    "......xxx.......",
    "................",
    "................",
]

EMPTY_CURIO = [
    "................",
    ".......xx.......",
    ".......xx.......",
    "......x##x......",
    "......x##x......",
    "..xxxx####xxxx..",
    "..x##########x..",
    "...x########x...",
    "....x######x....",
    ".....x####x.....",
    "....x##xx##x....",
    "....x#x..x#x....",
    "...xx......xx...",
    "................",
    "................",
    "................",
]


def gen_empty_slots():
    save(silhouette(EMPTY_BACKPACK), ASSETS, "textures", "item", "empty_slot_backpack.png")
    save(silhouette(EMPTY_UPGRADE), ASSETS, "textures", "item", "empty_slot_upgrade.png")
    save(silhouette(EMPTY_STACK), ASSETS, "textures", "item", "empty_slot_stack_upgrade.png")
    save(silhouette(EMPTY_OFFHAND), ASSETS, "textures", "item", "empty_slot_offhand.png")
    save(silhouette(EMPTY_CURIO), ASSETS, "textures", "item", "empty_slot_curio.png")


def gen_block_textures():
    side = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(side)
    d.rectangle([0, 0, 15, 15], fill=(146, 98, 57, 255))
    d.rectangle([0, 0, 15, 0], fill=(100, 64, 35, 255))
    d.rectangle([0, 15, 15, 15], fill=(100, 64, 35, 255))
    d.rectangle([0, 0, 0, 15], fill=(100, 64, 35, 255))
    d.rectangle([15, 0, 15, 15], fill=(100, 64, 35, 255))
    d.rectangle([2, 4, 13, 7], fill=(120, 78, 44, 255))
    d.rectangle([6, 5, 9, 8], fill=(196, 164, 90, 255))
    d.rectangle([7, 6, 8, 7], fill=(120, 78, 44, 255))
    save(side, ASSETS, "textures", "block", "backpack_side.png")

    top = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(top)
    d.rectangle([0, 0, 15, 15], fill=(120, 78, 44, 255))
    d.rectangle([0, 0, 15, 0], fill=(100, 64, 35, 255))
    d.rectangle([0, 15, 15, 15], fill=(100, 64, 35, 255))
    d.rectangle([0, 0, 0, 15], fill=(100, 64, 35, 255))
    d.rectangle([15, 0, 15, 15], fill=(100, 64, 35, 255))
    d.rectangle([3, 2, 5, 13], fill=(100, 64, 35, 255))
    d.rectangle([10, 2, 12, 13], fill=(100, 64, 35, 255))
    save(top, ASSETS, "textures", "block", "backpack_top.png")


def gen_icon():
    icon = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(icon)
    for y in range(128):
        t = y / 127
        col = (int(40 + 30 * t), int(44 + 20 * t), int(70 + 40 * t), 255)
        d.rectangle([0, y, 127, y], fill=col)
    d.rounded_rectangle([2, 2, 125, 125], radius=18, outline=(240, 214, 130, 255), width=3)
    art = from_map(BACKPACK_MAP, {k: v + (255,) for k, v in BACKPACK_TIERS[3].items()})
    art = art.resize((96, 96), Image.NEAREST)
    icon.alpha_composite(art, (16, 14))
    save(icon, ASSETS, "icon.png")


# ---------------------------------------------------------------- data jsons

def gen_models():
    # blockstate with horizontal facing
    write_json({
        "variants": {
            "facing=north": {"model": "betterinventory:block/backpack"},
            "facing=south": {"model": "betterinventory:block/backpack", "y": 180},
            "facing=west": {"model": "betterinventory:block/backpack", "y": 270},
            "facing=east": {"model": "betterinventory:block/backpack", "y": 90},
        }
    }, ASSETS, "blockstates", "backpack.json")

    write_json({
        "parent": "block/block",
        "textures": {
            "particle": "betterinventory:block/backpack_side",
            "side": "betterinventory:block/backpack_side",
            "top": "betterinventory:block/backpack_top",
        },
        "elements": [
            {
                "from": [3, 0, 4.5],
                "to": [13, 10, 11.5],
                "faces": {
                    "north": {"texture": "#side"},
                    "south": {"texture": "#side"},
                    "east": {"texture": "#side"},
                    "west": {"texture": "#side"},
                    "up": {"texture": "#top"},
                    "down": {"texture": "#top"},
                },
            }
        ],
    }, ASSETS, "models", "block", "backpack.json")

    items = [f"backpack_{i}" for i in range(1, 6)]
    items += [f"stack_upgrade_{i}" for i in range(1, 8)]
    items += [f"upgrade_{k}" for k in ("magnet", "feeding", "refill", "void", "pickup")]
    items += ["crafting_upgrade", "advanced_crafting_upgrade"]
    for item in items:
        write_json({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"betterinventory:item/{item}"},
        }, ASSETS, "models", "item", f"{item}.json")


def gen_lang():
    lang = {
        "itemGroup.betterinventory": "Better Inventory",
        "container.betterinventory.inventory": "Inventory",
        "container.betterinventory.settings": "Inventory Settings",
        "block.betterinventory.backpack": "Backpack",
        "key.categories.betterinventory": "Better Inventory",
        "key.betterinventory.offhand_selector": "Offhand Carousel (hold + scroll)",
        "item.betterinventory.backpack.level": "Level %s - %s upgrade slots",
        "item.betterinventory.backpack.contents": "%s items stored",
        "item.betterinventory.backpack.howto": "Right click to open. Sneak + right click to place or equip.",
        "item.betterinventory.stack_upgrade.desc": "Stacks in backpack and gather-hub storage grow to %s",
        "item.betterinventory.stack_upgrade.scope": "Applies to the gather hub and backpack tabs",
        "item.betterinventory.upgrade.howto": "Install via a backpack tab's settings screen",
        "item.betterinventory.upgrade_magnet.desc": "Pulls nearby dropped items toward you",
        "item.betterinventory.upgrade_feeding.desc": "Automatically eats food from this backpack when hungry",
        "item.betterinventory.upgrade_refill.desc": "Hotbar auto-refill may pull from this backpack",
        "item.betterinventory.upgrade_void.desc": "Destroys overflow of items locked in this backpack",
        "item.betterinventory.upgrade_pickup.desc": "Items this backpack knows are picked up straight into it",
        "item.betterinventory.upgrade_magnet": "Magnet",
        "item.betterinventory.upgrade_feeding": "Feeding",
        "item.betterinventory.upgrade_refill": "Refill",
        "item.betterinventory.upgrade_void": "Void",
        "item.betterinventory.upgrade_pickup": "Pickup",
        "screen.betterinventory.tab.empty": "Backpack slot %s is empty",
        "screen.betterinventory.tab.inventory": "Inventory",
        "screen.betterinventory.tab.crafting": "Crafting (%s)",
        "item.betterinventory.crafting_upgrade": "Simple Crafting Upgrade",
        "item.betterinventory.advanced_crafting_upgrade": "Advanced Crafting Upgrade",
        "item.betterinventory.crafting_upgrade.desc": "Extends the crafting tab to a %s grid",
        "item.betterinventory.crafting_upgrade.howto": "Place in the upgrade slot beside the stack upgrade",
        "screen.betterinventory.settings": "Tab Settings",
        "screen.betterinventory.settings.default_tab": "Default tab (player inventory)",
        "screen.betterinventory.settings.tab": "Backpack tab %s",
        "screen.betterinventory.settings.no_backpack": "No backpack in this tab",
        "screen.betterinventory.settings.gather": "Gather Hub",
        "screen.betterinventory.settings.refill": "Auto Refill",
        "screen.betterinventory.settings.combat": "Combat",
        "screen.betterinventory.settings.upgrades": "Upgrades",
        "screen.betterinventory.settings.no_upgrades": "No upgrades installed",
        "screen.betterinventory.settings.pickaxe2": "Pickaxe 2 Blocks (%s)",
        "screen.betterinventory.settings.pickaxe2.tip": "Choose which blocks the second pickaxe is used on.",
        "screen.betterinventory.settings.share.tip": "All: one shared value across every tab. Tab: this tab only.",
        "screen.betterinventory.settings.upgrade.tip": "Click to enable or disable this upgrade",
        "screen.betterinventory.on": "On",
        "screen.betterinventory.off": "Off",
        "screen.betterinventory.sword": "Sword",
        "screen.betterinventory.axe": "Axe",
        "screen.betterinventory.shared": "Shared",
        "screen.betterinventory.per_tab": "Per Tab",
        "screen.betterinventory.chooser.title": "Pickaxe 2 Block List",
        "screen.betterinventory.chooser.search": "Search blocks",
        "screen.betterinventory.chooser.done": "Save",
        "screen.betterinventory.chooser.none": "No blocks match",
        "screen.betterinventory.chooser.count": "%s blocks selected",
    }
    for i in range(1, 6):
        lang[f"item.betterinventory.backpack_{i}"] = f"Backpack (Level {i})"
    for i in range(1, 8):
        lang[f"item.betterinventory.stack_upgrade_{i}"] = f"Stack Upgrade {['I','II','III','IV','V','VI','VII'][i-1]}"
    lang["item.betterinventory.upgrade_magnet"] = "Magnet Upgrade"
    lang["item.betterinventory.upgrade_feeding"] = "Feeding Upgrade"
    lang["item.betterinventory.upgrade_refill"] = "Refill Upgrade"
    lang["item.betterinventory.upgrade_void"] = "Void Upgrade"
    lang["item.betterinventory.upgrade_pickup"] = "Pickup Upgrade"
    write_json(lang, ASSETS, "lang", "en_us.json")


def shaped(result, pattern, key, count=1):
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": pattern,
        "key": key,
        "result": {"id": result, "count": count},
    }


def gen_recipes():
    r = os.path.join(DATA, MODID, "recipe")
    write_json(shaped("betterinventory:backpack_1", ["SLS", "LCL", "LLL"], {
        "S": {"item": "minecraft:string"},
        "L": {"item": "minecraft:leather"},
        "C": {"item": "minecraft:chest"},
    }), r, "backpack_1.json")
    upgrades = {2: "minecraft:iron_ingot", 3: "minecraft:gold_ingot", 4: "minecraft:diamond", 5: "minecraft:netherite_ingot"}
    for level, material in upgrades.items():
        write_json(shaped(f"betterinventory:backpack_{level}", ["MMM", "MBM", "MMM"], {
            "M": {"item": material},
            "B": {"item": f"betterinventory:backpack_{level - 1}"},
        }), r, f"backpack_{level}.json")

    write_json(shaped("betterinventory:stack_upgrade_1", ["PPP", "ICI", "PPP"], {
        "P": {"item": "minecraft:paper"},
        "I": {"item": "minecraft:iron_ingot"},
        "C": {"item": "minecraft:chest"},
    }), r, "stack_upgrade_1.json")
    tier_mats = {2: "minecraft:copper_ingot", 3: "minecraft:iron_ingot", 4: "minecraft:gold_ingot",
                 5: "minecraft:emerald", 6: "minecraft:diamond", 7: "minecraft:netherite_ingot"}
    for tier, material in tier_mats.items():
        write_json(shaped(f"betterinventory:stack_upgrade_{tier}", ["MMM", "MUM", "MMM"], {
            "M": {"item": material},
            "U": {"item": f"betterinventory:stack_upgrade_{tier - 1}"},
        }), r, f"stack_upgrade_{tier}.json")

    write_json(shaped("betterinventory:upgrade_magnet", [" I ", "IRI", " E "], {
        "I": {"item": "minecraft:iron_ingot"},
        "R": {"item": "minecraft:redstone"},
        "E": {"item": "minecraft:ender_pearl"},
    }), r, "upgrade_magnet.json")
    write_json(shaped("betterinventory:upgrade_feeding", [" G ", "GAG", " B "], {
        "G": {"item": "minecraft:gold_ingot"},
        "A": {"item": "minecraft:golden_carrot"},
        "B": {"item": "minecraft:bowl"},
    }), r, "upgrade_feeding.json")
    write_json(shaped("betterinventory:upgrade_refill", [" A ", "AHA", " R "], {
        "A": {"item": "minecraft:arrow"},
        "H": {"item": "minecraft:hopper"},
        "R": {"item": "minecraft:redstone"},
    }), r, "upgrade_refill.json")
    write_json(shaped("betterinventory:upgrade_void", ["OOO", "OLO", "OOO"], {
        "O": {"item": "minecraft:obsidian"},
        "L": {"item": "minecraft:lava_bucket"},
    }), r, "upgrade_void.json")
    write_json(shaped("betterinventory:upgrade_pickup", [" S ", "SHS", " C "], {
        "S": {"item": "minecraft:string"},
        "H": {"item": "minecraft:hopper"},
        "C": {"item": "minecraft:chest"},
    }), r, "upgrade_pickup.json")
    write_json(shaped("betterinventory:crafting_upgrade", ["STS", "TCT", "STS"], {
        "S": {"item": "minecraft:stick"},
        "T": {"item": "minecraft:crafting_table"},
        "C": {"item": "minecraft:diamond"},
    }), r, "crafting_upgrade.json")
    write_json(shaped("betterinventory:advanced_crafting_upgrade", ["TTT", "TUT", "TNT"], {
        "T": {"item": "minecraft:crafting_table"},
        "U": {"item": "betterinventory:crafting_upgrade"},
        "N": {"item": "minecraft:netherite_ingot"},
    }), r, "advanced_crafting_upgrade.json")


def gen_tags():
    # Items accepted by the personal aux upgrade slot beside the stack upgrade.
    write_json({"replace": False,
                "values": [f"{MODID}:crafting_upgrade", f"{MODID}:advanced_crafting_upgrade"]},
               DATA, MODID, "tags", "item", "aux_upgrades.json")


def main():
    gen_inventory()
    gen_backpack_gui()
    gen_settings_gui()
    gen_hud()
    gen_items()
    gen_empty_slots()
    gen_block_textures()
    gen_icon()
    gen_models()
    gen_lang()
    gen_recipes()
    gen_tags()
    print("done")


if __name__ == "__main__":
    main()
