# ⚡ Cidz Bridge (Android APK)

Aplikasi forwarder notifikasi transaksi DANA Bisnis, SeaBank, BCA ke Payment Gateway **pg.cidz.ai.id** tanpa perlu MacroDroid (Nol iklan, jalan 24/7 di background).

## ✨ Fitur & Kelebihan
- **Ukuran super enteng**: ~1 MB (Java native, zero third-party bloatware).
- **RAM super irit**: ~15 MB saat idle di background.
- **Auto-Start on Boot**: Tetap aktif otomatis setelah HP restart.
- **Heartbeat & Fail-safe**: Melaporkan status hidup perangkat ke admin panel PG.
- **Configurable**: URL webhook dan X-Forwarder-Key bisa diatur langsung di aplikasi.

## 🚀 Cara Kompilasi / Download APK
Aplikasi ini sudah dilengkapi dengan **GitHub Actions**.

1. Buat repository baru di GitHub akun lu (misal: `cidz-bridge`).
2. Push seluruh isi folder ini ke repo tersebut:
   ```bash
   cd /home/container/home/cidz/service/cidz-bridge-apk
   git init
   git add .
   git commit -m "feat: initial cidz bridge lightweight apk"
   git branch -M main
   git remote add origin https://github.com/USERNAME/cidz-bridge.git
   git push -u origin main
   ```
3. Buka tab **Actions** di GitHub repository lu.
4. Workflow `Build Cidz Bridge APK` akan otomatis berjalan (~2 menit).
5. Klik build yang selesai, lalu download file artifact **`CidzBridge-debug`** (berisi file `.apk`).
6. Kirim dan pasang di HP Android lu.

## 📱 Panduan Penggunaan di HP
1. Buka aplikasi **Cidz Bridge**.
2. Klik tombol **Aktifkan Akses Notifikasi** dan izinkan *Cidz Bridge*.
3. Klik tombol **Matikan Optimasi Baterai** agar Android tidak mematikan service ini saat layar mati.
4. Masukkan **Forwarder Secret Key** (sesuai yang diatur di admin dashboard `pg.cidz.ai.id`).
5. Klik **Kirim Test Notifikasi ke PG** untuk memastikan koneksi sukses.
