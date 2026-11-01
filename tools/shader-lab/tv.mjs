// Renders the TV export (assets/export/tv.*) on a landscape and an upright sample, for review.
//
//   node tools/shader-lab/tv.mjs out-dir

import fs from 'node:fs';
import path from 'node:path';
import { openLab, repo } from './runner.mjs';

const outDir = path.resolve(process.argv[2] ?? 'tv-out');
fs.mkdirSync(outDir, { recursive: true });

const { page, close } = await openLab();
await page.goto(page.url().replace('lab.html', 'tv.html'));
await page.waitForFunction(() => window.tvReady);
const samples = [
  { name: 'landscape', image: 'app/src/main/res/drawable-nodpi/onboarding_vhs.webp' },
  // An upright 3:4 clip, cut from the middle of the landscape sample.
  { name: 'upright', image: 'app/src/main/res/drawable-nodpi/onboarding_vhs.webp', crop: [0.21875, 0, 0.5625, 1] },
  // Frames that still lie on their side, as some decoders hand them over.
  { name: 'upright_rotated', image: 'app/src/main/res/drawable-nodpi/onboarding_minidv.webp', rotation: 90 },
  { name: 'square', image: 'app/src/main/res/drawable-nodpi/thumb_vhs.webp' },
];
for (const sample of samples) {
  const data = fs.readFileSync(path.join(repo, sample.image)).toString('base64');
  const url = await page.evaluate(
    (opts) => window.renderTv(opts),
    { imageUrl: `data:image/webp;base64,${data}`, rotation: sample.rotation ?? 0, crop: sample.crop ?? null },
  );
  const file = path.join(outDir, `tv_${sample.name}.png`);
  fs.writeFileSync(file, Buffer.from(url.split(',')[1], 'base64'));
  console.log('wrote', file);
}
await close();
