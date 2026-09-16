// キャッシュ名固定。中身の差し替えは update.html（SW解除＋Cache削除＋no-store取得）で行う。
const CACHE_NAME = 'freetimer-cache-v8';
const ASSETS = [
  './',
  './index.html',
  './manifest.json',
  './icon-192.png',
  './icon-512.png',
  './icon-maskable-512.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(ASSETS))
      .then(() => self.skipWaiting())
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
  if (event.request.method !== 'GET') return;
  const url = new URL(event.request.url);
  // 更新ページは常にネット（古い update.html をキャッシュから出さない）
  if (url.pathname.endsWith('/update.html') || url.pathname.endsWith('update.html')) {
    event.respondWith(fetch(event.request, { cache: 'no-store' }));
    return;
  }
  // 通常はキャッシュ優先。TWA起動時の不要なネットワーク待ち・通信を避ける。
  event.respondWith(
    caches.match(event.request).then((cached) => {
      if (cached) return cached;
      // クエリ付き index.html / TWAナビゲーションは本体キャッシュへフォールバック。
      if (url.pathname.endsWith('/index.html') || url.pathname.endsWith('index.html') || event.request.mode === 'navigate') {
        return caches.match('./index.html').then((c) => c || fetch(event.request)).catch(() => caches.match('./index.html'));
      }
      return fetch(event.request).catch(() => cached);
    })
  );
});
