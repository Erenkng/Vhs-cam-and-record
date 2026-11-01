// The look dial's round thumbnails: a bright summer day (blue sky, a sun to glow and smear,
// green leaves for infrared to turn pink, a red roof and flowers), drawn from scratch, through
// every look, cropped square, and for the looks known by their on-screen text, that text.
// Used by thumbnails.mjs.

import { drawFilmStamp } from './osd.js';

function rng(seed) {
  let s = seed >>> 0;
  return () => {
    s = (s + 0x6d2b79f5) >>> 0;
    let t = s;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function drawDayScene(canvas, seed = 2004) {
  const ctx = canvas.getContext('2d');
  const w = canvas.width;
  const h = canvas.height;
  const rand = rng(seed);
  const horizon = h * 0.62;

  const sky = ctx.createLinearGradient(0, 0, 0, horizon);
  sky.addColorStop(0, '#2c64c0');
  sky.addColorStop(1, '#a8d8f4');
  ctx.fillStyle = sky;
  ctx.fillRect(0, 0, w, horizon);

  // The sun, high and bright, with its glare.
  const sunX = w * 0.66;
  const sunY = h * 0.25;
  const sunR = h * 0.075;
  const glare = ctx.createRadialGradient(sunX, sunY, sunR * 0.6, sunX, sunY, sunR * 4);
  glare.addColorStop(0, 'rgba(255, 252, 230, 0.8)');
  glare.addColorStop(1, 'rgba(255, 252, 230, 0)');
  ctx.fillStyle = glare;
  ctx.fillRect(0, 0, w, horizon);
  ctx.fillStyle = '#fffef4';
  ctx.beginPath();
  ctx.arc(sunX, sunY, sunR, 0, Math.PI * 2);
  ctx.fill();

  // A fair-weather cloud.
  ctx.fillStyle = '#ffffff';
  for (const [cx, cy, r] of [[0.24, 0.2, 0.06], [0.3, 0.17, 0.08], [0.37, 0.2, 0.06], [0.3, 0.22, 0.06]]) {
    ctx.beginPath();
    ctx.arc(w * cx, h * cy, h * r, 0, Math.PI * 2);
    ctx.fill();
  }

  // Hills, the far one bluer.
  ctx.fillStyle = '#4c8d6a';
  ctx.beginPath();
  ctx.moveTo(0, horizon);
  ctx.quadraticCurveTo(w * 0.25, h * 0.48, w * 0.55, horizon - h * 0.02);
  ctx.quadraticCurveTo(w * 0.8, h * 0.5, w, horizon - h * 0.04);
  ctx.lineTo(w, horizon + h * 0.05);
  ctx.lineTo(0, horizon + h * 0.05);
  ctx.closePath();
  ctx.fill();
  const meadow = ctx.createLinearGradient(0, horizon, 0, h);
  meadow.addColorStop(0, '#6cbf4f');
  meadow.addColorStop(1, '#2f7d32');
  ctx.fillStyle = meadow;
  ctx.fillRect(0, horizon, w, h - horizon);

  // A white house with a red roof.
  const hx = w * 0.6;
  const hy = horizon - h * 0.02;
  const hw = w * 0.12;
  const hh = h * 0.1;
  ctx.fillStyle = '#f4f1ea';
  ctx.fillRect(hx, hy - hh, hw, hh);
  ctx.fillStyle = '#d63a3a';
  ctx.beginPath();
  ctx.moveTo(hx - hw * 0.1, hy - hh);
  ctx.lineTo(hx + hw / 2, hy - hh * 1.75);
  ctx.lineTo(hx + hw * 1.1, hy - hh);
  ctx.closePath();
  ctx.fill();
  ctx.fillStyle = '#2b3a55';
  ctx.fillRect(hx + hw * 0.15, hy - hh * 0.7, hw * 0.22, hh * 0.32);
  ctx.fillRect(hx + hw * 0.62, hy - hh * 0.62, hw * 0.2, hh * 0.62);

  // A big round tree, lit from the right.
  const tx = w * 0.36;
  const ty = h * 0.42;
  const tr = h * 0.17;
  ctx.fillStyle = '#6b4429';
  ctx.fillRect(tx - h * 0.02, ty, h * 0.04, horizon - ty + h * 0.06);
  for (const [dx, dy, r, c] of [
    [0, 0, 1, '#2f7d3a'], [-0.55, 0.25, 0.65, '#2a6f34'], [0.55, 0.2, 0.7, '#3d9144'],
    [0.2, -0.45, 0.6, '#45a046'], [-0.3, -0.35, 0.55, '#3a8a40'], [0.35, -0.1, 0.45, '#6cc25a'],
  ]) {
    ctx.fillStyle = c;
    ctx.beginPath();
    ctx.arc(tx + dx * tr, ty + dy * tr, tr * r, 0, Math.PI * 2);
    ctx.fill();
  }

  // Red and yellow flowers in the grass.
  for (let i = 0; i < 90; i++) {
    const fx = rand() * w;
    const t = rand();
    const fy = horizon + h * 0.08 + t * (h - horizon - h * 0.08);
    ctx.fillStyle = rand() < 0.6 ? '#e63946' : '#ffd166';
    ctx.beginPath();
    ctx.arc(fx, fy, h * (0.006 + t * 0.012), 0, Math.PI * 2);
    ctx.fill();
  }

  const image = ctx.getImageData(0, 0, w, h);
  const d = image.data;
  for (let i = 0; i < d.length; i += 4) {
    const n = (rand() - 0.5) * 8;
    d[i] = Math.min(255, Math.max(0, d[i] + n));
    d[i + 1] = Math.min(255, Math.max(0, d[i + 1] + n));
    d[i + 2] = Math.min(255, Math.max(0, d[i + 2] + n));
  }
  ctx.putImageData(image, 0, 0);
}

function outlined(ctx, text, x, y, size, fill) {
  ctx.font = `${size}px VT323`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'alphabetic';
  ctx.lineJoin = 'round';
  ctx.lineWidth = Math.max(1, size * 0.14);
  ctx.strokeStyle = 'rgba(0,0,0,0.85)';
  ctx.strokeText(text, x, y);
  ctx.fillStyle = fill;
  ctx.fillText(text, x, y);
}

/** The text a look is known by, placed inside the round crop. */
function drawMark(ctx, s, mark) {
  if (!mark) return;
  if (mark.film) {
    // The orange data back date, centred near the bottom.
    const dh = s * 0.15;
    const hh = dh / 0.045;
    const ww = (s / 2 + dh) / 0.94;
    ctx.save();
    ctx.translate(0, s * 0.8 - hh * 0.93);
    drawFilmStamp(ctx, ww, hh, mark.film);
    ctx.restore();
    return;
  }
  outlined(ctx, mark.text, s / 2, s * (mark.top ? 0.32 : 0.82), s * 0.2, mark.color ?? '#FFFFFF');
}

/**
 * Renders [look] on the day scene and returns a [size] px square thumbnail as a data URL.
 *
 * By default the square is the middle of the result. [fit] 'contain' shows all of it on black
 * instead (for looks whose frame is the point: a fisheye, a tube, a mount); [zoom] and [focus]
 * (fractions of the result, from the top left) close in on a detail like a glow or the grain;
 * [anchor] 'top' or 'bottom' shows that end of a tall result (a receipt, an instant photo).
 */
window.thumbnailDataUrl = async ({
  look, size = 192, anchor = 'centre', fit = 'cover', zoom = 1, focus = null, mark, outWidth = 480, ...opts
}) => {
  const scene = document.createElement('canvas');
  scene.width = 1200;
  scene.height = 900;
  drawDayScene(scene);
  const rendered = await window.renderCanvas({ ...opts, look, imageUrl: scene.toDataURL('image/png'), outWidth });
  const out = document.createElement('canvas');
  out.width = size;
  out.height = size;
  const ctx = out.getContext('2d');
  ctx.imageSmoothingQuality = 'high';
  if (fit === 'contain') {
    const scale = size / Math.max(rendered.width, rendered.height);
    const w = rendered.width * scale;
    const h = rendered.height * scale;
    ctx.fillStyle = '#000000';
    ctx.fillRect(0, 0, size, size);
    ctx.drawImage(rendered, (size - w) / 2, (size - h) / 2, w, h);
  } else {
    const side = Math.min(rendered.width, rendered.height) / zoom;
    const clamp = (v, max) => Math.min(Math.max(v, 0), max);
    let sx = (rendered.width - side) / 2;
    let sy = (rendered.height - side) / 2;
    if (focus) {
      sx = clamp(focus[0] * rendered.width - side / 2, rendered.width - side);
      sy = clamp(focus[1] * rendered.height - side / 2, rendered.height - side);
    }
    if (anchor === 'top') sy = 0;
    if (anchor === 'bottom') sy = rendered.height - side;
    ctx.drawImage(rendered, sx, sy, side, side, 0, 0, size, size);
  }
  drawMark(ctx, size, mark);
  return out.toDataURL('image/webp', 0.86);
};

/**
 * A double exposure the way the app makes one: the beach and the summer day, each at 0.82 of
 * its light, screen-blended, then developed as 35 mm film. Needs scene.js loaded too.
 */
window.doubleExposureDataUrl = async ({ outWidth = 640, osd = {} } = {}) => {
  const canvas = document.createElement('canvas');
  canvas.width = 1600;
  canvas.height = 1200;
  const ctx = canvas.getContext('2d');
  const beach = new Image();
  beach.src = window.sceneDataUrl(1600, 1200, 1998);
  await beach.decode();
  const day = document.createElement('canvas');
  day.width = 1600;
  day.height = 1200;
  drawDayScene(day);
  ctx.filter = 'brightness(0.82)';
  ctx.drawImage(beach, 0, 0);
  ctx.globalCompositeOperation = 'screen';
  ctx.drawImage(day, 0, 0);
  const rendered = await window.renderCanvas({
    look: 'FILM', seed: 0.81, time: 3.0, frame: 90, osd, imageUrl: canvas.toDataURL('image/png'), outWidth,
  });
  return rendered.toDataURL('image/webp', 0.8);
};
