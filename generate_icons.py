#!/usr/bin/env python3
"""
Generate legacy launcher icons (PNG) for Android app.
Creates ic_launcher.png and ic_launcher_round.png for each density.
"""

from PIL import Image, ImageDraw
import os

# Icon sizes for each density
DENSITIES = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

# Colors from the app
BACKGROUND_COLOR = (103, 80, 164)  # #6750A4
WHITE = (255, 255, 255)
DARK = (28, 27, 31)  # #1C1B1F


def draw_qr_icon(draw, size, offset_x=0, offset_y=0):
    """Draw the QR code icon pattern."""
    # The icon is designed for 108dp canvas with 54dp content centered
    # Scale factor based on target size
    scale = size / 108.0

    # Content is centered in the 108dp canvas, starting at (27, 27)
    base_x = 27 * scale + offset_x
    base_y = 27 * scale + offset_y

    def rect(x, y, w, h, color):
        draw.rectangle(
            [base_x + x * scale, base_y + y * scale,
             base_x + (x + w) * scale - 1, base_y + (y + h) * scale - 1],
            fill=color
        )

    # Outer white frame
    rect(0, 0, 54, 54, WHITE)

    # Top-left corner pattern
    rect(4, 4, 18, 18, DARK)
    rect(8, 8, 10, 10, WHITE)
    rect(10, 10, 6, 6, DARK)

    # Top-right corner pattern
    rect(32, 4, 18, 18, DARK)
    rect(36, 8, 10, 10, WHITE)
    rect(38, 10, 6, 6, DARK)

    # Bottom-left corner pattern
    rect(4, 32, 18, 18, DARK)
    rect(8, 36, 10, 10, WHITE)
    rect(10, 38, 6, 6, DARK)

    # Center pattern
    rect(26, 26, 8, 8, DARK)

    # Data patterns
    rect(26, 4, 4, 4, DARK)
    rect(26, 12, 4, 4, DARK)
    rect(4, 26, 4, 4, DARK)
    rect(12, 26, 4, 4, DARK)

    rect(32, 38, 4, 4, DARK)
    rect(38, 32, 4, 4, DARK)
    rect(44, 38, 4, 4, DARK)
    rect(38, 44, 4, 4, DARK)


def create_square_icon(size):
    """Create a square launcher icon."""
    img = Image.new('RGBA', (size, size), BACKGROUND_COLOR)
    draw = ImageDraw.Draw(img)
    draw_qr_icon(draw, size)
    return img


def create_round_icon(size):
    """Create a round launcher icon."""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Draw circular background
    draw.ellipse([0, 0, size - 1, size - 1], fill=BACKGROUND_COLOR)

    # Draw QR code icon
    draw_qr_icon(draw, size)

    # Create circular mask
    mask = Image.new('L', (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.ellipse([0, 0, size - 1, size - 1], fill=255)

    # Apply mask
    img.putalpha(mask)

    return img


def main():
    base_path = os.path.dirname(os.path.abspath(__file__))
    res_path = os.path.join(base_path, 'app', 'src', 'main', 'res')

    for density, size in DENSITIES.items():
        output_dir = os.path.join(res_path, density)
        os.makedirs(output_dir, exist_ok=True)

        # Create square icon
        square_icon = create_square_icon(size)
        square_path = os.path.join(output_dir, 'ic_launcher.png')
        square_icon.save(square_path, 'PNG')
        print(f"Created: {square_path}")

        # Create round icon
        round_icon = create_round_icon(size)
        round_path = os.path.join(output_dir, 'ic_launcher_round.png')
        round_icon.save(round_path, 'PNG')
        print(f"Created: {round_path}")

    print("\nDone! Legacy icons generated successfully.")


if __name__ == '__main__':
    main()
