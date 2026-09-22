// toast-core.js: トーストメニューの開閉・削除確認(askRemoveItem)本体
// この並び順(index.htmlの<script>タグの順番)を変えると、他ファイルの関数/変数を先に参照してエラーになる場合があります。

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
  const colorDefault = isHeader ? (it.color || '#9b8bff') : (isRule ? (it.color || '#52617a') : (isGroup ? (it.color || '#555b68') : null));

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
        } else if (isGroup && refs[id]?.el){
          refs[id].el.style.borderColor = it.color;
          if (refs[id]?.nameEditor) refs[id].nameEditor.setColor(it.color);
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
