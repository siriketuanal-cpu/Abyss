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
    const applyChunk = (commitNow)=>{
      const raw = String(chunkEl.value || '').replace(/\D/g, '');
      const num = Number(raw);
      if (raw === '' || num === 0){
        delete it.useChunk;
        if (commitNow) chunkEl.value = '';
        if (pending40Id === it.id){ pending40Id = null; paintUseChunkPreview(it.id); }
      } else {
        it.useChunk = clampUseChunk(num);
        if (commitNow) chunkEl.value = String(it.useChunk);
        if (pending40Id === it.id) paintUseChunkPreview(it.id);
      }
      if (refs[it.id]) refs[it.id].shortAction = hasUseChunk(it) ? (e) => onUseChunkTap(it, e) : null;
      updateOneTimer(it);
      save();
    };
    chunkEl.addEventListener('change', ()=>applyChunk(true));
    chunkEl.addEventListener('blur', ()=>applyChunk(true));
    chunkEl.addEventListener('keydown', e=>{
      if (e.key === 'Enter'){ e.preventDefault(); chunkEl.blur(); }
    });
    chunkEl.addEventListener('pointerdown', e=>e.stopPropagation());
  }

  const intervalEl = toastEl.querySelector('input[data-role="stamInterval"]');
  if (intervalEl){
    const origDisplay = it.intervalMin || 5;
    const applyInterval = (commitNow)=>{
      const raw = String(intervalEl.value || '').replace(/\D/g, '');
      const newNum = raw === '' ? origDisplay : Math.max(1, Math.min(999, parseInt(raw, 10) || origDisplay));
      if (commitNow) intervalEl.value = String(newNum);
      if (newNum === it.intervalMin) return;
      const now = Date.now();
      preserveCycle(it, now);
      it.intervalMin = newNum;
      updateOneTimer(it);
      startTicking(true);
      save();
    };
    intervalEl.addEventListener('change', ()=>applyInterval(true));
    intervalEl.addEventListener('blur', ()=>applyInterval(true));
    intervalEl.addEventListener('keydown', e=>{ if (e.key === 'Enter'){ e.preventDefault(); intervalEl.blur(); } });
    intervalEl.addEventListener('pointerdown', e=>e.stopPropagation());
  }

  const maxEl = toastEl.querySelector('input[data-role="stamMax"]');
  const origMax = it.max;
  const applyMaxVal = (val, updateInput = true) => {
    const newMax = clampInt(val, 1, 999, origMax);
    if (maxEl && updateInput) maxEl.value = String(newMax);
    if (newMax === it.max) return;
    const now = Date.now();
    const wasFull = it.current >= origMax;
    preserveCycle(it, now);
    it.max = newMax;
    if (wasFull || it.current > newMax) {
      it.current = newMax;
      freezeIfFull(it, now);
    } else {
      it.current = Math.min(it.current, newMax);
    }
    if (pending40Id === it.id) pending40Id = null;
    updateOneTimer(it);
    startTicking(true);
    save();
  };

  if (maxEl){
    maxEl.addEventListener('focus', ()=>{ maxEl.value = ''; });
    const applyMax = (commitNow)=>{
      const raw = String(maxEl.value || '').replace(/\D/g, '');
      if (raw === '') { if (commitNow) maxEl.value = String(it.max); return; }
      const newMax = clampInt(raw, 1, 999, origMax);
      applyMaxVal(newMax, commitNow);
    };
    maxEl.addEventListener('change', ()=>applyMax(true));
    maxEl.addEventListener('blur', ()=>applyMax(true));
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
      applyMaxVal(currentVal + delta, true);
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
    const applyChunk = (commitNow)=>{
      const raw = String(chunkEl.value || '').replace(/\D/g, '');
      const num = Number(raw);
      if (raw === '' || num === 0){
        delete it.useChunk;
        if (commitNow) chunkEl.value = '';
        if (pending40Id === it.id){ pending40Id = null; paintUseChunkPreview(it.id); }
      } else {
        it.useChunk = clampUseChunk(num);
        if (commitNow) chunkEl.value = String(it.useChunk);
        if (pending40Id === it.id) paintUseChunkPreview(it.id);
      }
      if (refs[it.id]) refs[it.id].shortAction = hasUseChunk(it) ? (e) => onUseChunkTap(it, e) : null;
      updateOneTimer(it);
      save();
    };
    chunkEl.addEventListener('change', ()=>applyChunk(true));
    chunkEl.addEventListener('blur', ()=>applyChunk(true));
    chunkEl.addEventListener('keydown', e=>{
      if (e.key === 'Enter'){ e.preventDefault(); chunkEl.blur(); }
    });
    chunkEl.addEventListener('pointerdown', e=>e.stopPropagation());
  }

  const maxEl = toastEl.querySelector('input[data-role="orbMax"]');
  if (maxEl){
    maxEl.addEventListener('focus', ()=>{ maxEl.value = ''; });
    const applyMax = (commitNow)=>{
      const raw = String(maxEl.value || '').replace(/\D/g, '');
      if (raw === '') { if (commitNow) maxEl.value = String(it.max); return; }
      const newMax = clampInt(raw, 1, 99, it.max);
      if (commitNow) maxEl.value = String(newMax);
      if (newMax === it.max) return;
      const now = Date.now();
      const wasFull = it.current >= it.max;
      preserveOrbCycle(it, now);
      it.max = newMax;
      if (wasFull || it.current > newMax) {
        it.current = newMax;
        freezeOrbIfFull(it, now);
      } else {
        it.current = Math.min(it.current, newMax);
      }
      updateOneTimer(it);
      startTicking(true);
      save();
    };
    maxEl.addEventListener('change', ()=>applyMax(true));
    maxEl.addEventListener('blur', ()=>applyMax(true));
    maxEl.addEventListener('keydown', e=>{ if (e.key === 'Enter'){ e.preventDefault(); maxEl.blur(); } });
    maxEl.addEventListener('pointerdown', e=>e.stopPropagation());
  }

  const intervalEl = toastEl.querySelector('input[data-role="orbInterval"]');
  if (intervalEl){
    const isHours = it.intervalMin >= 60 && it.intervalMin % 60 === 0;
    const origDisplay = isHours ? (it.intervalMin / 60) : it.intervalMin;
    const applyInterval = (commitNow)=>{
      const raw = String(intervalEl.value || '').replace(/\D/g, '');
      const newNum = raw === '' ? origDisplay : Math.max(1, Math.min(999, parseInt(raw, 10) || origDisplay));
      if (commitNow) intervalEl.value = String(newNum);
      const newInterval = isHours ? (newNum * 60) : newNum;
      if (newInterval === it.intervalMin) return;
      const now = Date.now();
      preserveOrbCycle(it, now);
      it.intervalMin = newInterval;
      updateOneTimer(it);
      startTicking(true);
      save();
    };
    intervalEl.addEventListener('change', ()=>applyInterval(true));
    intervalEl.addEventListener('blur', ()=>applyInterval(true));
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
    orbFullRemEl.addEventListener('blur', applyOrbFullRem);
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
    for (const [k, v]] of [['durH', dh], ['durM', dm], ['remH', rh], ['remM', rm]]){
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
  box.addEventListener('focusin', e=>{
    const t = e.target;
    if (rowOf(t)) {
      t.value = '';
    }
  });
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
