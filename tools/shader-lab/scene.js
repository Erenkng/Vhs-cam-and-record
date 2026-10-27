// A summer-of-'98 beach at sunset, drawn from scratch so the onboarding samples need no
// third-party photo. Deterministic: the same seed always draws the same picture.

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

function palm(ctx, x, baseY, height, lean, rand) {
  const topX = x + lean * height;
  const topY = baseY - height;
  // Trunk: a tapered, slightly bent stroke.
  ctx.fillStyle = '#1b1022';
  ctx.beginPath();
  ctx.moveTo(x - height * 0.035, baseY);
  ctx.quadraticCurveTo(x + lean * height * 0.2, baseY - height * 0.55, topX - height * 0.012, topY);
  ctx.lineTo(topX + height * 0.012, topY);
  ctx.quadraticCurveTo(x + lean * height * 0.2 + height * 0.04, baseY - height * 0.55, x + height * 0.035, baseY);
  ctx.closePath();
  ctx.fill();
  // Fronds: drooping leaves with a few notches each.
  const fronds = 9;
  for (let i = 0; i < fronds; i++) {
    const angle = -Math.PI + (i / (fronds - 1)) * Math.PI + (rand() - 0.5) * 0.25;
    const length = height * (0.42 + rand() * 0.18);
    const droop = height * (0.18 + rand() * 0.12);
    const ex = topX + Math.cos(angle) * length;
    const ey = topY + Math.sin(angle) * length * 0.45 + droop;
    const cx = topX + Math.cos(angle) * length * 0.55;
    const cy = topY + Math.sin(angle) * length * 0.5 - height * 0.05;
    ctx.beginPath();
    ctx.moveTo(topX, topY);
    ctx.quadraticCurveTo(cx, cy - height * 0.03, ex, ey);
    ctx.quadraticCurveTo(cx, cy + height * 0.035, topX, topY + height * 0.01);
    ctx.fill();
    // Leaflets.
    ctx.strokeStyle = '#1b1022';
    ctx.lineWidth = Math.max(1, height * 0.006);
    for (let k = 1; k < 9; k++) {
      const t = k / 9;
      const px = (1 - t) * (1 - t) * topX + 2 * (1 - t) * t * cx + t * t * ex;
      const py = (1 - t) * (1 - t) * topY + 2 * (1 - t) * t * cy + t * t * ey;
      const leaf = height * 0.07 * (1 - t * 0.6);
      ctx.beginPath();
      ctx.moveTo(px, py);
      ctx.lineTo(px + Math.cos(angle + 1.9) * leaf, py + Math.abs(Math.sin(angle + 1.9)) * leaf + leaf * 0.6);
      ctx.moveTo(px, py);
      ctx.lineTo(px + Math.cos(angle - 1.9) * leaf, py + Math.abs(Math.sin(angle - 1.9)) * leaf + leaf * 0.6);
      ctx.stroke();
    }
  }
}

export function drawScene(canvas, seed = 1998) {
  const ctx = canvas.getContext('2d');
  const w = canvas.width;
  const h = canvas.height;
  const rand = rng(seed);
  const horizon = h * 0.6;

  // Sky.
  const sky = ctx.createLinearGradient(0, 0, 0, horizon);
  sky.addColorStop(0, '#20306e');
  sky.addColorStop(0.35, '#5b3a8c');
  sky.addColorStop(0.65, '#d9567a');
  sky.addColorStop(0.88, '#ff9a5a');
  sky.addColorStop(1, '#ffc46e');
  ctx.fillStyle = sky;
  ctx.fillRect(0, 0, w, horizon);

  // Sun and its glow.
  const sunX = w * 0.62;
  const sunY = horizon - h * 0.07;
  const sunR = h * 0.12;
  const glow = ctx.createRadialGradient(sunX, sunY, sunR * 0.5, sunX, sunY, sunR * 4.5);
  glow.addColorStop(0, 'rgba(255, 214, 140, 0.75)');
  glow.addColorStop(1, 'rgba(255, 140, 90, 0)');
  ctx.fillStyle = glow;
  ctx.fillRect(0, 0, w, horizon);
  const sun = ctx.createRadialGradient(sunX, sunY - sunR * 0.3, sunR * 0.1, sunX, sunY, sunR);
  sun.addColorStop(0, '#fffbe0');
  sun.addColorStop(0.7, '#ffe08a');
  sun.addColorStop(1, '#ffb45c');
  ctx.fillStyle = sun;
  ctx.beginPath();
  ctx.arc(sunX, sunY, sunR, 0, Math.PI * 2);
  ctx.fill();

  // Long soft clouds catching the light.
  ctx.save();
  ctx.filter = `blur(${Math.round(h * 0.008)}px)`;
  for (let i = 0; i < 7; i++) {
    const cy = h * (0.12 + rand() * 0.36);
    const cx = w * rand();
    const cw = w * (0.18 + rand() * 0.3);
    const ch = h * (0.012 + rand() * 0.022);
    ctx.fillStyle = cy > h * 0.3 ? 'rgba(255, 170, 150, 0.55)' : 'rgba(190, 140, 210, 0.5)';
    ctx.beginPath();
    ctx.ellipse(cx, cy, cw / 2, ch, 0, 0, Math.PI * 2);
    ctx.fill();
  }
  ctx.restore();

  // Sea with the sun's broken reflection.
  const sea = ctx.createLinearGradient(0, horizon, 0, h * 0.82);
  sea.addColorStop(0, '#6b3f7e');
  sea.addColorStop(1, '#25285a');
  ctx.fillStyle = sea;
  ctx.fillRect(0, horizon, w, h * 0.22);
  for (let i = 0; i < 70; i++) {
    const t = rand();
    const y = horizon + 2 + t * t * h * 0.21;
    const spread = sunR * (0.6 + t * 1.6);
    const x = sunX + (rand() - 0.5) * spread * 2;
    const lw = sunR * (0.25 + rand() * 0.9) * (1 - t * 0.4);
    ctx.fillStyle = `rgba(255, ${200 + Math.round(rand() * 40)}, 140, ${0.35 + rand() * 0.5})`;
    ctx.fillRect(x - lw / 2, y, lw, Math.max(1.5, h * 0.004));
  }
  // Wave lines.
  ctx.strokeStyle = 'rgba(255, 190, 200, 0.18)';
  ctx.lineWidth = Math.max(1, h * 0.002);
  for (let i = 0; i < 26; i++) {
    const y = horizon + h * 0.01 + rand() * h * 0.2;
    const x = rand() * w;
    ctx.beginPath();
    ctx.moveTo(x, y);
    ctx.lineTo(x + w * (0.04 + rand() * 0.12), y);
    ctx.stroke();
  }

  // Beach.
  const shore = h * 0.8;
  const sand = ctx.createLinearGradient(0, shore, 0, h);
  sand.addColorStop(0, '#e3a46a');
  sand.addColorStop(1, '#9c5f3a');
  ctx.fillStyle = sand;
  ctx.beginPath();
  ctx.moveTo(0, shore + h * 0.02);
  ctx.quadraticCurveTo(w * 0.5, shore - h * 0.025, w, shore + h * 0.015);
  ctx.lineTo(w, h);
  ctx.lineTo(0, h);
  ctx.closePath();
  ctx.fill();
  // Wet sand and foam along the waterline.
  ctx.strokeStyle = 'rgba(255, 236, 220, 0.7)';
  ctx.lineWidth = Math.max(2, h * 0.005);
  ctx.beginPath();
  ctx.moveTo(0, shore + h * 0.02);
  ctx.quadraticCurveTo(w * 0.5, shore - h * 0.025, w, shore + h * 0.015);
  ctx.stroke();

  // A surfboard stuck in the sand, and a beach umbrella.
  ctx.save();
  ctx.translate(w * 0.8, h * 0.93);
  ctx.rotate(0.12);
  const board = ctx.createLinearGradient(-h * 0.03, 0, h * 0.03, 0);
  board.addColorStop(0, '#ff5d73');
  board.addColorStop(0.5, '#ffd166');
  board.addColorStop(1, '#ff5d73');
  ctx.fillStyle = board;
  ctx.beginPath();
  ctx.ellipse(0, -h * 0.13, h * 0.035, h * 0.15, 0, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();

  ctx.fillStyle = '#2a1a2c';
  ctx.fillRect(w * 0.31, h * 0.74, Math.max(2, h * 0.006), h * 0.2);
  const umbrella = ctx.createLinearGradient(w * 0.24, 0, w * 0.38, 0);
  ['#2ec4b6', '#ffbf69', '#2ec4b6', '#ffbf69', '#2ec4b6'].forEach((c, i, all) => umbrella.addColorStop(i / (all.length - 1), c));
  ctx.fillStyle = umbrella;
  ctx.beginPath();
  ctx.moveTo(w * 0.24, h * 0.76);
  ctx.quadraticCurveTo(w * 0.313, h * 0.66, w * 0.386, h * 0.76);
  ctx.closePath();
  ctx.fill();

  // Palms, darker against the sky.
  palm(ctx, w * 0.12, h * 1.02, h * 0.78, 0.18, rand);
  palm(ctx, w * 0.2, h * 1.02, h * 0.55, -0.12, rand);
  palm(ctx, w * 0.93, h * 1.02, h * 0.62, -0.2, rand);

  // A couple of birds.
  ctx.strokeStyle = '#2a1830';
  ctx.lineWidth = Math.max(1.5, h * 0.003);
  for (const [bx, by, s] of [[0.45, 0.2, 1], [0.5, 0.17, 0.7], [0.41, 0.25, 0.6]]) {
    const x = w * bx;
    const y = h * by;
    const r = h * 0.018 * s;
    ctx.beginPath();
    ctx.moveTo(x - r, y);
    ctx.quadraticCurveTo(x - r / 2, y - r * 0.7, x, y);
    ctx.quadraticCurveTo(x + r / 2, y - r * 0.7, x + r, y);
    ctx.stroke();
  }

  // A little sensor-like noise so the looks have texture to work with.
  const image = ctx.getImageData(0, 0, w, h);
  const d = image.data;
  for (let i = 0; i < d.length; i += 4) {
    const n = (rand() - 0.5) * 10;
    d[i] = Math.min(255, Math.max(0, d[i] + n));
    d[i + 1] = Math.min(255, Math.max(0, d[i + 1] + n));
    d[i + 2] = Math.min(255, Math.max(0, d[i + 2] + n));
  }
  ctx.putImageData(image, 0, 0);
}

// Headless entry point: the scene as a PNG data URL.
window.sceneDataUrl = (width, height, seed) => {
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  drawScene(canvas, seed);
  return canvas.toDataURL('image/png');
};
