/* 深淵タイマー Service Worker — 同一オリジンは Cache First */
const CACHE_NAME = 'dotabyss-timer-v2';
const PRECACHE = [
  './',
  './index.html',
  './manifest.json',
  './icon-192.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(async (cache) => {
      // 1ファイル欠けてもインストール全体を落とさない
      for (const u of PRECACHE) {
        try {
          await cache.add(new Request(u, { cache: 'reload' }));
        } catch (e) { /* icon 未配置など */ }
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

  // 同一オリジン: キャッシュ優先 → なければネット → ネット成功時はキャッシュ更新
  if (url.origin === self.location.origin) {
    event.respondWith(
      caches.match(req).then((cached) => {
        if (cached) {
          // 裏で最新取得（stale-while-revalidate 風）
          event.waitUntil(
            fetch(req).then((res) => {
              if (res && res.ok) {
                const copy = res.clone();
                caches.open(CACHE_NAME).then((c) => c.put(req, copy));
              }
            }).catch(() => {})
          );
          return cached;
        }
        return fetch(req).then((res) => {
          if (res && res.ok) {
            const copy = res.clone();
            caches.open(CACHE_NAME).then((c) => c.put(req, copy));
          }
          return res;
        });
      })
    );
    return;
  }

  // Google Fonts 等: ブラウザ／ネットに任せる（CORS・更新の都合で SW に無理に積まない）
});
