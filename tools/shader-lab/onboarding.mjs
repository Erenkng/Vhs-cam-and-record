// Renders the onboarding samples: the drawn beach scene (scene.js) through every look and the
// camcorder effects, with the exact GLSL the app ships, as WebP files the app bundles.
//
//   node tools/shader-lab/onboarding.mjs [out-dir]
//
// Without out-dir the files go to app/src/main/res/drawable-nodpi.

import fs from 'node:fs';
import path from 'node:path';
import { openLab, repo } from './runner.mjs';

const outDir = path.resolve(process.argv[2] ?? path.join(repo, 'app/src/main/res/drawable-nodpi'));
fs.mkdirSync(outDir, { recursive: true });

const { page, close } = await openLab();
await page.addScriptTag({ url: '/tools/shader-lab/scene.js', type: 'module' });
await page.waitForFunction(() => window.sceneDataUrl);
const scene = await page.evaluate(() => window.sceneDataUrl(1600, 1200, 1998));
if (process.env.SCENE_PNG) fs.writeFileSync(process.env.SCENE_PNG, Buffer.from(scene.split(',')[1], 'base64'));

const osd = {
  timeText: '6:42 PM', dateText: 'AUG.14 1998', play: true, counter: '0:12:07',
  ccdText: '2004/08/14 18:42', filmText: "'98 8 14", cctvText: '14-08-1998 18:42:07',
};
const samples = [
  { name: 'vhs', look: 'VHS', speed: 'SP' },
  { name: 'ccd', look: 'CCD' },
  { name: 'film', look: 'FILM', seed: 0.81 },
  { name: 'instant', look: 'POLAROID' },
  // 8-bit pixels are crisp only at whole multiples of 128.
  { name: 'gameboy', look: 'GAMEBOY', outWidth: 512 },
  { name: 'webcam', look: 'WEBCAM' },
  { name: 'cctv', look: 'CCTV' },
  { name: 'super8', look: 'SUPER8' },
  { name: 'disposable', look: 'DISPOSABLE' },
  { name: 'gold', look: 'GOLD' },
  { name: 'portrait', look: 'PORTRAIT' },
  { name: 'cine', look: 'CINE' },
  { name: 'red_grain', look: 'RED_GRAIN' },
  { name: 'redscale', look: 'REDSCALE' },
  { name: 'vhs_night', look: 'VHS', effect: 'NIGHT', overrides: { lumaNoise: 0.08 } },
  { name: 'vhs_sepia', look: 'VHS', effect: 'SEPIA' },
  { name: 'vhs_mono', look: 'VHS', effect: 'MONO' },
  { name: 'vhs_mosaic', look: 'VHS', mosaic: 12 },
];
for (const sample of samples) {
  const url = await page.evaluate((opts) => window.renderToDataUrl(opts), {
    imageUrl: scene, outWidth: 640, time: 3.0, frame: 90, osd, ...sample, format: 'image/webp', quality: 0.8,
  });
  const file = path.join(outDir, `onboarding_${sample.name}.webp`);
  fs.writeFileSync(file, Buffer.from(url.split(',')[1], 'base64'));
  console.log('wrote', path.relative(repo, file), fs.statSync(file).size, 'bytes');
}
await close();
