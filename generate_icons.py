#!/usr/bin/env python3
"""
Generate simple placeholder launcher icons for the app.
"""
import os
import struct
import zlib

def create_png(width, height, bg_color, fg_text_color):
    """Create a minimal PNG file with gradient-like solid color."""
    # PNG signature
    sig = b'\x89PNG\r\n\x1a\n'
    
    def make_chunk(ctype, data):
        c = ctype + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)
    
    # IHDR
    ihdr = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)
    
    # IDAT - raw pixel data with filter bytes
    r, g, b = bg_color
    rows = []
    for y in range(height):
        row = b'\x00'  # filter type none
        for x in range(width):
            # Simple gradient effect
            cx, cy = width // 2, height // 2
            dist = ((x - cx)**2 + (y - cy)**2) ** 0.5
            max_dist = (cx**2 + cy**2) ** 0.5
            t = dist / max_dist
            # Blend from center color to edge color
            pr = int(r * (1 - t * 0.3))
            pg = int(g * (1 - t * 0.4))
            pb = int(b * (1 - t * 0.1))
            row += bytes([max(0,min(255,pr)), max(0,min(255,pg)), max(0,min(255,pb))])
        rows.append(row)
    
    raw = b''.join(rows)
    compressed = zlib.compress(raw, 6)
    
    png = sig
    png += make_chunk(b'IHDR', ihdr)
    png += make_chunk(b'IDAT', compressed)
    png += make_chunk(b'IEND', b'')
    return png

sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

res_dir = 'app/src/main/res'
# Purple-dominant background
bg = (42, 26, 78)    # deep purple
fg = (255, 140, 0)   # orange

for folder, size in sizes.items():
    path = os.path.join(res_dir, folder)
    os.makedirs(path, exist_ok=True)
    
    png_data = create_png(size, size, bg, fg)
    
    with open(os.path.join(path, 'ic_launcher.png'), 'wb') as f:
        f.write(png_data)
    with open(os.path.join(path, 'ic_launcher_round.png'), 'wb') as f:
        f.write(png_data)
    
    print(f"Created {folder}/ic_launcher.png ({size}x{size})")

print("Done!")
