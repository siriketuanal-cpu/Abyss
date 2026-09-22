// card-builders.js: 各カード(スタミナ/オーブ/放置/仕切り線/見出し)のDOM構築
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
