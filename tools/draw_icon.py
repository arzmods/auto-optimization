import sys
from PIL import Image, ImageDraw
# Draws the mod icon as 32x32 pixel art and saves it scaled up.
#     python3 tools/draw_icon.py art/icon.png   ->  art/icon-512.png, art/icon-128.png
out = sys.argv[1]
S = 32
im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
OUT = (27, 28, 43, 255)
# background tile with a lighter top band, like a Minecraft block face
d.rounded_rectangle((0, 0, S - 1, S - 1), radius=5, fill=(58, 46, 110, 255), outline=OUT)
d.rectangle((2, 2, S - 3, 9), fill=(74, 60, 138, 255))
d.rounded_rectangle((0, 0, S - 1, S - 1), radius=5, outline=OUT)
# speech bubble (voice chat)
WHITE = (242, 242, 242, 255); SHADE = (214, 214, 224, 255)
d.rounded_rectangle((3, 5, 22, 20), radius=3, fill=WHITE, outline=OUT)
d.rectangle((4, 17, 21, 19), fill=SHADE)                  # shading on the bubble
d.rounded_rectangle((3, 5, 22, 20), radius=3, outline=OUT)
# tail: a small staircase going down-left, merged into the bubble's bottom edge
for y, (x0, x1) in enumerate([(7, 10), (6, 9), (5, 7), (5, 6)], start=20):
    d.line((x0 - 1, y, x1 + 1, y), fill=OUT)
    d.line((x0, y, x1, y), fill=SHADE)
d.line((4, 24, 6, 24), fill=OUT)
# beamed music notes
NOTE = (255, 183, 3, 255); NOTE_D = (204, 120, 0, 255)
d.rectangle((9, 8, 17, 9), fill=OUT)                      # beam
d.line((9, 8, 9, 15), fill=OUT); d.line((17, 8, 17, 14), fill=OUT)
d.ellipse((5, 13, 9, 17), fill=NOTE, outline=OUT)
d.ellipse((13, 12, 17, 16), fill=NOTE, outline=OUT)
d.point([(6, 16), (14, 15)], fill=NOTE_D)
# sound waves
WAVE = (76, 201, 240, 255)
d.arc((17, 9, 27, 17), -50, 50, fill=WAVE, width=1)
d.arc((16, 6, 30, 20), -45, 45, fill=WAVE, width=1)
for size in (512, 128):
    im.resize((size, size), Image.NEAREST).save(out.replace(".png", f"-{size}.png"))
