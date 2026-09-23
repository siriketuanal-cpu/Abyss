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
  // トースト由来の pointer が残っていると focus 直後に blur されることがある
  closeToast();
  nameEditLabelEl.textContent = opts.label || '名前を編集';
  nameEditInputEl.value = opts.value != null ? String(opts.value) : '';
  nameEditInputEl.placeholder = opts.placeholder || '';
  nameEditOnOk = typeof opts.onOk === 'function' ? opts.onOk : null;
  nameEditOverlayEl.classList.add('show');
  nameEditOverlayEl.setAttribute('aria-hidden', 'false');
  // トースト閉鎖・前の pointer が落ち着いてから focus（初回キーボード落ち対策）
  nameEditFocusTimer = setTimeout(()=>{
    nameEditFocusTimer = null;
    if (!nameEditOverlayEl.classList.contains('show')) return;
    try {
      nameEditInputEl.focus({preventScroll:true});
    } catch (e) {}
  }, 120);
}
function confirmNameEdit(){
  const v = nameEditInputEl ? nameEditInputEl.value : '';
  const cb = nameEditOnOk;
  closeNameEditPopup();
  if (cb) cb(v);
}
if (nameEditOkEl){
  // pointerdown だと focus を奪ってキーボードが先に閉じることがある → click で確定
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
// engine.js: タイマー項目の生成・残り時間計算(stam/orb/idle)・tick判定
// この並び順(index.htmlの<script>タグの順番)を変えると、他ファイルの関数/変数を先に参照してエラーになる場合があります。

function newStamItem(intervalMin, max, useChunk){
  const m = clampInt(max, 1, 999, 100);
  const item = {
    id: uid(), type:'stam', name:'',
    current: m, max: m,
    intervalMin: clampInt(intervalMin, 1, 99999, 5),
    start: Date.now()
  };
  // useChunk あり → カード短押しで使い切りプレビュー/確定
  if (useChunk != null && Number(useChunk) > 0) item.useChunk = clampUseChunk(useChunk);
  return item;
}
function newOrbItem(intervalMin, max, useChunk){
  const m = clampInt(max, 1, 99, 4);
  const item = {
    id: uid(),
    type: 'orb',
    name: '',
    current: m,
    max: m,
    intervalMin: clampInt(intervalMin, 1, 99999, 360),
    start: Date.now(),
    orbMode: 'down'
  };
  if (useChunk != null && Number(useChunk) > 0) item.useChunk = clampUseChunk(useChunk);
  return item;
}
function hasUseChunk(it){
  if (!it || it.useChunk == null) return false;
  const n = Number(it.useChunk);
  return Number.isFinite(n) && n > 0 && (it.type === 'stam' || it.type === 'orb');
}
function newIdleItem(durationMin, countMode){
  return { id: uid(), type:'idle', name:'', durationMin, countMode: countMode || 'down', state:'running', start: Date.now() };
}
function newExpedItem(durationMin, countMode){
  return { id: uid(), type:'exped', name:'', durationMin, countMode: countMode || 'down', state:'running', start: Date.now() };
}
function newHeaderItem(){
  return { id: uid(), type:'header', name:'', color:'#9b8bff' };
}
function newRuleItem(color){
  return { id: uid(), type:'rule', color: color || '#52617a' };
}
function newGroupItem(){
  return { id: uid(), type:'group', name:'', children: [] };
}

// 残り時間ではなく「何時何分に完了するか」を時刻表記で返す(スタミナ用)。
function fmtClockAt(remainMs, now){
  const d = new Date((now || Date.now()) + Math.max(0, remainMs));
  const p = n => String(n).padStart(2,'0');
  return { h: p(d.getHours()), m: p(d.getMinutes()) };
}
// 残り時間を「あと◯:◯◯」のカウントダウン表記で返す(放置報酬用)。
function fmtCountdown(ms){
  ms = Math.max(0, ms);
  const totalMin = Math.ceil(ms/60000);
  const h = Math.floor(totalMin/60), m = totalMin%60;
  return h + ':' + String(m).padStart(2,'0');
}
// 経過時間を「◯:◯◯」のカウントアップ表記で返す(放置報酬用)。
function fmtElapsed(ms){
  ms = Math.max(0, ms);
  const totalMin = Math.floor(ms/60000);
  const h = Math.floor(totalMin/60), m = totalMin%60;
  return h + ':' + String(m).padStart(2,'0');
}

function stamInfo(it, now){
  // ベースが既に満タンなら回復計算しない（満タン時刻は start に固定）
  if (it.current >= it.max){
    return { cur: it.max, remainMs:0, isFull:true, fullAt: it.start };
  }
  const intervalMs = Math.max(1, it.intervalMin) * 60000;
  const elapsed = Math.max(0, now - it.start);
  const recovered = Math.floor(elapsed / intervalMs);
  const cur = Math.min(it.max, it.current + recovered);
  if (cur >= it.max){
    const fullAt = it.start + Math.max(0, it.max - it.current) * intervalMs;
    return { cur: it.max, remainMs:0, isFull:true, fullAt: Math.min(now, fullAt) };
  }
  const nextIn = intervalMs - (elapsed % intervalMs);
  const need = it.max - cur;
  const remainMs = (need - 1) * intervalMs + nextIn;
  return { cur, remainMs, isFull:false };
}
// 満タンにしたら start を今に固定して、以降の回復計算を止める
function freezeIfFull(it, now){
  if (it.current >= it.max){
    it.current = it.max;
    it.start = now;
  }
}

// ── オーブ専用の回復計算とサイクル管理 ──
function orbInfo(it, now){
  if (it.current >= it.max){
    return { cur: it.max, remainMs: 0, nextInMs: 0, isFull: true, fullAt: it.start };
  }
  const intervalMs = Math.max(1, it.intervalMin) * 60000;
  const elapsed = Math.max(0, now - it.start);
  const recovered = Math.floor(elapsed / intervalMs);
  const cur = Math.min(it.max, it.current + recovered);
  if (cur >= it.max){
    const fullAt = it.start + Math.max(0, it.max - it.current) * intervalMs;
    return { cur: it.max, remainMs: 0, nextInMs: 0, isFull: true, fullAt: Math.min(now, fullAt) };
  }
  const nextInMs = intervalMs - (elapsed % intervalMs);
  const need = it.max - cur;
  const remainMs = (need - 1) * intervalMs + nextInMs;
  return { cur, remainMs, nextInMs, isFull: false, fullAt: now + remainMs };
}
function freezeOrbIfFull(it, now){
  if (it.current >= it.max){
    it.current = it.max;
    it.start = now;
  }
}
function preserveOrbCycle(it, now){
  if (it.current >= it.max){
    it.start = now;
    return;
  }
  const intervalMs = Math.max(1, it.intervalMin) * 60000;
  const phase = ((now - it.start) % intervalMs + intervalMs) % intervalMs;
  it.start = now - phase;
}

function parseTimeMinutes(raw){
  const digits = String(raw || '').replace(/\D/g, '');
  if (!digits) return null;
  let h = 0, m = 0;
  if (digits.length >= 3){
    if (digits.length === 3){
      h = Number(digits.slice(0, 1));
      m = Number(digits.slice(1, 3));
    } else {
      h = Number(digits.slice(0, digits.length - 2));
      m = Number(digits.slice(-2));
    }
  } else {
    m = Number(digits);
  }
  m = Math.min(59, m);
  return h * 60 + m;
}

// オーブの時間入力処理
// isFullField: true の場合はトーストの「全回復/総蓄積」欄からの入力として扱い、false の場合はカード上の入力として判定
function applyOrbTimeInput(it, digits, now, isFullField){
  now = now || Date.now();
  const inputMin = parseTimeMinutes(digits);
  if (inputMin == null) return;
  const oneOrbMin = Math.max(1, it.intervalMin);
  const totalMaxMin = it.max * oneOrbMin;
  const info = orbInfo(it, now);

  if (it.orbMode === 'up'){
    // ── 累積（経過時間）モード ──
    if (!isFullField && inputMin <= oneOrbMin && info.cur < it.max){
      // カードから1個分以内の経過時間を指定：現在の個数を保ち、位相を進める
      const phaseMin = Math.min(oneOrbMin, Math.max(0, inputMin));
      if (phaseMin >= oneOrbMin){
        it.current = Math.min(it.max, info.cur + 1);
        it.start = now;
      } else {
        it.current = info.cur;
        it.start = now - (phaseMin * 60000);
      }
    } else {
      // 全体の蓄積時間（またはトースト欄からの入力）
      const elapsedMin = Math.min(totalMaxMin, Math.max(0, inputMin));
      if (elapsedMin >= totalMaxMin){
        it.current = it.max;
        it.start = now;
      } else {
        const cur = Math.min(it.max - 1, Math.max(0, Math.floor(elapsedMin / oneOrbMin)));
        const phaseMin = elapsedMin % oneOrbMin;
        it.current = cur;
        it.start = now - (phaseMin * 60000);
      }
    }
  } else {
    // ── 減算（残り時間）モード ──
    if (!isFullField && inputMin <= oneOrbMin && info.cur < it.max){
      // カードから「次の1個回復までの残り時間」を指定：現在の個数を維持し、次の回復までの時間をセット
      const remainInThisOrb = Math.min(oneOrbMin, Math.max(0, inputMin));
      if (remainInThisOrb <= 0){
        it.current = Math.min(it.max, info.cur + 1);
        it.start = now;
      } else {
        it.current = info.cur;
        const phaseMin = oneOrbMin - remainInThisOrb;
        it.start = now - (phaseMin * 60000);
      }
    } else {
      // 全回復までの残り時間（トースト欄、または1個の時間を超える全体の残り時間入力）
      const totalRemainMin = inputMin;
      if (totalRemainMin <= 0){
        it.current = it.max;
        it.start = now;
      } else if (totalRemainMin >= totalMaxMin){
        it.current = 0;
        it.start = now;
      } else {
        const elapsedMin = totalMaxMin - totalRemainMin;
        const cur = Math.min(it.max - 1, Math.max(0, Math.floor(elapsedMin / oneOrbMin)));
        const phaseMin = elapsedMin % oneOrbMin;
        it.current = cur;
        it.start = now - (phaseMin * 60000);
      }
    }
  }
  freezeOrbIfFull(it, now);
}

// 現在値/上限値を書き換えても「次の1回復までの残り時間」がリセットされないよう、
// サイクルの位相(既にどれだけ経過しているか)を維持したまま基準時刻だけを進める。
function clampUseChunk(v){
  const n = Math.floor(Number(v) || 0);
  return Math.max(1, Math.min(999, n || 1));
}
// 使い切り後の残り。スタミナは剰余（220→20）、オーブは設定数だけ単発減算（5→4）
function remainingAfterUse(cur, it){
  cur = Math.max(0, Math.floor(Number(cur) || 0));
  const fallback = 1;
  const c = clampUseChunk(it && it.useChunk != null ? it.useChunk : fallback);
  if (it && it.type === 'orb'){
    return Math.max(0, cur - c);
  }
  return cur % c;
}
function preserveCycle(it, now){
  const intervalMs = Math.max(1, it.intervalMin) * 60000;
  const phase = ((now - it.start) % intervalMs + intervalMs) % intervalMs;
  it.start = now - phase;
}
function idleInfo(it, now){
  const durMs = Math.max(1, it.durationMin) * 60000;
  const elapsed = Math.max(0, now - it.start);
  const remainMs = Math.max(0, durMs - elapsed);
  return { elapsed, remainMs, isFull: remainMs<=0, fullAt: it.start + durMs };
}
function fmtHM(ts){
  const d = new Date(ts);
  const p = n => String(n).padStart(2,'0');
  return p(d.getHours()) + ':' + p(d.getMinutes());
}
// 満タン、または満タンまで2時間未満 → 赤
function isNearFull(remainMs, isFull){
  return !!isFull || (remainMs > 0 && remainMs < 7200000);
}
// 現在値の赤表示（文字に1回だけ当てる）
function setNearOnCurrent(editor, near){
  if (!editor || !editor.text) return;
  if (near) editor.text.style.setProperty('color', 'var(--danger)', 'important');
  else editor.text.style.removeProperty('color');
}

function timerNeedsTick(it, now){
  if (it.type==='stam') return !stamInfo(it, now).isFull;
  if (it.type==='orb') return !orbInfo(it, now).isFull;
  // claim 中は表示固定のため tick 不要。
  if (it.type==='idle' || it.type==='exped') return it.state !== 'claim' && !idleInfo(it, now).isFull;
  return false;
}
function needsTicking(now){
  now = now || Date.now();
  return state.items.some(it=>{
    if (it.type==='group') return it.children.some(c=>timerNeedsTick(c, now));
    return timerNeedsTick(it, now);
  });
}

// ── 描画前段：自然満タンの状態同期を一括処理（描画関数から副作用を分離）──
function syncFullStamItems(now){
  let changed = false;
  const check = (it) => {
    if (it && it.type === 'stam' && it.current < it.max){
      const info = stamInfo(it, now);
      if (info.isFull){
        it.current = it.max;
        it.start = info.fullAt || now;
        changed = true;
      }
    } else if (it && it.type === 'orb' && it.current < it.max){
      const info = orbInfo(it, now);
      if (info.isFull){
        it.current = it.max;
        it.start = info.fullAt || now;
        changed = true;
      }
    }
  };
  for (const it of state.items){
    if (it.type === 'group') it.children.forEach(check);
    else check(it);
  }
  if (changed) saveAfterPaint();
}

function tickRender(now){
  now = now || Date.now();
  syncFullStamItems(now);
  for (const it of state.items){
    const r = refs[it.id];
    if (!r) continue;
    if (it.type==='group'){
      for (const child of it.children) updateTimerCard(child, r.childRefs[child.id], now);
    } else if (it.type==='stam' || it.type==='orb' || it.type==='idle' || it.type==='exped'){
      updateTimerCard(it, r, now);
    }
  }
}
function scheduleTick(gen){
  if (gen !== tickGen || !needsTicking()) return;
  const unit = 60000;
  const delay = Math.max(200, unit - (Date.now() % unit));
  tickId = setTimeout(()=>{
    tickId = null;
    if (gen !== tickGen) return;
    // 毎分はDOM構造を再構築せず、既存ノードの表示だけ差分更新。
    tickRender(Date.now());
    if (needsTicking()) scheduleTick(gen);
  }, delay);
}
function startTicking(force){
  if (force){ tickGen++; if (tickId) clearTimeout(tickId); tickId=null; }
  if (document.hidden) return;
  if (!tickId && needsTicking()) scheduleTick(tickGen);
}

// 現在値編集中やメニューボタンは長押し削除／使い切りと分離
function isEditTarget(target){
  if (!target || !target.closest) return false;
  return !!(
    target.closest('.inline-edit-input') ||
    target.closest('[data-role="curWrap"]') ||
    target.closest('.timer-menu-btn') ||
    target.closest('.header-menu-button')
  );
}

// タイマーカード用の通常タップだけを高速処理。
function bindTimerShortAction(el, shortAction){
  let active = false, moved = false, pid = null;
  let sx = 0, sy = 0;
  const MOVE_PX = 10;
  const isPrimary = e => e.isPrimary !== false && !(e.pointerType==='mouse' && e.button!==0);
  const cleanup = ()=>{ active=false; moved=false; pid=null; };

  el.addEventListener('pointerdown', e=>{
    if (!isPrimary(e) || active || editingId != null || isEditTarget(e.target)) return;
    active = true; moved = false; pid = e.pointerId;
    sx = e.clientX; sy = e.clientY;
  }, {capture:true, passive:true});

  el.addEventListener('pointermove', e=>{
    if (!active || e.pointerId !== pid || moved) return;
    if (Math.abs(e.clientX - sx) > MOVE_PX || Math.abs(e.clientY - sy) > MOVE_PX) moved = true;
  }, {capture:true, passive:true});

  el.addEventListener('pointerup', e=>{
    if (!active || e.pointerId !== pid) return;
    const shouldAct = !moved && !isEditTarget(e.target) && typeof shortAction === 'function';
    cleanup();
    if (shouldAct) shortAction(e);
  }, {capture:true, passive:false});

  el.addEventListener('pointercancel', e=>{
    if (!active || e.pointerId !== pid) return;
    cleanup();
  }, {capture:true, passive:true});
}
let toastTapDownHandler = null;
let longPressTargetEl = null;
let menuOpenRaf = 0;
function clearLongPressTarget(){
  longPressTargetEl = null;
  // 取りこぼし防止：参照がずれても赤枠が残らないよう、残っているものを全部外す
  if (cardsEl){
    const left = cardsEl.querySelectorAll('.longpress-target');
    for (let i = 0; i < left.length; i++) left[i].classList.remove('longpress-target');
  }
}
// トースト＝確認UI専用。開く/閉じる/2段階確定をここに集約。
let currentToastId = null;
function closeToast(){
  clearLongPressTarget();
  currentToastId = null;
  if (!toastEl) return;
  const focused = toastEl.querySelector('input:focus');
  if (focused) try { focused.blur(); } catch(e){}
  toastEl.classList.remove('show');
  if (toastTapDownHandler){
    toastEl.removeEventListener('pointerdown', toastTapDownHandler);
    toastTapDownHandler = null;
  }
}// toast-core.js: トーストメニューの開閉・削除確認(askRemoveItem)本体
// この並び順(index.htmlの<script>タグの順番)を変えると、他ファイルの関数/変数を先に参照してエラーになる場合があります。

function showConfirmToast(html, onAct){
  if (!toastEl) return;
  closeToast();
  toastEl.innerHTML = html;
  toastEl.classList.add('show');
  const onClick = (e)=>{
    const btn = e.target.closest('button[data-act]');
    if (!btn) return;
    e.preventDefault();
    e.stopPropagation();
    const act = btn.dataset.act;

    // トグル系：トーストを閉じずに即時切り替え＆表示更新
    if (act && act.startsWith('toggle')){
      if (typeof onAct === 'function') onAct(act, btn);
      return;
    }

    // トーストを閉じた瞬間に背後要素がタップ反応・ハイライトを受けるのを防止
    if (cardsEl){
      cardsEl.style.pointerEvents = 'none';
      setTimeout(()=>{ if (cardsEl) cardsEl.style.pointerEvents = ''; }, 180);
    }

    // 移動・複製・入れ替え：トーストを閉じて即時実行
    if (act === 'startMove' || act === 'cloneGroup' || act === 'swap'){
      closeToast();
      if (typeof onAct === 'function') onAct(act, btn);
      return;
    }

    // 完了・閉じる
    if (act === 'done' || act === 'no' || act === 'cancel'){
      const focused = toastEl.querySelector('input:focus');
      if (focused) focused.blur();
      closeToast();
      if (typeof onAct === 'function') onAct(act, btn);
      return;
    }

    // 削除：2回タップで確定（誤タップ防止）
    if (act === 'yes'){
      if (!btn.classList.contains('armed')){
        toastEl.querySelectorAll('button.armed').forEach(b => {
          b.classList.remove('armed');
          b.textContent = '削除';
        });
        btn.classList.add('armed');
        btn.textContent = '本当に削除？';
        return;
      }
      closeToast();
      if (typeof onAct === 'function') onAct('yes', btn);
      return;
    }

    closeToast();
    if (typeof onAct === 'function') onAct(act, btn);
  };
  toastTapDownHandler = onClick;
  toastEl.addEventListener('pointerdown', onClick);
}
function cancelAllPendingStates(){
  if (movingItemId != null && typeof cancelMove === 'function') cancelMove();
  if (pending40Id){
    const prev = pending40Id;
    pending40Id = null;
    paintUseChunkPreview(prev);
  }
  if (claimId){
    const claimItem = findItemById(claimId);
    if (claimItem && (claimItem.type === 'idle' || claimItem.type === 'exped')){
      claimItem.state = 'running';
      updateOneTimer(claimItem);
    }
    claimId = null;
  }
  if (document.activeElement && typeof document.activeElement.blur === 'function'){
    try { document.activeElement.blur(); } catch(e){}
  }
}

function getItemDisplayName(it){
  if (!it) return '要素';
  if (it.type === 'group') return it.name || 'アカウント枠';
  if (it.type === 'header') return it.name || '見出し';
  if (it.type === 'rule') return '仕切り線';
  if (it.name) return it.name;
  return 'タイマー';
}

function cloneGroup(id){
  const topId = getTopLevelItemId(id) || id;
  const topIdx = state.items.findIndex(i => i.id === topId);
  if (topIdx === -1) return;
  const orig = state.items[topIdx];
  if (!orig || orig.type !== 'group') return;

  const copy = JSON.parse(JSON.stringify(orig));
  copy.id = uid();
  const now = Date.now();
  if (copy.name) copy.name = copy.name + ' (コピー)';

  if (Array.isArray(copy.children)){
    copy.children.forEach(c => {
      c.id = uid();
      c.start = now;
      c.updatedAt = now;
      if (c.state === 'claim') c.state = 'running';
    });
  }

  state.items.splice(topIdx + 1, 0, copy);
  save(); render(); startTicking(true);
  showNotice('枠を複製しました', 1500);
}

function askRemoveItem(id){
  if (toastEl && toastEl.classList.contains('show') && currentToastId === id){
    return;
  }
  cancelAllPendingStates();
  const it = findItemById(id);
  if (!toastEl){ removeItem(id); return; }
  const isHeader = it && it.type === 'header';
  const isGroup = it && it.type === 'group';
  const isRule = it && it.type === 'rule';
  const isStam = it && it.type === 'stam';
  const isOrb = it && it.type === 'orb';
  const isIdle = it && (it.type === 'idle' || it.type === 'exped');
  const canAdd = isGroup && it.children.length < 4;
  const colorDefault = isHeader ? (it.color || '#9b8bff') : (isRule ? (it.color || '#52617a') : (isGroup ? (it.color || '#555b68') : null));

  let fields = '';
  if (isGroup){
    fields = `
      <button type="button" data-act="editName">アカウント名を変更</button>
      <label class="toast-color-btn"><span class="toast-color-label">色</span><input type="color" value="${colorDefault}" aria-label="色"></label>
      <button type="button" class="toast-half-btn" data-act="toggleGroupLayout">${it.layout === '2x2' ? '配置：⊞ 2×2' : '配置：☰ 1行'}</button>
      <div class="toast-fields-btns">
        ${canAdd ? '<button type="button" data-act="add">タイマー追加</button>' : ''}
        <button type="button" data-act="insertBelow"${!canAdd ? ' style="grid-column:1/-1;"' : ''}>枠を追加</button>
      </div>
      <div class="toast-fields-btns">
        <button type="button" data-act="startMove">移動</button>
        <button type="button" data-act="cloneGroup">枠を複製</button>
      </div>
    `;
  } else if (isHeader){
    fields = `
      <button type="button" data-act="editName">見出し名を変更</button>
      <label class="toast-color-btn"><span class="toast-color-label">色</span><input type="color" value="${colorDefault}" aria-label="色"></label>
      <button type="button" class="toast-half-btn" data-act="toggleFoldLock">${it.foldLock ? '折りたたみ：🔒' : '折りたたみ：🔓'}</button>
      <div class="toast-fields-btns">
        <button type="button" data-act="insertBelow">枠を追加</button>
        <button type="button" data-act="startMove">移動</button>
      </div>
    `;
  } else if (isRule){
    fields = `
      <label class="toast-color-btn wide"><span class="toast-color-label">色</span><input type="color" value="${colorDefault}" aria-label="色"></label>
      <div class="toast-fields-btns">
        <button type="button" data-act="insertBelow">枠を追加</button>
        <button type="button" data-act="startMove">移動</button>
      </div>
    `;
  } else {
    fields = (isStam ? stamToastRowsHtml(it) : '')
      + (isOrb ? orbToastRowsHtml(it) : '')
      + (isIdle ? idleToastRowsHtml(it) : '');
  }

  const extra = fields ? `<div class="toast-fields">${fields}</div>` : '';
  showConfirmToast(
    extra
    + `<div class="toast-actions">`
    + '<button type="button" class="del" data-act="yes">削除</button>'
    + '<button type="button" class="done" data-act="done">完了</button>'
    + '</div>',
    (act, btn)=>{
      if (act === 'yes') removeItem(id);
      else if (act === 'add') openAddPanel({ groupId: id });
      else if (act === 'insertBelow') openAddPanel({ insertAfterId: id });
      else if (act === 'startMove') startMoveItem(id);
      else if (act === 'cloneGroup') cloneGroup(id);
      else if (act === 'toggleGroupLayout'){
        it.layout = (it.layout === '2x2') ? '1row' : '2x2';
        if (btn) btn.textContent = (it.layout === '2x2') ? '配置：⊞ 2×2' : '配置：☰ 1行';
        save(); render();
      }
      else if (act === 'toggleFoldLock'){
        it.foldLock = !it.foldLock;
        if (it.foldLock) it.collapsed = false;
        if (btn) btn.textContent = it.foldLock ? '折りたたみ：🔒' : '折りたたみ：🔓';
        save(); render();
      }
      else if (act === 'toggleOrbMode'){
        it.orbMode = (it.orbMode === 'up') ? 'down' : 'up';
        const isUp = it.orbMode === 'up';
        if (btn) btn.textContent = isUp ? '方式：▲ 経過時間(蓄積)' : '方式：▼ 残り時間(減算)';
        const lbl = toastEl.querySelector('[data-role="orbTimeLabel"]');
        if (lbl) lbl.textContent = isUp ? '蓄積時間' : '全回復';
        const inp = toastEl.querySelector('input[data-role="orbFullRem"]');
        if (inp){
          inp.setAttribute('aria-label', isUp ? '蓄積時間' : '全回復');
          const info = orbInfo(it, Date.now());
          const oneOrbMin = Math.max(1, it.intervalMin);
          const totalMin = isUp
            ? (info.isFull ? (it.max * oneOrbMin) : Math.floor(info.cur * oneOrbMin + Math.max(0, oneOrbMin * 60000 - info.nextInMs) / 60000))
            : (info.isFull ? 0 : Math.ceil(info.remainMs / 60000));
          const rh = Math.floor(totalMin / 60);
          const rm = totalMin % 60;
          inp.value = String(rh).padStart(2, '0') + ':' + String(rm).padStart(2, '0');
        }
        updateOneTimer(it);
        save();
      }
      else if (act === 'editName'){
        const ed = refs[id]?.nameEditor;
        openNameEditPopup({
          label: isHeader ? '見出し（ゲーム名など）' : 'アカウント名',
          value: it.name || '',
          placeholder: isHeader ? '見出し(ゲーム名など)' : 'アカウント',
          onOk: (v)=>{
            const next = String(v == null ? '' : v);
            if (it.name === next) return; // 変更なし＝DOMも触らない
            it.name = next;
            if (ed && typeof ed.setText === 'function') ed.setText(next);
            else if (ed && typeof ed.setValue === 'function') ed.setValue(next);
            save();
          }
        });
      }
    }
  );
  currentToastId = id;
  // closeToast の後にハイライトを付け直す
  const targetRef = refs[id];
  if (targetRef && targetRef.el){
    targetRef.el.classList.add('longpress-target');
    longPressTargetEl = targetRef.el;
  }
  if (colorDefault){
    const colorEl = toastEl.querySelector('.toast-color input[type="color"]');
    if (colorEl){
      const onColorChange = ()=>{
        it.color = colorEl.value;
        if (isHeader){
          if (refs[id]?.nameEditor) refs[id].nameEditor.setColor(it.color);
          else if (refs[id]?.nameEl) refs[id].nameEl.style.color = it.color;
          if (refs[id]?.menuBtn) refs[id].menuBtn.style.setProperty('--header-color', it.color);
        } else if (isRule && refs[id]?.el){
          refs[id].el.style.borderTopColor = it.color;
        } else if (isGroup && refs[id]?.el){
          refs[id].el.style.borderColor = it.color;
          if (refs[id]?.nameEditor) refs[id].nameEditor.setColor(it.color);
        }
        save();
      };
      colorEl.addEventListener('input', onColorChange);
      colorEl.addEventListener('change', onColorChange);
      colorEl.addEventListener('pointerdown', e=>e.stopPropagation());
    }
  }
  if (isStam) bindStamToastRows(it);
  if (isOrb) bindOrbToastRows(it);
  if (isIdle) bindIdleToastRows(it);
}

let noticeTimer = null;
function showNotice(msg, timeoutMs = 2800){
  if (!toastEl) return;
  closeToast();
  if (noticeTimer) { clearTimeout(noticeTimer); noticeTimer = null; }
  toastEl.innerHTML = `<div class="notice-msg">${msg}</div>`;
  toastEl.classList.add('show');
  if (timeoutMs > 0){
    noticeTimer = setTimeout(()=>{
      if (toastEl && toastEl.innerHTML.includes(msg)){
        closeToast();
      }
    }, timeoutMs);
  }
}
// toast-fields.js: トーストメニュー内の種類別フィールド(スタミナ/オーブ/放置)のHTML生成とバインド
// この並び順(index.htmlの<script>タグの順番)を変えると、他ファイルの関数/変数を先に参照してエラーになる場合があります。

function stamToastRowsHtml(it){
  const chunkVal = it.useChunk != null ? clampUseChunk(it.useChunk) : '';
  return `<label class="toast-field"><span class="toast-color"><span class="toast-label">回復</span><input type="text" inputmode="numeric" pattern="[0-9]*" value="${it.intervalMin || 5}" maxlength="3" data-role="stamInterval" aria-label="回復時間"></span></label>`
    + `<label class="toast-field"><span class="toast-color"><span class="toast-label">最大</span><input type="text" inputmode="numeric" pattern="[0-9]*" value="${it.max}" maxlength="3" data-role="stamMax" aria-label="最大スタミナ"></span></label>`
    + `<label class="toast-field"><span class="toast-color"><span class="toast-label">使い切り</span><input type="text" inputmode="numeric" pattern="[0-9]*" value="${chunkVal}" maxlength="3" data-role="useChunk" aria-label="使い切り数" placeholder="なし"></span></label>`
    + `<div class="toast-quick-max-btns">`
      + `<button type="button" data-act="addMax" data-val="1">1</button>`
      + `<button type="button" data-act="addMax" data-val="5">5</button>`
      + `<button type="button" data-act="addMax" data-val="10">10</button>`
    + `</div>`;
}

function bindStamToastRows(it){
  const chunkEl = toastEl.querySelector('input[data-role="useChunk"]');
  if (chunkEl){
    chunkEl.addEventListener('focus', ()=>{ chunkEl.value = ''; });
    const applyChunk = ()=>{
      const raw = String(chunkEl.value || '').replace(/\D/g, '');
      const num = Number(raw);
      if (raw === '' || num === 0){
        delete it.useChunk;
        chunkEl.value = '';
        if (pending40Id === it.id){ pending40Id = null; paintUseChunkPreview(it.id); }
      } else {
        it.useChunk = clampUseChunk(num);
        chunkEl.value = String(it.useChunk);
        if (pending40Id === it.id) paintUseChunkPreview(it.id);
      }
      if (refs[it.id]) refs[it.id].shortAction = hasUseChunk(it) ? (e) => onUseChunkTap(it, e) : null;
      updateOneTimer(it);
      save();
    };
    chunkEl.addEventListener('change', applyChunk);
    chunkEl.addEventListener('keydown', e=>{
      if (e.key === 'Enter'){ e.preventDefault(); chunkEl.blur(); }
    });
    chunkEl.addEventListener('pointerdown', e=>e.stopPropagation());
  }

  const intervalEl = toastEl.querySelector('input[data-role="stamInterval"]');
  if (intervalEl){
    const origDisplay = it.intervalMin || 5;
    intervalEl.addEventListener('focus', ()=>{ intervalEl.value = ''; });
    const applyInterval = ()=>{
      const raw = String(intervalEl.value || '').replace(/\D/g, '');
      const newNum = raw === '' ? origDisplay : Math.max(1, Math.min(999, parseInt(raw, 10) || origDisplay));
      intervalEl.value = String(newNum);
      if (newNum === it.intervalMin) return;
      const now = Date.now();
      preserveCycle(it, now);
      it.intervalMin = newNum;
      updateOneTimer(it);
      startTicking(true);
      save();
    };
    intervalEl.addEventListener('change', applyInterval);
    intervalEl.addEventListener('keydown', e=>{ if (e.key === 'Enter'){ e.preventDefault(); intervalEl.blur(); } });
    intervalEl.addEventListener('pointerdown', e=>e.stopPropagation());
  }

  const maxEl = toastEl.querySelector('input[data-role="stamMax"]');
  const origMax = it.max;
  const applyMaxVal = (val) => {
    const newMax = clampInt(val, 1, 999, origMax);
    if (maxEl) maxEl.value = String(newMax);
    if (newMax === it.max) return;
    const now = Date.now();
    const curNow = stamInfo(it, now).cur;
    preserveCycle(it, now);
    it.max = newMax;
    it.current = Math.min(curNow, newMax);
    freezeIfFull(it, now);
    if (pending40Id === it.id) pending40Id = null;
    updateOneTimer(it);
    startTicking(true);
    save();
  };

  if (maxEl){
    maxEl.addEventListener('focus', ()=>{ maxEl.value = ''; });
    const applyMax = ()=>{
      const raw = String(maxEl.value || '').replace(/\D/g, '');
      const newMax = raw === '' ? origMax : raw;
      applyMaxVal(newMax);
    };
    maxEl.addEventListener('change', applyMax);
    maxEl.addEventListener('keydown', e=>{ if (e.key === 'Enter'){ e.preventDefault(); maxEl.blur(); } });
    maxEl.addEventListener('pointerdown', e=>e.stopPropagation());
  }

  const addMaxBtns = toastEl.querySelectorAll('button[data-act="addMax"]');
  addMaxBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      const delta = parseInt(btn.getAttribute('data-val'), 10) || 0;
      const currentVal = parseInt(maxEl ? maxEl.value : it.max, 10) || it.max || 1;
      applyMaxVal(currentVal + delta);
    });
    btn.addEventListener('pointerdown', e => e.stopPropagation());
  });
}

function orbToastRowsHtml(it){
  const isHours = it.intervalMin >= 60 && it.intervalMin % 60 === 0;
  const intervalVal = isHours ? (it.intervalMin / 60) : it.intervalMin;
  const intervalUnit = isHours ? '時間' : '分';
  const info = orbInfo(it, Date.now());
  const isUp = it.orbMode === 'up';
  const timeLabel = isUp ? '蓄積時間' : '全回復';
  const oneOrbMin = Math.max(1, it.intervalMin);
  const totalMin = isUp
    ? (info.isFull ? (it.max * oneOrbMin) : Math.floor(info.cur * oneOrbMin + Math.max(0, oneOrbMin * 60000 - info.nextInMs) / 60000))
    : (info.isFull ? 0 : Math.ceil(info.remainMs / 60000));
  const rh = Math.floor(totalMin / 60);
  const rm = totalMin % 60;
  const fullRemFormatted = String(rh).padStart(2, '0') + ':' + String(rm).padStart(2, '0');
  const chunkVal = it.useChunk != null ? clampUseChunk(it.useChunk) : '';

  return `<label class="toast-field"><span class="toast-color"><span class="toast-label">最大</span><input type="text" inputmode="numeric" pattern="[0-9]*" value="${it.max}" maxlength="2" data-role="orbMax" aria-label="最大個数"></span></label>`
    + `<label class="toast-field"><span class="toast-color"><span class="toast-label">回復(${intervalUnit})</span><input type="text" inputmode="numeric" pattern="[0-9]*" value="${intervalVal}" maxlength="3" data-role="orbInterval" aria-label="回復時間"></span></label>`
    + `<label class="toast-field"><span class="toast-color"><span class="toast-label" data-role="orbTimeLabel">${timeLabel}</span><input type="text" inputmode="numeric" pattern="[0-9]*" value="${fullRemFormatted}" maxlength="5" data-role="orbFullRem" placeholder="00:00" aria-label="${timeLabel}"></span></label>`
    + `<label class="toast-field"><span class="toast-color"><span class="toast-label">消費数</span><input type="text" inputmode="numeric" pattern="[0-9]*" value="${chunkVal}" maxlength="2" data-role="useChunk" aria-label="消費数" placeholder="なし"></span></label>`
    + `<button type="button" data-act="toggleOrbMode">${isUp ? '方式：▲ 経過時間(蓄積)' : '方式：▼ 残り時間(減算)'}</button>`;
}

function bindOrbToastRows(it){
  const chunkEl = toastEl.querySelector('input[data-role="useChunk"]');
  if (chunkEl){
    chunkEl.addEventListener('focus', ()=>{ chunkEl.value = ''; });
    const applyChunk = ()=>{
      const raw = String(chunkEl.value || '').replace(/\D/g, '');
      const num = Number(raw);
      if (raw === '' || num === 0){
        delete it.useChunk;
        chunkEl.value = '';
        if (pending40Id === it.id){ pending40Id = null; paintUseChunkPreview(it.id); }
      } else {
        it.useChunk = clampUseChunk(num);
        chunkEl.value = String(it.useChunk);
        if (pending40Id === it.id) paintUseChunkPreview(it.id);
      }
      if (refs[it.id]) refs[it.id].shortAction = hasUseChunk(it) ? (e) => onUseChunkTap(it, e) : null;
      updateOneTimer(it);
      save();
    };
    chunkEl.addEventListener('change', applyChunk);
    chunkEl.addEventListener('keydown', e=>{
      if (e.key === 'Enter'){ e.preventDefault(); chunkEl.blur(); }
    });
    chunkEl.addEventListener('pointerdown', e=>e.stopPropagation());
  }

  const maxEl = toastEl.querySelector('input[data-role="orbMax"]');
  if (maxEl){
    maxEl.addEventListener('focus', ()=>{ maxEl.value = ''; });
    const applyMax = ()=>{
      const raw = String(maxEl.value || '').replace(/\D/g, '');
      const newMax = clampInt(raw === '' ? it.max : raw, 1, 99, it.max);
      maxEl.value = String(newMax);
      if (newMax === it.max) return;
      const now = Date.now();
      const curNow = orbInfo(it, now).cur;
      preserveOrbCycle(it, now);
      it.max = newMax;
      it.current = Math.min(curNow, newMax);
      freezeOrbIfFull(it, now);
      updateOneTimer(it);
      startTicking(true);
      save();
    };
    maxEl.addEventListener('change', applyMax);
    maxEl.addEventListener('keydown', e=>{ if (e.key === 'Enter'){ e.preventDefault(); maxEl.blur(); } });
    maxEl.addEventListener('pointerdown', e=>e.stopPropagation());
  }

  const intervalEl = toastEl.querySelector('input[data-role="orbInterval"]');
  if (intervalEl){
    const isHours = it.intervalMin >= 60 && it.intervalMin % 60 === 0;
    const origDisplay = isHours ? (it.intervalMin / 60) : it.intervalMin;
    intervalEl.addEventListener('focus', ()=>{ intervalEl.value = ''; });
    const applyInterval = ()=>{
      const raw = String(intervalEl.value || '').replace(/\D/g, '');
      const newNum = raw === '' ? origDisplay : Math.max(1, Math.min(999, parseInt(raw, 10) || origDisplay));
      intervalEl.value = String(newNum);
      const newInterval = isHours ? (newNum * 60) : newNum;
      if (newInterval === it.intervalMin) return;
      const now = Date.now();
      preserveOrbCycle(it, now);
      it.intervalMin = newInterval;
      updateOneTimer(it);
      startTicking(true);
      save();
    };
    intervalEl.addEventListener('change', applyInterval);
    intervalEl.addEventListener('keydown', e=>{ if (e.key === 'Enter'){ e.preventDefault(); intervalEl.blur(); } });
    intervalEl.addEventListener('pointerdown', e=>e.stopPropagation());
  }

  const orbFullRemEl = toastEl.querySelector('input[data-role="orbFullRem"]');
  if (orbFullRemEl){
    orbFullRemEl.addEventListener('focus', ()=>{ orbFullRemEl.value = ''; });
    const applyOrbFullRem = ()=>{
      const raw = String(orbFullRemEl.value || '').trim();
      const now = Date.now();
      if (raw){
        applyOrbTimeInput(it, raw, now, true);
        updateOneTimer(it);
        startTicking(true);
        save();
      }
      const info = orbInfo(it, Date.now());
      const isUp = it.orbMode === 'up';
      const oneOrbMin = Math.max(1, it.intervalMin);
      const totalMin = isUp
        ? (info.isFull ? (it.max * oneOrbMin) : Math.floor(info.cur * oneOrbMin + Math.max(0, oneOrbMin * 60000 - info.nextInMs) / 60000))
        : (info.isFull ? 0 : Math.ceil(info.remainMs / 60000));
      const rh = Math.floor(totalMin / 60);
      const rm = totalMin % 60;
      orbFullRemEl.value = String(rh).padStart(2, '0') + ':' + String(rm).padStart(2, '0');
    };
    orbFullRemEl.addEventListener('change', applyOrbFullRem);
    orbFullRemEl.addEventListener('keydown', e=>{ if (e.key === 'Enter'){ e.preventDefault(); orbFullRemEl.blur(); } });
    orbFullRemEl.addEventListener('pointerdown', e=>e.stopPropagation());
  }
}

/* ═══════════ 放置トースト：設定時間／残り時間（ここから）═══════════ */
function idleHm(min){
  min = Math.max(0, Math.floor(min));
  return [Math.floor(min / 60), min % 60];
}
function idleModeRowHtml(countMode){
  const isUp = countMode === 'up';
  const modeLabel = isUp ? '▲ カウントアップ' : '▼ カウントダウン';
  return `<div class="toast-field wide"><span class="toast-color"><span class="toast-label">方式</span>`
    + `<button type="button" data-role="idleModeToggle" class="idle-mode-btn">${modeLabel}</button>`
    + `</span></div>`;
}
function idleToastRowsHtml(it){
  const isUp = it.countMode === 'up';
  const modeRow = idleModeRowHtml(it.countMode);
  const [dh, dm] = idleHm(it.durationMin);
  const info = idleInfo(it, Date.now());
  const curMin = isUp ? Math.floor(info.elapsed / 60000) : Math.ceil(info.remainMs / 60000);
  const [rh, rm] = idleHm(curMin);
  const rowLabel = isUp ? '経過' : '残り';
  const row = (key, label, h, m) =>
    `<div class="toast-field wide"><span class="toast-color"><span class="toast-label">${label}</span><span class="toast-hm">`
    + `<input type="text" inputmode="numeric" pattern="[0-9]*" maxlength="3" value="${h}" data-idle="${key}H" aria-label="${label}(時間)"><span>h</span>`
    + `<input type="text" inputmode="numeric" pattern="[0-9]*" maxlength="2" value="${m}" data-idle="${key}M" aria-label="${label}(分)"><span>m</span>`
    + `</span></span></div>`;
  return modeRow + row('dur', '設定', dh, dm) + row('rem', rowLabel, rh, rm);
}
function bindIdleToastRows(it){
  const box = toastEl && toastEl.querySelector('.toast-fields');
  if (!box) return;
  const dirty = { dur:false, rem:false };
  const q = k => box.querySelector(`[data-idle="${k}"]`);
  const rowOf = el => (el && el.dataset && el.dataset.idle) ? el.dataset.idle.slice(0, 3) : null;
  const read = (hk, mk) => clampInt(q(hk).value, 0, 999, 0) * 60 + clampInt(q(mk).value, 0, 59, 0);
  const sync = ()=>{
    const isUp = it.countMode === 'up';
    const [dh, dm] = idleHm(it.durationMin);
    const info = idleInfo(it, Date.now());
    const curMin = isUp ? Math.floor(info.elapsed / 60000) : Math.ceil(info.remainMs / 60000);
    const [rh, rm] = idleHm(curMin);
    for (const [k, v] of [['durH', dh], ['durM', dm], ['remH', rh], ['remM', rm]]){
      const el = q(k);
      if (el){ el.value = String(v); el.defaultValue = String(v); }
    }
  };
  const commit = row => {
    const now = Date.now();
    if (row === 'dur'){
      const total = Math.max(1, read('durH', 'durM'));
      if (total === it.durationMin) return;
      it.durationMin = total;
    } else {
      const isUp = it.countMode === 'up';
      const durMs = Math.max(1, it.durationMin) * 60000;
      if (isUp){
        const elapsedMs = Math.min(durMs, read('remH', 'remM') * 60000);
        it.start = now - elapsedMs;
      } else {
        const remainMs = Math.min(durMs, read('remH', 'remM') * 60000);
        it.start = now - (durMs - remainMs);
      }
      if (it.state === 'claim'){ it.state = 'running'; if (claimId === it.id) claimId = null; }
    }
    updateOneTimer(it);
    startTicking(true);
    save();
  };

  const modeBtn = box.querySelector('[data-role="idleModeToggle"]');
  if (modeBtn){
    bindTapDown(modeBtn, (e)=>{
      e.stopPropagation();
      it.countMode = it.countMode === 'up' ? 'down' : 'up';
      const isUp = it.countMode === 'up';
      modeBtn.textContent = isUp ? '▲ カウントアップ' : '▼ カウントダウン';
      const remLabelEl = box.querySelector('[data-idle="remH"]')?.closest('.toast-field')?.querySelector('.toast-label');
      if (remLabelEl) remLabelEl.textContent = isUp ? '経過' : '残り';
      sync();
      updateOneTimer(it);
      save();
    });
  }
  box.addEventListener('pointerdown', e=>e.stopPropagation());
  box.addEventListener('focusin', e=>{ if (rowOf(e.target)) e.target.value = ''; });
  box.addEventListener('input', e=>{ const r = rowOf(e.target); if (r) dirty[r] = true; });
  box.addEventListener('focusout', e=>{
    const t = e.target, row = rowOf(t);
    if (!row) return;
    if (t.value.trim() === '') t.value = t.defaultValue;
    setTimeout(()=>{
      if (rowOf(document.activeElement) === row) return;
      if (dirty[row]){ dirty[row] = false; commit(row); sync(); }
    }, 0);
  });
  box.addEventListener('keydown', e=>{
    if (e.key === 'Enter' && rowOf(e.target)){ e.preventDefault(); e.target.blur(); }
  });
}
/* ═══════════ 放置トースト：設定時間／残り時間（ここまで）═══════════ */// card-builders.js: 各カード(スタミナ/オーブ/放置/仕切り線/見出し)のDOM構築
// この並び順(index.htmlの<script>タグの順番)を変えると、他ファイルの関数/変数を先に参照してエラーになる場合があります。


function makeCardEl(type, id, innerHtml){
  const wrap = document.createElement('div');
  wrap.className = 'card ' + type;
  wrap.dataset.id = id;
  wrap.innerHTML = innerHtml;

  const btn = document.createElement('button');
  btn.type = 'button';
  btn.className = 'timer-menu-btn';
  btn.setAttribute('aria-label', 'タイマーメニュー');
  btn.title = 'タイマーメニュー';
  wrap.appendChild(btn);
  return wrap;
}

function valrowStamHtml(){
  return `<div class="row valrow-stam">
      <span class="stamval" data-role="curWrap"></span>
      <span class="slash">/</span>
      <span class="stammax" data-role="maxWrap"></span>
    </div>`;
}

function applyStamInput(it, rawVal, now){
  now = now || Date.now();
  const digits = String(rawVal || '').replace(/\D/g, '');
  if (!digits) return;
  const info = stamInfo(it, now);
  const num = Number(digits);
  if (isNaN(num)) return;
  const newCur = clampInt(num, 0, it.max, info.cur);
  preserveCycle(it, now);
  it.current = newCur;
  freezeIfFull(it, now);
}

function attachStamCurrentEditor(curWrap, it, extraCommit){
  const clearFortyPreview = ()=>{
    if (hasUseChunk(it) && pending40Id === it.id){
      pending40Id = null;
      paintUseChunkPreview(it.id);
    }
  };
  const curEditor = createInlineNumber(stamInfo(it, Date.now()).cur, value => {
    const now = Date.now();
    applyStamInput(it, value, now);
    if (typeof extraCommit === 'function') extraCommit();
    updateOneTimer(it);
    startTicking(true);
    saveAfterPaint();
  }, it.id + ':cur', clearFortyPreview, 3);
  curWrap.appendChild(curEditor.wrap);
  return curEditor;
}

function onUseChunkTap(it, e){
  if (e){ e.stopPropagation(); e.preventDefault(); }
  if (!hasUseChunk(it)) return;
  const now = Date.now();
  const isOrb = it.type === 'orb';
  const info = isOrb ? orbInfo(it, now) : stamInfo(it, now);
  if (pending40Id === it.id){
    if (isOrb){
      preserveOrbCycle(it, now);
      it.current = remainingAfterUse(info.cur, it);
      freezeOrbIfFull(it, now);
    } else {
      preserveCycle(it, now);
      it.current = remainingAfterUse(info.cur, it);
      freezeIfFull(it, now);
    }
    pending40Id = null;
    paintUseChunkPreview(it.id);
    startTicking(true);
    saveAfterPaint();
  } else {
    if (pending40Id && pending40Id !== it.id){
      const prev = pending40Id;
      pending40Id = null;
      paintUseChunkPreview(prev);
    }
    pending40Id = it.id;
    paintUseChunkPreview(it.id);
  }
}

function buildStamCard(it){
  const wrap = makeCardEl('stam', it.id, `
    <div class="clockstack stam-clock" data-role="clock"></div>
    ${valrowStamHtml()}
  `);
  const curWrap = wrap.querySelector('[data-role="curWrap"]');
  const maxWrap = wrap.querySelector('[data-role="maxWrap"]');
  const curEditor = attachStamCurrentEditor(curWrap, it, () => { pending40Id = null; });
  maxWrap.textContent = String(it.max);
  const r = { el: wrap, curEl: curEditor, maxLabel: maxWrap,
    clockEl: wrap.querySelector('[data-role="clock"]'),
    shortAction: hasUseChunk(it) ? (e) => onUseChunkTap(it, e) : null };
  refs[it.id] = r;
  bindTimerShortAction(wrap, (e) => r.shortAction ? r.shortAction(e) : null);
  return wrap;
}

function applyOrbInput(it, rawVal, now){
  now = now || Date.now();
  const digits = String(rawVal || '').replace(/\D/g, '');
  if (!digits) return;
  const info = orbInfo(it, now);
  if (digits.length <= 2){
    const num = Number(digits);
    if (isNaN(num)) return;
    const newCur = clampInt(num, 0, it.max, info.cur);
    preserveOrbCycle(it, now);
    it.current = newCur;
    freezeOrbIfFull(it, now);
    return;
  }
  applyOrbTimeInput(it, digits, now, false);
}

function attachOrbCurrentEditor(curWrap, it){
  const clearChunkPreview = ()=>{
    if (hasUseChunk(it) && pending40Id === it.id){
      pending40Id = null;
      paintUseChunkPreview(it.id);
    }
  };
  const curEditor = createInlineNumber(orbInfo(it, Date.now()).cur, value => {
    const now = Date.now();
    applyOrbInput(it, value, now);
    updateOneTimer(it);
    startTicking(true);
    saveAfterPaint();
  }, it.id + ':cur', clearChunkPreview, 4);
  curWrap.appendChild(curEditor.wrap);
  return curEditor;
}

function buildOrbCard(it){
  const wrap = makeCardEl('orb', it.id, `
    <div class="orb-toprow">
      <div class="orb-next-rem" data-role="nextRem">
        <span class="orb-next-lbl">次</span><span class="orb-next-val" data-role="nextVal"></span>
      </div>
      <div class="clockstack orb-clock" data-role="clock"></div>
    </div>
    ${valrowStamHtml()}
  `);
  const curWrap = wrap.querySelector('[data-role="curWrap"]');
  const maxWrap = wrap.querySelector('[data-role="maxWrap"]');
  const curEditor = attachOrbCurrentEditor(curWrap, it);
  maxWrap.textContent = String(it.max);
  const r = { el: wrap, curEl: curEditor, maxLabel: maxWrap,
    clockEl: wrap.querySelector('[data-role="clock"]'),
    nextRemEl: wrap.querySelector('[data-role="nextRem"]'),
    nextValEl: wrap.querySelector('[data-role="nextVal"]'),
    shortAction: hasUseChunk(it) ? (e) => onUseChunkTap(it, e) : null };
  refs[it.id] = r;
  bindTimerShortAction(wrap, (e) => r.shortAction ? r.shortAction(e) : null);
  return wrap;
}

function buildIdleCard(it){
  const wrap = makeCardEl(it.type, it.id, `
    <div class="clockstack idle-clock" data-role="clock"></div>
    <div class="row valrow-idle">
      <div class="main" data-role="cur"></div>
    </div>
  `);
  refs[it.id] = { el: wrap,
    curEl: wrap.querySelector('[data-role="cur"]'), clockEl: wrap.querySelector('[data-role="clock"]') };
  bindTimerShortAction(wrap, ()=>toggleIdle(it));
  return wrap;
}

function buildTimerChild(it){
  if (it.type==='stam') return buildStamCard(it);
  if (it.type==='orb') return buildOrbCard(it);
  if (it.type==='idle' || it.type==='exped') return buildIdleCard(it);
  return null;
}
function buildGroupCard(it){
  const wrap = document.createElement('div');
  wrap.className = 'card group';
  wrap.dataset.id = it.id;
  if (it.color){
    wrap.style.borderColor = it.color;
  }
  wrap.innerHTML = `
    <div class="row namerow group-namerow" data-role="namerow"></div>
    <div class="group-body" data-role="body"></div>
  `;
  const nameRowEl = wrap.querySelector('[data-role="namerow"]');
  const nameEditor = createInlineText(it.name || '', 'アカウント', (value)=>{
    if (it.name !== value){ it.name = value; save(); }
  });
  if (it.color){
    nameEditor.setColor(it.color);
  }
  nameRowEl.appendChild(nameEditor.wrap);
  refs[it.id] = { el: wrap, nameEl: nameEditor.wrap, nameEditor,
    bodyEl: wrap.querySelector('[data-role="body"]'),
    childRefs: {} };
  renderGroupBody(it);
  return wrap;
}
function renderGroupBody(it){
  const r = refs[it.id];
  if (!r) return;
  if (!Array.isArray(it.children)) it.children = [];
  it.children = it.children.filter(c => c && typeof c === 'object' && c.id && (c.type === 'stam' || c.type === 'idle' || c.type === 'orb' || c.type === 'exped'));
  const ids = new Set(it.children.map(c => c.id));
  for (const id of Object.keys(r.childRefs)){
    if (!ids.has(id)) delete r.childRefs[id];
  }
  const count = it.children.length;
  const is2x2 = (it.layout === '2x2' && count > 2);
  r.el.classList.toggle('single', count <= 1);
  r.el.classList.toggle('two', count === 2);
  r.el.classList.toggle('three', count === 3 && !is2x2);
  r.el.classList.toggle('four', count === 4 && !is2x2);
  r.el.classList.toggle('layout-2x2', is2x2);

  if (is2x2){
    r.bodyEl.style.gridTemplateColumns = 'repeat(2, 1fr)';
  } else {
    r.bodyEl.style.gridTemplateColumns = `repeat(${Math.max(1, count)}, 1fr)`;
  }

  const emptySlot = r.bodyEl.querySelector('[data-role="emptySlot"]');
  if (count === 0){
    if (!emptySlot){
      const slot = document.createElement('div');
      slot.className = 'group-empty-slot';
      slot.dataset.role = 'emptySlot';
      slot.innerHTML = '<button type="button" class="group-empty-add" aria-label="タイマーを追加">＋</button>';
      const addEl = slot.querySelector('.group-empty-add');
      bindTapDown(addEl, (e)=>{
        e.stopPropagation();
        openAddPanel({ groupId: it.id });
      });
      r.bodyEl.appendChild(slot);
    }
  } else if (emptySlot){
    emptySlot.remove();
  }

  for (const child of it.children){
    if (r.childRefs[child.id]) continue;
    const el = buildTimerChild(child);
    if (!el) continue;
    r.childRefs[child.id] = refs[child.id];
    r.bodyEl.appendChild(el);
  }
}
function buildRuleCard(it){
  const wrap = document.createElement('div');
  wrap.className = 'rule-line';
  wrap.dataset.id = it.id;
  wrap.style.borderTopColor = it.color || '#52617a';
  wrap.innerHTML = '';
  refs[it.id] = { el: wrap };
  return wrap;
}

// トーストメニューを開く入口はすべてここに集約（#cards への委譲・pointerdown）。
// タイマー丸ボタン／見出し丸ボタン／アカウント名ラベル／仕切り線の右端ゾーン。
// MENU_BTN_SEL はこのファイルでのみ定義。card-logic.js と resume-fit.js から参照される
// （両方ともindex.html上でこのファイルより後に読み込まれるので今は安全。並び順を変える場合は要注意）。
const MENU_BTN_SEL = '.timer-menu-btn, .header-menu-button, .group-namerow, .rule-line';
function bindMenuButtonDelegation(){
  if (!cardsEl) return;
  const isPrimary = e => e.isPrimary !== false && !(e.pointerType === 'mouse' && e.button !== 0);
  const menuBtnOf = e => (e.target && e.target.closest) ? e.target.closest(MENU_BTN_SEL) : null;
  cardsEl.addEventListener('pointerdown', e=>{
    const btn = menuBtnOf(e);
    if (!btn || !isPrimary(e)) return;
    cancelAllPendingStates();
    e.preventDefault();
    e.stopPropagation();
    btn.blur();
    const host = btn.closest('[data-id]');
    const id = host ? host.dataset.id : null;
    if (id == null) return;
    if (toastEl && toastEl.classList.contains('show') && currentToastId === id){
      closeToast();
      return;
    }
    // 指を離すまでのイベントを確実に遮断し、開いたトースト側へのゴースト判定を防ぐ
    const pid = e.pointerId;
    const blockUntilUp = (ev)=>{
      if (ev.pointerId === pid || ev.type === 'click'){
        ev.preventDefault();
        ev.stopPropagation();
        if (ev.type === 'pointerup' || ev.type === 'pointercancel'){
          window.removeEventListener('pointerup', blockUntilUp, true);
          window.removeEventListener('pointercancel', blockUntilUp, true);
          window.removeEventListener('click', blockUntilUp, true);
        }
      }
    };
    window.addEventListener('pointerup', blockUntilUp, {capture:true, passive:false});
    window.addEventListener('pointercancel', blockUntilUp, {capture:true, passive:false});
    window.addEventListener('click', blockUntilUp, {capture:true, passive:false});

    menuOpenRaf = requestAnimationFrame(()=>{
      menuOpenRaf = 0;
      askRemoveItem(id);
      if (toastEl){
        toastEl.style.pointerEvents = 'none';
        setTimeout(()=>{ if (toastEl) toastEl.style.pointerEvents = ''; }, 120);
      }
    });
  }, {capture:true, passive:false});
  cardsEl.addEventListener('click', e=>{
    if (!menuBtnOf(e)) return;
    e.preventDefault();
    e.stopPropagation();
  }, {capture:true, passive:false});
  // 入口を長押ししてもOSの選択・メニューを出さない
  cardsEl.addEventListener('contextmenu', e=>{
    if (menuBtnOf(e)) e.preventDefault();
  });
}
bindMenuButtonDelegation();

function buildHeaderCard(it){
  const wrap = document.createElement('div');
  wrap.className = 'divider';
  const nameEditor = createInlineText(it.name || '', '見出し(ゲーム名など)', (value)=>{
    if (it.name !== value){ it.name = value; save(); }
  });
  nameEditor.setColor(it.color || 'var(--accent)');
  wrap.appendChild(nameEditor.wrap);
  wrap.dataset.id = it.id;

  const badgeEl = document.createElement('span');
  badgeEl.className = 'header-fold-badge';
  badgeEl.textContent = '';
  wrap.appendChild(badgeEl);

  const menuBtn = document.createElement('button');
  menuBtn.type = 'button';
  menuBtn.className = 'header-menu-button';
  menuBtn.setAttribute('aria-label', '見出しメニュー');
  menuBtn.title = '見出しメニュー';
  menuBtn.style.setProperty('--header-color', it.color || '#9b8bff');
  menuBtn.textContent = '';
  wrap.appendChild(menuBtn);
  refs[it.id] = { el: wrap, nameEl: nameEditor.wrap, nameEditor, badgeEl, menuBtn };

  // 見出しタップで折りたたみトグル（誤タップ防止のロック時は無視）
  let pointerStart = null;
  wrap.addEventListener('pointerdown', (e)=>{
    if (e.target.closest('.header-menu-button')) return;
    if (e.isPrimary === false) return;
    if (e.pointerType === 'mouse' && e.button !== 0) return;
    if (it.foldLock) return;
    pointerStart = { x: e.clientX, y: e.clientY };
  });
  wrap.addEventListener('pointerup', (e)=>{
    if (!pointerStart) return;
    const dx = e.clientX - pointerStart.x;
    const dy = e.clientY - pointerStart.y;
    pointerStart = null;
    if (Math.hypot(dx, dy) < 10){
      it.collapsed = !it.collapsed;
      save();
      render();
    }
  });
  wrap.addEventListener('pointercancel', ()=>{ pointerStart = null; });

  return wrap;
}
// card-logic.js: カード参照・移動・削除・render()本体・updateTimerCard
// この並び順(index.htmlの<script>タグの順番)を変えると、他ファイルの関数/変数を先に参照してエラーになる場合があります。

function getTimerRef(id){
  const direct = refs[id];
  if (direct) return direct;
  for (const it of state.items){
    if (it.type==='group' && it.children.some(c=>c.id===id)) return refs[it.id]?.childRefs[id] || null;
  }
  return null;
}
function updateOneTimer(it){
  updateTimerCard(it, getTimerRef(it.id), Date.now());
}
function toggleIdle(it){
  if (it.state === 'claim'){
    it.state = 'running';
    it.start = Date.now();
    if (claimId === it.id) claimId = null;
    updateOneTimer(it);
    startTicking(true);
  } else {
    it.state = 'claim';
    claimId = it.id;
    updateOneTimer(it);
    tickGen++;
    if (tickId){ clearTimeout(tickId); tickId = null; }
  }
  saveAfterPaint();
}
function cancelClaim(target){
  const id = (target && typeof target === 'object') ? target.id : target;
  const it = findItemById(id);
  if (it){
    it.state = 'running';
    updateOneTimer(it);
  }
  if (claimId === id) claimId = null;
  startTicking(true);
  saveAfterPaint();
}

// 「枠外タップで閉じる」系はここに集約（document-levelのpointerdownは1本だけにする）。
document.addEventListener('pointerdown', (e) => {
  try {
    if (document.activeElement !== e.target && window.getSelection){
      const sel = window.getSelection();
      if (sel && sel.rangeCount) sel.removeAllRanges();
    }
  } catch(err){}
  // 移動選択中（入れ替え待機中）の場合：
  if (movingItemId != null){
    e.stopPropagation();

    const host = (e.target && e.target.closest) ? e.target.closest('[data-id]') : null;
    const targetTopId = host ? (getTopLevelItemId(host.dataset.id) || host.dataset.id) : null;

    if (!targetTopId || targetTopId === movingItemId){
      // 自身または余白タップ：キャンセル
      cancelMove();
      return;
    }

    // 別のトップレベル要素（アカウント枠など）がタップされた：即座にスワップ！
    const srcId = movingItemId;
    cancelMove();
    swapTopItems(srcId, targetTopId);
    return;
  }

  // 待機中（受取・再出発・使い切り計算中）のタイマーがある場合：
  const activePendingId = claimId || pending40Id;
  if (activePendingId != null){
    const r = getTimerRef(activePendingId);
    if (!r || !r.el.contains(e.target)){
      if (claimId != null) cancelClaim(claimId);
      if (pending40Id != null){
        const id = pending40Id;
        pending40Id = null;
        paintUseChunkPreview(id);
      }
      e.stopPropagation();
      return;
    }
  }
  // ポップアップモーダル（トースト・追加パネル・設定パネル）の枠外タップ処理を一本化
  const isSetupOpen = setupPanelEl && setupPanelEl.classList.contains('show');
  const isAddOpen = addPanelEl && addPanelEl.classList.contains('show');
  const isToastOpen = toastEl && toastEl.classList.contains('show');

  if (isSetupOpen || isAddOpen || isToastOpen){
    const inSetup = isSetupOpen && setupPanelEl.contains(e.target);
    const inAdd = isAddOpen && (addPanelEl.contains(e.target) || e.target === addBtnEl);
    const inToast = isToastOpen && (toastEl.contains(e.target) || e.target.closest(MENU_BTN_SEL));

    if (!inSetup && !inAdd && !inToast){
      e.preventDefault();
      e.stopPropagation();
      try {
        const activeInput = document.querySelector('#setupFields input:focus, #toast input:focus');
        if (activeInput) activeInput.blur();
      } catch (err) {}
      if (isSetupOpen) closeSetup();
      if (isAddOpen){
        closeAddPanel();
        pendingAddGroupId = null;
        pendingInsertAfterId = null;
      }
      if (isToastOpen) closeToast();
      return;
    }
  }
}, {capture:true, passive:false});

function getTopLevelItemId(id){
  if (!id) return null;
  const top = state.items.find(i => i.id === id);
  if (top) return top.id;
  for (const it of state.items){
    if (it.type === 'group' && it.children && it.children.some(c => c.id === id)){
      return it.id;
    }
  }
  return null;
}

function startMoveItem(id){
  cancelAllPendingStates();
  const topId = getTopLevelItemId(id) || id;
  const it = findItemById(topId);
  if (!it) return;

  movingItemId = topId;
  const ref = refs[topId];
  if (ref && ref.el){
    ref.el.classList.add('moving-source');
  }
}

function cancelMove(){
  if (movingItemId != null){
    const ref = refs[movingItemId];
    if (ref && ref.el){
      ref.el.classList.remove('moving-source');
    }
    if (cardsEl){
      cardsEl.querySelectorAll('.moving-source').forEach(el => el.classList.remove('moving-source'));
    }
    movingItemId = null;
  }
}

function swapTopItems(idA, idB){
  const idxA = state.items.findIndex(i => i.id === idA);
  const idxB = state.items.findIndex(i => i.id === idB);
  if (idxA === -1 || idxB === -1 || idxA === idxB) return;
  const temp = state.items[idxA];
  state.items[idxA] = state.items[idxB];
  state.items[idxB] = temp;
  save();
  render();
  startTicking(true);
}

function removeItem(id){
  if (refs[id]) refs[id].el.remove();
  delete refs[id];
  const topIdx = state.items.findIndex(i=>i.id===id);
  if (topIdx !== -1){
    state.items.splice(topIdx, 1);
  } else {
    for (const it of state.items){
      if (it.type !== 'group') continue;
      const idx = it.children.findIndex(c=>c.id===id);
      if (idx !== -1){
        it.children.splice(idx, 1);
        const parentRef = refs[it.id];
        if (parentRef) delete parentRef.childRefs[id];
        break;
      }
    }
  }
  save(); render(); startTicking(true);
}

function renderClock(el, remainMs, isFull, now, fullAt){
  if (!el) return;
  const timeStr = fmtHM(isFull ? (fullAt || now) : ((now || Date.now()) + Math.max(0, remainMs)));
  const key = isFull ? ('F:' + timeStr) : timeStr;
  if (el.dataset.clk === key) return;
  el.dataset.clk = key;
  if (isFull) el.innerHTML = `<span class="full-at">${timeStr}</span>`;
  else el.textContent = timeStr;
}

function findItemById(id){
  for (const it of state.items){
    if (it.id === id) return it;
    if (it.type === 'group'){
      const c = it.children.find(ch=>ch.id===id);
      if (c) return c;
    }
  }
  return null;
}
function paintUseChunkPreview(id){
  const it = findItemById(id);
  if (it && (it.type === 'stam' || it.type === 'orb')) updateOneTimer(it);
}
function render(){
  const now = Date.now();
  syncFullStamItems(now);
  emptyEl.style.display = state.items.length ? 'none' : 'block';

  const existingIds = new Set(state.items.map(i=>i.id));
  state.items.forEach(it=>{ if (it.type==='group') it.children.forEach(c=>existingIds.add(c.id)); });
  Object.keys(refs).forEach(id=>{ if(!existingIds.has(id)) delete refs[id]; });

  for (const it of state.items){
    if (!refs[it.id]){
      it.type==='stam' ? buildStamCard(it) : it.type==='orb' ? buildOrbCard(it) : (it.type==='idle' || it.type==='exped') ? buildIdleCard(it) : it.type==='rule' ? buildRuleCard(it) : it.type==='group' ? buildGroupCard(it) : buildHeaderCard(it);
    } else if (it.type==='group'){
      renderGroupBody(it);
    }
  }

  const topEls = state.items.map(it => refs[it.id].el);

  let needOrder = false;
  const kids = cardsEl.children;
  for (let i = 0; i < topEls.length; i++){
    if (kids[i] !== topEls[i]){ needOrder = true; break; }
  }
  if (needOrder){
    for (const el of topEls) cardsEl.appendChild(el);
  }

  let currentCollapsed = false;
  let collapsedCount = 0;
  let currentHeaderRef = null;

  for (let i = 0; i < state.items.length; i++){
    const it = state.items[i];
    const r = refs[it.id];
    if (!r || !r.el) continue;

    if (it.type === 'header'){
      if (currentHeaderRef && currentCollapsed && currentHeaderRef.badgeEl){
        currentHeaderRef.badgeEl.textContent = `▸ ${collapsedCount}件`;
        currentHeaderRef.badgeEl.style.display = collapsedCount > 0 ? 'inline-block' : 'none';
      }
      currentCollapsed = !!it.collapsed && !it.foldLock;
      currentHeaderRef = r;
      collapsedCount = 0;
      r.el.style.display = '';
      r.el.classList.toggle('is-collapsed', currentCollapsed);
      r.el.classList.toggle('is-locked', !!it.foldLock);
      if (r.badgeEl && !currentCollapsed){
        r.badgeEl.style.display = 'none';
      }
    } else {
      if (currentCollapsed){
        r.el.style.display = 'none';
        if (it.type !== 'rule') collapsedCount++;
      } else {
        r.el.style.display = '';
      }
    }

    if (it.type==='rule') r.el.style.borderTopColor = it.color || '#52617a';
    if (it.type==='header' && r.nameEditor){
      r.nameEditor.setColor(it.color || '#9b8bff');
      if (!r.nameEditor.isEditing()) r.nameEditor.setText(it.name || '');
    }
    if (it.type==='group'){
      if (r.nameEditor && !r.nameEditor.isEditing()) r.nameEditor.setText(it.name || '');
      for (const child of it.children) updateTimerCard(child, r.childRefs[child.id], now);
    } else {
      updateTimerCard(it, r, now);
    }
  }

  if (currentHeaderRef && currentCollapsed && currentHeaderRef.badgeEl){
    currentHeaderRef.badgeEl.textContent = `▸ ${collapsedCount}件`;
    currentHeaderRef.badgeEl.style.display = collapsedCount > 0 ? 'inline-block' : 'none';
  }
}

// 描画専用：副作用を持たず純粋にDOMに値を反映する
let updateTimerCard = function(it, r, now){
  if (!it || !r) return;
  if (it.type==='stam'){
    const info = stamInfo(it, now);
    const waitChunk = hasUseChunk(it) && pending40Id === it.id;
    r.el.classList.toggle('full', info.isFull);
    r.el.classList.toggle('claim', waitChunk);
    renderClock(r.clockEl, info.remainMs, info.isFull, now, info.fullAt);
    if (r.curEl && !r.curEl.isEditing()) r.curEl.setText(waitChunk ? remainingAfterUse(info.cur, it) : info.cur);
    if (r.maxLabel){
      const v = String(it.max);
      if (r.maxLabel.textContent !== v) r.maxLabel.textContent = v;
    }
    setNearOnCurrent(r.curEl, isNearFull(info.remainMs, info.isFull));
    if (r.el){ fitObserve(r.el); fitApply(r.el); }
  } else if (it.type==='orb'){
    const info = orbInfo(it, now);
    const waitChunk = hasUseChunk(it) && pending40Id === it.id;
    r.el.classList.toggle('full', info.isFull);
    r.el.classList.toggle('claim', waitChunk);
    renderClock(r.clockEl, info.remainMs, info.isFull, now, info.fullAt);
    if (r.curEl && !r.curEl.isEditing()) r.curEl.setText(waitChunk ? remainingAfterUse(info.cur, it) : info.cur);
    if (r.maxLabel){
      const v = String(it.max);
      if (r.maxLabel.textContent !== v) r.maxLabel.textContent = v;
    }
    if (r.nextRemEl){
      if (info.isFull){
        if (r.nextRemEl.style.display !== 'none') r.nextRemEl.style.display = 'none';
      } else {
        if (r.nextRemEl.style.display !== '') r.nextRemEl.style.display = '';
        const cd = fmtCountdown(info.nextInMs);
        if (r.nextValEl && r.nextValEl.textContent !== cd) r.nextValEl.textContent = cd;
      }
    }
    setNearOnCurrent(r.curEl, isNearFull(info.remainMs, info.isFull));
  } else if (it.type==='idle' || it.type==='exped'){
    if (it.state === 'claim' && claimId !== it.id){
      it.state = 'running';
    }
    const info = idleInfo(it, now);
    r.el.classList.toggle('claim', it.state==='claim');
    r.el.classList.toggle('full', info.isFull && it.state!=='claim');
    const runningLabel = (it.countMode === 'up') ? fmtElapsed(info.elapsed) : fmtCountdown(info.remainMs);
    const fullText = it.type === 'exped' ? '帰還' : 'MAX';
    const label = it.state==='claim' ? (it.type === 'exped' ? '再出発' : '受取') : (info.isFull ? fullText : runningLabel);
    if (r.curEl.textContent !== label) r.curEl.textContent = label;
    const isWarn = !info.isFull && (info.remainMs > 0 && info.remainMs < 7200000);
    const isStop = info.isFull || it.state==='claim';
    r.curEl.classList.toggle('warn', isWarn);
    r.curEl.classList.toggle('stop', isStop);
    r.curEl.classList.remove('near');
    renderClock(r.clockEl, info.remainMs, info.isFull, now, info.fullAt);
  }
}
// panels.js: 追加パネル・設定パネル(色選択含む)の開閉と確定処理
// この並び順(index.htmlの<script>タグの順番)を変えると、他ファイルの関数/変数を先に参照してエラーになる場合があります。

let pendingAddGroupId = null;
let pendingInsertAfterId = null;
let setupType = null;
let setupIdleMode = 'down';
let setupOrbMode = 'down';

const addPanelLabelEl = document.getElementById('addPanelLabel');

function closeAddPanel(){
  addPanelEl.classList.remove('show');
  addPanelEl.classList.remove('group-mode');
}
function openAddPanel(opts){
  opts = opts || {};
  pendingAddGroupId = opts.groupId || null;
  pendingInsertAfterId = opts.insertAfterId || null;
  const isGroup = !!pendingAddGroupId;
  addPanelEl.classList.toggle('group-mode', isGroup);
  if (addPanelLabelEl){
    addPanelLabelEl.textContent = isGroup ? 'タイマーを追加' : '枠を追加';
  }
  if (!opts.groupId) pendingAddGroupId = null;
  if (!opts.insertAfterId) pendingInsertAfterId = null;
  addPanelEl.classList.add('show');
  closeSetup();
}
function closeSetup(){
  setupType = null;
  setupPanelEl.classList.remove('show');
}
const SHARED_COLORS = ['#9b8bff','#b48cff','#6fc7ff','#70d6b0','#ffd166','#ff9f68','#ff7b9c','#d7dbe7','#52617a'];
function fillColorPalette(defaultColor){
  const current = defaultColor || SHARED_COLORS[0];
  setupFieldsEl.innerHTML = `<div class="header-palette">${SHARED_COLORS.map(c=>`<button type="button" class="header-swatch${c===current?' selected':''}" data-color="${c}" style="background:${c}" aria-label="${c}"></button>`).join('')}</div>`;
  setupFieldsEl.dataset.color = current;
  setupFieldsEl.querySelectorAll('.header-swatch').forEach(btn=>{
    bindTapDown(btn, ()=>{
      setupFieldsEl.querySelectorAll('.header-swatch').forEach(b=>b.classList.remove('selected'));
      btn.classList.add('selected');
      setupFieldsEl.dataset.color = btn.dataset.color;
    });
  });
}
function openSetup(type){
  setupType = type;
  closeAddPanel();
  setupFieldsEl.innerHTML = '';
  delete setupFieldsEl.dataset.color;
  if (type === 'header'){
    setupLabelEl.textContent = '見出しの文字色';
    fillColorPalette('#9b8bff');
  } else if (type === 'stam'){
    setupLabelEl.textContent = 'スタミナ設定';
    setupFieldsEl.innerHTML = `
      <label class="toast-field">
        <span class="toast-color">
          <span class="toast-label">回復</span>
          <span class="toast-val-unit">
            <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupInterval" value="5" maxlength="3" aria-label="回復時間">
            <span class="toast-unit">分</span>
          </span>
        </span>
      </label>
      <label class="toast-field">
        <span class="toast-color">
          <span class="toast-label">最大</span>
          <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupMax" value="100" maxlength="3" aria-label="最大スタミナ">
        </span>
      </label>
      <label class="toast-field wide">
        <span class="toast-color">
          <span class="toast-label">使い切り</span>
          <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupUseChunk" value="" maxlength="3" placeholder="なし" aria-label="使い切り数">
        </span>
      </label>`;
  } else if (type === 'orb'){
    setupLabelEl.textContent = 'オーブ設定';
    setupOrbMode = 'down';
    setupFieldsEl.innerHTML = `
      <label class="toast-field">
        <span class="toast-color">
          <span class="toast-label">最大</span>
          <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupMax" value="4" maxlength="2" aria-label="最大個数">
        </span>
      </label>
      <label class="toast-field">
        <span class="toast-color">
          <span class="toast-label">回復(時間)</span>
          <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupOrbHours" value="6" maxlength="3" aria-label="回復時間">
        </span>
      </label>
      <label class="toast-field wide">
        <span class="toast-color">
          <span class="toast-label">消費数</span>
          <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupOrbChunk" value="" maxlength="2" placeholder="なし" aria-label="消費数">
        </span>
      </label>
      <div class="toast-field wide">
        <span class="toast-color">
          <span class="toast-label">方式</span>
          <button type="button" class="idle-mode-btn" data-role="orbModeToggle">▼ 残り時間(減算)</button>
        </span>
      </div>`;
    const modeBtn = setupFieldsEl.querySelector('[data-role="orbModeToggle"]');
    if (modeBtn){
      bindTapDown(modeBtn, (e)=>{
        e.stopPropagation();
        setupOrbMode = (setupOrbMode === 'up') ? 'down' : 'up';
        modeBtn.textContent = (setupOrbMode === 'up') ? '▲ 経過時間(蓄積)' : '▼ 残り時間(減算)';
      });
    }
  } else if (type === 'rule'){
    setupLabelEl.textContent = '仕切り線の色';
    fillColorPalette('#52617a');
  } else if (type === 'exped'){
    setupLabelEl.textContent = '遠征タイマーの設定';
    setupIdleMode = 'down';
    setupFieldsEl.innerHTML = `
      <div class="toast-field wide">
        <span class="toast-color">
          <span class="toast-label">方式</span>
          <button type="button" class="idle-mode-btn" data-role="idleModeToggle">▼ カウントダウン</button>
        </span>
      </div>
      <div class="toast-field wide">
        <span class="toast-color">
          <span class="toast-label">設定</span>
          <span class="toast-hm">
            <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupH" value="4" maxlength="3" aria-label="設定(時間)"><span>h</span>
            <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupM" value="0" maxlength="2" aria-label="設定(分)"><span>m</span>
          </span>
        </span>
      </div>`;
    const modeBtn = setupFieldsEl.querySelector('[data-role="idleModeToggle"]');
    if (modeBtn){
      bindTapDown(modeBtn, (e)=>{
        e.stopPropagation();
        setupIdleMode = setupIdleMode === 'up' ? 'down' : 'up';
        modeBtn.textContent = setupIdleMode === 'up' ? '▲ カウントアップ' : '▼ カウントダウン';
      });
    }
  } else {
    setupLabelEl.textContent = '放置報酬の設定';
    setupIdleMode = 'down';
    setupFieldsEl.innerHTML = `
      <div class="toast-field wide">
        <span class="toast-color">
          <span class="toast-label">方式</span>
          <button type="button" class="idle-mode-btn" data-role="idleModeToggle">▼ カウントダウン</button>
        </span>
      </div>
      <div class="toast-field wide">
        <span class="toast-color">
          <span class="toast-label">設定</span>
          <span class="toast-hm">
            <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupH" value="12" maxlength="3" aria-label="設定(時間)"><span>h</span>
            <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupM" value="0" maxlength="2" aria-label="設定(分)"><span>m</span>
          </span>
        </span>
      </div>`;
    const modeBtn = setupFieldsEl.querySelector('[data-role="idleModeToggle"]');
    if (modeBtn){
      bindTapDown(modeBtn, (e)=>{
        e.stopPropagation();
        setupIdleMode = setupIdleMode === 'up' ? 'down' : 'up';
        modeBtn.textContent = setupIdleMode === 'up' ? '▲ カウントアップ' : '▼ カウントダウン';
      });
    }
  }
  setupFieldsEl.querySelectorAll('input').forEach(el=>{
    if (el.type === 'color') return;
    clearOnFocus(el);
  });
  setupPanelEl.classList.add('show');
}
function pushTopItem(item){
  const targetId = getTopLevelItemId(pendingInsertAfterId);
  if (targetId){
    const idx = state.items.findIndex(i => i.id === targetId);
    if (idx !== -1){
      state.items.splice(idx + 1, 0, item);
    } else {
      state.items.push(item);
    }
    pendingInsertAfterId = null;
  } else {
    state.items.push(item);
  }
}
function pushNewTimer(item){
  const group = pendingAddGroupId ? state.items.find(g=>g.id===pendingAddGroupId && g.type==='group') : null;
  if (pendingAddGroupId){
    if (!group || group.children.length >= 4){
      pendingAddGroupId = null;
      closeAddPanel();
      return false;
    }
    group.children.push(item);
  } else {
    pushTopItem(item);
  }
  pendingAddGroupId = null;
  closeAddPanel();
  return true;
}
function confirmSetup(){
  if (!setupType) return;
  if (setupType === 'header'){
    const item = newHeaderItem();
    item.color = setupFieldsEl.dataset.color || '#9b8bff';
    pushTopItem(item);
  } else if (setupType === 'stam'){
    const interval = clampInt(setupFieldsEl.querySelector('#setupInterval')?.value, 1, 999, 5);
    const max = clampInt(setupFieldsEl.querySelector('#setupMax')?.value, 1, 999, 100);
    const chunkRaw = String(setupFieldsEl.querySelector('#setupUseChunk')?.value || '').replace(/\D/g, '');
    const useChunk = chunkRaw === '' ? null : clampUseChunk(chunkRaw);
    if (!pushNewTimer(newStamItem(interval, max, useChunk))) return;
  } else if (setupType === 'orb'){
    const hours = clampInt(setupFieldsEl.querySelector('#setupOrbHours')?.value, 1, 999, 6);
    const max = clampInt(setupFieldsEl.querySelector('#setupMax')?.value, 1, 99, 4);
    const chunkRaw = String(setupFieldsEl.querySelector('#setupOrbChunk')?.value || '').replace(/\D/g, '');
    const useChunk = chunkRaw === '' ? null : clampUseChunk(chunkRaw);
    const item = newOrbItem(hours * 60, max, useChunk);
    item.orbMode = setupOrbMode || 'down';
    if (!pushNewTimer(item)) return;
  } else if (setupType === 'rule'){
    const color = setupFieldsEl.dataset.color || '#52617a';
    pushTopItem(newRuleItem(color));
  } else if (setupType === 'exped'){
    const h = clampInt(setupFieldsEl.querySelector('#setupH')?.value, 0, 999, 4);
    const m = clampInt(setupFieldsEl.querySelector('#setupM')?.value, 0, 59, 0);
    if (!pushNewTimer(newExpedItem(h*60+m, setupIdleMode))) return;
  } else if (setupType === 'idle'){
    const h = clampInt(setupFieldsEl.querySelector('#setupH')?.value, 0, 999, 12);
    const m = clampInt(setupFieldsEl.querySelector('#setupM')?.value, 0, 59, 0);
    if (!pushNewTimer(newIdleItem(h*60+m, setupIdleMode))) return;
  }
  closeSetup();
  save(); render(); startTicking(true);
}

bindTapDown(addBtnEl, ()=>{
  if (addPanelEl.classList.contains('show') && !addPanelEl.classList.contains('group-mode')){
    closeAddPanel();
  } else {
    openAddPanel();
  }
});
bindTapDown(addPanelEl, (e)=>{
  const btn = e.target.closest('button[data-type]');
  if (!btn) return;
  const type = btn.dataset.type;
  if (type === 'group'){
    pushTopItem(newGroupItem());
    closeAddPanel();
    save(); render(); startTicking(true);
    return;
  }
  openSetup(type);
});
bindTapDown(setupConfirmEl, (e)=>{
  e.stopPropagation();
  confirmSetup();
});
if (setupCancelEl){
  bindTapDown(setupCancelEl, (e)=>{
    e.stopPropagation();
    closeSetup();
  });
}
// resume-fit.js: バックグラウンド復帰処理(二重RAF)・機種差フィット・起動時イベント登録
// この並び順(index.htmlの<script>タグの順番)を変えると、他ファイルの関数/変数を先に参照してエラーになる場合があります。

let wasBackgrounded = false;

// ── バックグラウンド・非表示・ページ離脱時の即時保存 ──
function flushPendingSaveOnHide(){
  try {
    if (document.activeElement && typeof document.activeElement.blur === 'function'){
      document.activeElement.blur();
    }
  } catch(e){}
  if (pendingSave){
    saveNow();
  }
}

// 復帰処理：黒幕や無駄な待機・二重描画を省き、即座に差分更新してtick再開
function resumeApp(){
  // 万一DOMノードが消失していた場合の自己修復フォールバック
  if (!cardsEl || !cardsEl.firstElementChild){
    render();
  } else {
    tickRender(Date.now()); // tickRender内でsyncFullStamItemsも実行される
  }
  startTicking(true);
}

let resumeRaf = 0;
function handlePause(){
  flushPendingSaveOnHide();
  wasBackgrounded = true;
  if (resumeRaf){ cancelAnimationFrame(resumeRaf); resumeRaf = 0; }
  if (menuOpenRaf){ cancelAnimationFrame(menuOpenRaf); menuOpenRaf = 0; }
  tickGen++;
  if (tickId){ clearTimeout(tickId); tickId = null; }
}

// ダブルRAF：1フレーム目でGPU描画領域の復元・ビューポート確定を待ち、2フレーム目で時刻同期。
// visibilitychange / resume / pageshow / focus が連発しても1回に集約する。
function handleResume(){
  wasBackgrounded = false;
  if (resumeRaf) return;
  resumeRaf = requestAnimationFrame(()=>{
    resumeRaf = requestAnimationFrame(()=>{
      resumeRaf = 0;
      if (document.hidden) return;
      resumeApp();
    });
  });
}

document.addEventListener('visibilitychange', ()=>{
  if (document.hidden) handlePause();
  else handleResume();
});

// Android Chrome / Chromium の Page Lifecycle API（凍結・復帰）
document.addEventListener('freeze', handlePause);
document.addEventListener('resume', handleResume);

window.addEventListener('pagehide', handlePause);
window.addEventListener('beforeunload', flushPendingSaveOnHide);
window.addEventListener('pageshow', (e)=>{ handleResume(); });
window.addEventListener('focus', ()=>{
  if (!document.hidden) handleResume();
});

/* ═══════════════ 機種差フィット（ここから）═══════════════
   スタミナの現在値/最大値が枠に入り切らない端末向けに、必要な分だけ文字を縮める保険。
   digitEmの実測（DOM生成を伴う）は端末ごとに一度で済むよう localStorage にキャッシュし、
   フォント設定（サイズ/フォント/太さ）が前回と同じなら、次回起動以降は測定自体を丸ごとスキップする。
   updateTimerCardへの割り込み（上書き）はやめ、stam分岐の最後から直接呼ぶだけにしている。 */
const FIT_MIN_FONT = 11, FIT_SAFETY = 0.98, FIT_TOLERANCE = 0.5;
const FIT_CACHE_KEY = 'abyssFitDigitEmV1';
let fitDigitEm = 0, fitBaseFont = 18;
let fitRO = null;
const fitSeen = new WeakSet();

function fitCalibrate(slot){
  const cs = getComputedStyle(slot);
  fitBaseFont = parseFloat(cs.fontSize) || 18;
  const sig = fitBaseFont + '|' + cs.fontFamily + '|' + cs.fontWeight;
  try {
    const cached = JSON.parse(localStorage.getItem(FIT_CACHE_KEY) || 'null');
    if (cached && cached.sig === sig && cached.digitEm){
      fitDigitEm = cached.digitEm;
      return;
    }
  } catch(err){}
  const probe = document.createElement('span');
  probe.textContent = '0123456789';
  probe.style.cssText = 'position:absolute;left:-9999px;top:0;visibility:hidden;white-space:nowrap;line-height:1;font-size:100px;font-variant-numeric:tabular-nums;';
  probe.style.fontFamily = cs.fontFamily;
  probe.style.fontWeight = cs.fontWeight;
  document.body.appendChild(probe);
  const node = probe.firstChild, rg = document.createRange();
  let w = 0;
  for (let i = 0; i < 10; i++){
    rg.setStart(node, i); rg.setEnd(node, i + 1);
    w = Math.max(w, rg.getBoundingClientRect().width);
  }
  probe.remove();
  fitDigitEm = w / 100;
  if (fitDigitEm){
    try { localStorage.setItem(FIT_CACHE_KEY, JSON.stringify({ sig, digitEm: fitDigitEm })); } catch(err){}
  }
}

function fitApply(card){
  const slotW = card._slotW;
  if (!slotW) return;
  const cur = card._curEl || (card._curEl = card.querySelector('.stamval'));
  const max = card._maxEl || (card._maxEl = card.querySelector('.stammax'));
  const curText = card._curTextEl || (card._curTextEl = (cur && cur.querySelector('.inline-number-text')));
  if (!cur || !max || !curText) return;
  if (!fitDigitEm){ fitCalibrate(cur); if (!fitDigitEm) return; }

  const lc = curText.textContent.length, lm = max.textContent.length;
  const key = slotW + '|' + lc + '|' + lm;
  if (card._fitKey === key) return;
  card._fitKey = key;

  const need = Math.max(lc, lm) * fitDigitEm * fitBaseFont;
  let px = '';
  if (need > slotW + FIT_TOLERANCE){
    px = Math.max(FIT_MIN_FONT, Math.floor(fitBaseFont * (slotW / need) * FIT_SAFETY * 10) / 10) + 'px';
  }
  if (cur.style.fontSize !== px){ cur.style.fontSize = px; max.style.fontSize = px; }
}

function fitObserve(card){
  if (typeof ResizeObserver === 'undefined') return;
  if (!fitRO){
    fitRO = new ResizeObserver(entries => {
      for (const e of entries){
        const c = e.target.closest && e.target.closest('.card');
        if (!c) continue;
        c._slotW = e.contentRect.width;
        fitApply(c);
      }
    });
  }
  if (!fitSeen.has(card)){
    const slot = card.querySelector('.stamval');
    if (slot){ fitSeen.add(card); fitRO.observe(slot); }
  }
}
/* ═══════════════ 機種差フィット（ここまで）═══════════════ */

// 合成clickがモーダル外（トースト・追加パネル・設定パネル）に貫通するのを防ぐ
document.addEventListener('click', (e)=>{
  const isAnyModalOpen = (toastEl && toastEl.classList.contains('show')) ||
                         (addPanelEl && addPanelEl.classList.contains('show')) ||
                         (setupPanelEl && setupPanelEl.classList.contains('show'));
  if (isAnyModalOpen){
    const inToast = toastEl && toastEl.contains(e.target);
    const inAdd = addPanelEl && addPanelEl.contains(e.target);
    const inSetup = setupPanelEl && setupPanelEl.contains(e.target);
    if (!inToast && !inAdd && !inSetup && !e.target.closest(MENU_BTN_SEL)){
      e.preventDefault();
      e.stopPropagation();
    }
  }
}, {capture:true, passive:false});

render();
startTicking(true);

// 長押しメニュー(コピー等)やドラッグ選択を出さない。
document.addEventListener('contextmenu', (e)=> e.preventDefault());
document.addEventListener('dragstart', (e)=> e.preventDefault());
// テキスト選択は入力欄側で必要な範囲だけ抑止する。
document.addEventListener('paste', (e)=> e.preventDefault());

// ── 完全オフライン保護 ＆ 高速スマート更新（In-App Smart Update） ──
let isUpdating = false;

async function performSmartUpdate(){
  if (isUpdating) return;
  
  // 1. オフライン検出（完全オフライン保護）
  // 端末がオフラインなら、キャッシュを絶対に削除・解除せず安全に保護する
  if (!navigator.onLine){
    showNotice('オフラインです（現在のキャッシュを維持します）', 3000);
    return;
  }

  isUpdating = true;
  showNotice('最新版を読み込み中…', 0);

  const bust = Date.now().toString(36);

  const files = [
    './index.html',
    './styles.css',
    './state.js',
    './engine.js',
    './toast-core.js',
    './toast-fields.js',
    './card-builders.js',
    './card-logic.js',
    './panels.js',
    './resume-fit.js',
    './sw.js',
    './manifest.json',
    './icon-192.png',
    './icon-512.png',
    './icon-maskable-512.png'
  ];

  try {
    // 2. 全アセットをネットワークからキャッシュ無視で一括取得
    const fetchedResults = await Promise.all(files.map(async f => {
      const res = await fetch(f + '?r=' + bust, { cache: 'no-store', credentials: 'same-origin' });
      if (!res.ok) throw new Error(`${f} (${res.status})`);
      return { path: f, res };
    }));

    // 3. 最新 sw.js の中身から CACHE_NAME を特定
    const swItem = fetchedResults.find(x => x.path === './sw.js');
    let cacheName = 'v' + bust;
    if (swItem){
      const swText = await swItem.res.clone().text();
      const match = swText.match(/CACHE_NAME\s*=\s*['"]([^'"]+)['"]/);
      if (match) cacheName = match[1];
    }

    // 4. 古いSWを一度解除し、新しいCache Storageに直接書き込み
    if ('serviceWorker' in navigator){
      const regs = await navigator.serviceWorker.getRegistrations();
      await Promise.all(regs.map(r => r.unregister()));
    }

    if ('caches' in window){
      const newCache = await caches.open(cacheName);
      for (const item of fetchedResults){
        await newCache.put(item.path, item.res.clone());
        if (item.path === './index.html'){
          await newCache.put('./', item.res.clone());
        }
      }

      // 古いキャッシュをクリア
      const allKeys = await caches.keys();
      await Promise.all(allKeys.filter(k => k !== cacheName).map(k => caches.delete(k)));
    }

    showNotice('更新完了！再起動します…', 1200);

    // 5. 新バージョンで確実に再読み込み＆完全再描画
    setTimeout(()=>{
      location.replace('./index.html?r=' + bust);
    }, 400);

  } catch(err){
    console.warn('Smart update failed:', err);
    isUpdating = false;
    showNotice('更新に失敗しました（現在のバージョンを維持します）', 3500);
  }
}

bindTapDown(document.getElementById('updateBtn'), performSmartUpdate);

// Service worker: プレビュー・iframe環境ではSWを解除してキャッシュ滞留を防止
const isIframe = window.self !== window.top;
if ('serviceWorker' in navigator){
  if (isIframe) {
    navigator.serviceWorker.getRegistrations().then(regs => {
      regs.forEach(r => r.unregister());
    }).catch(()=>{});
  } else {
    window.addEventListener('load', ()=>{
      navigator.serviceWorker.register('./sw.js', { scope: './' }).catch(err => {
        console.warn('ServiceWorker registration bypassed:', err?.message || err);
      });
    });
  }
}
