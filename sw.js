/* 深淵タイマー Service Worker — 起動性能最適化版 */
/* 公開時にindex.htmlまたは静的ファイルを更新したら CACHE_NAME を上げる。 */
const CACHE_PREFIX = 'dotabyss-timer-';
const CACHE_NAME = 'dotabyss-timer-v10';

// 起動に必須の最小アプリシェル。ここは失敗するとPWAを有効化しない。
const CORE_ASSETS = [
  './index.html',
  './manifest.json'
];

// アイコンはインストール体験に必要だが、欠落してもアプリ起動は妨げない。
const OPTIONAL_ASSETS = [
  './icon-192.png',
  './icon-512.png',
  './icon-maskable-512.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE_NAME);
    await cache.addAll(CORE_ASSETS);
    await Promise.allSettled(
      OPTIONAL_ASSETS.map((url) => cache.add(url))
    );
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(
      keys
        .filter((key) => key.startsWith(CACHE_PREFIX) && key !== CACHE_NAME)
        .map((key) => caches.delete(key))
    );
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;

  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  // 起動時のHTMLはネットワークを待たず、常にキャッシュ済みアプリシェルを返す。
  if (req.mode === 'navigate') {
    event.respondWith((async () => {
      const cachedAppShell = await caches.match('./index.html');
      if (cachedAppShell) return cachedAppShell;

      try {
        const response = await fetch(req);
        if (response && response.ok) {
          const cache = await caches.open(CACHE_NAME);
          cache.put('./index.html', response.clone());
        }
        return response;
      } catch (error) {
        return new Response('オフラインで起動できません。オンラインで一度開いてから再試行してください。', {
          status: 503,
          headers: { 'Content-Type': 'text/plain; charset=utf-8' }
        });
      }
    })());
    return;
  }

  // 静的ファイルもキャッシュ優先。初回だけネットワークから取得して以後の起動を軽量化する。
  event.respondWith((async () => {
    const cached = await caches.match(req);
    if (cached) return cached;

    try {
      const response = await fetch(req);
      if (response && response.ok) {
        const cache = await caches.open(CACHE_NAME);
        cache.put(req, response.clone());
      }
      return response;
    } catch (error) {
      return new Response('', { status: 503, statusText: 'Offline' });
    }
  })());
});
