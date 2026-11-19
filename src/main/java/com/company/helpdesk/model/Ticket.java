package com.company.helpdesk.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "judul", nullable = false, length = 200)
    private String judul;

    @Column(name = "deskripsi", nullable = false, columnDefinition = "TEXT")
    private String deskripsi;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            columnDefinition = "ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED','REOPENED','REJECTED')"
    )
    private TicketStatus status = TicketStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kategori_id", nullable = false)
    private Category kategori;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dibuat_oleh", nullable = false)
    private User dibuatOleh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ditugaskan_ke")
    private User ditugaskanKe;

    @Column(name = "tanggal_dibuat", nullable = false, updatable = false)
    private LocalDateTime tanggalDibuat;

    @Column(name = "tanggal_selesai")
    private LocalDateTime tanggalSelesai;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = TicketStatus.OPEN;
        }
        if (this.tanggalDibuat == null) {
            this.tanggalDibuat = LocalDateTime.now();
        }
    }
}