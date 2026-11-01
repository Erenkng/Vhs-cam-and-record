// Renders the README's pictures (tools/readme/*.html) into docs/images as WebP, at twice the
// CSS size so they stay sharp: the banner, the looks sheet and, given a folder of the CI's
// screenshots (refs/screenshots/pr-<n>), a strip of phone screens.
//
//   node tools/readme/render.mjs [screenshots-dir]

import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { openLab, repo } from '../shader-lab/runner.mjs';

const out = path.join(repo, 'docs/images');
fs.mkdirSync(out, { recursive: true });
const shots = process.argv[2];

const { page: lab, close } = await openLab();
const base = lab.url().replace('/tools/shader-lab/lab.html', '');
const page = await lab.context().browser().newPage({ deviceScaleFactor: 2 });
page.on('console', (m) => { if (m.type() === 'error') console.error('[page]', m.text()); });

/** Screenshots [url] at [width] CSS pixels (the whole page unless [height] is given). */
async function render(name, url, width, height) {
  await page.setViewportSize({ width, height: height ?? 800 });
  await page.goto(`${base}${url}`);
  await page.evaluate(() => document.fonts.ready);
  await page.waitForFunction(() => [...document.images].every((i) => i.complete));
  const png = path.join(out, `${name}.png`);
  await page.screenshot({ path: png, fullPage: height == null });
  const webp = png.replace(/\.png$/, '.webp');
  execFileSync('python3', ['-c', 'import sys; from PIL import Image; Image.open(sys.argv[1]).save(sys.argv[2], quality=86, method=6)', png, webp]);
  fs.unlinkSync(png);
  console.log('wrote', path.relative(repo, webp));
}

await render('banner', '/tools/readme/banner.html', 1280, 440);
await render('looks', '/tools/readme/looks.html', 1280);
if (shots) {
  const list = ['03_camera_photo_film', '16_look_browser', '13_onboarding_2', '04_gallery'];
  const images = list.map((n) => `<img src="data:image/png;base64,${fs.readFileSync(path.join(shots, `${n}.png`)).toString('base64')}">`);
  const strip = path.join(repo, 'tools/readme/.screens.html');
  fs.writeFileSync(strip, `<!doctype html><meta charset="utf-8"><style>
    html, body { margin: 0; } body { width: 1280px; background: #08100c; }
    .row { display: flex; gap: 28px; justify-content: center; padding: 30px 20px;
      background: radial-gradient(ellipse 60% 60% at 50% 40%, rgba(0, 168, 107, .25), transparent 70%); }
    img { width: 280px; border-radius: 30px; box-shadow: 0 0 0 7px #1b1f1d, 0 22px 40px rgba(0, 0, 0, .6); }
  </style><div class="row">${images.join('')}</div>`);
  try {
    await render('screens', '/tools/readme/.screens.html', 1280);
  } finally {
    fs.unlinkSync(strip);
  }
}
await close();
