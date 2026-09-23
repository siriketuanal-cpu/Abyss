// state.js: データモデル・保存/読込・名前編集ポップアップ・汎用UI部品(createInlineNumber/Text等)
// この並び順(index.htmlの<script>タグの順番)を変えると、他ファイルの関数/変数を先に参照してエラーになる場合があります。

const STORAGE_KEY = 'freetimer:v1';
let state = load();
// アカウント枠外に置かれたタイマーを、起動時にアカウント枠へ収める（既存データ互換）。
function migrateOrphanTimers(){
  if (!state || !Array.isArray(state.items)) return false;
  const orphan = state.items.filter(it => it && (it.type==='stam' || it.type==='idle' || it.type==='exped' || it.type==='orb'));
  if (!orphan.length) return false;
  const keep = state.items.filter(it => !orphan.includes(it));
  const groups = [];
  for (let i=0; i<orphan.length; i+=2){
    groups.push({ id:'g' + Date.now().toString(36) + Math.random().toString(36).slice(2,7), type:'group', name:'', children:orphan.slice(i,i+2) });
  }
  // 最初の孤立タイマー位置にアカウント枠を挿入し、他の順序は維持する。
  const firstIndex = state.items.findIndex(it => orphan.includes(it));
  const beforeCount = state.items.slice(0, firstIndex).filter(it => !orphan.includes(it)).length;
  state.items = keep.slice(0,beforeCount).concat(groups, keep.slice(beforeCount));
  saveNow();
  return true;
}
if (migrateOrphanTimers()) { /* 一度きりのデータ互換処理 */ }
let refs = {};
let tickId = null;
let tickGen = 0;
let pending40Id = null;
let claimId = null;
let movingItemId = null;
// インライン数字編集中の対象。null | `${itemId}:cur` | `${itemId}:max`
let editingId = null;

// DOM参照は一度だけ取得して使い回す(毎分ティックや毎クリックで検索し直さない)。
const cardsEl = document.getElementById('cards');
const emptyEl = document.getElementById('empty');
const addPanelEl = document.getElementById('addPanel');
const addBtnEl = document.getElementById('addBtn');
const setupPanelEl = document.getElementById('setupPanel');
const setupLabelEl = document.getElementById('setupLabel');
const setupFieldsEl = document.getElementById('setupFields');
const setupConfirmEl = document.getElementById('setupConfirm');
const setupCancelEl = document.getElementById('setupCancel');
const toastEl = document.getElementById('toast');
const nameEditOverlayEl = document.getElementById('nameEditOverlay');
const nameEditLabelEl = document.getElementById('nameEditLabel');
const nameEditInputEl = document.getElementById('nameEditInput');
const nameEditOkEl = document.getElementById('nameEditOk');
const nameEditCancelEl = document.getElementById('nameEditCancel');
let nameEditOnOk = null;
let nameEditFocusTimer = null;

function closeNameEditPopup(){
  if (!nameEditOverlayEl) return;
  if (nameEditFocusTimer){ clearTimeout(nameEditFocusTimer); nameEditFocusTimer = null; }
  nameEditOverlayEl.classList.remove('show');
  nameEditOverlayEl.setAttribute('aria-hidden', 'true');
  nameEditOnOk = null;
  try { nameEditInputEl && nameEditInputEl.blur(); } catch (e) {}
}
function openNameEditPopup(opts){
  opts = opts || {};
  if (!nameEditOverlayEl || !nameEditInputEl) return;
  if (nameEditFocusTimer){ clearTimeout(nameEditFocusTimer); nameEditFocusTimer = null; }
  closeToast();
  nameEditLabelEl.textContent = opts.label || '名前を編集';
  nameEditInputEl.value = opts.value != null ? String(opts.value) : '';
  nameEditInputEl.placeholder = opts.placeholder || '';
  nameEditOnOk = typeof opts.onOk === 'function' ? opts.onOk : null;
  nameEditOverlayEl.classList.add('show');
  nameEditOverlayEl.setAttribute('aria-hidden', 'false');
  nameEditFocusTimer = setTimeout(()=>{
    nameEditFocusTimer = null;
    if (!nameEditOverlayEl.classList.contains('show')) return;
    try {
      nameEditInputEl.focus({preventScroll:true});
    } catch (e) {}
  }, 100);
}
function confirmNameEdit(){
  const v = nameEditInputEl ? nameEditInputEl.value : '';
  const cb = nameEditOnOk;
  closeNameEditPopup();
  if (cb) cb(v);
}
if (nameEditOkEl){
  nameEditOkEl.addEventListener('click', e=>{
    e.preventDefault();
    e.stopPropagation();
    confirmNameEdit();
  });
}
if (nameEditCancelEl){
  nameEditCancelEl.addEventListener('click', e=>{
    e.preventDefault();
    e.stopPropagation();
    closeNameEditPopup();
  });
}
if (nameEditInputEl){
  nameEditInputEl.addEventListener('keydown', e=>{
    if (e.key === 'Enter'){
      e.preventDefault();
      confirmNameEdit();
    } else if (e.key === 'Escape'){
      e.preventDefault();
      closeNameEditPopup();
    }
  });
}
if (nameEditOverlayEl){
  nameEditOverlayEl.addEventListener('pointerdown', e=>{
    if (e.target === nameEditOverlayEl){
      e.preventDefault();
      e.stopPropagation();
      confirmNameEdit();
    }
  });
}

// ── カラーパレット ＆ RGB調色ミキサー ──
const CUSTOM_COLORS_STORAGE_KEY = 'freetimer:custom_colors:v1';
const DEFAULT_CUSTOM_COLORS = ['#38bdf8', '#f472b6', '#4ade80', '#fbbf24', '#a78bfa', '#fb923c'];

const PRESET_COLOR_ROW_1 = [
  '#ffffff', // 白（黒の場所を白に）
  '#ef4444', // 赤
  '#f97316', // 橙
  '#eab308', // 黄
  '#22c55e', // 緑
  '#06b6d4', // 水色
  '#3b82f6', // 青
  '#a855f7'  // 紫
];

const PRESET_COLOR_ROW_2 = [
  '#ec4899', // ピンク
  '#f43f5e', // ローズ
  '#10b981', // エメラルド
  '#60a5fa', // ライトブルー
  '#6366f1', // インディゴ
  '#f59e0b', // アンバー
  '#9b8bff', // 見出し色
  '#555b68'  // ★アカウント枠デフォルト色（一番右下）
];

const colorPickerOverlayEl = document.getElementById('colorPickerOverlay');
const colorPaletteViewEl = document.getElementById('colorPaletteView');
const colorRgbViewEl = document.getElementById('colorRgbView');
const colorPickerLabelEl = document.getElementById('colorPickerLabel');
const colorPresetRow1El = document.getElementById('colorPresetRow1');
const colorPresetRow2El = document.getElementById('colorPresetRow2');
const colorCustomSlotsEl = document.getElementById('colorCustomSlots');
const customColorAddBtnEl = document.getElementById('customColorAddBtn');
const colorPickerCloseEl = document.getElementById('colorPickerClose');

const colorRgbBackBtnEl = document.getElementById('colorRgbBackBtn');
const colorRgbPreviewEl = document.getElementById('colorRgbPreview');
const colorRgbHexEl = document.getElementById('colorRgbHex');
const colorSliderREl = document.getElementById('colorSliderR');
const colorSliderGEl = document.getElementById('colorSliderG');
const colorSliderBEl = document.getElementById('colorSliderB');
const colorValREl = document.getElementById('colorValR');
const colorValGEl = document.getElementById('colorValG');
const colorValBEl = document.getElementById('colorValB');
const colorRgbTargetSlotsEl = document.getElementById('colorRgbTargetSlots');
const colorRgbApplyOnlyBtnEl = document.getElementById('colorRgbApplyOnlyBtn');

let colorPickerOnSelect = null;
let colorPickerCurrentColor = '#555b68';
let colorPickerOpenTime = 0;

function hexToRgb(hex){
  hex = String(hex || '#555b68').replace('#', '');
  if (hex.length === 3) hex = hex.split('').map(c=>c+c).join('');
  const num = parseInt(hex, 16);
  if (isNaN(num)) return { r: 85, g: 91, b: 104 };
  return {
    r: (num >> 16) & 255,
    g: (num >> 8) & 255,
    b: num & 255
  };
}

function rgbToHex(r, g, b){
  const toHex = (n) => Math.max(0, Math.min(255, Math.round(n))).toString(16).padStart(2, '0');
  return ('#' + toHex(r) + toHex(g) + toHex(b)).toLowerCase();
}

function loadCustomColors(){
  try {
    const raw = localStorage.getItem(CUSTOM_COLORS_STORAGE_KEY);
    if (raw){
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed) && parsed.length >= 4){
        const list = parsed.map(c => (typeof c === 'string' && c.startsWith('#')) ? c : '#38bdf8');
        while (list.length < 6) list.push(DEFAULT_CUSTOM_COLORS[list.length] || '#38bdf8');
        return list.slice(0, 6);
      }
    }
  } catch(e){}
  return [...DEFAULT_CUSTOM_COLORS];
}

function saveCustomColors(colors){
  try {
    localStorage.setItem(CUSTOM_COLORS_STORAGE_KEY, JSON.stringify(colors.slice(0, 6)));
  } catch(e){}
}

function closeColorPickerPopup(){
  if (!colorPickerOverlayEl) return;
  colorPickerOverlayEl.classList.remove('show');
  colorPickerOverlayEl.setAttribute('aria-hidden', 'true');
  colorPickerOnSelect = null;
  // 次回開く時のためにパレットビューに戻しておく
  if (colorPaletteViewEl) colorPaletteViewEl.style.display = 'block';
  if (colorRgbViewEl) colorRgbViewEl.style.display = 'none';
  closeToast();
}

function renderColorPickerSwatches(currentColor){
  if (!colorPresetRow1El || !colorPresetRow2El || !colorCustomSlotsEl) return;
  const curLower = (currentColor || '').toLowerCase();

  // 1段目
  colorPresetRow1El.innerHTML = '';
  PRESET_COLOR_ROW_1.forEach(hex => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'color-swatch-btn';
    btn.style.backgroundColor = hex;
    btn.title = hex;
    if (hex.toLowerCase() === curLower) btn.classList.add('active');
    btn.addEventListener('click', (e)=>{
      e.preventDefault();
      e.stopPropagation();
      if (Date.now() - colorPickerOpenTime < 180) return;
      if (colorPickerOnSelect) colorPickerOnSelect(hex);
      closeColorPickerPopup();
    });
    colorPresetRow1El.appendChild(btn);
  });

  // 2段目
  colorPresetRow2El.innerHTML = '';
  PRESET_COLOR_ROW_2.forEach(hex => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'color-swatch-btn';
    btn.style.backgroundColor = hex;
    btn.title = hex === '#555b68' ? 'アカウント枠初期色' : hex;
    if (hex.toLowerCase() === curLower) btn.classList.add('active');
    btn.addEventListener('click', (e)=>{
      e.preventDefault();
      e.stopPropagation();
      if (Date.now() - colorPickerOpenTime < 180) return;
      if (colorPickerOnSelect) colorPickerOnSelect(hex);
      closeColorPickerPopup();
    });
    colorPresetRow2El.appendChild(btn);
  });

  // 3段目 (カスタム6枠)
  const customColors = loadCustomColors();
  colorCustomSlotsEl.innerHTML = '';
  customColors.forEach((hex, idx) => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'color-swatch-btn custom-slot';
    btn.style.backgroundColor = hex;
    btn.title = `カスタム ${idx + 1} (${hex})`;
    if (hex.toLowerCase() === curLower) btn.classList.add('active');
    btn.addEventListener('click', (e)=>{
      e.preventDefault();
      e.stopPropagation();
      if (Date.now() - colorPickerOpenTime < 180) return;
      if (colorPickerOnSelect) colorPickerOnSelect(hex);
      closeColorPickerPopup();
    });
    colorCustomSlotsEl.appendChild(btn);
  });
}

function updateRgbMixerDisplay(){
  if (!colorSliderREl || !colorSliderGEl || !colorSliderBEl) return;
  const r = parseInt(colorSliderREl.value, 10) || 0;
  const g = parseInt(colorSliderGEl.value, 10) || 0;
  const b = parseInt(colorSliderBEl.value, 10) || 0;
  if (colorValREl) colorValREl.textContent = r;
  if (colorValGEl) colorValGEl.textContent = g;
  if (colorValBEl) colorValBEl.textContent = b;
  const hex = rgbToHex(r, g, b);
  if (colorRgbPreviewEl) colorRgbPreviewEl.style.backgroundColor = hex;
  if (colorRgbHexEl) colorRgbHexEl.textContent = hex.toUpperCase();
}

function renderRgbTargetSlots(){
  if (!colorRgbTargetSlotsEl) return;
  const customColors = loadCustomColors();
  colorRgbTargetSlotsEl.innerHTML = '';
  customColors.forEach((hex, idx) => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'color-swatch-btn';
    btn.style.backgroundColor = hex;
    btn.title = `スロット ${idx + 1} に保存して適用`;
    btn.addEventListener('click', (e)=>{
      e.preventDefault();
      e.stopPropagation();
      const r = parseInt(colorSliderREl.value, 10) || 0;
      const g = parseInt(colorSliderGEl.value, 10) || 0;
      const b = parseInt(colorSliderBEl.value, 10) || 0;
      const newHex = rgbToHex(r, g, b);
      const list = loadCustomColors();
      list[idx] = newHex;
      saveCustomColors(list);
      if (colorPickerOnSelect) colorPickerOnSelect(newHex);
      closeColorPickerPopup();
    });
    colorRgbTargetSlotsEl.appendChild(btn);
  });
}

function openRgbMixerView(){
  if (!colorPaletteViewEl || !colorRgbViewEl) return;
  colorPaletteViewEl.style.display = 'none';
  colorRgbViewEl.style.display = 'flex';
  colorRgbViewEl.style.flexDirection = 'column';
  colorRgbViewEl.style.gap = '10px';
  const rgb = hexToRgb(colorPickerCurrentColor);
  if (colorSliderREl) colorSliderREl.value = rgb.r;
  if (colorSliderGEl) colorSliderGEl.value = rgb.g;
  if (colorSliderBEl) colorSliderBEl.value = rgb.b;
  updateRgbMixerDisplay();
  renderRgbTargetSlots();
}

function openColorPickerPopup(opts){
  opts = opts || {};
  if (!colorPickerOverlayEl) return;
  closeToast();
  colorPickerOpenTime = Date.now();
  if (colorPickerLabelEl) colorPickerLabelEl.textContent = opts.label || '色を選択';
  colorPickerOnSelect = typeof opts.onSelect === 'function' ? opts.onSelect : null;
  colorPickerCurrentColor = opts.currentColor || '#555b68';
  if (colorPaletteViewEl) colorPaletteViewEl.style.display = 'block';
  if (colorRgbViewEl) colorRgbViewEl.style.display = 'none';
  renderColorPickerSwatches(colorPickerCurrentColor);
  colorPickerOverlayEl.classList.add('show');
  colorPickerOverlayEl.setAttribute('aria-hidden', 'false');
}

if (customColorAddBtnEl){
  customColorAddBtnEl.addEventListener('click', (e)=>{
    e.preventDefault();
    e.stopPropagation();
    openRgbMixerView();
  });
}

if (colorRgbBackBtnEl){
  colorRgbBackBtnEl.addEventListener('click', (e)=>{
    e.preventDefault();
    e.stopPropagation();
    if (colorRgbViewEl) colorRgbViewEl.style.display = 'none';
    if (colorPaletteViewEl) colorPaletteViewEl.style.display = 'block';
    renderColorPickerSwatches(colorPickerCurrentColor);
  });
}

if (colorSliderREl) colorSliderREl.addEventListener('input', updateRgbMixerDisplay);
if (colorSliderGEl) colorSliderGEl.addEventListener('input', updateRgbMixerDisplay);
if (colorSliderBEl) colorSliderBEl.addEventListener('input', updateRgbMixerDisplay);

if (colorRgbApplyOnlyBtnEl){
  colorRgbApplyOnlyBtnEl.addEventListener('click', (e)=>{
    e.preventDefault();
    e.stopPropagation();
    const r = parseInt(colorSliderREl.value, 10) || 0;
    const g = parseInt(colorSliderGEl.value, 10) || 0;
    const b = parseInt(colorSliderBEl.value, 10) || 0;
    const newHex = rgbToHex(r, g, b);
    if (colorPickerOnSelect) colorPickerOnSelect(newHex);
    closeColorPickerPopup();
  });
}

if (colorPickerCloseEl){
  colorPickerCloseEl.addEventListener('click', (e)=>{
    e.preventDefault();
    e.stopPropagation();
    closeColorPickerPopup();
  });
}

if (colorPickerOverlayEl){
  const onOverlayBgTap = (e)=>{
    if (e.target === colorPickerOverlayEl){
      e.preventDefault();
      e.stopPropagation();
      closeColorPickerPopup();
    }
  };
  colorPickerOverlayEl.addEventListener('pointerdown', onOverlayBgTap);
  colorPickerOverlayEl.addEventListener('click', onOverlayBgTap);
}

function normalizeIdleClaim(it){
  // claim は一時的なUI状態（放置=受取 / 遠征=再出発）。永続化すると claimId とズレるため、読み込み時は running に戻す。
  if (it && (it.type === 'idle' || it.type === 'exped') && it.state === 'claim') it.state = 'running';
}
function load(){
  try{
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw){
      const data = JSON.parse(raw);
      if (data && Array.isArray(data.items)){
        data.items = data.items.filter(it => it && typeof it === 'object' && it.id);
        data.items.forEach(it=>{
          if (!it) return;
          normalizeIdleClaim(it);
          if (it.type === 'group'){
            if (!Array.isArray(it.children)) it.children = [];
            it.children = it.children.filter(ch => ch && typeof ch === 'object' && ch.id && (ch.type === 'stam' || ch.type === 'idle' || ch.type === 'orb' || ch.type === 'exped'));
            it.children.forEach(ch=>{
              normalizeIdleClaim(ch);
            });
          }
        });
      }
      return data;
    }
  }catch(e){}
  return { items: [] };
}

// ── 保存管理（全保存経路を単一の pendingSave で完全1本化 ＆ バックグラウンド移行時即時確定）──
let pendingSave = false;
let pendingSaveTimer = null;
let pendingSaveRaf = null;

function saveNow(){
  if (pendingSaveTimer){ clearTimeout(pendingSaveTimer); pendingSaveTimer = null; }
  if (pendingSaveRaf){ cancelAnimationFrame(pendingSaveRaf); pendingSaveRaf = null; }
  pendingSave = false;
  try{
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }catch(e){}
}

function requestSave(mode){
  if (mode === 'now'){
    saveNow();
    return;
  }
  if (pendingSave) return; // 既に予約があれば1本にまとめる
  pendingSave = true;
  if (mode === 'afterPaint'){
    pendingSaveRaf = requestAnimationFrame(()=>{
      pendingSaveRaf = null;
      pendingSaveTimer = setTimeout(()=>{
        pendingSaveTimer = null;
        if (pendingSave) saveNow();
      }, 0);
    });
  } else {
    pendingSaveTimer = setTimeout(()=>{
      pendingSaveTimer = null;
      if (pendingSave) saveNow();
    }, 0);
  }
}
function save(immediate){ requestSave(immediate ? 'now' : 'debounced'); }
function saveAfterPaint(){ requestSave('afterPaint'); }

function uid(){ return 'x' + Date.now().toString(36) + Math.random().toString(36).slice(2,7); }
function clampInt(v, min, max, fb){
  const n = parseInt(v, 10);
  return Number.isFinite(n) ? Math.max(min, Math.min(max, n)) : fb;
}

// アプリ内のタップ操作は pointerdown に統一。ネイティブ入力の編集開始だけは、
// 長押しでキーボードを出さないため短い指離し時にfocusする。
function bindTapDown(el, handler){
  el.addEventListener('pointerdown', (e)=>{
    if (e.isPrimary === false) return;
    if (e.pointerType === 'mouse' && e.button !== 0) return;
    handler(e);
  });
}

function clearOnFocus(input){
  input.addEventListener('focus', () => { input.value = ''; });
}

// スタミナ現在値用インライン編集
function createInlineNumber(initialValue, onCommit, editKey, onOpen, maxDigits){
  const maxLen = maxDigits || 3;
  const normalize = v => String(Math.max(0, Math.floor(Number(String(v).replace(/\D/g, '')) || Number(v) || 0)));
  const sanitize = v => String(v).replace(/\D/g, '').slice(0, maxLen);

  const wrap = document.createElement('span');
  wrap.className = 'inline-number';
  const text = document.createElement('span');
  text.className = 'inline-number-text';
  let shownValue = normalize(initialValue);

  const paint = ()=>{
    if (text.textContent !== shownValue) text.textContent = shownValue;
  };
  paint();
  wrap.appendChild(text);

  // input は常設。編集時のみ表示。
  const input = document.createElement('input');
  input.type = 'text';
  input.inputMode = 'numeric';
  input.maxLength = maxLen;
  input.className = 'inline-edit-input';
  input.autocomplete = 'off';
  input.autocapitalize = 'off';
  input.autocorrect = 'off';
  input.spellcheck = false;
  input.tabIndex = -1;
  input.setAttribute('aria-hidden','true');
  input.style.visibility = 'hidden';
  input.style.pointerEvents = 'none';
  wrap.appendChild(input);

  let editor = null;
  let guardBlur = false;
  let originalValue = shownValue;

  const setValue = v => {
    shownValue = normalize(v);
    if (!editor) paint();
  };
  const isEditing = () => editor !== null || (editKey != null && editingId === editKey);

  const closeInput = ()=>{
    input.style.visibility = 'hidden';
    input.style.pointerEvents = 'none';
    input.tabIndex = -1;
    input.setAttribute('aria-hidden','true');
  };
  const focusInput = ()=>{
    if (editor !== input) return;
    input.style.visibility = 'visible';
    input.style.pointerEvents = 'auto';
    input.tabIndex = 0;
    input.removeAttribute('aria-hidden');
    input.value = '';
    wrap.classList.add('editing');
    try { input.focus({preventScroll:true}); } catch (e) {}
  };

  const finish = (commit=true)=>{
    if (editor !== input) return;
    if (guardBlur && commit){
      try { input.focus({preventScroll:true}); } catch (e) {}
      return;
    }
    let next;
    let raw = '';
    if (!commit){
      next = originalValue;
    } else {
      raw = sanitize(input.value);
      next = (raw === '') ? originalValue : normalize(raw);
    }
    editor = null;
    wrap.classList.remove('editing');
    if (editKey != null && editingId === editKey) editingId = null;
    try { window.getSelection()?.removeAllRanges(); } catch (e) {}
    closeInput();
    clearWrapHold();
    shownValue = next;
    paint();
    if (commit && typeof onCommit === 'function') onCommit(raw || next);
  };

  const open = ()=>{
    if (editor) return;
    originalValue = shownValue;
    if (typeof onOpen === 'function') onOpen();
    if (editKey != null) editingId = editKey;
    input.value = '';
    editor = input;
    guardBlur = true;
    setTimeout(()=>{ guardBlur = false; }, 350);
  };

  input.addEventListener('input', ()=>{
    const v = sanitize(input.value);
    if (input.value !== v) input.value = v;
  });
  input.addEventListener('blur', ()=> finish(true));
  input.addEventListener('keydown', e=>{
    if (e.key === 'Enter'){ e.preventDefault(); guardBlur = false; input.blur(); }
    else if (e.key === 'Escape'){ e.preventDefault(); input.value = ''; guardBlur = false; finish(false); }
  });
  input.addEventListener('contextmenu', e=>{
    e.preventDefault();
    e.stopPropagation();
  }, {capture:true});

  let wrapHoldTimer = null;
  let wrapPointerId = null;
  let wrapHoldX = 0, wrapHoldY = 0;
  const clearWrapHold = ()=>{
    if (wrapHoldTimer !== null){ clearTimeout(wrapHoldTimer); wrapHoldTimer = null; }
    wrapPointerId = null;
  };

  wrap.addEventListener('pointerdown', e=>{
    if (e.isPrimary === false) return;
    if (e.pointerType === 'mouse' && e.button !== 0) return;
    e.preventDefault();
    clearWrapHold();
    wrapPointerId = e.pointerId;
    wrapHoldX = e.clientX; wrapHoldY = e.clientY;
    if (!editor) open();
    if (!editor) return;
    wrapHoldTimer = setTimeout(()=>{
      wrapHoldTimer = null;
      wrapPointerId = null;
      if (editor === input){
        guardBlur = false;
        finish(false);
      }
    }, 450);
  }, {capture:true, passive:false});

  wrap.addEventListener('pointermove', e=>{
    if (e.pointerId !== wrapPointerId) return;
    if (Math.hypot(e.clientX-wrapHoldX, e.clientY-wrapHoldY) > 14){
      if (wrapHoldTimer !== null){ clearTimeout(wrapHoldTimer); wrapHoldTimer = null; }
    }
  }, {capture:true, passive:true});

  wrap.addEventListener('pointerup', e=>{
    if (e.pointerId !== wrapPointerId) return;
    clearWrapHold();
    focusInput();
  }, {capture:true, passive:false});

  wrap.addEventListener('pointercancel', ()=>{
    clearWrapHold();
    if (editor === input && input.style.visibility === 'hidden'){
      guardBlur = false;
      finish(false);
    }
  }, {capture:true, passive:true});

  return {
    wrap, text, input, open,
    setText: setValue,
    setValue,
    setColor: c => { wrap.style.color = c || ''; },
    isEditing
  };
}

// 名前・見出しは表示専用（編集はポップアップ）。input も雫対策も持たない。
function createInlineText(initialValue, placeholder, _onCommit){
  const wrap = document.createElement('span');
  wrap.className = 'name';
  const text = document.createElement('span');
  text.className = 'name-text';
  wrap.appendChild(text);
  let shownValue = String(initialValue == null ? '' : initialValue);
  const paint = ()=>{
    if (shownValue){
      if (text.textContent !== shownValue) text.textContent = shownValue;
      text.classList.remove('is-placeholder');
    } else {
      const ph = placeholder || '';
      if (text.textContent !== ph) text.textContent = ph;
      text.classList.add('is-placeholder');
    }
  };
  paint();
  const setValue = v => {
    shownValue = String(v == null ? '' : v);
    paint();
  };
  return {
    wrap, text, input: null,
    open: ()=>{},
    setText: setValue,
    setValue,
    setColor: c => { wrap.style.color = c || ''; },
    isEditing: () => false
  };
}
