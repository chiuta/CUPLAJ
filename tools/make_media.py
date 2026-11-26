"""Renders docs/media: 6 illustrative screens (webp) and 4 feature clips (mp4) from tools/scenes.html.
These are ILLUSTRATIONS built from the app's real concepts, not captures from a device."""
import asyncio, os, subprocess, shutil, glob
from playwright.async_api import async_playwright
from PIL import Image
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SC = "file://" + os.path.join(ROOT, "tools", "scenes.html"); OUT = os.path.join(ROOT, "docs", "media"); TMP = "/tmp/clips"
os.makedirs(OUT, exist_ok=True); shutil.rmtree(TMP, ignore_errors=True)
SHOTS = ["spectator", "actor", "control", "cues", "air", "poll"] + ["f%d" % i for i in range(20)] + ["e0", "e1", "e2", "e3", "e4", "e5"]
POSTER = {"party": "3", "cues": "4", "air": "5", "poll": "6"}
CLIPS = {"party": 13500, "cues": 13500, "air": 12000, "poll": 11000}
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(executable_path="/usr/bin/google-chrome", args=["--no-sandbox"])
        for n in SHOTS:
            c = await b.new_context(viewport={"width": 390, "height": 470}, device_scale_factor=2)
            pg = await c.new_page(); await pg.goto(SC + "?shot=" + n); await pg.wait_for_timeout(300)
            await pg.screenshot(path=f"/tmp/{n}.png"); await c.close()
            Image.open(f"/tmp/{n}.png").save(os.path.join(OUT, f"screen-{n}.webp"), quality=82)
        for n, ms in CLIPS.items():
            c = await b.new_context(viewport={"width": 390, "height": 700}, record_video_dir=TMP, record_video_size={"width": 390, "height": 700})
            pg = await c.new_page(); await pg.goto(SC + "?s=" + n); await pg.wait_for_timeout(ms)
            await pg.screenshot(path=f"/tmp/{n}-poster.png"); vp = await pg.video.path(); await c.close()
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", vp, "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "30", "-preset", "veryfast", "-movflags", "+faststart", "-an", os.path.join(OUT, f"clip-{n}.mp4")], check=True)
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-ss", POSTER[n], "-i", os.path.join(OUT, f"clip-{n}.mp4"), "-frames:v", "1", f"/tmp/{n}-poster.png"], check=True)
            Image.open(f"/tmp/{n}-poster.png").save(os.path.join(OUT, f"clip-{n}.webp"), quality=80)
        await b.close()
asyncio.run(main())
