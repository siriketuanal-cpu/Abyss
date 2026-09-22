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
// 経過時間を「◯:◯◯」のカウントアップ表記で返す(放置報酬用)。
function fmtElapsed(ms){
  ms = Math.max(0, ms);
  const totalMin = Math.floor(ms/60000);
  const h = Math.floor(totalMin/60), m = totalMin%60;
  return h + ':' + String(m).padStart(2,'0');
}

function stamInfo(it, now){
  // ベースが既に満タンなら回復計算しない（満タン時刻は start に固定）
  if (it.current >= it.max){
    return { cur: it.max, remainMs:0, isFull:true, fullAt: it.start };
  }
  const intervalMs = Math.max(1, it.intervalMin) * 60000;
  const elapsed = Math.max(0, now - it.start);
  const recovered = Math.floor(elapsed / intervalMs);
  const cur = Math.min(it.max, it.current + recovered);
  if (cur >= it.max){
    const fullAt = it.start + Math.max(0, it.max - it.current) * intervalMs;
    return { cur: it.max, remainMs:0, isFull:true, fullAt: Math.min(now, fullAt) };
  }
  const nextIn = intervalMs - (elapsed % intervalMs);
  const need = it.max - cur;
  const remainMs = (need - 1) * intervalMs + nextIn;
  return { cur, remainMs, isFull:false };
}
// 満タンにしたら start を今に固定して、以降の回復計算を止める
function freezeIfFull(it, now){
  if (it.current >= it.max){
    it.current = it.max;
    it.start = now;
  }
}

// ── オーブ専用の回復計算とサイクル管理 ──
function orbInfo(it, now){
  if (it.current >= it.max){
    return { cur: it.max, remainMs: 0, nextInMs: 0, isFull: true, fullAt: it.start };
  }
  const intervalMs = Math.max(1, it.intervalMin) * 60000;
  const elapsed = Math.max(0, now - it.start);
  const recovered = Math.floor(elapsed / intervalMs);
  const cur = Math.min(it.max, it.current + recovered);
  if (cur >= it.max){
    const fullAt = it.start + Math.max(0, it.max - it.current) * intervalMs;
    return { cur: it.max, remainMs: 0, nextInMs: 0, isFull: true, fullAt: Math.min(now, fullAt) };
  }
  const nextInMs = intervalMs - (elapsed % intervalMs);
  const need = it.max - cur;
  const remainMs = (need - 1) * intervalMs + nextInMs;
  return { cur, remainMs, nextInMs, isFull: false, fullAt: now + remainMs };
}
function freezeOrbIfFull(it, now){
  if (it.current >= it.max){
    it.current = it.max;
    it.start = now;
  }
}
function preserveOrbCycle(it, now){
  if (it.current >= it.max){
    it.start = now;
    return;
  }
  const intervalMs = Math.max(1, it.intervalMin) * 60000;
  const phase = ((now - it.start) % intervalMs + intervalMs) % intervalMs;
  it.start = now - phase;
}

function parseTimeMinutes(raw){
  const digits = String(raw || '').replace(/\D/g, '');
  if (!digits) return null;
  let h = 0, m = 0;
  if (digits.length >= 3){
    if (digits.length === 3){
      h = Number(digits.slice(0, 1));
      m = Number(digits.slice(1, 3));
    } else {
      h = Number(digits.slice(0, digits.length - 2));
      m = Number(digits.slice(-2));
    }
  } else {
    m = Number(digits);
  }
  m = Math.min(59, m);
  return h * 60 + m;
}

// オーブの時間入力処理
// isFullField: true の場合はトーストの「全回復/総蓄積」欄からの入力として扱い、false の場合はカード上の入力として判定
function applyOrbTimeInput(it, digits, now, isFullField){
  now = now || Date.now();
  const inputMin = parseTimeMinutes(digits);
  if (inputMin == null) return;
  const oneOrbMin = Math.max(1, it.intervalMin);
  const totalMaxMin = it.max * oneOrbMin;
  const info = orbInfo(it, now);

  if (it.orbMode === 'up'){
    // ── 累積（経過時間）モード ──
    if (!isFullField && inputMin <= oneOrbMin && info.cur < it.max){
      // カードから1個分以内の経過時間を指定：現在の個数を保ち、位相を進める
      const phaseMin = Math.min(oneOrbMin, Math.max(0, inputMin));
      if (phaseMin >= oneOrbMin){
        it.current = Math.min(it.max, info.cur + 1);
        it.start = now;
      } else {
        it.current = info.cur;
        it.start = now - (phaseMin * 60000);
      }
    } else {
      // 全体の蓄積時間（またはトースト欄からの入力）
      const elapsedMin = Math.min(totalMaxMin, Math.max(0, inputMin));
      if (elapsedMin >= totalMaxMin){
        it.current = it.max;
        it.start = now;
      } else {
        const cur = Math.min(it.max - 1, Math.max(0, Math.floor(elapsedMin / oneOrbMin)));
        const phaseMin = elapsedMin % oneOrbMin;
        it.current = cur;
        it.start = now - (phaseMin * 60000);
      }
    }
  } else {
    // ── 減算（残り時間）モード ──
    if (!isFullField && inputMin <= oneOrbMin && info.cur < it.max){
      // カードから「次の1個回復までの残り時間」を指定：現在の個数を維持し、次の回復までの時間をセット
      const remainInThisOrb = Math.min(oneOrbMin, Math.max(0, inputMin));
      if (remainInThisOrb <= 0){
        it.current = Math.min(it.max, info.cur + 1);
        it.start = now;
      } else {
        it.current = info.cur;
        const phaseMin = oneOrbMin - remainInThisOrb;
        it.start = now - (phaseMin * 60000);
      }
    } else {
      // 全回復までの残り時間（トースト欄、または1個の時間を超える全体の残り時間入力）
      const totalRemainMin = inputMin;
      if (totalRemainMin <= 0){
        it.current = it.max;
        it.start = now;
      } else if (totalRemainMin >= totalMaxMin){
        it.current = 0;
        it.start = now;
      } else {
        const elapsedMin = totalMaxMin - totalRemainMin;
        const cur = Math.min(it.max - 1, Math.max(0, Math.floor(elapsedMin / oneOrbMin)));
        const phaseMin = elapsedMin % oneOrbMin;
        it.current = cur;
        it.start = now - (phaseMin * 60000);
      }
    }
  }
  freezeOrbIfFull(it, now);
}

// 現在値/上限値を書き換えても「次の1回復までの残り時間」がリセットされないよう、
// サイクルの位相(既にどれだけ経過しているか)を維持したまま基準時刻だけを進める。
function clampUseChunk(v){
  const n = Math.floor(Number(v) || 0);
  return Math.max(1, Math.min(999, n || 1));
}
// 使い切り後の残り。mod: 220→20
function remainingAfterUse(cur, it){
  cur = Math.max(0, Math.floor(Number(cur) || 0));
  const fallback = 1;
  const c = clampUseChunk(it && it.useChunk != null ? it.useChunk : fallback);
  return cur % c;
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
  return { elapsed, remainMs, isFull: remainMs<=0, fullAt: it.start + durMs };
}
function fmtHM(ts){
  const d = new Date(ts);
  const p = n => String(n).padStart(2,'0');
  return p(d.getHours()) + ':' + p(d.getMinutes());
}
// 満タン、または満タンまで2時間未満 → 赤
function isNearFull(remainMs, isFull){
  return !!isFull || (remainMs > 0 && remainMs < 7200000);
}
// 現在値の赤表示（文字に1回だけ当てる）
function setNearOnCurrent(editor, near){
  if (!editor || !editor.text) return;
  if (near) editor.text.style.setProperty('color', 'var(--danger)', 'important');
  else editor.text.style.removeProperty('color');
}

function timerNeedsTick(it, now){
  if (it.type==='stam') return !stamInfo(it, now).isFull;
  if (it.type==='orb') return !orbInfo(it, now).isFull;
  // claim 中は表示固定のため tick 不要。
  if (it.type==='idle' || it.type==='exped') return it.state !== 'claim' && !idleInfo(it, now).isFull;
  return false;
}
function needsTicking(now){
  now = now || Date.now();
  return state.items.some(it=>{
    if (it.type==='group') return it.children.some(c=>timerNeedsTick(c, now));
    return timerNeedsTick(it, now);
  });
}

// ── 描画前段：自然満タンの状態同期を一括処理（描画関数から副作用を分離）──
function syncFullStamItems(now){
  let changed = false;
  const check = (it) => {
    if (it && it.type === 'stam' && it.current < it.max){
      const info = stamInfo(it, now);
      if (info.isFull){
        it.current = it.max;
        it.start = info.fullAt || now;
        changed = true;
      }
    } else if (it && it.type === 'orb' && it.current < it.max){
      const info = orbInfo(it, now);
      if (info.isFull){
        it.current = it.max;
        it.start = info.fullAt || now;
        changed = true;
      }
    }
  };
  for (const it of state.items){
    if (it.type === 'group') it.children.forEach(check);
    else check(it);
  }
  if (changed) saveAfterPaint();
}

function tickRender(now){
  now = now || Date.now();
  syncFullStamItems(now);
  for (const it of state.items){
    const r = refs[it.id];
    if (!r) continue;
    if (it.type==='group'){
      for (const child of it.children) updateTimerCard(child, r.childRefs[child.id], now);
    } else if (it.type==='stam' || it.type==='orb' || it.type==='idle' || it.type==='exped'){
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

// 現在値編集中やメニューボタンは長押し削除／使い切りと分離
