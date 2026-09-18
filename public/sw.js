// キャッシュ名。中身の差し替えは update.html（SW解除＋Cache削除＋no-store取得）で行う。
const CACHE_NAME = 'v14';
const STATIC_ASSETS = [
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
      .then((cache) => cache.addAll(STATIC_ASSETS))
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

  // ナビゲーションリクエスト（HTML画面表示）
  if (event.request.mode === 'navigate' || url.pathname.endsWith('/index.html') || url.pathname.endsWith('index.html')) {
    event.respondWith(
      caches.match('./index.html').then((cachedIndex) => {
        if (cachedIndex) return cachedIndex;
        return fetch(event.request).then((networkRes) => {
          if (networkRes && networkRes.status === 200) {
            const resClone = networkRes.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put('./index.html', resClone));
          }
          return networkRes;
        }).catch(() => caches.match('./index.html'));
      })
    );
    return;
  }

  // 静的アセット（JS, CSS, 画像など）：キャッシュにあれば返し、なければネットから取得してキャッシュに保管
  event.respondWith(
    caches.match(event.request).then((cached) => {
      if (cached) return cached;
      return fetch(event.request).then((networkRes) => {
        if (networkRes && networkRes.status === 200 && networkRes.type === 'basic') {
          const resClone = networkRes.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, resClone);
          });
        }
        return networkRes;
      }).catch((err) => {
        console.warn('SW fetch failed:', event.request.url, err);
        return cached;
      });
    })
  );
});
