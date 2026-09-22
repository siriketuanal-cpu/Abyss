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
function newOrbItem(intervalMin, max){
  const m = clampInt(max, 1, 99, 4);
  return {
    id: uid(),
    type: 'orb',
    name: '',
    current: m,
    max: m,
    intervalMin: clampInt(intervalMin, 1, 99999, 360),
    start: Date.now(),
    orbMode: 'down'
  };
}
function hasUseChunk(it){
  return !!(it && it.type === 'stam' && it.useChunk != null && Number(it.useChunk) > 0);
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
