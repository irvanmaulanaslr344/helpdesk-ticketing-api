package com.company.helpdesk.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status_lama",
            columnDefinition = "ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED','REOPENED','REJECTED')"
    )
    private TicketStatus statusLama;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status_baru",
            nullable = false,
            columnDefinition = "ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED','REOPENED','REJECTED')"
    )
    private TicketStatus statusBaru;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diubah_oleh", nullable = false)
    private User diubahOleh;

    @Column(name = "catatan", columnDefinition = "TEXT")
    private String catatan;

    @Column(name = "tanggal", nullable = false, updatable = false)
    private LocalDateTime tanggal;

    @PrePersist
    protected void onCreate() {
        if (this.tanggal == null) {
            this.tanggal = LocalDateTime.now();
        }
    }
}