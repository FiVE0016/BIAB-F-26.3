#!/usr/bin/env python3
"""Generates a plain icon/logo/banner set for BetterInventory from the mod icon art.

SUPERSEDED: the branding that actually ships (the pixel-framed `betterinventory-*` set shared with
every other mod on the CurseForge pages) comes from
`make-it-compatible-split/branding/generate.py betterinventory`, and lands in `publish/.../icons/`.
Output goes to tools/legacy-icons/ so it stays out of the publish bundle, which per
PUBLISHING.md section 5 holds exactly the three shipped brand files.
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_ICON = os.path.join(ROOT, "src", "main", "resources", "assets", "betterinventory", "icon.png")
OUT = os.path.join(ROOT, "tools", "legacy-icons")
os.makedirs(OUT, exist_ok=True)


def gradient(w, h):
    img = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        d.rectangle([0, y, w, y], fill=(int(40 + 30 * t), int(44 + 20 * t), int(70 + 40 * t), 255))
    return img


base = Image.open(SRC_ICON).convert("RGBA")

icon = base.resize((400, 400), Image.NEAREST)
icon.save(os.path.join(OUT, "betterinventory-icon.png"))

logo = gradient(800, 256)
art = base.resize((224, 224), Image.NEAREST)
logo.alpha_composite(art, (24, 16))
d = ImageDraw.Draw(logo)
# Blocky wordmark: BI in chunky pixel capitals
WORDMARK = "BI"
PIX = {
    "B": ["##.", "#.#", "##.", "#.#", "##."],
    "I": ["###", ".#.", ".#.", ".#.", "###"],
}
x0, y0, s = 290, 78, 20
for ch in WORDMARK:
    glyph = PIX[ch]
    for gy, row in enumerate(glyph):
        for gx, c in enumerate(row):
            if c == "#":
                d.rectangle([x0 + gx * s, y0 + gy * s, x0 + gx * s + s - 2, y0 + gy * s + s - 2],
                            fill=(240, 214, 130, 255))
    x0 += 4 * s
logo.save(os.path.join(OUT, "betterinventory-logo.png"))

banner = gradient(1280, 320)
art = base.resize((256, 256), Image.NEAREST)
banner.alpha_composite(art, (48, 32))
d = ImageDraw.Draw(banner)
x0, y0, s = 380, 105, 22
for ch in WORDMARK:
    glyph = PIX[ch]
    for gy, row in enumerate(glyph):
        for gx, c in enumerate(row):
            if c == "#":
                d.rectangle([x0 + gx * s, y0 + gy * s, x0 + gx * s + s - 2, y0 + gy * s + s - 2],
                            fill=(240, 214, 130, 255))
    x0 += 4 * s
banner.save(os.path.join(OUT, "betterinventory-banner.png"))
print("icons written to", OUT)
