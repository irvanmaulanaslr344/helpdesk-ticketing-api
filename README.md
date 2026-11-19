# Helpdesk / Ticketing System API

Sistem pelaporan dan manajemen tiket kerusakan fasilitas kantor berbasis **Monolith SOA** menggunakan **Java 25 LTS** dan **Spring Boot 4.1.x**. Dirancang dengan standar keamanan OWASP, transaksi ACID, caching berlapis, dan orkestrasi container penuh menggunakan Docker serta Nginx Reverse Proxy (HTTPS).

---

## Fitur Utama

- **Role-Based Access Control (RBAC):** Pemisahan hak akses ketat untuk 3 peran (`KARYAWAN`, `ADMIN`, `SUPERVISOR`) serta isolasi kepemilikan data (anti-IDOR).
- **Ticket State Machine:** Pengelolaan siklus hidup tiket (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `REOPENED`, `REJECTED`) dengan validasi transisi status otomatis.
- **Audit Trail Otomatis:** Pencatatan riwayat setiap perubahan status, kategori, dan penugasan teknisi ke dalam tabel `ticket_histories`.
- **Two-Factor Authentication (2FA):** Mendukung autentikasi dua faktor berbasis TOTP (RFC 6238) yang kompatibel dengan Google Authenticator dan Authy.
- **Dashboard Analitik:** Statistik ringkasan tiket per role serta metrik performa teknisi dan rata-rata waktu penyelesaian untuk Supervisor.
- **Notifikasi Email Asinkron:** Pengiriman notifikasi otomatis berbasis Virtual Threads saat tiket dibuat, ditugaskan ke teknisi, atau diperbarui.
- **Pencarian & Filter Lanjutan:** Mendukung pencarian kata kunci (`FULLTEXT`), filter status, kategori, rentang tanggal, pagination, dan sorting.

---

## Stack Teknologi

| Komponen | Teknologi |
|---|---|
| **Bahasa & Framework** | Java 25 LTS, Spring Boot 4.1.x, Spring Security, Spring Data JPA |
| **Database & Migrasi** | MySQL 8.4.13, Flyway Migration (Normalisasi BCNF + Database Indexing) |
| **Caching** | Redis 7.4 (Application Cache) & HTTP Caching (`ETag` + `Cache-Control`) |
| **Keamanan** | JWT Bearer + `HttpOnly` Cookie (`SameSite=Strict`), BCrypt, TOTP 2FA, CORS, CSP, OWASP Headers |
| **Infrastruktur & Proxy** | Docker, Docker Compose, Nginx 1.27 (HTTPS TLS 1.2/1.3 & Rate Limiting) |
| **Dokumentasi API** | Swagger UI / OpenAPI 3 (`/swagger-ui.html`) |

---

## Struktur Proyek

```text
helpdesk-ticketing/
├── nginx/
│   ├── certs/                 # Sertifikat SSL/TLS lokal (di-ignore Git)
│   └── nginx.conf             # Konfigurasi Reverse Proxy, HTTPS & Rate Limiting
├── src/
│   └── main/
│       ├── java/              # Layer Controller, Service, Repository, Model, Security, DTO
│       └── resources/
│           ├── db/migration/  # Skrip migrasi Flyway (V1 schema & V2 seeding)
│           └── application.yml
├── .env.example               # Template variabel environment
├── .gitignore
├── docker-compose.yml
├── Dockerfile                 # Multi-stage build dengan non-root container user
├── pom.xml
└── README.md

```

---

## Prasyarat

- **Docker** dan **Docker Compose** terpasang di sistem.
- Port `80` dan `443` tidak sedang digunakan oleh web server lain.

---

## Cara Menjalankan via Docker

1. **Salin file konfigurasi environment:**
   ```bash
   cp .env.example .env
   ```
   *(Sesuaikan nilai kredensial di dalam `.env` jika diperlukan).*

2. **Buat sertifikat SSL lokal untuk Nginx (`nginx/certs/`):**
   - **Windows (PowerShell):**
     ```powershell
     mkdir -Force nginx/certs
     docker run --rm -v "${PWD}/nginx/certs:/certs" alpine/openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout /certs/privkey.pem -out /certs/fullchain.pem -subj "/CN=localhost"
     ```
   - **Linux / macOS / Git Bash:**
     ```bash
     mkdir -p nginx/certs
     docker run --rm -v "$(pwd)/nginx/certs:/certs" alpine/openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout /certs/privkey.pem -out /certs/fullchain.pem -subj "/CN=localhost"
     ```

3. **Build dan jalankan seluruh container (`MySQL`, `Redis`, `Spring Boot`, `Nginx`):**
   ```bash
   docker compose up -d --build
   ```

4. **Verifikasi status container:**
   ```bash
   docker compose ps
   ```
   Pastikan `helpdesk-app`, `helpdesk-mysql`, `helpdesk-redis`, dan `helpdesk-nginx` berstatus `Up`.

5. **Akses Dokumentasi & Pengujian API (Swagger UI):**
   Buka `https://localhost/swagger-ui.html` di browser *(lewati peringatan sertifikat self-signed untuk pengujian lokal)*.

---

## Akun Default (Seeding Otomatis)

Saat container dijalankan pertama kali, Flyway otomatis membuat akun bawaan berikut:

| Role | Email | Password |
|---|---|---|
| `ADMIN` | `admin@company.com` | `Admin123!` |
| `SUPERVISOR` | `supervisor@company.com` | `Supervisor123!` |
| `KARYAWAN` | `karyawan@company.com` | `Karyawan123!` |

---

## Daftar Endpoint Utama (`/api/v1`)

### Autentikasi & 2FA
- `POST /api/v1/auth/register` — Registrasi akun karyawan baru
- `POST /api/v1/auth/login` — Login (mengembalikan JWT Bearer & mengatur cookie `HttpOnly`)
- `POST /api/v1/auth/logout` — Logout dan menghapus cookie sesi
- `GET /api/v1/auth/me` — Mengambil profil pengguna yang sedang login
- `PATCH /api/v1/auth/password` — Mengubah kata sandi akun
- `POST /api/v1/auth/2fa/setup` — Membuat secret & URI QR code untuk 2FA Authenticator
- `POST /api/v1/auth/2fa/verify` — Verifikasi kode OTP 6-digit dan mengaktifkan 2FA
- `POST /api/v1/auth/2fa/disable` — Menonaktifkan 2FA

### Manajemen Tiket
- `POST /api/v1/tickets` — Membuat laporan tiket baru (`KARYAWAN`)
- `GET /api/v1/tickets` — Menampilkan daftar tiket dengan filter, search, & pagination
- `GET /api/v1/tickets/{id}` — Menampilkan detail tiket beserta timeline `ticket_histories`
- `PATCH /api/v1/tickets/{id}/status` — Memperbarui status tiket dan catatan solusi (`ADMIN`)
- `PATCH /api/v1/tickets/{id}/assign` — Menugaskan teknisi ke tiket (`ADMIN`)
- `PATCH /api/v1/tickets/{id}/kategori` — Mengubah kategori tiket (`ADMIN`)
- `PATCH /api/v1/tickets/{id}/reject` — Menolak tiket dengan alasan (`ADMIN`)
- `PATCH /api/v1/tickets/{id}/confirm` — Konfirmasi penyelesaian tiket (`KARYAWAN`)
- `PATCH /api/v1/tickets/{id}/reopen` — Membuka kembali tiket yang ditolak/selesai (`KARYAWAN`)

### Kategori, Dashboard & Manajemen Pengguna
- `GET /api/v1/categories` — Daftar kategori tiket (`Software` & `Hardware`)
- `GET /api/v1/dashboard/karyawan` — Statistik tiket milik karyawan
- `GET /api/v1/dashboard/admin` — Statistik operasional tiket untuk admin
- `GET /api/v1/dashboard/supervisor` — Statistik ringkasan & performa teknisi untuk supervisor
- `GET /api/v1/users` — Daftar seluruh pengguna (`ADMIN`)
- `POST /api/v1/users` — Menambah pengguna baru (`ADMIN`)
- `PATCH /api/v1/users/{id}` — Memperbarui role atau status aktif pengguna (`ADMIN`)
- `POST /api/v1/users/{id}/reset-password` — Reset kata sandi pengguna (`ADMIN`)

---

