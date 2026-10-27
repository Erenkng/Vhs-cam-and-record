// On-screen-display drawing for the lab. The app draws the same layouts with Android's Canvas
// in app/src/main/kotlin/.../gl/OsdPainter.kt; keep the two in sync.

function outlinedText(ctx, text, x, y, size, fill) {
  ctx.lineJoin = 'round';
  ctx.lineWidth = Math.max(1, size * 0.12);
  ctx.strokeStyle = 'rgba(0,0,0,0.85)';
  ctx.strokeText(text, x, y);
  ctx.fillStyle = fill;
  ctx.fillText(text, x, y);
}

/** Camcorder date/time, burned into the picture before it is degraded. Bottom-left. */
export function drawCamcorderStamp(ctx, w, h, { timeText, dateText }) {
  const size = h * 0.075;
  ctx.font = `${size}px VT323`;
  ctx.textBaseline = 'alphabetic';
  const x = w * 0.07;
  let y = h * 0.91;
  const lines = [timeText, dateText].filter(Boolean);
  for (let i = lines.length - 1; i >= 0; i--) {
    outlinedText(ctx, lines[i], x, y, size, '#FFFFFF');
    y -= size * 1.05;
  }
}

/** VCR overlay drawn crisp on top: "PLAY ▶" top-left, speed and tape counter top-right. */
export function drawVcrOverlay(ctx, w, h, { play, counter, speed }) {
  const size = h * 0.07;
  ctx.font = `${size}px VT323`;
  ctx.textBaseline = 'alphabetic';
  const y = h * 0.13;
  if (play) {
    const x = w * 0.07;
    outlinedText(ctx, 'PLAY', x, y, size, '#FFFFFF');
    const tx = x + ctx.measureText('PLAY ').width;
    const th = size * 0.5;
    ctx.beginPath();
    ctx.moveTo(tx, y - th * 1.15);
    ctx.lineTo(tx + th * 0.9, y - th * 0.65);
    ctx.lineTo(tx, y - th * 0.15);
    ctx.closePath();
    ctx.lineWidth = Math.max(1, size * 0.12);
    ctx.strokeStyle = 'rgba(0,0,0,0.85)';
    ctx.stroke();
    ctx.fillStyle = '#FFFFFF';
    ctx.fill();
  }
  if (counter) {
    const text = `${speed}  ${counter}`;
    const tw = ctx.measureText(text).width;
    outlinedText(ctx, text, w * 0.93 - tw, y, size, '#FFFFFF');
  }
}

/** Digital camera imprint, amber, bottom-right. */
export function drawDigitalStamp(ctx, w, h, text) {
  const size = h * 0.055;
  ctx.font = `${size}px VT323`;
  ctx.textBaseline = 'alphabetic';
  const tw = ctx.measureText(text).width;
  outlinedText(ctx, text, w * 0.94 - tw, h * 0.94, size, '#FFB238');
}

const SEGMENTS = {
  '0': 'abcdef', '1': 'bc', '2': 'abged', '3': 'abgcd', '4': 'fgbc',
  '5': 'afgcd', '6': 'afgedc', '7': 'abc', '8': 'abcdefg', '9': 'abcdfg',
};

/** Seven-segment LED imprint of a 35 mm data back, glowing orange, bottom-right. */
export function drawFilmStamp(ctx, w, h, text) {
  const dh = h * 0.045;               // digit height
  const dw = dh * 0.56;               // digit width
  const t = dh * 0.13;                // segment thickness
  const gap = dh * 0.22;              // space between glyphs
  const slant = Math.tan(8 * Math.PI / 180);
  const advance = (ch) => (ch === ' ' ? dw * 0.6 : ch === "'" ? dh * 0.22 : dw) + gap;
  let total = 0;
  for (const ch of text) total += advance(ch);
  let x = w * 0.94 - total;
  const base = h * 0.93;

  ctx.save();
  ctx.fillStyle = '#FF6A1A';
  ctx.shadowColor = 'rgba(255, 110, 30, 0.9)';
  ctx.shadowBlur = dh * 0.35;
  const poly = (pts) => {
    ctx.beginPath();
    pts.forEach(([px, py], i) => {
      const sx = px + (base - py) * slant;
      if (i === 0) ctx.moveTo(sx, py); else ctx.lineTo(sx, py);
    });
    ctx.closePath();
    ctx.fill();
  };
  const hSeg = (x0, x1, y) => poly([[x0, y], [x0 + t / 2, y - t / 2], [x1 - t / 2, y - t / 2], [x1, y], [x1 - t / 2, y + t / 2], [x0 + t / 2, y + t / 2]]);
  const vSeg = (xx, y0, y1) => poly([[xx, y0], [xx + t / 2, y0 + t / 2], [xx + t / 2, y1 - t / 2], [xx, y1], [xx - t / 2, y1 - t / 2], [xx - t / 2, y0 + t / 2]]);
  for (const ch of text) {
    if (ch === "'") {
      poly([[x + t * 0.5, base - dh], [x + t * 1.4, base - dh], [x + t * 0.9, base - dh * 0.72], [x + t * 0.2, base - dh * 0.72]]);
    } else if (SEGMENTS[ch]) {
      const top = base - dh;
      const mid = base - dh / 2;
      const l = x + t / 2;
      const r = x + dw - t / 2;
      for (const s of SEGMENTS[ch]) {
        if (s === 'a') hSeg(l, r, top);
        if (s === 'g') hSeg(l, r, mid);
        if (s === 'd') hSeg(l, r, base);
        if (s === 'f') vSeg(l, top, mid);
        if (s === 'b') vSeg(r, top, mid);
        if (s === 'e') vSeg(l, mid, base);
        if (s === 'c') vSeg(r, mid, base);
      }
    }
    x += advance(ch);
  }
  ctx.restore();
}

/** Security camera overlay: camera name top-left, date and time top-right. */
export function drawCctvOverlay(ctx, w, h, { camera = 'CAM 01', stamp }) {
  const size = h * 0.06;
  ctx.font = `${size}px VT323`;
  ctx.textBaseline = 'alphabetic';
  const y = h * 0.1;
  outlinedText(ctx, camera, w * 0.05, y, size, '#FFFFFF');
  if (stamp) {
    const tw = ctx.measureText(stamp).width;
    outlinedText(ctx, stamp, w * 0.95 - tw, y, size, '#FFFFFF');
  }
}
