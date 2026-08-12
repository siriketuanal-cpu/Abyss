/* 深淵タイマー Service Worker — PWA専用 */
/* デプロイ時は CACHE_NAME を上げる（v9, v10…） */
const CACHE_NAME = 'dotabyss-timer-v9';
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
        } catch (e) { /* 欠落ファイルはスキップ */ }
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

// 同一オリジン: キャッシュ即表示 + 裏で更新（2回目以降の起動を速く）
self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  event.respondWith(
    caches.match(req).then((cached) => {
      const fetching = fetch(req).then((res) => {
        if (res && res.ok) {
          const copy = res.clone();
          caches.open(CACHE_NAME).then((c) => c.put(req, copy));
        }
        return res;
      }).catch(() => cached);
      return cached || fetching.catch(() => caches.match('./index.html'));
    })
  );
});
