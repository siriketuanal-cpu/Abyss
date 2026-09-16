
const STORAGE_KEY = 'freetimer:v1';
let state = load();
// 旧版で存在し得た「アカウント外タイマー」を起動時にアカウントへ移行。
// 現行UIではタイマーはアカウント枠の子としてのみ作成する。
function migrateOrphanTimers(){
  if (!state || !Array.isArray(state.items)) return false;
  const orphan = state.items.filter(it => it && (it.type==='stam' || it.type==='abyss' || it.type==='idle'));
  if (!orphan.length) return false;
  const keep = state.items.filter(it => !orphan.includes(it));
  const groups = [];
  for (let i=0; i<orphan.length; i+=2){
    groups.push({ id:'g' + Date.now().toString(36) + Math.random().toString(36).slice(2,7), type:'group', name:'', children:orphan.slice(i,i+2) });
  }
  // 最初の孤立タイマーがあった位置に移行後のアカウント枠を挿入し、
  // それ以外の既存項目の順序はなるべく維持する。
  const firstIndex = state.items.findIndex(it => orphan.includes(it));
  const beforeCount = state.items.slice(0, firstIndex).filter(it => !orphan.includes(it)).length;
  state.items = keep.slice(0,beforeCount).concat(groups, keep.slice(beforeCount));
  saveNow();
  return true;
}
if (migrateOrphanTimers()) { /* 旧データの一度きりの移行 */ }
let refs = {};
let tickId = null;
let tickGen = 0;
let pending40Id = null;
let claimId = null;

// DOM参照は一度だけ取得して使い回す(毎分ティックや毎クリックで検索し直さない)。
const cardsEl = document.getElementById('cards');
const emptyEl = document.getElementById('empty');
const addPanelEl = document.getElementById('addPanel');
const addBtnEl = document.getElementById('addBtn');
const setupPanelEl = document.getElementById('setupPanel');
const setupLabelEl = document.getElementById('setupLabel');
const setupFieldsEl = document.getElementById('setupFields');
const setupConfirmEl = document.getElementById('setupConfirm');

function load(){
  try{
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw){
      const data = JSON.parse(raw);
      if (data && Array.isArray(data.items)){
        data.items.forEach(it=>{
          if (it && it.type === 'abyss'){
            if (!it.rank) it.rank = 1;
            // max はランクから再計算（旧データの手編集maxは捨てる）
            it.max = 240 + (Math.max(1, Math.min(200, it.rank|0)) - 1) * 5;
            if (it.current > it.max) it.current = it.max;
          }
        });
      }
      return data;
    }
  }catch(e){}
  return { items: [] };
}
// 同期localStorageは描画を止めることがある → 既定は次タスクに遅延。
// 即時が必要なときだけ save(true)
let saveTimer = null;
function saveNow(){
  try{
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }catch(e){
  }
}
function save(immediate){
  if (immediate){
    if (saveTimer){ clearTimeout(saveTimer); saveTimer = null; }
    saveNow();
    return;
  }
  if (saveTimer) return; // 連続操作は1回にまとめる
  saveTimer = setTimeout(()=>{
    saveTimer = null;
    saveNow();
  }, 0);
}
// 描画を先にコミットしてから保存（確定タップ用）
function saveAfterPaint(){
  requestAnimationFrame(()=>{
    setTimeout(()=> saveNow(), 0);
  });
}
function uid(){ return 'x' + Date.now().toString(36) + Math.random().toString(36).slice(2,7); }

// アプリ内のタップ操作は pointerdown に統一。ネイティブ入力の編集開始だけは、
// 長押しでキーボードを出さないため短い指離し時にfocusする。
function bindTapDown(el, handler){
  el.addEventListener('pointerdown', (e)=>{
    if (e.isPrimary === false) return;
    if (e.pointerType === 'mouse' && e.button !== 0) return;
    handler(e);
  });
}

// 入力欄フォーカス時は常に末尾へカーソルを固定する。
function forceCursorToEnd(input){
  const place = () => { try{ input.setSelectionRange(input.value.length, input.value.length); }catch(e){} };
  input.addEventListener('focus', () => { place(); setTimeout(place, 0); });
  input.addEventListener('pointerdown', place);
}
// アカウント枠の名前欄専用：長押し判定(bindLongPressDelete)が同じ入力欄の
// pointerdown で readonly の付け外しを行っているため、forceCursorToEnd の
// pointerdown リスナーまで重ねると早押し時にフォーカスの取り合いになり、
// キーボードが開いてすぐ閉じる現象が起きていた。focus起点の処理だけに絞る。
function placeCursorOnFocus(input){
  const place = () => { try{ input.setSelectionRange(input.value.length, input.value.length); }catch(e){} };
  input.addEventListener('focus', () => { place(); setTimeout(place, 0); });
}
// 数字入力の共通処理。インライン編集側ではキャレット自体を非表示にする。
function clearOnFocus(input){
  input.addEventListener('focus', () => { input.value = ''; });
}
// 数字は通常の文字として表示し、タップ時だけ数字の真上に入力欄を重ねる。
// 入力欄はレイアウトから外してあるので、編集開始/終了で周囲の数字や「/」がズレない。
function createInlineNumber(initialValue, onCommit){
  const wrap = document.createElement('span');
  wrap.className = 'inline-number';
  const text = document.createElement('span');
  text.className = 'inline-number-text';
  text.textContent = String(initialValue);
  wrap.appendChild(text);

  let editor = null;
  const setText = v => { text.textContent = String(v); };
  const isEditing = () => !!editor;
  const open = () => {
    if (editor) return;
    editor = document.createElement('input');
    editor.type = 'text';
    editor.inputMode = 'numeric';
    editor.pattern = '[0-9]*';
    editor.className = 'inline-edit-input';
    editor.autocomplete = 'off';
    editor.autocapitalize = 'off';
    editor.autocorrect = 'off';
    editor.spellcheck = false;
    editor.maxLength = 3;
    const originalValue = text.textContent;
    editor.value = '';
    wrap.classList.add('editing');
    wrap.appendChild(editor);

    const finish = () => {
      if (!editor) return;
      const value = editor.value;
      const old = editor;
      editor = null;
      old.remove();
      wrap.classList.remove('editing');
      onCommit(value);
    };
    editor.addEventListener('input', () => {
      const v = editor.value.replace(/\D/g, '').slice(0,3);
      if (editor.value !== v) editor.value = v;
    });
    editor.addEventListener('blur', finish, {once:true});
    editor.addEventListener('keydown', e => {
      if (e.key === 'Enter') { e.preventDefault(); editor.blur(); }
      else if (e.key === 'Escape') { e.preventDefault(); editor.value = originalValue; editor.blur(); }
    });
    // フォーカスした瞬間に空欄へ。元の数字は下の文字を隠しているので重なって見えない。
    editor.addEventListener('focus', () => { editor.value = ''; }, {once:true});
    editor.focus({preventScroll:true});
  };
  return {wrap, text, open, setText, isEditing};
}

function newStamItem(intervalMin){
  return { id: uid(), type:'stam', name:'', current:0, max:100, intervalMin, start: Date.now() };
}
function newAbyssItem(intervalMin){
  // Dot Abyss 風: 最大はランク連動。最大値タップで40使い切り。削除は長押し。
  const rank = 1;
  return { id: uid(), type:'abyss', name:'', rank, current:0, max: abyssMaxFor(rank), intervalMin, start: Date.now() };
}
function newIdleItem(durationMin){
  return { id: uid(), type:'idle', name:'', durationMin, state:'running', start: Date.now() };
}
function newHeaderItem(){
  return { id: uid(), type:'header', name:'' };
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

function stamInfo(it, now){
  const intervalMs = Math.max(1, it.intervalMin) * 60000;
  const elapsed = Math.max(0, now - it.start);
  const recovered = Math.floor(elapsed / intervalMs);
  const cur = Math.min(it.max, it.current + recovered);
  const isFull = cur >= it.max;
  if (isFull) {
    const fullAt = it.start + Math.max(0, it.max - it.current) * intervalMs;
    return { cur, remainMs:0, isFull, fullAt: Math.min(now, fullAt) };
  }
  const nextIn = intervalMs - (elapsed % intervalMs);
  const need = it.max - cur;
  const remainMs = (need-1)*intervalMs + nextIn;
  return { cur, remainMs, isFull };
}
// 現在値/上限値を書き換えても「次の1回復までの残り時間」がリセットされないよう、
// サイクルの位相(既にどれだけ経過しているか)を維持したまま基準時刻だけを進める。
function remainingAfter40(cur){
  cur = Math.max(0, Math.floor(Number(cur) || 0));
  return cur % 40; // 例: 220→20, 40→0, 39→39
}
// Dot Abyss 相当: 基礎240 + (ランク-1)×5（最大は計算のみ・手編集しない）
function abyssMaxFor(rank){
  const r = Math.max(1, Math.min(200, Math.floor(Number(rank) || 1)));
  return 240 + (r - 1) * 5;
}
function syncAbyssMax(it){
  it.rank = Math.max(1, Math.min(200, Math.floor(Number(it.rank) || 1)));
  it.max = abyssMaxFor(it.rank);
  if (it.current > it.max) it.current = it.max;
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
  return { remainMs, isFull: remainMs<=0, fullAt: it.start + durMs };
}
function fmtHM(ts){
  const d = new Date(ts);
  const p = n => String(n).padStart(2,'0');
  return p(d.getHours()) + ':' + p(d.getMinutes());
}

function timerNeedsTick(it, now){
  if (it.type==='stam' || it.type==='abyss') return !stamInfo(it, now).isFull;
  if (it.type==='idle') return !idleInfo(it, now).isFull;
  return false;
}
function needsTicking(now){
  now = now || Date.now();
  return state.items.some(it=>{
    if (it.type==='group') return it.children.some(c=>timerNeedsTick(c, now));
    return timerNeedsTick(it, now);
  });
}
function tickRender(now){
  now = now || Date.now();
  for (const it of state.items){
    const r = refs[it.id];
    if (!r) continue;
    if (it.type==='group'){
      for (const child of it.children) updateTimerCard(child, r.childRefs[child.id], now);
    } else if (it.type==='stam' || it.type==='abyss' || it.type==='idle'){
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

// 長押し削除は全カードで共通化。
// 重要：pointerdown では通常のクリックを潰さない。短押しはブラウザ/既存の click 処理へ渡し、
// 長押しだけ削除トーストを開いて直後の click を1回だけ抑止する。
let suppressNextClick = false;
let suppressClearTimer = null;

function bindLongPressDelete(el, id, shortAction){
  const HOLD_MS = 520, MOVE_PX = 24; // 指の震え等で長押しが途中キャンセルされにくいよう少し広めに
  let timer = null, active = false, moved = false, longFired = false;
  let sx = 0, sy = 0, pid = null, nameInput = null;

  const isPrimary = e => e.isPrimary !== false && !(e.pointerType==='mouse' && e.button!==0);
  const clearSelection = ()=>{ try{ window.getSelection()?.removeAllRanges(); }catch(e){} };
  const isGroupName = target => {
    const n = target && target.closest ? target.closest('.group .name') : null;
    return !!n && el.contains(n);
  };
  const cleanup = ()=>{
    if (timer !== null){ clearTimeout(timer); timer = null; }
    active = false; moved = false; longFired = false; pid = null; nameInput = null;
  };

  el.addEventListener('pointerdown', e=>{
    if (e.target && e.target.closest && e.target.closest('.inline-edit-input')) return;
    if (!isPrimary(e) || active) return;
    active = true; moved = false; longFired = false;
    sx = e.clientX; sy = e.clientY; pid = e.pointerId;

    // 名前欄も、短押しは邪魔せずブラウザのネイティブなフォーカス/キーボード表示に
    // 任せる（先回りしてreadonly/preventDefaultすると、短押しでも稀にフォーカスが
    // 一瞬だけ入ってすぐ外れる＝キーボードが開いてすぐ閉じる、という副作用が出ていた）。
    // 長押しが確定した時にだけ、この入力欄からフォーカスを外す。
    if (isGroupName(e.target)){
      nameInput = e.target.closest('.group .name');
    }

    timer = setTimeout(()=>{
      if (!active || moved) return;
      timer = null;
      longFired = true;
      clearSelection();
      if (nameInput) nameInput.blur();
      suppressNextClick = true;
      if (suppressClearTimer) clearTimeout(suppressClearTimer);
      suppressClearTimer = setTimeout(()=>{ suppressNextClick=false; suppressClearTimer=null; }, 1000);
      askRemoveItem(id);
    }, HOLD_MS);
  }, {capture:true, passive:false});

  el.addEventListener('pointermove', e=>{
    if (!active || e.pointerId !== pid) return;
    if (Math.hypot(e.clientX-sx, e.clientY-sy) > MOVE_PX){
      moved = true;
      if (timer !== null){ clearTimeout(timer); timer = null; }
    }
  }, {capture:true, passive:true});

  el.addEventListener('pointerup', e=>{
    if (!active || e.pointerId !== pid) return;
    const wasLong = longFired, wasMoved = moved, input = nameInput;
    if (timer !== null){ clearTimeout(timer); timer = null; }

    if (wasLong){
      // 長押し後に発生するclickだけを抑止する。
      e.preventDefault();
      e.stopPropagation();
    } else if (!wasMoved && typeof shortAction === 'function'){
      // 短押しは各カード本来の操作へ渡す。
      // アカウント名行では、入力欄そのものをタップした時だけ編集開始。
      // 行の余白タップでは名前欄へフォーカスしない。
      if (!el.classList.contains('group-namerow') || input){
        shortAction(e);
      }
      if (input) e.stopPropagation();
    }
    cleanup();
  }, {capture:true, passive:false});

  el.addEventListener('pointercancel', e=>{
    if (!active || e.pointerId !== pid) return;
    cleanup();
  }, {capture:true, passive:true});

  el.addEventListener('contextmenu', e=>{
    if (isGroupName(e.target)){
      e.preventDefault(); e.stopPropagation(); clearSelection();
    }
  });
}
let toastTapDownHandler = null;
let longPressTargetEl = null;
function clearLongPressTarget(){
  if (longPressTargetEl){
    longPressTargetEl.classList.remove('longpress-target');
    longPressTargetEl = null;
  }
}
function askRemoveItem(id){
  clearLongPressTarget();
  const targetRef = refs[id];
  if (targetRef && targetRef.el){
    targetRef.el.classList.add('longpress-target');
    longPressTargetEl = targetRef.el;
  }
  const it = findItemById(id);
  const toast = document.getElementById('toast');
  if (!toast){ removeItem(id); return; }
  if (toastTapDownHandler){ toast.removeEventListener('pointerdown', toastTapDownHandler); toastTapDownHandler = null; }
  const canAdd = it && it.type === 'group' && it.children.length < 2;
  toast.innerHTML = (canAdd ? '<button type="button" data-act="add">タイマー追加</button>' : '')
    + '<button type="button" class="ok" data-act="yes">削除</button>'
    + '<button type="button" data-act="no">やめる</button>';
  toast.classList.add('show');
  const onClick = (e)=>{
    const btn = e.target.closest('button');
    if (!btn) return;
    const act = btn.dataset.act;
    if (act === 'no'){
      // やめるは即閉じ
      clearLongPressTarget();
      toast.classList.remove('show');
      toast.removeEventListener('pointerdown', onClick);
      toastTapDownHandler = null;
      return;
    }
    if (act === 'yes' || act === 'add'){
      // 1回目: 破線で囲って確定待ち。他ボタンの破線は外す
      if (!btn.classList.contains('armed')){
        toast.querySelectorAll('button.armed').forEach(b => b.classList.remove('armed'));
        btn.classList.add('armed');
        return;
      }
      // 2回目: 実行
      clearLongPressTarget();
      toast.classList.remove('show');
      toast.removeEventListener('pointerdown', onClick);
      toastTapDownHandler = null;
      if (act === 'yes'){
        removeItem(id);
      } else {
        pendingAddGroupId = id;
        addPanelEl.classList.add('group-mode');
        addPanelEl.classList.add('show');
      }
    }
  };
  toastTapDownHandler = onClick;
  toast.addEventListener('pointerdown', onClick);
}
function buildStamCard(it){
  const wrap = document.createElement('div');
  wrap.className = 'card stam';
  wrap.innerHTML = `
    <div class="clockstack stam-clock" data-role="clock"></div>
    <div class="row valrow-stam">
      <span class="stamval" data-role="curWrap"></span>
      <span class="slash">/</span>
      <span class="stammax" data-role="maxWrap"></span>
    </div>
  `;
  const curWrap = wrap.querySelector('[data-role="curWrap"]');
  const maxWrap = wrap.querySelector('[data-role="maxWrap"]');
  const curEditor = createInlineNumber(stamInfo(it, Date.now()).cur, value => {
    const now = Date.now();
    const info = stamInfo(it, now);
    const newCur = clampInt(value, 0, it.max, info.cur);
    preserveCycle(it, now);
    it.current = newCur;
    save(); render();
  });
  const maxEditor = createInlineNumber(it.max, value => {
    const now = Date.now();
    const curNow = stamInfo(it, now).cur;
    const newMax = clampInt(value, 1, 999, it.max);
    preserveCycle(it, now);
    it.max = newMax;
    it.current = Math.min(curNow, newMax);
    save(); render();
  });
  curWrap.appendChild(curEditor.wrap);
  maxWrap.appendChild(maxEditor.wrap);

  refs[it.id] = { el: wrap, curEl: curEditor, maxEl: maxEditor,
    clockEl: wrap.querySelector('[data-role="clock"]') };
  bindLongPressDelete(wrap, it.id, (e)=>{
    const target = e.target;
    if (target.closest && target.closest('.inline-edit-input')) return;
    if (target.closest && target.closest('[data-role="curWrap"]')) { curEditor.open(); return; }
    if (target.closest && target.closest('[data-role="maxWrap"]')) { maxEditor.open(); return; }
  });
  return wrap;
}

function buildAbyssCard(it){
  syncAbyssMax(it);
  const wrap = document.createElement('div');
  wrap.className = 'card abyss';
  wrap.innerHTML = `
    <div class="row abyss-head">
      <div class="clockstack abyss-clock" data-role="clock"></div>
      <input class="rank" data-role="rankInput" inputmode="numeric" pattern="[0-9]*" title="ランク">
    </div>
    <div class="abyss-cur-hit" data-role="curHit"></div>
    <div class="row valrow-stam">
      <span class="stamval" data-role="curWrap"></span>
      <span class="slash">/</span>
      <span class="stammax" data-role="maxLabel"></span>
    </div>
  `;
  const rankInput = wrap.querySelector('[data-role="rankInput"]');
  const curWrap = wrap.querySelector('[data-role="curWrap"]');
  const curHit = wrap.querySelector('[data-role="curHit"]');
  const maxLabel = wrap.querySelector('[data-role="maxLabel"]');
  const curEditor = createInlineNumber(stamInfo(it, Date.now()).cur, value => {
    const now = Date.now();
    syncAbyssMax(it);
    const info = stamInfo(it, now);
    const newCur = clampInt(value, 0, it.max, info.cur);
    preserveCycle(it, now);
    it.current = newCur;
    pending40Id = null;
    save(); render();
  });
  curWrap.appendChild(curEditor.wrap);
  // ランク欄は従来どおりネイティブ入力。フォーカス時だけ空欄にする。
  clearOnFocus(rankInput);
  // 長押しの貼り付けメニュー等を出さない
  ['paste','copy','cut','contextmenu','selectstart'].forEach(ev=>{
    rankInput.addEventListener(ev, e=>{ e.preventDefault(); });
  });
  rankInput.setAttribute('autocomplete','off');
  rankInput.setAttribute('autocapitalize','off');
  rankInput.setAttribute('autocorrect','off');
  rankInput.setAttribute('spellcheck','false');
  rankInput.addEventListener('focus', ()=>{
    if (pending40Id === it.id){ pending40Id = null; paintAbyssFortyPreview(it.id); }
  });
  rankInput.addEventListener('blur', ()=>{
    const now = Date.now();
    const live = stamInfo(it, now).cur;
    it.rank = clampInt(rankInput.value, 1, 200, it.rank || 1);
    syncAbyssMax(it);
    preserveCycle(it, now);
    it.current = Math.min(live, it.max);
    pending40Id = null;
    save(); render(); startTicking(true);
  });
  const clockEl = wrap.querySelector('[data-role="clock"]');
  // 最大 or 予定時刻タップ → 40使い切りプレビュー/確定（ルナビィに近い）
  const onForty = (e)=>{
    e.stopPropagation();
    e.preventDefault();
    const now = Date.now();
    syncAbyssMax(it);
    const info = stamInfo(it, now);
    if (pending40Id === it.id){
      preserveCycle(it, now);
      it.current = remainingAfter40(info.cur);
      pending40Id = null;
      paintAbyssFortyPreview(it.id);
      startTicking(true);
      saveAfterPaint();
    } else {
      // 他カードのプレビューが残っていれば外す
      if (pending40Id && pending40Id !== it.id){
        const prev = pending40Id;
        pending40Id = null;
        paintAbyssFortyPreview(prev);
      }
      pending40Id = it.id;
      paintAbyssFortyPreview(it.id);
    }
  };
  // 注意：maxLabel/clockEl の短押しは bindLongPressDelete の shortAction 経由で
  // onForty を呼ぶので、ここで別途 bindTapDown も付けると1回のタップで
  // onForty が2回(pointerdown時点とpointerup時点)発火し、
  // 「1回のタップで確定まで進んでしまい、確定待ちのまま外タップで戻せない」
  // 原因になっていた。二重登録はしない。
  // 初回から数字を出しておく（更新直後の空表示防止）
  const info0 = stamInfo(it, Date.now());
  curEditor.setText(info0.cur);
  maxLabel.textContent = String(it.max);
  rankInput.value = 'Lv.' + (it.rank || 1);
  refs[it.id] = { el: wrap, rankInput, curEl: curEditor, maxLabel, clockEl, curHit };
  bindLongPressDelete(wrap, it.id, (e)=>{
    const input = e.target && e.target.closest ? e.target.closest('input') : null;
    if (input === rankInput){
      input.focus();
      try{ input.setSelectionRange(input.value.length,input.value.length); }catch(err){}
      return;
    }
    if (e.target.closest && (e.target.closest('[data-role="curWrap"]') || e.target.closest('[data-role="curHit"]'))){
      curEditor.open();
      return;
    }
    // 入力欄以外はカード全体が同じ40使い切り操作。
    // max/時刻/余白/枠の間でタップ判定が分かれないようにする。
    onForty(e);
  });
  return wrap;
}

function buildIdleCard(it){
  const wrap = document.createElement('div');
  wrap.className = 'card idle';
  wrap.innerHTML = `
    <div class="clockstack idle-clock" data-role="clock"></div>
    <div class="row valrow-idle">
      <div class="main" data-role="cur"></div>
    </div>
  `;
  refs[it.id] = { el: wrap,
    curEl: wrap.querySelector('[data-role="cur"]'), clockEl: wrap.querySelector('[data-role="clock"]') };
  bindLongPressDelete(wrap, it.id, ()=>toggleIdle(it));
  return wrap;
}


function buildTimerChild(it){
  return it.type==='stam' ? buildStamCard(it) : it.type==='abyss' ? buildAbyssCard(it) : buildIdleCard(it);
}
function buildGroupCard(it){
  const wrap = document.createElement('div');
  wrap.className = 'card group';
  wrap.innerHTML = `
    <div class="row namerow group-namerow">
      <input class="name" placeholder="アカウント" value="${esc(it.name)}">
    </div>
    <div class="group-body" data-role="body"></div>
  `;
  const nameEl = wrap.querySelector('.name');
  nameEl.addEventListener('blur', ()=>{ it.name = nameEl.value; save(); });
  placeCursorOnFocus(nameEl);
  const nameRowEl = wrap.querySelector('.group-namerow');
  refs[it.id] = { el: wrap, nameEl,
    bodyEl: wrap.querySelector('[data-role="body"]'),
    childRefs: {} };
  // アカウント枠の長押しは「名前欄の行」だけに限定する。
  // wrap(カード全体)に付けてしまうと、枠内の子タイマーを長押しした時に
  // 子タイマー自身の長押し処理と二重に発火して競合し、動作がおかしくなるため。
  bindLongPressDelete(nameRowEl, it.id, (e)=>{
    if(e.target===nameEl || e.target.closest('.group-namerow')){
      nameEl.removeAttribute('readonly');
      nameEl.focus();
      try{ nameEl.setSelectionRange(nameEl.value.length,nameEl.value.length); }catch(err){}
    }
  });
  renderGroupBody(it);
  return wrap;
}
function renderGroupBody(it){
  const r = refs[it.id];
  if (!r) return;
  // 子カードの追加/削除に合わせてDOMを作り直す（並べ替えは発生しない：移動機能なし）
  const existingChildIds = new Set(it.children.map(c=>c.id));
  Object.keys(r.childRefs).forEach(id=>{ if(!existingChildIds.has(id)) delete r.childRefs[id]; });
  const single = it.children.length <= 1;
  r.el.classList.toggle('single', single);
  r.bodyEl.classList.toggle('one', single);
  for (const child of it.children){
    if (!r.childRefs[child.id]){
      const el = buildTimerChild(child);
      r.childRefs[child.id] = refs[child.id];
      r.bodyEl.appendChild(el);
    }
  }
}
function buildRuleCard(it){
  const wrap = document.createElement('div');
  wrap.className = 'rule-line';
  wrap.style.borderTopColor = it.color || '#52617a';
  wrap.innerHTML = '';
  refs[it.id] = { el: wrap };
  bindLongPressDelete(wrap, it.id);
  return wrap;
}
function buildHeaderCard(it){
  const wrap = document.createElement('div');
  wrap.className = 'divider';
  wrap.innerHTML = `
    <input class="name" placeholder="見出し(ゲーム名など)" value="${esc(it.name)}">
  `;
  const nameEl = wrap.querySelector('.name');
  nameEl.addEventListener('blur', ()=>{ it.name = nameEl.value; save(); });
  forceCursorToEnd(nameEl);
  refs[it.id] = { el: wrap, nameEl };
  bindLongPressDelete(wrap, it.id);
  return wrap;
}

// 構造を変えない操作は対象カードだけを更新する。
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
// タップで「受取」表示に切替。内部の実時間は止めない(表示だけの上書き)。
// もう一度タップ＝受取確定→フル時間で再開。
function toggleIdle(it){
  if (it.state === 'claim'){
    it.state = 'running';
    it.start = Date.now();
    if (claimId === it.id) claimId = null;
  } else {
    it.state = 'claim';
    claimId = it.id;
  }
  updateOneTimer(it);
  startTicking(true);
  saveAfterPaint();
}
function cancelClaim(it){
  it.state = 'running';
  if (claimId === it.id) claimId = null;
  updateOneTimer(it);
  startTicking(true);
  saveAfterPaint();
}

// ×タップ→確認状態(赤)→もう一度タップで削除。ネイティブconfirm()はPWA環境で
// 動作しないことがあるため使わない。

document.addEventListener('pointerdown', (e) => {
  try { if (document.activeElement !== e.target && window.getSelection) window.getSelection().removeAllRanges(); } catch(err){}
  if (claimId != null){
    const claimed = findItemById(claimId);
    const r = claimed ? getTimerRef(claimId) : null;
    if (!claimed || !r || !r.el.contains(e.target)) cancelClaim(claimed || {id:claimId});
  }
  if (pending40Id != null){
    const r = refs[pending40Id];
    if (!r || !r.el.contains(e.target)){
      const id = pending40Id;
      pending40Id = null;
      paintAbyssFortyPreview(id);
    }
  }
  if (addPanelEl.classList.contains('show') && !addPanelEl.contains(e.target) && e.target !== addBtnEl){
    addPanelEl.classList.remove('show');
  }
  const toastEl = document.getElementById('toast');
  if (toastEl && toastEl.classList.contains('show') && !toastEl.contains(e.target)){
    clearLongPressTarget();
    toastEl.classList.remove('show');
    if (toastTapDownHandler){ toastEl.removeEventListener('pointerdown', toastTapDownHandler); toastTapDownHandler = null; }
  }
  // 作成パネルは枠外タップで閉じない（キーボード閉じでキャンセルされるのを防ぐ）
}, true);

function clampInt(v, min, max, fb){
  const n = parseInt(v,10);
  return Number.isFinite(n) ? Math.max(min, Math.min(max,n)) : fb;
}
function esc(s){ return String(s||'').replace(/[&<>"']/g, c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }

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

// 04/20のようなデジタル時計表示を組み立てる共通処理(スタミナ・放置報酬で共用)。
function renderClock(el, remainMs, isFull, now, fullAt){
  if (!el) return;
  const key = isFull ? 'full:' + fmtHM(fullAt || now) : (()=>{ const c = fmtClockAt(remainMs, now); return c.h+c.m; })();
  if (el.dataset.clk === key) return;
  el.dataset.clk = key;
  if (isFull) el.innerHTML = `<span class="full-at">${fmtHM(fullAt || now)}</span>`;
  else el.textContent = fmtHM((now || Date.now()) + Math.max(0, remainMs));
}


// 40計算プレビュー/確定は当該Abyssカードだけ触る（全renderしない）
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
function paintAbyssFortyPreview(id){
  const it = findItemById(id);
  const r = refs[id];
  if (!it || !r || it.type!=='abyss') return;
  syncAbyssMax(it);
  const now = Date.now();
  const info = stamInfo(it, now);
  const wait = pending40Id === id;
  r.el.classList.toggle('claim', wait);
  if (r.curEl && !r.curEl.isEditing()) r.curEl.setText(wait ? remainingAfter40(info.cur) : info.cur);
  if (r.maxLabel) r.maxLabel.textContent = String(it.max);
  renderClock(r.clockEl, info.remainMs, info.isFull, now, info.fullAt);
}
function render(){
  const now = Date.now();
  emptyEl.style.display = state.items.length ? 'none' : 'block';

  const existingIds = new Set(state.items.map(i=>i.id));
  state.items.forEach(it=>{ if (it.type==='group') it.children.forEach(c=>existingIds.add(c.id)); });
  Object.keys(refs).forEach(id=>{ if(!existingIds.has(id)) delete refs[id]; });

  for (const it of state.items){
    if (!refs[it.id]){
      const el = it.type==='stam' ? buildStamCard(it) : it.type==='abyss' ? buildAbyssCard(it) : it.type==='idle' ? buildIdleCard(it) : it.type==='rule' ? buildRuleCard(it) : it.type==='group' ? buildGroupCard(it) : buildHeaderCard(it);
    } else if (it.type==='group'){
      renderGroupBody(it);
    }
  }

  const topEls = state.items.map(it => refs[it.id].el);

  // 順序が違うときだけ並べ替え（毎分の append を避ける）
  let needOrder = false;
  const kids = cardsEl.children;
  for (let i = 0; i < topEls.length; i++){
    if (kids[i] !== topEls[i]){ needOrder = true; break; }
  }
  if (needOrder){
    for (const el of topEls) cardsEl.appendChild(el);
  }

  for (const it of state.items){
    const r = refs[it.id];
    if (it.type==='rule') r.el.style.borderTopColor = it.color || '#52617a';
    if (it.type==='group'){
      if (document.activeElement !== r.nameEl && r.nameEl.value !== it.name) r.nameEl.value = it.name;
      for (const child of it.children) updateTimerCard(child, r.childRefs[child.id], now);
    } else {
      updateTimerCard(it, r, now);
    }
  }
}
// stam/abyss/idle の値更新をトップレベル/アカウント枠の子どちらでも共用する
function updateTimerCard(it, r, now){
  if (!it || !r) return;
  if (it.type==='stam'){
    const info = stamInfo(it, now);
    r.el.classList.toggle('full', info.isFull);
    renderClock(r.clockEl, info.remainMs, info.isFull, now, info.fullAt);
    if (r.curEl && !r.curEl.isEditing()) r.curEl.setText(info.cur);
    if (r.maxEl && !r.maxEl.isEditing()) r.maxEl.setText(it.max);
  } else if (it.type==='abyss'){
    // maxはランク変更時だけ変わる。毎分のsyncは不要だが安価なので維持
    if (it.max !== abyssMaxFor(it.rank||1)) syncAbyssMax(it);
    const info = stamInfo(it, now);
    const wait40 = pending40Id === it.id;
    r.el.classList.toggle('full', info.isFull);
    r.el.classList.toggle('claim', wait40);
    renderClock(r.clockEl, info.remainMs, info.isFull, now, info.fullAt);
    if (r.rankInput && document.activeElement !== r.rankInput){
      const v = 'Lv.' + (it.rank || 1);
      if (r.rankInput.value !== v) r.rankInput.value = v;
    }
    if (r.curEl && !r.curEl.isEditing()) r.curEl.setText(wait40 ? remainingAfter40(info.cur) : info.cur);
    if (r.maxLabel){
      const v = String(it.max);
      if (r.maxLabel.textContent !== v) r.maxLabel.textContent = v;
    }
  } else if (it.type==='idle'){
    const info = idleInfo(it, now);
    r.el.classList.toggle('claim', it.state==='claim');
    r.el.classList.toggle('full', info.isFull && it.state!=='claim');
    const label = it.state==='claim' ? '受取' : (info.isFull ? fmtHM(info.fullAt) : fmtCountdown(info.remainMs));
    if (r.curEl.textContent !== label) r.curEl.textContent = label;
    renderClock(r.clockEl, info.remainMs, info.isFull, now, info.fullAt);
  }
}

let pendingAddGroupId = null;
bindTapDown(addBtnEl, ()=>{
  pendingAddGroupId = null;
  addPanelEl.classList.remove('group-mode');
  addPanelEl.classList.toggle('show');
});
bindTapDown(addPanelEl, (e)=>{
  const btn = e.target.closest('button[data-type]');
  if (!btn) return;
  const type = btn.dataset.type;
  if (type === 'group'){
    state.items.push(newGroupItem());
    addPanelEl.classList.remove('show');
    save(); render(); startTicking(true);
    return;
  }
  if (type === 'header'){
    state.items.push(newHeaderItem());
    addPanelEl.classList.remove('show');
    save(); render(); startTicking(true);
    return;
  }
  if (type === 'rule'){
    openSetup('rule');
    return;
  }
  // stam / abyss / idle（アカウント枠の中の「タイマー追加」で出てくる選択肢）
  openSetup(type);
});

// 追加種別ごとの設定ダイアログ(回復間隔 / 満タンまでの時間をここで固定する)。
let setupType = null;
function openSetup(type){
  setupType = type;
  addPanelEl.classList.remove('show');
  if (type === 'stam' || type === 'abyss'){
    setupLabelEl.textContent = type === 'abyss' ? 'Abyssスタミナ回復間隔' : 'スタミナ回復間隔';
    setupFieldsEl.innerHTML = `<input type="text" inputmode="numeric" pattern="[0-9]*" id="setupInterval" value="${type==='abyss'?'3':'5'}"><span>分で1回復</span>`;
  } else if (type === 'rule'){
    setupLabelEl.textContent = '仕切り線の色（#RRGGBB）';
    setupFieldsEl.innerHTML = `<input type="color" id="setupColorPick" value="#52617a" style="width:42px;height:36px;padding:0;border:1px solid var(--line);background:transparent;">
      <input type="text" id="setupColor" value="#52617a" maxlength="7" style="width:7em;background:var(--card2);border:1px solid var(--line);border-radius:8px;color:var(--text);font-size:14px;padding:8px;">`;
  } else {
    setupLabelEl.textContent = '満タンまでの時間';
    setupFieldsEl.innerHTML = `<input type="text" inputmode="numeric" pattern="[0-9]*" id="setupH" value="12"><span>h</span>
      <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupM" value="0"><span>m</span>`;
  }
  setupFieldsEl.querySelectorAll('input').forEach(el=>{
    if (el.type === 'color') return;
    forceCursorToEnd(el);
    clearOnFocus(el); // フォーカスで空欄（放置の h/m や回復間隔）
  });
  if (type === 'rule'){
    const pick = setupFieldsEl.querySelector('#setupColorPick');
    const text = setupFieldsEl.querySelector('#setupColor');
    if (pick && text){
      pick.addEventListener('input', ()=>{ text.value = pick.value; });
      text.addEventListener('input', ()=>{
        const v = text.value.trim();
        if (/^#[0-9a-fA-F]{6}$/.test(v)) pick.value = v;
      });
    }
  }
  setupPanelEl.classList.add('show');
}
function closeSetup(){
  setupType = null;
  setupPanelEl.classList.remove('show');
}
function pushNewTimer(item){
  const group = pendingAddGroupId ? state.items.find(g=>g.id===pendingAddGroupId && g.type==='group') : null;
  if (!group || group.children.length >= 2){
    pendingAddGroupId = null;
    addPanelEl.classList.remove('group-mode');
    return false;
  }
  group.children.push(item);
  pendingAddGroupId = null;
  addPanelEl.classList.remove('group-mode');
  return true;
}
bindTapDown(setupConfirmEl, (e)=>{
  e.stopPropagation();
  if (setupType === 'stam'){
    const interval = clampInt(setupFieldsEl.querySelector('#setupInterval').value, 1, 999, 5);
    if (!pushNewTimer(newStamItem(interval))) return;
  } else if (setupType === 'abyss'){
    const interval = clampInt(setupFieldsEl.querySelector('#setupInterval').value, 1, 999, 3);
    if (!pushNewTimer(newAbyssItem(interval))) return;
  } else if (setupType === 'rule'){
    let color = (setupFieldsEl.querySelector('#setupColor')||{}).value || '#52617a';
    color = String(color).trim();
    if (!/^#[0-9a-fA-F]{6}$/.test(color)) color = '#52617a';
    state.items.push(newRuleItem(color));
  } else if (setupType === 'idle'){
    const h = clampInt(setupFieldsEl.querySelector('#setupH').value, 0, 999, 12);
    const m = clampInt(setupFieldsEl.querySelector('#setupM').value, 0, 59, 0);
    if (!pushNewTimer(newIdleItem(h*60+m))) return;
  }
  closeSetup();
  save(); render(); startTicking(true);
});

const resumeVeilEl = document.getElementById('resumeVeil');
let resumeTimer = null;
function resumeFromBackground(){
  if (resumeTimer) clearTimeout(resumeTimer);
  // TWA/Android復帰直後はWebViewの合成・レイアウトが安定するまで短時間だけ覆う。
  resumeVeilEl.classList.add('show');
  requestAnimationFrame(()=>{
    // DOM構造は既に残っているため、復帰時は全体renderではなく表示値だけ再同期。
    tickRender(Date.now());
    startTicking(true);
    requestAnimationFrame(()=>{
      resumeTimer = setTimeout(()=>{
        resumeTimer = null;
        resumeVeilEl.classList.remove('show');
      }, 180);
    });
  });
}
document.addEventListener('visibilitychange', ()=>{
  if (document.hidden){
    tickGen++;
    if (tickId){ clearTimeout(tickId); tickId=null; }
    return;
  }
  resumeFromBackground();
});
window.addEventListener('pageshow', (e)=>{
  if (e.persisted) resumeFromBackground();
});

render();
startTicking(true);

// 長押しメニュー(コピー等)やドラッグ選択を出さない。
document.addEventListener('contextmenu', (e)=> e.preventDefault());
document.addEventListener('dragstart', (e)=> e.preventDefault());
// テキスト選択は入力欄側で必要な範囲だけ抑止する。
document.addEventListener('paste', (e)=> e.preventDefault());

bindTapDown(document.getElementById('updateBtn'), ()=>{
  // クエリ付きで update.html 自体のキャッシュも避ける
  location.href = 'update.html?r=' + Date.now().toString(36);
});

// Service worker: register once, never poll for updates automatically.
if ('serviceWorker' in navigator){
  window.addEventListener('load', ()=>{
    navigator.serviceWorker.register('sw.js');
  });
}
