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
  const info = stamInfo(it, now);
  if (pending40Id === it.id){
    preserveCycle(it, now);
    it.current = remainingAfterUse(info.cur, it);
    freezeIfFull(it, now);
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
  const curEditor = createInlineNumber(orbInfo(it, Date.now()).cur, value => {
    const now = Date.now();
    applyOrbInput(it, value, now);
    updateOneTimer(it);
    startTicking(true);
    saveAfterPaint();
  }, it.id + ':cur', null, 4);
  curWrap.appendChild(curEditor.wrap);
  return curEditor;
}

function buildOrbCard(it){
  const wrap = makeCardEl('orb', it.id, `
    <div class="clockstack orb-clock" data-role="clock"></div>
    <div class="orb-next-rem" data-role="nextRem"></div>
    ${valrowStamHtml()}
  `);
  const curWrap = wrap.querySelector('[data-role="curWrap"]');
  const maxWrap = wrap.querySelector('[data-role="maxWrap"]');
  const curEditor = attachOrbCurrentEditor(curWrap, it);
  maxWrap.textContent = String(it.max);
  const r = { el: wrap, curEl: curEditor, maxLabel: maxWrap,
    clockEl: wrap.querySelector('[data-role="clock"]'),
    nextRemEl: wrap.querySelector('[data-role="nextRem"]'),
    shortAction: null };
  refs[it.id] = r;
  // オーブはカードタップでの減算機能を外した（数値の直接編集のみ）
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
  wrap.innerHTML = `
    <div class="row namerow group-namerow" data-role="namerow"></div>
    <div class="group-body" data-role="body"></div>
  `;
  const nameRowEl = wrap.querySelector('[data-role="namerow"]');
  const nameEditor = createInlineText(it.name || '', 'アカウント', (value)=>{
    if (it.name !== value){ it.name = value; save(); }
  });
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
  if (claimId != null){
    const r = getTimerRef(claimId);
    if (!r || !r.el.contains(e.target)) cancelClaim(claimId);
  }
  if (pending40Id != null){
    const r = getTimerRef(pending40Id);
    if (!r || !r.el.contains(e.target)){
      const id = pending40Id;
      pending40Id = null;
      paintUseChunkPreview(id);
    }
  }
  if (addPanelEl.classList.contains('show') && !addPanelEl.contains(e.target) && e.target !== addBtnEl){
    closeAddPanel();
    pendingAddGroupId = null;
    pendingInsertAfterId = null;
  }
  if (setupType === 'rule' && setupPanelEl.classList.contains('show') && !setupPanelEl.contains(e.target)){
    closeSetup();
  }
  // トースト表示中に枠外をタップした場合は、背後要素への貫通（短押し消費・見出し開閉など）を遮断してトーストを閉じるだけにする
  if (toastEl && toastEl.classList.contains('show')){
    if (!toastEl.contains(e.target) && !e.target.closest(MENU_BTN_SEL)){
      e.preventDefault();
      e.stopPropagation();
      const activeInput = toastEl.querySelector('input:focus');
      if (activeInput) activeInput.blur();
      closeToast();
    }
  }
}, {capture:true, passive:false});

function clampInt(v, min, max, fb){
  const n = parseInt(v,10);
  return Number.isFinite(n) ? Math.max(min, Math.min(max,n)) : fb;
}

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

function getItemSpan(it){
  if (!it) return 1;
  if (it.type === 'header' || it.type === 'rule') return 4;
  if (it.type === 'group'){
    return (it.children && it.children.length > 1) ? 2 : 1;
  }
  return 1;
}

function getRowAssignments(){
  const rows = [];
  let currentRow = [];
  let currentCol = 0;
  for (let i = 0; i < state.items.length; i++){
    const it = state.items[i];
    const span = getItemSpan(it);
    if (span === 4){
      if (currentRow.length > 0){
        rows.push(currentRow);
        currentRow = [];
        currentCol = 0;
      }
      rows.push([i]);
      continue;
    }
    if (currentCol + span > 4){
      rows.push(currentRow);
      currentRow = [];
      currentCol = 0;
    }
    currentRow.push(i);
    currentCol += span;
    if (currentCol >= 4){
      rows.push(currentRow);
      currentRow = [];
      currentCol = 0;
    }
  }
  if (currentRow.length > 0){
    rows.push(currentRow);
  }
  return rows;
}

function moveTopItem(id, dir){
  const topId = getTopLevelItemId(id) || id;
  const idx = state.items.findIndex(i => i.id === topId);
  if (idx === -1) return;
  const it = state.items[idx];
  const isFullRow = (it.type === 'header' || it.type === 'rule');

  if (isFullRow){
    const rows = getRowAssignments();
    const rIdx = rows.findIndex(r => r.includes(idx));
    if (rIdx === -1) return;
    const targetRowIdx = rIdx + dir;
    if (targetRowIdx < 0 || targetRowIdx >= rows.length) return;
    const targetRow = rows[targetRowIdx];
    const targetIdx = (dir > 0) ? targetRow[targetRow.length - 1] : targetRow[0];
    const [item] = state.items.splice(idx, 1);
    state.items.splice(targetIdx, 0, item);
  } else {
    const targetIdx = idx + dir;
    if (targetIdx < 0 || targetIdx >= state.items.length) return;
    const [item] = state.items.splice(idx, 1);
    state.items.splice(targetIdx, 0, item);
  }
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
  if (it && it.type === 'stam') updateOneTimer(it);
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
    r.el.classList.toggle('full', info.isFull);
    renderClock(r.clockEl, info.remainMs, info.isFull, now, info.fullAt);
    if (r.curEl && !r.curEl.isEditing()) r.curEl.setText(info.cur);
    if (r.maxLabel){
      const v = String(it.max);
      if (r.maxLabel.textContent !== v) r.maxLabel.textContent = v;
    }
    if (r.nextRemEl){
      const nextTxt = info.isFull ? '' : ('次 ' + fmtCountdown(info.nextInMs));
      if (r.nextRemEl.textContent !== nextTxt) r.nextRemEl.textContent = nextTxt;
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

let pendingAddGroupId = null;
let pendingInsertAfterId = null;
let setupType = null;
let setupIdleMode = 'down';
let setupOrbMode = 'down';

