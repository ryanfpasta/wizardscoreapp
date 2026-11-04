"""Draws the original Wizard Scorekeeper icon: a fan of cards with a wizard-hat face card on a starry night."""
import math, os, random, sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = sys.argv[1]
S = 1024  # master canvas
SERIF = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"

INDIGO_C = (52, 36, 120)
INDIGO_E = (14, 10, 38)
GOLD = (242, 193, 78)
GOLD_D = (196, 140, 40)
CREAM = (251, 246, 233)
PURPLE = (110, 72, 200)
PURPLE_D = (66, 40, 140)


def background(size=S, stars=True):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    cx = cy = size / 2
    maxd = math.hypot(cx, cy)
    for y in range(size):
        for x in range(size):
            t = min(1.0, math.hypot(x - cx, y - cy * 0.8) / maxd)
            px[x, y] = tuple(int(INDIGO_C[i] + (INDIGO_E[i] - INDIGO_C[i]) * t) for i in range(3)) + (255,)
    if stars:
        d = ImageDraw.Draw(img)
        rnd = random.Random(7)
        for _ in range(70):
            x, y = rnd.uniform(0, size), rnd.uniform(0, size)
            r = rnd.choice([1.5, 2, 2.5, 3.5]) * size / 1024
            c = rnd.choice([(255, 255, 255, 200), (255, 230, 160, 220), (200, 190, 255, 160)])
            d.ellipse([x - r, y - r, x + r, y + r], fill=c)
    return img


def star(d, cx, cy, r, fill, points=5, inner=0.45, rot=-90):
    pts = []
    for i in range(points * 2):
        rr = r if i % 2 == 0 else r * inner
        a = math.radians(rot + i * 180 / points)
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    d.polygon(pts, fill=fill)


def card_layer(kind, w=360, h=520):
    """Return an RGBA image of one card, centered on a big transparent canvas."""
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    x0, y0 = (S - w) / 2, (S - h) / 2
    x1, y1 = x0 + w, y0 + h
    rad = 34
    # shadow is added later
    if kind == "back":
        d.rounded_rectangle([x0, y0, x1, y1], rad, fill=GOLD)
        d.rounded_rectangle([x0 + 12, y0 + 12, x1 - 12, y1 - 12], rad - 10, fill=PURPLE_D)
        # diamond lattice
        step = 46
        for gy in range(int(y0) + 30, int(y1) - 20, step):
            for gx in range(int(x0) + 30, int(x1) - 20, step):
                cx, cy = gx + step / 2 - 10, gy + step / 2 - 10
                if x0 + 30 < cx < x1 - 30 and y0 + 30 < cy < y1 - 30:
                    d.polygon([(cx, cy - 14), (cx + 10, cy), (cx, cy + 14), (cx - 10, cy)], fill=PURPLE)
    else:
        d.rounded_rectangle([x0, y0, x1, y1], rad, fill=GOLD)
        d.rounded_rectangle([x0 + 10, y0 + 10, x1 - 10, y1 - 10], rad - 8, fill=CREAM)
        f = ImageFont.truetype(SERIF, 92)
        d.text((x0 + 30, y0 + 20), "W", font=f, fill=PURPLE_D)
        # upside-down corner letter
        tmp = Image.new("RGBA", (120, 120), (0, 0, 0, 0))
        ImageDraw.Draw(tmp).text((8, 0), "W", font=f, fill=PURPLE_D)
        tmp = tmp.rotate(180)
        layer.alpha_composite(tmp, (int(x1 - 30 - 112), int(y1 - 20 - 118)))
        # wizard hat in the middle
        cx, base = S / 2, y0 + h * 0.70
        tip = (cx + 46, y0 + h * 0.22)
        d.polygon([(cx - 105, base), tip, (cx + 105, base)], fill=PURPLE)
        d.polygon([(cx + 10, base), tip, (cx + 105, base)], fill=PURPLE_D)
        d.polygon([(cx - 92, base - 12), (cx + 92, base - 12), (cx + 80, base - 50), (cx - 80, base - 50)], fill=GOLD)
        d.ellipse([cx - 150, base - 20, cx + 150, base + 34], fill=PURPLE_D)
        star(d, cx - 22, y0 + h * 0.47, 22, GOLD)
        star(d, cx + 30, y0 + h * 0.38, 13, GOLD)
        star(d, tip[0], tip[1], 16, GOLD)
    return layer


def with_shadow(layer):
    a = layer.split()[3]
    sh = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    sh.putalpha(a.point(lambda v: v * 0.55))
    sh = sh.filter(ImageFilter.GaussianBlur(14))
    out = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    out.alpha_composite(sh, (10, 16))
    out.alpha_composite(layer)
    return out


def fan(scale=1.0):
    """The card fan, centered on a transparent S x S canvas."""
    canvas = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    pivot_dy = 330
    for kind, ang, dx in (("back", 24, -20), ("back", -24, 20), ("face", 0, 0)):
        c = with_shadow(card_layer(kind))
        # rotate around a point below the card so the cards fan out
        big = Image.new("RGBA", (S, S + 2 * pivot_dy), (0, 0, 0, 0))
        big.alpha_composite(c, (0, 0))
        big = big.rotate(ang, resample=Image.BICUBIC, center=(S / 2, S / 2 + pivot_dy))
        canvas.alpha_composite(big.crop((0, 0, S, S)), (dx, 0))
    # small sparkles
    d = ImageDraw.Draw(canvas)
    star(d, 190, 210, 34, GOLD, points=4, inner=0.3)
    star(d, 835, 250, 24, GOLD, points=4, inner=0.3)
    star(d, 860, 780, 18, (255, 255, 255, 230), points=4, inner=0.3)
    if scale != 1.0:
        n = int(S * scale)
        small = canvas.resize((n, n), Image.LANCZOS)
        canvas = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        canvas.alpha_composite(small, ((S - n) // 2, (S - n) // 2 + int(S * 0.02)))
    return canvas


def save(img, path, size):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.resize((size, size), Image.LANCZOS).save(path)


def main():
    bg = background()
    art_full = fan(0.86)
    art_adaptive = fan(0.60)  # keep inside the 66/108 safe zone

    # legacy square-ish and round icons
    legacy = bg.copy(); legacy.alpha_composite(art_full)
    mask = Image.new("L", (S, S), 0); ImageDraw.Draw(mask).rounded_rectangle([0, 0, S - 1, S - 1], 200, fill=255)
    sq = Image.new("RGBA", (S, S), (0, 0, 0, 0)); sq.paste(legacy, (0, 0), mask)
    rmask = Image.new("L", (S, S), 0); ImageDraw.Draw(rmask).ellipse([0, 0, S - 1, S - 1], fill=255)
    rd = Image.new("RGBA", (S, S), (0, 0, 0, 0)); rd.paste(legacy, (0, 0), rmask)

    dens = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
    for name, m in dens.items():
        save(sq, f"{OUT}/res/mipmap-{name}/ic_launcher.png", int(48 * m))
        save(rd, f"{OUT}/res/mipmap-{name}/ic_launcher_round.png", int(48 * m))
        save(bg, f"{OUT}/res/mipmap-{name}/ic_launcher_bg.png", int(108 * m))
        save(art_adaptive, f"{OUT}/res/mipmap-{name}/ic_launcher_fg.png", int(108 * m))
    save(rd, f"{OUT}/res/drawable-nodpi/logo.png", 384)
    save(sq, f"{OUT}/icon_preview_512.png", 512)


main()
