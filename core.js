
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
function clampInt(v, min, max, fallback){
  const n = parseInt(v, 10);
  return isNaN(n) ? (fallback != null ? fallback : min) : Math.max(min, Math.min(max, n));
}

