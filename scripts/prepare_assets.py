from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parents[1]
master = Image.open(root / 'branding/ww-transparent.png').convert('RGBA')
dest = root / 'app/res/drawable-nodpi'
dest.mkdir(parents=True, exist_ok=True)
# Crop unused transparent canvas for packaging; retain the mark's aspect ratio.
mask = master.getchannel('A').point(lambda alpha: 255 if alpha >= 128 else 0)
mark = master.crop(mask.getbbox())
mark.save(dest / 'ww_mark.png')
icon = Image.new('RGBA', (512, 512))
icon_mark = mark.copy()
icon_mark.thumbnail((416, 416), Image.Resampling.LANCZOS)
icon.alpha_composite(icon_mark, ((512-icon_mark.width)//2, (512-icon_mark.height)//2))
icon.save(dest / 'ww_icon.png')
banner = Image.new('RGBA', (320, 180), (250, 250, 254, 255))
banner_mark = mark.copy()
banner_mark.thumbnail((230, 130), Image.Resampling.LANCZOS)
banner.alpha_composite(banner_mark, ((320-banner_mark.width)//2, (180-banner_mark.height)//2))
banner.save(dest / 'ww_banner.png')
