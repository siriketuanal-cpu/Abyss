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
}
function showConfirmToast(html, onAct){
  if (!toastEl) return;
  closeToast();
  toastEl.innerHTML = html;
  toastEl.classList.add('show');
  const onClick = (e)=>{
    const btn = e.target.closest('button[data-act]');
    if (!btn) return;
    const act = btn.dataset.act;

    // トグル系：トーストを閉じずに即時切り替え＆表示更新
    if (act && act.startsWith('toggle')){
      if (typeof onAct === 'function') onAct(act, btn);
      return;
    }

    // 移動系：即時移動しつつトーストを閉じる
    if (act === 'moveUp' || act === 'moveDown'){
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
  const canInsertOrMove = isHeader || isGroup || isRule;
  const canEditName = isHeader || isGroup;
  const colorDefault = isHeader ? (it.color || '#9b8bff') : (isRule ? (it.color || '#52617a') : null);

  const moveButtons = canInsertOrMove
    ? `<div class="toast-fields-btns"><button type="button" data-act="moveUp">↑ 上へ移動</button><button type="button" data-act="moveDown">↓ 下へ移動</button></div>`
    : '';
  const fields =
    (colorDefault ? `<label class="toast-field"><span class="toast-color"><span class="toast-label">色</span><input type="color" value="${colorDefault}" aria-label="色"></span></label>` : '')
    + (isStam ? stamToastRowsHtml(it) : '')
    + (isOrb ? orbToastRowsHtml(it) : '')
    + (isHeader ? `<button type="button" data-act="toggleFoldLock">${it.foldLock ? '折りたたみ：🔒 固定中' : '折りたたみ：🔓 開閉可能'}</button>` : '')
    + (isGroup ? `<button type="button" data-act="toggleGroupLayout">${it.layout === '2x2' ? '配置：⊞ 2×2' : '配置：☰ 1行'}</button>` : '')
    + (canEditName ? `<button type="button" data-act="editName">${isHeader ? '見出し名を変更' : 'アカウント名を変更'}</button>` : '')
    + (canAdd ? '<button type="button" data-act="add">＋ タイマーを追加</button>' : '')
    + (canInsertOrMove ? '<button type="button" data-act="insertBelow">＋ 下に枠を挿入</button>' : '')
    + moveButtons
    + (isIdle ? idleToastRowsHtml(it) : '');   // 放置：設定時間／残り時間の入力行
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
      else if (act === 'moveUp') moveTopItem(id, -1);
      else if (act === 'moveDown') moveTopItem(id, 1);
      else if (act === 'toggleGroupLayout'){
        it.layout = (it.layout === '2x2') ? '1row' : '2x2';
        if (btn) btn.textContent = (it.layout === '2x2') ? '配置：⊞ 2×2' : '配置：☰ 1行';
        save(); render();
      }
      else if (act === 'toggleFoldLock'){
        it.foldLock = !it.foldLock;
        if (it.foldLock) it.collapsed = false;
        if (btn) btn.textContent = it.foldLock ? '折りたたみ：🔒 固定中' : '折りたたみ：🔓 開閉可能';
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
      colorEl.addEventListener('input', ()=>{
        it.color = colorEl.value;
        if (isHeader){
          if (refs[id]?.nameEditor) refs[id].nameEditor.setColor(it.color);
          else if (refs[id]?.nameEl) refs[id].nameEl.style.color = it.color;
          if (refs[id]?.menuBtn) refs[id].menuBtn.style.setProperty('--header-color', it.color);
        } else if (isRule && refs[id]?.el){
          refs[id].el.style.borderTopColor = it.color;
        }
        save();
      });
      colorEl.addEventListener('pointerdown', e=>e.stopPropagation());
    }
  }
  if (isStam) bindStamToastRows(it);
  if (isOrb) bindOrbToastRows(it);
  if (isIdle) bindIdleToastRows(it);
}

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

  return `<label class="toast-field"><span class="toast-color"><span class="toast-label">最大</span><input type="text" inputmode="numeric" pattern="[0-9]*" value="${it.max}" maxlength="2" data-role="orbMax" aria-label="最大個数"></span></label>`
    + `<label class="toast-field"><span class="toast-color"><span class="toast-label">回復(${intervalUnit})</span><input type="text" inputmode="numeric" pattern="[0-9]*" value="${intervalVal}" maxlength="3" data-role="orbInterval" aria-label="回復時間"></span></label>`
    + `<label class="toast-field wide"><span class="toast-color"><span class="toast-label" data-role="orbTimeLabel">${timeLabel}</span><input type="text" inputmode="numeric" pattern="[0-9]*" value="${fullRemFormatted}" maxlength="5" data-role="orbFullRem" placeholder="17:59" aria-label="${timeLabel}"></span></label>`
    + `<button type="button" data-act="toggleOrbMode">${isUp ? '方式：▲ 経過時間(蓄積)' : '方式：▼ 残り時間(減算)'}</button>`;
}

function bindOrbToastRows(it){
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
/* ═══════════ 放置トースト：設定時間／残り時間（ここまで）═══════════ */

