// Renders the look dial's round thumbnails (thumbs.js): a summer day through every look,
// cropped square, as WebP files the app bundles.
//
//   node tools/shader-lab/thumbnails.mjs [out-dir]
//
// Without out-dir the files go to app/src/main/res/drawable-nodpi.

import fs from 'node:fs';
import path from 'node:path';
import { openLab, repo } from './runner.mjs';

const outDir = path.resolve(process.argv[2] ?? path.join(repo, 'app/src/main/res/drawable-nodpi'));
fs.mkdirSync(outDir, { recursive: true });

const { page, close } = await openLab();
await page.addScriptTag({ url: '/tools/shader-lab/thumbs.js', type: 'module' });
await page.waitForFunction(() => window.thumbnailDataUrl);

const osd = { receiptDate: '14.08.1998  18:42' };
const thumbs = [
  { name: 'vhs', look: 'VHS', mark: { text: 'PLAY ▶', top: true } },
  // Looks known by their frame show all of it: the gate, the fisheye's hood, the tube.
  { name: 'super8', look: 'SUPER8', fit: 'contain' },
  { name: 'minidv', look: 'MINIDV', fit: 'contain' },
  { name: 'nightshot', look: 'NIGHTSHOT' },
  { name: 'crt', look: 'CRT', fit: 'contain', outWidth: 960 },
  { name: 'film', look: 'FILM', seed: 0.81, mark: { film: "'98" } },
  // Film stocks close in on what tells them apart: golden light, soft pastels, the red grain;
  // cinema film shows the whole scene for its teal shadows and glowing sun.
  { name: 'gold', look: 'GOLD', zoom: 1.5, focus: [0.6, 0.35] },
  { name: 'portrait', look: 'PORTRAIT', zoom: 1.5, focus: [0.55, 0.55] },
  { name: 'cine', look: 'CINE' },
  { name: 'red_grain', look: 'RED_GRAIN', zoom: 2.6, focus: [0.45, 0.25] },
  { name: 'redscale', look: 'REDSCALE' },
  { name: 'silver', look: 'SILVER' },
  { name: 'aerochrome', look: 'AEROCHROME' },
  { name: 'cross', look: 'CROSS' },
  { name: 'slide', look: 'SLIDE', fit: 'contain' },
  { name: 'disposable', look: 'DISPOSABLE' },
  // The thick bottom edge of the card is what makes it an instant photo.
  { name: 'instant', look: 'POLAROID', anchor: 'bottom' },
  { name: 'ccd', look: 'CCD', mark: { text: '2004', color: '#FFB238' } },
  { name: 'mavica', look: 'MAVICA' },
  { name: 'gameboy', look: 'GAMEBOY', outWidth: 256 },
  { name: 'webcam', look: 'WEBCAM' },
  { name: 'flip', look: 'FLIP' },
  // The receipt's header shows it is printed on till roll.
  { name: 'thermal', look: 'THERMAL', outWidth: 384, anchor: 'top', zoom: 1.15 },
  { name: 'cctv', look: 'CCTV', mark: { text: 'CAM 1', top: true } },
];
for (const thumb of thumbs) {
  const url = await page.evaluate((opts) => window.thumbnailDataUrl(opts), { time: 3.0, frame: 90, osd, ...thumb });
  const file = path.join(outDir, `thumb_${thumb.name}.webp`);
  fs.writeFileSync(file, Buffer.from(url.split(',')[1], 'base64'));
  console.log('wrote', path.relative(repo, file), fs.statSync(file).size, 'bytes');
}
await close();
