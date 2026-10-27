// Renders every look over a folder of test images with headless Chromium (WebGL2), using the
// exact GLSL files the app ships.
//
//   node tools/shader-lab/render.mjs <images-dir> [out-dir]
//
// ONLY=GAMEBOY,NIGHT renders just the looks or extras whose look/tag is listed.

import fs from 'node:fs';
import path from 'node:path';
import { here, openLab } from './runner.mjs';

const imagesDir = path.resolve(process.argv[2] ?? path.join(here, 'images'));
const outDir = path.resolve(process.argv[3] ?? path.join(here, 'out'));
fs.mkdirSync(outDir, { recursive: true });

const { page, close } = await openLab({ imagesDir });

const osd = {
  timeText: '12:00 AM', dateText: 'JAN.01 1997', play: true, counter: '0:00:12',
  ccdText: '2003/06/14 15:42', filmText: "'98 6 14", cctvText: '26-10-2026 21:47:12',
};
const only = process.env.ONLY ? new Set(process.env.ONLY.split(',')) : null;
const wanted = (l) => !only || only.has(l.look) || (l.tag && only.has(l.tag));
const looks = [
  { look: 'VHS', speed: 'SP' }, { look: 'VHS', speed: 'EP' },
  { look: 'CCD' }, { look: 'FILM' }, { look: 'POLAROID' },
  // 8-bit pixels are crisp only at whole multiples of 128.
  { look: 'GAMEBOY', outWidth: 1024 }, { look: 'WEBCAM' }, { look: 'CCTV' },
].filter(wanted);
const images = fs.readdirSync(imagesDir).filter((f) => /\.(png|jpe?g)$/i.test(f));
for (const image of images) {
  for (const l of looks) {
    for (const time of l.look === 'VHS' ? [3.0, 41.0] : [3.0]) {
      const dataUrl = await page.evaluate((opts) => window.renderToDataUrl(opts), {
        imageUrl: `/__images/${image}`, outWidth: 960, ...l, time, frame: Math.round(time * 30), osd,
      });
      const name = `${path.parse(image).name}_${l.look}${l.speed ? '-' + l.speed : ''}_t${time}.png`;
      fs.writeFileSync(path.join(outDir, name), Buffer.from(dataUrl.split(',')[1], 'base64'));
      console.log('wrote', name);
    }
  }
}
// Extra frames that show the rarer artefacts: a tracking error band and light leaks.
const extras = [
  { look: 'VHS', speed: 'LP', time: 7.0, overrides: { tracking: 1.0 }, tag: 'tracking' },
  { look: 'FILM', seed: 0.81, tag: 'leak' },
  // Camcorder digital effects.
  { look: 'VHS', speed: 'SP', effect: 'NIGHT', overrides: { lumaNoise: 0.08 }, tag: 'NIGHT' },
  { look: 'VHS', speed: 'SP', effect: 'SEPIA', tag: 'SEPIA' },
  { look: 'VHS', speed: 'SP', effect: 'MONO', tag: 'MONO' },
  { look: 'VHS', speed: 'SP', mosaic: 12, tag: 'MOSAIC' },
  { look: 'VHS', speed: 'SP', mosaic: 3, tag: 'DZOOM' },
].filter(wanted);
for (const image of images.slice(0, 2)) {
  for (const e of extras) {
    const dataUrl = await page.evaluate((opts) => window.renderToDataUrl(opts), {
      imageUrl: `/__images/${image}`, time: 3.0, frame: 90, osd, outWidth: 960, ...e,
    });
    const name = `${path.parse(image).name}_${e.look}_${e.tag}.png`;
    fs.writeFileSync(path.join(outDir, name), Buffer.from(dataUrl.split(',')[1], 'base64'));
    console.log('wrote', name);
  }
}
await close();
