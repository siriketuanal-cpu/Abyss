// panels.js: 追加パネル・設定パネル(色選択含む)の開閉と確定処理
// この並び順(index.htmlの<script>タグの順番)を変えると、他ファイルの関数/変数を先に参照してエラーになる場合があります。

let pendingAddGroupId = null;
let pendingInsertAfterId = null;
let setupType = null;
let setupIdleMode = 'down';
let setupOrbMode = 'down';

function closeAddPanel(){
  addPanelEl.classList.remove('show');
  addPanelEl.classList.remove('group-mode');
}
function openAddPanel(opts){
  opts = opts || {};
  pendingAddGroupId = opts.groupId || null;
  pendingInsertAfterId = opts.insertAfterId || null;
  addPanelEl.classList.toggle('group-mode', !!pendingAddGroupId);
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
    setupLabelEl.textContent = 'ゲーム名ヘッダーの文字色';
    fillColorPalette('#9b8bff');
  } else if (type === 'stam'){
    setupLabelEl.textContent = 'スタミナ設定';
    setupFieldsEl.innerHTML = `
      <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupInterval" value="5"><span>分で1</span>
      <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupMax" value="100" style="margin-left:6px;"><span>最大</span>
      <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupUseChunk" value="" style="margin-left:6px;" placeholder="—"><span>使い切り</span>`;
  } else if (type === 'orb'){
    setupLabelEl.textContent = 'オーブ設定';
    setupOrbMode = 'down';
    setupFieldsEl.innerHTML = `
      <div style="width:100%;margin-bottom:6px;">
        <button type="button" class="idle-mode-btn" data-role="orbModeToggle" style="width:100%;text-align:center;">▼ 残り時間入力（カウントダウン）</button>
      </div>
      <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupOrbHours" value="6"><span>時間で1個</span>
      <input type="text" inputmode="numeric" pattern="[0-9]*" id="setupMax" value="4" style="margin-left:6px;"><span>最大</span>`;
    const modeBtn = setupFieldsEl.querySelector('[data-role="orbModeToggle"]');
    if (modeBtn){
      bindTapDown(modeBtn, (e)=>{
        e.stopPropagation();
        setupOrbMode = (setupOrbMode === 'up') ? 'down' : 'up';
        modeBtn.textContent = (setupOrbMode === 'up') ? '▲ 経過時間入力（カウントアップ）' : '▼ 残り時間入力（カウントダウン）';
      });
    }
  } else if (type === 'rule'){
    setupLabelEl.textContent = '仕切り線の色';
    fillColorPalette('#52617a');
  } else if (type === 'exped'){
    setupLabelEl.textContent = '遠征タイマーの設定';
    setupIdleMode = 'down';
    setupFieldsEl.innerHTML = `<div style="width:100%;margin-bottom:6px;">`
      + idleModeRowHtml(setupIdleMode)
      + `</div>`
      + `<input type="text" inputmode="numeric" pattern="[0-9]*" id="setupH" value="4"><span>h</span>`
      + `<input type="text" inputmode="numeric" pattern="[0-9]*" id="setupM" value="0"><span>m</span>`;
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
    setupFieldsEl.innerHTML = `<div style="width:100%;margin-bottom:6px;">`
      + idleModeRowHtml(setupIdleMode)
      + `</div>`
      + `<input type="text" inputmode="numeric" pattern="[0-9]*" id="setupH" value="12"><span>h</span>`
      + `<input type="text" inputmode="numeric" pattern="[0-9]*" id="setupM" value="0"><span>m</span>`;
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
    const item = newOrbItem(hours * 60, max);
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
