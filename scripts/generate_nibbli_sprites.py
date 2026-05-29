#!/usr/bin/env python3
"""Generate nibbli sprites — hollow outline Tamagotchi style (cute on green LCD)."""

from __future__ import annotations

import os
from PIL import Image

FRAME = 32
COLS = 12
INK = 1
HOLE = 0
BLACK = (26, 26, 30, 255)


def blank() -> list[list[int]]:
    return [[0] * FRAME for _ in range(FRAME)]


def set_px(grid: list[list[int]], x: int, y: int, v: int = INK) -> None:
    if 0 <= x < FRAME and 0 <= y < FRAME:
        grid[y][x] = v


def fill_rect(grid: list[list[int]], x0: int, y0: int, w: int, h: int, v: int = INK) -> None:
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            set_px(grid, x, y, v)


def ellipse_val(x: int, y: int, cx: float, cy: float, rx: float, ry: float) -> float:
    dx = (x - cx) / rx
    dy = (y - cy) / ry
    return dx * dx + dy * dy


def draw_outline_ellipse(grid: list[list[int]], cx: float, cy: float, rx: float, ry: float, thick: int = 1) -> None:
    """Hollow body — green LCD shows through the middle."""
    outer = 1.05
    inner = max(0.55, 1.0 - thick * 0.12)
    for y in range(int(cy - ry) - 2, int(cy + ry) + 3):
        for x in range(int(cx - rx) - 2, int(cx + rx) + 3):
            v = ellipse_val(x, y, cx, cy, rx, ry)
            if inner < v <= outer:
                set_px(grid, x, y, INK)


def draw_dot_eyes(grid: list[list[int]], y: int = 12) -> None:
    """Tiny bead eyes — the classic cute look."""
    set_px(grid, 13, y, INK)
    set_px(grid, 19, y, INK)


def draw_eyes_happy(grid: list[list[int]], y: int = 11) -> None:
    for base in (12, 18):
        set_px(grid, base, y + 1, INK)
        set_px(grid, base + 1, y, INK)
        set_px(grid, base + 2, y + 1, INK)


def draw_eyes_closed(grid: list[list[int]], y: int = 12) -> None:
    set_px(grid, 12, y, INK)
    set_px(grid, 13, y, INK)
    set_px(grid, 14, y, INK)
    set_px(grid, 18, y, INK)
    set_px(grid, 19, y, INK)
    set_px(grid, 20, y, INK)


def draw_eyes_sleepy(grid: list[list[int]], y: int = 12) -> None:
    set_px(grid, 13, y, INK)
    set_px(grid, 19, y, INK)


def draw_eyes_wide(grid: list[list[int]], y: int = 11) -> None:
    set_px(grid, 12, y, INK)
    set_px(grid, 13, y + 1, INK)
    set_px(grid, 14, y, INK)
    set_px(grid, 18, y, INK)
    set_px(grid, 19, y + 1, INK)
    set_px(grid, 20, y, INK)


def draw_eyes_sad(grid: list[list[int]], y: int = 12) -> None:
    set_px(grid, 13, y + 1, INK)
    set_px(grid, 19, y + 1, INK)
    set_px(grid, 12, y, INK)
    set_px(grid, 20, y, INK)


def draw_mouth_smile(grid: list[list[int]], y: int = 15) -> None:
    set_px(grid, 14, y, INK)
    set_px(grid, 15, y + 1, INK)
    set_px(grid, 16, y + 1, INK)
    set_px(grid, 17, y, INK)


def draw_mouth_grin(grid: list[list[int]], y: int = 15) -> None:
    set_px(grid, 13, y, INK)
    set_px(grid, 14, y + 1, INK)
    set_px(grid, 15, y + 1, INK)
    set_px(grid, 16, y + 1, INK)
    set_px(grid, 17, y + 1, INK)
    set_px(grid, 18, y, INK)


def draw_mouth_o(grid: list[list[int]], y: int = 15) -> None:
    set_px(grid, 15, y, INK)
    set_px(grid, 16, y, INK)
    set_px(grid, 15, y + 1, INK)
    set_px(grid, 16, y + 1, INK)


def draw_mouth_wavy(grid: list[list[int]], y: int = 16) -> None:
    set_px(grid, 14, y, INK)
    set_px(grid, 16, y, INK)
    set_px(grid, 18, y, INK)


def draw_cheek_dots(grid: list[list[int]], y: int = 14) -> None:
    set_px(grid, 10, y, INK)
    set_px(grid, 22, y, INK)


def draw_feet(grid: list[list[int]], y: int = 24) -> None:
    set_px(grid, 13, y, INK)
    set_px(grid, 14, y, INK)
    set_px(grid, 17, y, INK)
    set_px(grid, 18, y, INK)


def draw_arms(grid: list[list[int]], y: int = 17, raised: bool = False) -> None:
    if raised:
        set_px(grid, 8, y - 1, INK)
        set_px(grid, 8, y, INK)
        set_px(grid, 23, y - 1, INK)
        set_px(grid, 23, y, INK)
    else:
        set_px(grid, 9, y, INK)
        set_px(grid, 22, y, INK)


def draw_antenna(grid: list[list[int]], cx: int = 16, top: int = 6) -> None:
    set_px(grid, cx, top, INK)
    set_px(grid, cx, top + 1, INK)
    set_px(grid, cx - 1, top + 2, INK)
    set_px(grid, cx + 1, top + 2, INK)


def draw_sparkle(grid: list[list[int]], x: int, y: int) -> None:
    set_px(grid, x, y, INK)
    set_px(grid, x - 1, y + 1, INK)
    set_px(grid, x + 1, y + 1, INK)


def draw_heart(grid: list[list[int]], x: int, y: int) -> None:
    set_px(grid, x + 1, y, INK)
    set_px(grid, x + 3, y, INK)
    set_px(grid, x, y + 1, INK)
    set_px(grid, x + 1, y + 1, INK)
    set_px(grid, x + 2, y + 1, INK)
    set_px(grid, x + 3, y + 1, INK)
    set_px(grid, x + 4, y + 1, INK)
    set_px(grid, x + 1, y + 2, INK)
    set_px(grid, x + 2, y + 2, INK)
    set_px(grid, x + 3, y + 2, INK)


def draw_zzz(grid: list[list[int]]) -> None:
    fill_rect(grid, 22, 4, 2, 1, INK)
    fill_rect(grid, 24, 5, 2, 1, INK)
    fill_rect(grid, 23, 7, 2, 1, INK)


def draw_halo(grid: list[list[int]]) -> None:
    draw_outline_ellipse(grid, 16, 5, 5, 1.5, thick=1)


def draw_creature(
    grid: list[list[int]],
    *,
    cy: float = 15,
    eyes: str = "dot",
    mouth: str = "smile",
    arms_raised: bool = False,
    cheeks: bool = True,
    antenna: bool = True,
) -> None:
    draw_outline_ellipse(grid, 16, cy, 7, 9, thick=2)
    if antenna:
        draw_antenna(grid, top=int(cy - 10))
    draw_feet(grid)
    draw_arms(grid, int(cy + 2), raised=arms_raised)
    if cheeks:
        draw_cheek_dots(grid, int(cy - 1))
    eye_y = int(cy - 3)
    if eyes == "dot":
        draw_dot_eyes(grid, eye_y)
    elif eyes == "happy":
        draw_eyes_happy(grid, eye_y - 1)
    elif eyes == "closed":
        draw_eyes_closed(grid, eye_y)
    elif eyes == "sleepy":
        draw_eyes_sleepy(grid, eye_y)
    elif eyes == "wide":
        draw_eyes_wide(grid, eye_y - 1)
    elif eyes == "sad":
        draw_eyes_sad(grid, eye_y)
    mouth_y = int(cy + 0)
    if mouth == "smile":
        draw_mouth_smile(grid, mouth_y)
    elif mouth == "grin":
        draw_mouth_grin(grid, mouth_y)
    elif mouth == "o":
        draw_mouth_o(grid, mouth_y)
    elif mouth == "wavy":
        draw_mouth_wavy(grid, mouth_y + 1)


def grid_to_frame(grid: list[list[int]]) -> Image.Image:
    img = Image.new("RGBA", (FRAME, FRAME), (0, 0, 0, 0))
    px = img.load()
    for y in range(FRAME):
        for x in range(FRAME):
            if grid[y][x]:
                px[x, y] = BLACK
    return img


def make_egg() -> list[list[int]]:
    g = blank()
    draw_outline_ellipse(g, 16, 16, 5.5, 7.5, thick=2)
    draw_dot_eyes(g, 14)
    draw_mouth_smile(g, 17)
    draw_heart(g, 22, 8)
    set_px(g, 13, 13, INK)
    set_px(g, 18, 18, INK)
    return g


def make_idle(open_: bool) -> list[list[int]]:
    g = blank()
    draw_creature(g, eyes="dot" if open_ else "happy", mouth="smile")
    return g


def make_happy() -> list[list[int]]:
    g = blank()
    draw_creature(g, eyes="happy", mouth="grin", arms_raised=True)
    draw_sparkle(g, 7, 6)
    draw_sparkle(g, 24, 5)
    return g


def make_hungry() -> list[list[int]]:
    g = blank()
    draw_creature(g, eyes="sad", mouth="o")
    set_px(g, 21, 15, INK)
    set_px(g, 22, 15, INK)
    return g


def make_eating_a() -> list[list[int]]:
    g = blank()
    draw_creature(g, eyes="happy", mouth="grin")
    set_px(g, 21, 15, INK)
    set_px(g, 22, 15, INK)
    set_px(g, 21, 16, INK)
    set_px(g, 22, 16, INK)
    return g


def make_eating_b() -> list[list[int]]:
    g = blank()
    draw_creature(g, eyes="dot", mouth="smile")
    set_px(g, 22, 16, INK)
    return g


def make_sleeping() -> list[list[int]]:
    g = blank()
    draw_creature(g, cy=16, eyes="sleepy", mouth="smile", cheeks=False, antenna=False)
    draw_zzz(g)
    return g


def make_sick() -> list[list[int]]:
    g = blank()
    draw_creature(g, eyes="sad", mouth="wavy")
    set_px(g, 22, 8, INK)
    set_px(g, 22, 9, INK)
    set_px(g, 23, 10, INK)
    return g


def make_playful() -> list[list[int]]:
    g = blank()
    draw_creature(g, cy=14, eyes="wide", mouth="grin", arms_raised=True)
    draw_heart(g, 6, 7)
    return g


def make_attention() -> list[list[int]]:
    g = blank()
    draw_creature(g, eyes="wide", mouth="o", arms_raised=True)
    draw_heart(g, 15, 3)
    return g


def make_dead() -> list[list[int]]:
    g = blank()
    draw_creature(g, cy=16, eyes="happy", mouth="smile", cheeks=False, antenna=False)
    draw_halo(g)
    return g


def draw_star_patch(grid: list[list[int]], cx: int = 16, cy: int = 18) -> None:
    """Small chest star — cosmetic overlay."""
    set_px(grid, cx, cy - 1, INK)
    set_px(grid, cx - 1, cy, INK)
    set_px(grid, cx, cy, INK)
    set_px(grid, cx + 1, cy, INK)
    set_px(grid, cx, cy + 1, INK)


def make_overlay_sparkle_collar() -> list[list[int]]:
    g = blank()
    for x in range(11, 22):
        set_px(g, x, 19, INK)
    set_px(g, 10, 20, INK)
    set_px(g, 22, 20, INK)
    draw_sparkle(g, 8, 17)
    draw_sparkle(g, 24, 17)
    return g


def make_overlay_star_patch() -> list[list[int]]:
    g = blank()
    draw_star_patch(g, 16, 18)
    return g


def make_overlay_aurora_aura(wide: bool = False) -> list[list[int]]:
    g = blank()
    rx = 9.5 if wide else 8.5
    ry = 11.5 if wide else 10.5
    draw_outline_ellipse(g, 16, 15, rx, ry, thick=1)
    if wide:
        draw_outline_ellipse(g, 16, 15, rx - 0.8, ry - 0.8, thick=1)
    return g


def main() -> None:
    frames = [
        make_egg(),
        make_idle(True),
        make_idle(False),
        make_happy(),
        make_hungry(),
        make_eating_a(),
        make_eating_b(),
        make_sleeping(),
        make_sick(),
        make_playful(),
        make_attention(),
        make_dead(),
    ]
    overlays = [
        make_overlay_sparkle_collar(),
        make_overlay_star_patch(),
        make_overlay_aurora_aura(wide=False),
    ]
    atlas = Image.new("RGBA", (FRAME * COLS, FRAME * 2), (0, 0, 0, 0))
    for i, grid in enumerate(frames):
        atlas.paste(grid_to_frame(grid), (i * FRAME, 0))
    for i, grid in enumerate(overlays):
        atlas.paste(grid_to_frame(grid), (i * FRAME, FRAME))

    out = os.path.join(
        os.path.dirname(__file__),
        "..",
        "feature",
        "pet",
        "src",
        "main",
        "res",
        "drawable-nodpi",
        "nibbli_sprites.png",
    )
    out = os.path.normpath(out)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    atlas.save(out)
    print(f"Saved {out} ({atlas.size[0]}x{atlas.size[1]})")


if __name__ == "__main__":
    main()
