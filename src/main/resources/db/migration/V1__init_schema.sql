CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nama VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('KARYAWAN', 'ADMIN', 'SUPERVISOR') NOT NULL,
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    totp_secret VARCHAR(64) NULL,
    totp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nama_kategori VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_categories_nama UNIQUE (nama_kategori)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tickets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    judul VARCHAR(200) NOT NULL,
    deskripsi TEXT NOT NULL,
    status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED', 'REJECTED') NOT NULL DEFAULT 'OPEN',
    kategori_id BIGINT NOT NULL,
    dibuat_oleh BIGINT NOT NULL,
    ditugaskan_ke BIGINT NULL,
    tanggal_dibuat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tanggal_selesai DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tickets_kategori FOREIGN KEY (kategori_id) REFERENCES categories(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_tickets_dibuat_oleh FOREIGN KEY (dibuat_oleh) REFERENCES users(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_tickets_ditugaskan_ke FOREIGN KEY (ditugaskan_ke) REFERENCES users(id) ON UPDATE RESTRICT ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ticket_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    status_lama ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED', 'REJECTED') NULL,
    status_baru ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED', 'REJECTED') NOT NULL,
    diubah_oleh BIGINT NOT NULL,
    catatan TEXT NULL,
    tanggal DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_histories_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_histories_diubah_oleh FOREIGN KEY (diubah_oleh) REFERENCES users(id) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Database Indexing untuk performa 1000+ tiket (filter, sorting, dashboard, pencarian, FK)
CREATE INDEX idx_users_role_aktif ON users (role, aktif);
CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_kategori_id ON tickets (kategori_id);
CREATE INDEX idx_tickets_dibuat_oleh ON tickets (dibuat_oleh);
CREATE INDEX idx_tickets_ditugaskan_ke ON tickets (ditugaskan_ke);
CREATE INDEX idx_tickets_tanggal_dibuat ON tickets (tanggal_dibuat);
CREATE INDEX idx_tickets_status_tanggal ON tickets (status, tanggal_dibuat);
CREATE INDEX idx_tickets_perf_teknisi ON tickets (ditugaskan_ke, status, tanggal_selesai);
CREATE INDEX idx_histories_ticket_tanggal ON ticket_histories (ticket_id, tanggal);
CREATE FULLTEXT INDEX ft_tickets_judul_deskripsi ON tickets (judul, deskripsi);