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
      if (isSetupOpen){
        closeSetup();
        pendingAddGroupId = null;
        pendingInsertAfterId = null;
      }
      if (isAddOpen){
        closeAddPanel();
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
      if (r.nameEditor){
        r.nameEditor.setColor(it.color || '#9b8bff');
        if (!r.nameEditor.isEditing()) r.nameEditor.setText(it.name || '');
      }
    } else {
      if (currentCollapsed){
        r.el.style.display = 'none';
        if (it.type !== 'rule') collapsedCount++;
      } else {
        r.el.style.display = '';
        if (it.type === 'rule'){
          r.el.style.borderTopColor = it.color || '#52617a';
        } else if (it.type === 'group'){
          if (r.nameEditor && !r.nameEditor.isEditing()) r.nameEditor.setText(it.name || '');
          for (const child of it.children) updateTimerCard(child, r.childRefs[child.id], now);
        } else {
          updateTimerCard(it, r, now);
        }
      }
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
    if (r.el){ fitObserve(r.el); scheduleFit(r.el); }
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
