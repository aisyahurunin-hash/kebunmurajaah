/* Kebun Murojaah: service worker.
   - Menyimpan aset web + daftar halaman mushaf (api.alquran.cloud) supaya app tetap terbuka & bisa murojaah offline.
   - Teks Al-Qur'an dan terjemah sudah ada di dalam index.html, jadi ikut tersimpan.
   - Menangani ketukan notifikasi pengingat.
   Murottal (audio everyayah.com) sengaja TIDAK disimpan karena besar. */
const CACHE='km-v27';
const CORE=['./','./index.html','./manifest.json','./icon-192.png','./icon-512.png','./monetization-config.js','./privacy-policy.html','./offline.html'];
const API_HOSTS=['api.alquran.cloud'];
self.addEventListener('install',e=>{
  self.skipWaiting();
  e.waitUntil(caches.open(CACHE).then(c=>Promise.all(CORE.map(u=>c.add(u).catch(()=>{})))));
});
self.addEventListener('activate',e=>e.waitUntil(
  caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())
));
const offlinePage=()=>caches.match('./offline.html');
self.addEventListener('fetch',e=>{
  const r=e.request;
  if(r.method!=='GET')return;
  const u=new URL(r.url);
  if(API_HOSTS.includes(u.hostname)){
    /* daftar halaman mushaf: pakai simpanan dulu, perbarui di latar */
    e.respondWith(caches.open(CACHE).then(c=>c.match(r).then(m=>{
      const net=fetch(r).then(res=>{if(res&&res.ok)c.put(r,res.clone()).catch(()=>{});return res}).catch(()=>m);
      return m||net;
    })));
    return;
  }
  if(u.origin!==location.origin)return;
  e.respondWith(fetch(r).then(res=>{
    if(res&&res.ok){const cp=res.clone();caches.open(CACHE).then(c=>c.put(r,cp)).catch(()=>{})}
    return res;
  }).catch(()=>caches.match(r).then(m=>m||(r.mode==='navigate'?(caches.match('./index.html').then(x=>x||offlinePage())):undefined)).then(m=>m||new Response('',{status:504}))));
});
self.addEventListener('notificationclick',e=>{
  e.notification.close();
  e.waitUntil(self.clients.matchAll({type:'window',includeUncontrolled:true}).then(list=>{
    for(const c of list){if('focus' in c)return c.focus()}
    return self.clients.openWindow('./');
  }));
});
