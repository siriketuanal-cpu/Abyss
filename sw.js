/* 深淵タイマー Service Worker */
/* デプロイのたびに CACHE_NAME を上げると、古いキャッシュを捨てて新版へ切り替わる */
const CACHE_NAME = 'dotabyss-timer-v5';
const PRECACHE = [
  './',
  './index.html',
  './manifest.json',
  './icon-192.png',
  './icon-512.png',
  './icon-maskable-512.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(async (cache) => {
      for (const u of PRECACHE) {
        try {
          await cache.add(new Request(u, { cache: 'reload' }));
        } catch (e) { /* 任意ファイル欠落は無視 */ }
      }
    }).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;

  const url = new URL(req.url);
  // 同一オリジンのリソースのみを対象にする
  if (url.origin !== self.location.origin) return;

  // 全リソース（HTML含む）：Stale-While-Revalidate 戦略
  // キャッシュがあれば即表示し、裏で最新版を取得・更新する
  event.respondWith(
    caches.match(req).then((cached) => {
      const fetching = fetch(req).then((res) => {
        if (res && res.ok) {
          const copy = res.clone();
          caches.open(CACHE_NAME).then((c) => c.put(req, copy));
        }
        return res;
      }).catch(() => {
        // オフライン等でネットワークエラーの場合、キャッシュを返す
        return cached;
      });

      // キャッシュがあれば即返却、無ければネットワーク取得を待つ
      // （※画面遷移時のフォールバックとして ./index.html も用意）
      return cached || fetching.catch(() => caches.match('./index.html'));
    })
  );
});
