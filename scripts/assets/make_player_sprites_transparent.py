#!/usr/bin/env python3
"""
Remove solid-color backgrounds from bundled player sprite PNGs.

The canonical PNGs live in apps/mobile/assets/source/. After running this script,
run sync_battle_assets.py so Android drawable and iOS Assets.xcassets are updated
from the source images.

Usage:
  ./scripts/assets/run.sh scripts/assets/make_player_sprites_transparent.py
  ./scripts/assets/run.sh scripts/assets/sync_battle_assets.py
  ./scripts/assets/run.sh scripts/assets/validate_assets.py
"""

from __future__ import annotations

import argparse
import sys
from collections import deque
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))

from lib.manifest import SOURCE_DIR, load_manifest  # noqa: E402


DEFAULT_TOLERANCE = 28
DEFAULT_FEATHER = 10


def color_distance_sq(a: tuple[int, int, int], b: tuple[int, int, int]) -> int:
    return sum((int(x) - int(y)) ** 2 for x, y in zip(a, b))


def average_corner_color(image: Image.Image, sample_size: int) -> tuple[int, int, int]:
    rgb = image.convert("RGB")
    width, height = rgb.size
    pixels = rgb.load()
    samples: list[tuple[int, int, int]] = []

    boxes = [
        (0, 0),
        (max(0, width - sample_size), 0),
        (0, max(0, height - sample_size)),
        (max(0, width - sample_size), max(0, height - sample_size)),
    ]

    for x0, y0 in boxes:
        for y in range(y0, min(y0 + sample_size, height)):
            for x in range(x0, min(x0 + sample_size, width)):
                samples.append(pixels[x, y])

    if not samples:
        return (255, 255, 255)

    return tuple(sum(channel) // len(samples) for channel in zip(*samples))  # type: ignore[return-value]


def background_mask_from_edges(
    image: Image.Image,
    bg_color: tuple[int, int, int],
    tolerance: int,
) -> set[tuple[int, int]]:
    """Flood-fill edge-connected pixels close to bg_color.

    This avoids punching holes in similarly colored parts inside the character.
    """
    rgb = image.convert("RGB")
    width, height = rgb.size
    pixels = rgb.load()
    threshold = tolerance * tolerance

    visited: set[tuple[int, int]] = set()
    q: deque[tuple[int, int]] = deque()

    for x in range(width):
        q.append((x, 0))
        q.append((x, height - 1))
    for y in range(height):
        q.append((0, y))
        q.append((width - 1, y))

    while q:
        x, y = q.popleft()
        if (x, y) in visited or not (0 <= x < width and 0 <= y < height):
            continue
        if color_distance_sq(pixels[x, y], bg_color) > threshold:
            continue

        visited.add((x, y))
        q.append((x + 1, y))
        q.append((x - 1, y))
        q.append((x, y + 1))
        q.append((x, y - 1))

    return visited


def transparentize(
    path: Path,
    tolerance: int,
    feather: int,
    sample_size: int,
    dry_run: bool,
) -> bool:
    image = Image.open(path).convert("RGBA")
    bg_color = average_corner_color(image, sample_size)
    mask = background_mask_from_edges(image, bg_color, tolerance)
    if not mask:
        print(f"skip {path.name}: no matching edge background detected")
        return False

    pixels = image.load()
    feather_threshold = (tolerance + feather) * (tolerance + feather)

    changed = False
    for x, y in mask:
        r, g, b, a = pixels[x, y]
        dist = color_distance_sq((r, g, b), bg_color)
        if dist <= tolerance * tolerance:
            new_alpha = 0
        elif feather > 0 and dist <= feather_threshold:
            # Softly fade near-background edge pixels to reduce a visible halo.
            new_alpha = int(255 * (dist - tolerance * tolerance) / max(1, feather_threshold - tolerance * tolerance))
        else:
            new_alpha = a

        if new_alpha != a:
            pixels[x, y] = (r, g, b, new_alpha)
            changed = True

    if changed and not dry_run:
        image.save(path)

    action = "would update" if dry_run else "updated"
    print(f"{action} {path.relative_to(SOURCE_DIR.parent)} bg={bg_color} pixels={len(mask)}")
    return changed


def collect_player_sprite_files() -> list[Path]:
    manifest = load_manifest()
    files: list[Path] = []
    for entry in manifest.get("battle_assets", []):
        if entry.get("kind") != "player":
            continue
        filename = entry.get("file")
        if not filename:
            continue
        path = SOURCE_DIR / filename
        if path.is_file():
            files.append(path)
    return sorted(files)


def main() -> int:
    parser = argparse.ArgumentParser(description="Make player sprite backgrounds transparent")
    parser.add_argument("--tolerance", type=int, default=DEFAULT_TOLERANCE)
    parser.add_argument("--feather", type=int, default=DEFAULT_FEATHER)
    parser.add_argument("--sample-size", type=int, default=12)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    files = collect_player_sprite_files()
    if not files:
        print("No player sprite PNGs found in source/.", file=sys.stderr)
        return 1

    changed = 0
    for path in files:
        if transparentize(path, args.tolerance, args.feather, args.sample_size, args.dry_run):
            changed += 1

    print(f"Done. {changed}/{len(files)} player sprite file(s) changed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
