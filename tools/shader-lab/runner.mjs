// Serves the repository to a headless Chromium (WebGL2 on SwiftShader) and opens the lab page.
//
// Playwright must be resolvable (it is preinstalled in the CI/agent images; elsewhere run
// `npm i -D playwright` first).

import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { chromium } = require('playwright');

export const here = path.dirname(fileURLToPath(import.meta.url));
export const repo = path.resolve(here, '../..');

const TYPES = {
  '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript',
  '.glsl': 'text/plain', '.frag': 'text/plain', '.vert': 'text/plain',
  '.ttf': 'font/ttf', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg',
};

/**
 * Opens tools/shader-lab/lab.html. Files under [imagesDir] are served at /__images/.
 * Returns the page and a function that shuts everything down.
 */
export async function openLab({ imagesDir = path.join(here, 'images') } = {}) {
  const server = http.createServer((req, res) => {
    const url = decodeURIComponent(new URL(req.url, 'http://x').pathname);
    const file = url.startsWith('/__images/')
      ? path.join(imagesDir, url.slice('/__images/'.length))
      : path.join(repo, url);
    const base = url.startsWith('/__images/') ? imagesDir : repo;
    if (!file.startsWith(base) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
      res.writeHead(404).end();
      return;
    }
    res.writeHead(200, { 'Content-Type': TYPES[path.extname(file)] ?? 'application/octet-stream' });
    fs.createReadStream(file).pipe(res);
  });
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const port = server.address().port;

  const browser = await chromium.launch({
    args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'],
  });
  const page = await browser.newPage();
  page.on('console', (m) => { if (m.type() === 'error') console.error('[page]', m.text()); });
  await page.goto(`http://127.0.0.1:${port}/tools/shader-lab/lab.html`);
  await page.waitForFunction(() => window.labReady || window.labError, null, { timeout: 60_000 });
  const labError = await page.evaluate(() => window.labError);
  if (labError) {
    await browser.close();
    server.close();
    throw new Error(labError);
  }
  return {
    page,
    close: async () => {
      await browser.close();
      server.close();
    },
  };
}
