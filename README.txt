KEBUN MUROJAAH - website murojaah Al-Qur'an (tugas UAS)

CARA PAKAI
1. Ekstrak zip ini.
2. Klik dua kali file index.html, nanti terbuka di browser (Chrome, Edge, Firefox, Safari).
   Tidak perlu install apa pun, tidak perlu internet (kecuali supaya font imutnya tampil, lihat catatan).
3. Untuk menaruh online gratis: upload index.html ke GitHub Pages, Netlify Drop (drag and drop), atau Cloudflare Pages.

LOGIN
- Saat web dibuka, muncul layar Masuk / Daftar akun (username + kata sandi).
- Tombol "Coba sebagai tamu" masuk dengan data contoh, tanpa daftar. Cocok untuk demo ke dosen.
- Tiap akun punya data sendiri (streak, riwayat, checklist, karakter) yang terpisah.
- Di halaman "akun & target": keluar, ganti kata sandi, hapus akun.
- PENTING: akun disimpan di browser perangkat ini (localStorage), bukan di server. Kata sandi disimpan dalam bentuk hash
  (PBKDF2), tapi ini login sederhana untuk tugas, bukan pengamanan sungguhan. Ganti browser atau perangkat = akun tidak ada.
  Kalau mau login sungguhan lintas perangkat, perlu server/database (misalnya Firebase atau Supabase).

ISI FITUR
- Dashboard progres harian dan grafik 7 hari
- Timer murojaah (hitung mundur / stopwatch) + catat sesi manual
- Streak api, checklist (bisa tambah/hapus), target harian dan bulanan
- Juz 1-30 (bunga mekar sesuai tingkat hafalan), bacaan Arab + terjemah Indonesia, mode tes hafalan
- Penanda warna tajwid otomatis (idzhar, ikhfa, idgham, iqlab, mim sukun, ghunnah, qalqalah, mad)
- Kalender aktivitas, statistik, riwayat, lencana
- Pengaturan target, tema terang/gelap, tema warna pink/mint/lavender
- Karakter chibi cewek berhijab dan cowok, bisa ganti baju, penutup kepala, aksesori, dan teman hewan
  (item tertentu terbuka lewat streak)
- Pemutar audio murattal dari file milikmu sendiri

CATATAN
- Tidak memakai AI sama sekali, jadi gratis. Hanya HTML, CSS, dan JavaScript.
- Gunakan tombol "Salin cadangan" di halaman akun untuk memindahkan data antar browser.
- Font (Mali, Fredoka, Scheherazade New) dimuat dari Google Fonts. Tanpa internet tampilan tetap jalan, hanya fontnya diganti font bawaan.
- Teks Al-Qur'an dan terjemah: Kemenag RI, diambil dari proyek quran-json (https://github.com/risan/quran-json).
  Cek lisensi proyek itu kalau website dipublikasikan.
- Penanda tajwid dibuat otomatis dari aturan huruf, jadi bisa meleset pada kasus tertentu. Tetap rujuk guru atau mushaf.
