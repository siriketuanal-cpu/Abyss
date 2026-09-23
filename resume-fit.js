// resume-fit.js: バックグラウンド復帰処理(二重RAF)・機種差フィット・起動時イベント登録
// この並び順(index.htmlの<script>タグの順番)を変えると、他ファイルの関数/変数を先に参照してエラーになる場合があります。

let wasBackgrounded = false;

// ── バックグラウンド・非表示・ページ離脱時の即時保存 ──
function flushPendingSaveOnHide(){
  try {
    if (document.activeElement && typeof document.activeElement.blur === 'function'){
      document.activeElement.blur();
    }
  } catch(e){}
  if (pendingSave){
    saveNow();
  }
}

// 復帰処理：黒幕や無駄な待機・二重描画を省き、即座に差分更新してtick再開
function resumeApp(){
  // 万一DOMノードが消失していた場合の自己修復フォールバック
  if (!cardsEl || !cardsEl.firstElementChild){
    render();
  } else {
    tickRender(Date.now()); // tickRender内でsyncFullStamItemsも実行される
  }
  startTicking(true);
}

let resumeRaf = 0;
function handlePause(){
  flushPendingSaveOnHide();
  wasBackgrounded = true;
  if (resumeRaf){ cancelAnimationFrame(resumeRaf); resumeRaf = 0; }
  if (menuOpenRaf){ cancelAnimationFrame(menuOpenRaf); menuOpenRaf = 0; }
  tickGen++;
  if (tickId){ clearTimeout(tickId); tickId = null; }
}

// ダブルRAF：1フレーム目でGPU描画領域の復元・ビューポート確定を待ち、2フレーム目で時刻同期。
// visibilitychange / resume / pageshow / focus が連発しても1回に集約する。
function handleResume(){
  wasBackgrounded = false;
  if (resumeRaf) return;
  resumeRaf = requestAnimationFrame(()=>{
    resumeRaf = requestAnimationFrame(()=>{
      resumeRaf = 0;
      if (document.hidden) return;
      resumeApp();
    });
  });
}

document.addEventListener('visibilitychange', ()=>{
  if (document.hidden) handlePause();
  else handleResume();
});

// Android Chrome / Chromium の Page Lifecycle API（凍結・復帰）
document.addEventListener('freeze', handlePause);
document.addEventListener('resume', handleResume);

window.addEventListener('pagehide', handlePause);
window.addEventListener('beforeunload', flushPendingSaveOnHide);
window.addEventListener('pageshow', (e)=>{ handleResume(); });
window.addEventListener('focus', ()=>{
  if (!document.hidden) handleResume();
});

/* ═══════════════ 機種差フィット（ここから）═══════════════
   スタミナの現在値/最大値が枠に入り切らない端末向けに、必要な分だけ文字を縮める保険。
   digitEmの実測（DOM生成を伴う）は端末ごとに一度で済むよう localStorage にキャッシュし、
   フォント設定（サイズ/フォント/太さ）が前回と同じなら、次回起動以降は測定自体を丸ごとスキップする。
   updateTimerCardへの割り込み（上書き）はやめ、stam分岐の最後から直接呼ぶだけにしている。 */
const FIT_MIN_FONT = 11, FIT_SAFETY = 0.98, FIT_TOLERANCE = 0.5;
const FIT_CACHE_KEY = 'abyssFitDigitEmV1';
let fitDigitEm = 0, fitBaseFont = 18;
let fitRO = null;
const fitSeen = new WeakSet();

function fitCalibrate(slot){
  const cs = getComputedStyle(slot);
  fitBaseFont = parseFloat(cs.fontSize) || 18;
  const sig = fitBaseFont + '|' + cs.fontFamily + '|' + cs.fontWeight;
  try {
    const cached = JSON.parse(localStorage.getItem(FIT_CACHE_KEY) || 'null');
    if (cached && cached.sig === sig && cached.digitEm){
      fitDigitEm = cached.digitEm;
      return;
    }
  } catch(err){}
  const probe = document.createElement('span');
  probe.textContent = '0123456789';
  probe.style.cssText = 'position:absolute;left:-9999px;top:0;visibility:hidden;white-space:nowrap;line-height:1;font-size:100px;font-variant-numeric:tabular-nums;';
  probe.style.fontFamily = cs.fontFamily;
  probe.style.fontWeight = cs.fontWeight;
  document.body.appendChild(probe);
  const node = probe.firstChild, rg = document.createRange();
  let w = 0;
  for (let i = 0; i < 10; i++){
    rg.setStart(node, i); rg.setEnd(node, i + 1);
    w = Math.max(w, rg.getBoundingClientRect().width);
  }
  probe.remove();
  fitDigitEm = w / 100;
  if (fitDigitEm){
    try { localStorage.setItem(FIT_CACHE_KEY, JSON.stringify({ sig, digitEm: fitDigitEm })); } catch(err){}
  }
}

function fitApply(card){
  const slotW = card._slotW;
  if (!slotW) return;
  const cur = card._curEl || (card._curEl = card.querySelector('.stamval'));
  const max = card._maxEl || (card._maxEl = card.querySelector('.stammax'));
  const curText = card._curTextEl || (card._curTextEl = (cur && cur.querySelector('.inline-number-text')));
  if (!cur || !max || !curText) return;
  if (!fitDigitEm){ fitCalibrate(cur); if (!fitDigitEm) return; }

  const lc = curText.textContent.length, lm = max.textContent.length;
  const key = slotW + '|' + lc + '|' + lm;
  if (card._fitKey === key) return;
  card._fitKey = key;

  const need = Math.max(lc, lm) * fitDigitEm * fitBaseFont;
  let px = '';
  if (need > slotW + FIT_TOLERANCE){
    px = Math.max(FIT_MIN_FONT, Math.floor(fitBaseFont * (slotW / need) * FIT_SAFETY * 10) / 10) + 'px';
  }
  if (cur.style.fontSize !== px){ cur.style.fontSize = px; max.style.fontSize = px; }
}

function fitObserve(card){
  if (typeof ResizeObserver === 'undefined') return;
  if (!fitRO){
    fitRO = new ResizeObserver(entries => {
      for (const e of entries){
        const c = e.target.closest && e.target.closest('.card');
        if (!c) continue;
        c._slotW = e.contentRect.width;
        fitApply(c);
      }
    });
  }
  if (!fitSeen.has(card)){
    const slot = card.querySelector('.stamval');
    if (slot){ fitSeen.add(card); fitRO.observe(slot); }
  }
}
/* ═══════════════ 機種差フィット（ここまで）═══════════════ */

// 合成clickがモーダル外（トースト・追加パネル・設定パネル）に貫通するのを防ぐ
document.addEventListener('click', (e)=>{
  const isAnyModalOpen = (toastEl && toastEl.classList.contains('show')) ||
                         (addPanelEl && addPanelEl.classList.contains('show')) ||
                         (setupPanelEl && setupPanelEl.classList.contains('show'));
  if (isAnyModalOpen){
    const inToast = toastEl && toastEl.contains(e.target);
    const inAdd = addPanelEl && addPanelEl.contains(e.target);
    const inSetup = setupPanelEl && setupPanelEl.contains(e.target);
    if (!inToast && !inAdd && !inSetup && !e.target.closest(MENU_BTN_SEL)){
      e.preventDefault();
      e.stopPropagation();
    }
  }
}, {capture:true, passive:false});

render();
startTicking(true);

// 長押しメニュー(コピー等)やドラッグ選択を出さない。
document.addEventListener('contextmenu', (e)=> e.preventDefault());
document.addEventListener('dragstart', (e)=> e.preventDefault());
// テキスト選択は入力欄側で必要な範囲だけ抑止する。
document.addEventListener('paste', (e)=> e.preventDefault());

// ── 完全オフライン保護 ＆ 高速スマート更新（In-App Smart Update） ──
let isUpdating = false;

async function performSmartUpdate(){
  if (isUpdating) return;
  
  // 1. オフライン検出（完全オフライン保護）
  // 端末がオフラインなら、キャッシュを絶対に削除・解除せず安全に保護する
  if (!navigator.onLine){
    showNotice('オフラインです（現在のキャッシュを維持します）', 3000);
    return;
  }

  isUpdating = true;
  showNotice('最新版を読み込み中…', 0);

  const bust = Date.now().toString(36);

  const files = [
    './index.html',
    './styles.css',
    './state.js',
    './engine.js',
    './toast-core.js',
    './toast-fields.js',
    './card-builders.js',
    './card-logic.js',
    './panels.js',
    './resume-fit.js',
    './sw.js',
    './manifest.json',
    './icon-192.png',
    './icon-512.png',
    './icon-maskable-512.png'
  ];

  try {
    // 2. 全アセットをネットワークからキャッシュ無視で一括取得
    const fetchedResults = await Promise.all(files.map(async f => {
      const res = await fetch(f + '?r=' + bust, { cache: 'no-store', credentials: 'same-origin' });
      if (!res.ok) throw new Error(`${f} (${res.status})`);
      return { path: f, res };
    }));

    // 3. 最新 sw.js の中身から CACHE_NAME を特定
    const swItem = fetchedResults.find(x => x.path === './sw.js');
    let cacheName = 'v' + bust;
    if (swItem){
      const swText = await swItem.res.clone().text();
      const match = swText.match(/CACHE_NAME\s*=\s*['"]([^'"]+)['"]/);
      if (match) cacheName = match[1];
    }

    // 4. 古いSWを一度解除し、新しいCache Storageに直接書き込み
    if ('serviceWorker' in navigator){
      const regs = await navigator.serviceWorker.getRegistrations();
      await Promise.all(regs.map(r => r.unregister()));
    }

    if ('caches' in window){
      const newCache = await caches.open(cacheName);
      for (const item of fetchedResults){
        await newCache.put(item.path, item.res.clone());
        if (item.path === './index.html'){
          await newCache.put('./', item.res.clone());
        }
      }

      // 古いキャッシュをクリア
      const allKeys = await caches.keys();
      await Promise.all(allKeys.filter(k => k !== cacheName).map(k => caches.delete(k)));
    }

    showNotice('更新完了！再起動します…', 1200);

    // 5. 新バージョンで確実に再読み込み＆完全再描画
    setTimeout(()=>{
      location.replace('./index.html?r=' + bust);
    }, 400);

  } catch(err){
    console.warn('Smart update failed:', err);
    isUpdating = false;
    showNotice('更新に失敗しました（現在のバージョンを維持します）', 3500);
  }
}

bindTapDown(document.getElementById('updateBtn'), performSmartUpdate);

// Service worker: プレビュー・iframe環境ではSWを解除してキャッシュ滞留を防止
const isIframe = window.self !== window.top;
if ('serviceWorker' in navigator){
  if (isIframe) {
    navigator.serviceWorker.getRegistrations().then(regs => {
      regs.forEach(r => r.unregister());
    }).catch(()=>{});
  } else {
    window.addEventListener('load', ()=>{
      navigator.serviceWorker.register('./sw.js', { scope: './' }).catch(err => {
        console.warn('ServiceWorker registration bypassed:', err?.message || err);
      });
    });
  }
}
