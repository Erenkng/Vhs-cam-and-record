// WebGL2 harness that runs the app's GLSL passes (app/src/main/assets/shaders) on still images.
// The pass graph mirrors app/src/main/kotlin/.../gl/LookRenderer.kt.

import * as Presets from './presets.js';
import { drawCamcorderStamp, drawDigitalStamp, drawFilmStamp, drawVcrOverlay } from './osd.js';

const SHADERS = '../../app/src/main/assets/shaders/';
const LOOK_ASPECT = { VHS: 4 / 3, CCD: 4 / 3, FILM: 3 / 2, POLAROID: 1 };
const UINT_UNIFORMS = new Set(['uFrame']);

async function fetchText(path) {
  const r = await fetch(path);
  if (!r.ok) throw new Error(`${path}: ${r.status}`);
  return r.text();
}

function affine(a, b, c, d, e, f) {
  // Column-major 4x4 of (x, y) -> (a x + c y + e, b x + d y + f).
  return new Float32Array([a, b, 0, 0, c, d, 0, 0, 0, 0, 1, 0, e, f, 0, 1]);
}

export class LookLab {
  constructor(canvas) {
    this.canvas = canvas;
    this.gl = canvas.getContext('webgl2', { preserveDrawingBuffer: true, antialias: false });
    if (!this.gl) throw new Error('WebGL2 is not available');
  }

  async init() {
    const gl = this.gl;
    const common = await fetchText(SHADERS + 'common.glsl');
    const vert = await fetchText(SHADERS + 'quad.vert');
    const load = async (name, defines = []) => {
      const head = '#version 300 es\n' + defines.map((d) => `#define ${d} 1\n`).join('');
      const frag = await fetchText(SHADERS + name);
      return this.link(head + vert, head + common + '\n' + frag, name);
    };
    this.programs = {
      input: await load('input.frag', ['INPUT_2D']),
      chroma: await load('vhs_chroma.frag'),
      tape: await load('vhs_tape.frag'),
      present: await load('present.frag'),
      ccd: await load('ccd.frag'),
      glow: await load('glow.frag'),
      film: await load('film.frag'),
      polaroid: await load('polaroid.frag'),
    };
    this.vao = gl.createVertexArray();
    gl.bindVertexArray(this.vao);
    const buffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);
    gl.enableVertexAttribArray(0);
    gl.vertexAttribPointer(0, 2, gl.FLOAT, false, 0, 0);
    this.blank = this.texture(1, 1, new Uint8Array([0, 0, 0, 0]));
    await document.fonts.load('40px VT323');
  }

  link(vs, fs, name) {
    const gl = this.gl;
    const compile = (type, src) => {
      const s = gl.createShader(type);
      gl.shaderSource(s, src);
      gl.compileShader(s);
      if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) {
        throw new Error(`${name}: ${gl.getShaderInfoLog(s)}`);
      }
      return s;
    };
    const program = gl.createProgram();
    gl.attachShader(program, compile(gl.VERTEX_SHADER, vs));
    gl.attachShader(program, compile(gl.FRAGMENT_SHADER, fs));
    gl.bindAttribLocation(program, 0, 'aPosition');
    gl.linkProgram(program);
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
      throw new Error(`${name} link: ${gl.getProgramInfoLog(program)}`);
    }
    return program;
  }

  texture(w, h, pixels = null, source = null) {
    const gl = this.gl;
    const tex = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, tex);
    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, source != null);
    gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, source != null);
    if (source) {
      gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, source);
    } else {
      gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, w, h, 0, gl.RGBA, gl.UNSIGNED_BYTE, pixels);
    }
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    return tex;
  }

  target(w, h) {
    const gl = this.gl;
    const tex = this.texture(w, h);
    const fbo = gl.createFramebuffer();
    gl.bindFramebuffer(gl.FRAMEBUFFER, fbo);
    gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, tex, 0);
    return { tex, fbo, w, h };
  }

  draw(program, target, uniforms = {}, textures = {}) {
    const gl = this.gl;
    gl.useProgram(program);
    gl.bindFramebuffer(gl.FRAMEBUFFER, target ? target.fbo : null);
    const w = target ? target.w : this.canvas.width;
    const h = target ? target.h : this.canvas.height;
    gl.viewport(0, 0, w, h);
    const all = { uUvTransform: affine(1, 0, 0, 1, 0, 0), ...uniforms };
    for (const [name, value] of Object.entries(all)) {
      const loc = gl.getUniformLocation(program, name);
      if (loc == null) continue;
      if (UINT_UNIFORMS.has(name)) gl.uniform1ui(loc, value >>> 0);
      else if (value instanceof Float32Array) gl.uniformMatrix4fv(loc, false, value);
      else if (Array.isArray(value) && value.length === 2) gl.uniform2f(loc, ...value);
      else if (Array.isArray(value) && value.length === 3) gl.uniform3f(loc, ...value);
      else gl.uniform1f(loc, value);
    }
    let unit = 0;
    for (const [name, tex] of Object.entries(textures)) {
      const loc = gl.getUniformLocation(program, name);
      if (loc == null) continue;
      gl.activeTexture(gl.TEXTURE0 + unit);
      gl.bindTexture(gl.TEXTURE_2D, tex);
      gl.uniform1i(loc, unit);
      unit++;
    }
    gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
  }

  osdTexture(w, h, paint) {
    const c = document.createElement('canvas');
    c.width = w;
    c.height = h;
    const ctx = c.getContext('2d');
    paint(ctx, w, h);
    return this.texture(w, h, null, c);
  }

  /**
   * Renders [image] with a look. Returns the canvas with the result (Polaroid adds its frame,
   * CCD bakes in JPEG blocking just like the app's photo path).
   */
  async render({ image, look, speed = 'SP', time = 3.0, frame = 90, outWidth = 960, seed = 0.37, osd = {}, overrides = {} }) {
    const gl = this.gl;
    const aspect = LOOK_ASPECT[look];
    const imgAspect = image.width / image.height;
    let sx = 1;
    let sy = 1;
    if (imgAspect > aspect) sx = aspect / imgAspect; else sy = imgAspect / aspect;
    const crop = affine(sx, 0, 0, sy, (1 - sx) / 2, (1 - sy) / 2);

    const H = look === 'VHS' ? 480 : 1200;
    const W = Math.round(H * aspect);
    const outW = outWidth;
    const outH = Math.round(outWidth / aspect);
    this.canvas.width = outW;
    this.canvas.height = outH;

    const input = this.texture(0, 0, null, image);
    const burned = this.osdTexture(W, H, (ctx, w, h) => {
      if (look === 'VHS' && osd.dateText) drawCamcorderStamp(ctx, w, h, osd);
      if (look === 'FILM' && osd.filmText) drawFilmStamp(ctx, w, h, osd.filmText);
    });
    const overlay = this.osdTexture(Math.round(720 * aspect), 720, (ctx, w, h) => {
      if (look === 'VHS') drawVcrOverlay(ctx, w, h, { ...osd, speed });
      // The digital stamp is written after in-camera processing, so it stays crisp.
      if (look === 'CCD' && osd.ccdText) drawDigitalStamp(ctx, w, h, osd.ccdText);
    });

    const a = this.target(W, H);
    this.draw(this.programs.input, a, {
      uUvTransform: crop,
      uPixelX: [sx / W, 0],
      uPixelY: [0, sy / H],
      uOsdEnabled: 1,
      uOsdMirror: 0,
    }, { uInput: input, uOsd: burned });

    const out = this.target(W, H);
    let vignetteAmount = 0;
    let scanlines = 0;
    if (look === 'VHS') {
      const p = { ...Presets.VHS[speed], ...overrides };
      const chroma = this.target(Math.round(W / 4), H);
      this.draw(this.programs.chroma, chroma, {
        uSrcSize: [W, H], uChromaBlur: p.chromaBlur, uChromaShift: p.chromaShift,
      }, { uSrc: a.tex });
      this.draw(this.programs.tape, out, {
        uSrcSize: [W, H], uTime: time, uFrame: frame,
        uLumaBlur: p.lumaBlur, uSharpen: p.sharpen, uChromaNoise: p.chromaNoise,
        uLumaNoise: p.lumaNoise, uJitter: p.jitter, uTracking: p.tracking,
        uHeadSwitch: p.headSwitch, uDropouts: p.dropouts, uSaturation: p.saturation,
        uContrast: p.contrast, uBlackLift: p.blackLift, uTint: p.tint, uGlitch: 0,
      }, { uSrc: a.tex, uChroma: chroma.tex });
      vignetteAmount = p.vignette;
      scanlines = p.scanlines;
    } else if (look === 'CCD') {
      const p = Presets.CCD;
      this.draw(this.programs.ccd, out, {
        uSrcSize: [W, H], uScale: H / 1200, uFrame: frame, uSaturation: p.saturation,
        uContrast: p.contrast, uKnee: p.highlightKnee, uFringe: p.fringe, uSharpen: p.sharpen,
        uNoise: p.noise, uTint: p.tint, uVignette: p.vignette,
      }, { uSrc: a.tex });
    } else {
      const glow = this.target(Math.round(W / 4), Math.round(H / 4));
      const film = look === 'FILM';
      this.draw(this.programs.glow, glow, {
        uSrcSize: [W, H], uThreshold: film ? 0.72 : 0.6, uRadius: H * (film ? 0.02 : 0.03),
      }, { uSrc: a.tex });
      if (film) {
        const p = { ...Presets.FILM, ...overrides };
        this.draw(this.programs.film, out, {
          uSrcSize: [W, H], uScale: H / 1600, uFrame: frame, uSaturation: p.saturation,
          uContrast: p.contrast, uFade: p.fade, uWarm: p.warm, uGrain: p.grain,
          uGrainSize: p.grainSize, uHalation: p.halation, uLeak: p.lightLeak, uLeakSeed: seed,
          uVignette: p.vignette, uCornerSoftness: p.cornerSoftness,
        }, { uSrc: a.tex, uGlow: glow.tex });
      } else {
        const p = Presets.POLAROID;
        this.draw(this.programs.polaroid, out, {
          uSrcSize: [W, H], uScale: H / 1600, uSeed: seed, uSaturation: p.saturation,
          uContrast: p.contrast, uFade: p.fade, uTint: p.tint, uSoftness: p.softness,
          uBloom: p.bloom, uChemistry: p.chemistry, uVignette: p.vignette,
        }, { uSrc: a.tex, uGlow: glow.tex });
      }
    }

    this.draw(this.programs.present, null, {
      uSrcSize: [W, H], uOverlayEnabled: look === 'VHS' || look === 'CCD' ? 1 : 0, uOverlayMirror: 0,
      uVignette: vignetteAmount, uScanlines: scanlines, uPeaking: 0, uPeakColor: [1, 0.2, 0.6],
    }, { uSrc: out.tex, uOverlay: overlay, uPeakSrc: a.tex });

    if (look === 'CCD') return this.jpegRoundTrip(Presets.CCD.jpegQuality / 100);
    if (look === 'POLAROID') return this.polaroidFrame();
    return this.canvas;
  }

  async jpegRoundTrip(quality) {
    const blob = await new Promise((resolve) => this.canvas.toBlob(resolve, 'image/jpeg', quality));
    const bitmap = await createImageBitmap(blob);
    const c = document.createElement('canvas');
    c.width = bitmap.width;
    c.height = bitmap.height;
    c.getContext('2d').drawImage(bitmap, 0, 0);
    return c;
  }

  polaroidFrame() {
    // Polaroid 600 proportions: 88 x 107 mm card, 79 x 79 mm picture.
    const s = this.canvas.width;
    const c = document.createElement('canvas');
    c.width = Math.round(s * 88 / 79);
    c.height = Math.round(s * 107 / 79);
    const ctx = c.getContext('2d');
    ctx.fillStyle = '#F3F0E8';
    ctx.fillRect(0, 0, c.width, c.height);
    ctx.drawImage(this.canvas, Math.round(s * 4.5 / 79), Math.round(s * 6 / 79));
    return c;
  }
}

async function loadImage(url) {
  const img = new Image();
  img.src = url;
  await img.decode();
  return img;
}

const canvas = document.getElementById('out');
const lab = new LookLab(canvas);
const ready = lab.init();

// Headless entry point used by render.mjs.
window.renderToDataUrl = async (opts) => {
  await ready;
  const image = await loadImage(opts.imageUrl);
  const result = await lab.render({ ...opts, image });
  return result.toDataURL('image/png');
};

ready.then(() => {
  window.labReady = true;
}).catch((e) => {
  window.labError = String(e);
  console.error(e);
});

// Interactive use.
const fileInput = document.getElementById('file');
const lookSelect = document.getElementById('look');
const timeInput = document.getElementById('time');
let currentImage = null;
async function rerender() {
  if (!currentImage) return;
  await ready;
  const [look, speed] = lookSelect.value.split(':');
  const result = await lab.render({
    image: currentImage, look, speed: speed || 'SP', time: Number(timeInput.value),
    osd: { timeText: '12:00 AM', dateText: 'JAN.01 1997', play: true, counter: '0:00:12', ccdText: '2003/06/14 15:42', filmText: "'98 6 14" },
  });
  document.getElementById('result').src = result.toDataURL('image/png');
}
fileInput?.addEventListener('change', async () => {
  const file = fileInput.files[0];
  if (!file) return;
  currentImage = await loadImage(URL.createObjectURL(file));
  rerender();
});
lookSelect?.addEventListener('change', rerender);
timeInput?.addEventListener('change', rerender);
