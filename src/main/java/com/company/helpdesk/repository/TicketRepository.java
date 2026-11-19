package com.company.helpdesk.repository;

import com.company.helpdesk.model.Ticket;
import com.company.helpdesk.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @EntityGraph(attributePaths = {"kategori", "dibuatOleh"})
    @Override
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"kategori", "dibuatOleh", "ditugaskanKe"})
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findDetailById(@Param("id") Long id);

    // Dashboard Karyawan
    long countByDibuatOlehId(Long userId);

    long countByDibuatOlehIdAndStatusIn(Long userId, Collection<TicketStatus> statuses);

    // Dashboard Admin
    long countByTanggalDibuatGreaterThanEqualAndTanggalDibuatLessThan(LocalDateTime start, LocalDateTime end);

    long countByStatusIn(Collection<TicketStatus> statuses);

    @Query("""
            SELECT c.namaKategori AS kategori, COUNT(t.id) AS jumlah
            FROM Ticket t
            JOIN t.kategori c
            GROUP BY c.namaKategori
            ORDER BY c.namaKategori ASC
            """)
    List<CategoryCountProjection> countTicketsByCategory();

    // Dashboard Supervisor (rentang tanggal opsional)
    @Query("""
            SELECT COUNT(t.id)
            FROM Ticket t
            WHERE (:fromDate IS NULL OR t.tanggalDibuat >= :fromDate)
              AND (:toDate IS NULL OR t.tanggalDibuat <= :toDate)
            """)
    long countAllInDateRange(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    @Query("""
            SELECT t.status AS status, COUNT(t.id) AS jumlah
            FROM Ticket t
            WHERE (:fromDate IS NULL OR t.tanggalDibuat >= :fromDate)
              AND (:toDate IS NULL OR t.tanggalDibuat <= :toDate)
            GROUP BY t.status
            """)
    List<StatusCountProjection> countByStatusInDateRange(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
            SELECT c.namaKategori AS kategori, COUNT(t.id) AS jumlah
            FROM Ticket t
            JOIN t.kategori c
            WHERE (:fromDate IS NULL OR t.tanggalDibuat >= :fromDate)
              AND (:toDate IS NULL OR t.tanggalDibuat <= :toDate)
            GROUP BY c.namaKategori
            ORDER BY c.namaKategori ASC
            """)
    List<CategoryCountProjection> countByCategoryInDateRange(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
            SELECT
                u.nama AS nama,
                COUNT(t.id) AS totalSelesai,
                ROUND(AVG(TIMESTAMPDIFF(SECOND, t.tanggal_dibuat, t.tanggal_selesai)) / 86400.0, 2) AS rataRataHari
            FROM tickets t
            INNER JOIN users u ON u.id = t.ditugaskan_ke
            WHERE t.status IN ('RESOLVED', 'CLOSED')
              AND t.tanggal_selesai IS NOT NULL
              AND (:fromDate IS NULL OR t.tanggal_dibuat >= :fromDate)
              AND (:toDate IS NULL OR t.tanggal_dibuat <= :toDate)
            GROUP BY u.id, u.nama
            ORDER BY totalSelesai DESC, rataRataHari ASC
            """, nativeQuery = true)
    List<TechnicianPerformanceProjection> findTechnicianPerformance(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
            SELECT t
            FROM Ticket t
            WHERE t.status IN :unresolvedStatuses
              AND (:fromDate IS NULL OR t.tanggalDibuat >= :fromDate)
              AND (:toDate IS NULL OR t.tanggalDibuat <= :toDate)
            ORDER BY t.tanggalDibuat ASC
            """)
    List<Ticket> findOldestUnresolvedTickets(
            @Param("unresolvedStatuses") Collection<TicketStatus> unresolvedStatuses,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    interface CategoryCountProjection {
        String getKategori();
        long getJumlah();
    }

    interface StatusCountProjection {
        TicketStatus getStatus();
        long getJumlah();
    }

    interface TechnicianPerformanceProjection {
        String getNama();
        long getTotalSelesai();
        Double getRataRataHari();
    }
}