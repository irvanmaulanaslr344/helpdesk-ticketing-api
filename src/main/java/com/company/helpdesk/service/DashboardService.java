package com.company.helpdesk.service;

import com.company.helpdesk.config.RedisCacheConfig;
import com.company.helpdesk.dto.DashboardDtos;
import com.company.helpdesk.model.TicketStatus;
import com.company.helpdesk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<TicketStatus> UNRESOLVED_STATUSES = List.of(
            TicketStatus.OPEN,
            TicketStatus.IN_PROGRESS,
            TicketStatus.REOPENED
    );
    private static final List<TicketStatus> COMPLETED_STATUSES = List.of(
            TicketStatus.RESOLVED,
            TicketStatus.CLOSED
    );

    private final TicketRepository ticketRepository;

    @Cacheable(value = RedisCacheConfig.CACHE_DASHBOARD_KARYAWAN, key = "#karyawanId")
    @Transactional(readOnly = true)
    public DashboardDtos.KaryawanDashboardResponse getKaryawanDashboard(Long karyawanId) {
        long total = ticketRepository.countByDibuatOlehId(karyawanId);
        long open = ticketRepository.countByDibuatOlehIdAndStatusIn(karyawanId, UNRESOLVED_STATUSES);
        long selesai = ticketRepository.countByDibuatOlehIdAndStatusIn(karyawanId, COMPLETED_STATUSES);
        return new DashboardDtos.KaryawanDashboardResponse(total, open, selesai);
    }

    @Cacheable(value = RedisCacheConfig.CACHE_DASHBOARD_ADMIN, key = "'summary'")
    @Transactional(readOnly = true)
    public DashboardDtos.AdminDashboardResponse getAdminDashboard() {
        LocalDate today = LocalDate.now();
        long masukHariIni = ticketRepository.countByTanggalDibuatGreaterThanEqualAndTanggalDibuatLessThan(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
        long belumSelesai = ticketRepository.countByStatusIn(List.of(
                TicketStatus.OPEN,
                TicketStatus.IN_PROGRESS,
                TicketStatus.REOPENED
        ));
        List<DashboardDtos.CategoryCountItem> grafikKategori = ticketRepository.countTicketsByCategory()
                .stream()
                .map(p -> new DashboardDtos.CategoryCountItem(p.getKategori(), p.getJumlah()))
                .toList();

        return new DashboardDtos.AdminDashboardResponse(masukHariIni, belumSelesai, grafikKategori);
    }

    @Cacheable(
            value = RedisCacheConfig.CACHE_DASHBOARD_SUPERVISOR,
            key = "(#tanggalDari != null ? #tanggalDari.toString() : 'all') + '_' + (#tanggalSampai != null ? #tanggalSampai.toString() : 'all')"
    )
    @Transactional(readOnly = true)
    public DashboardDtos.SupervisorDashboardResponse getSupervisorDashboard(LocalDate tanggalDari, LocalDate tanggalSampai) {
        LocalDateTime fromDate = tanggalDari != null ? tanggalDari.atStartOfDay() : null;
        LocalDateTime toDate = tanggalSampai != null ? tanggalSampai.atTime(LocalTime.MAX) : null;

        long totalTiket = ticketRepository.countAllInDateRange(fromDate, toDate);

        Map<String, Long> perStatus = new LinkedHashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            perStatus.put(status.name(), 0L);
        }
        ticketRepository.countByStatusInDateRange(fromDate, toDate)
                .forEach(p -> perStatus.put(p.getStatus().name(), p.getJumlah()));

        Map<String, Long> perKategori = new LinkedHashMap<>();
        ticketRepository.countByCategoryInDateRange(fromDate, toDate)
                .forEach(p -> perKategori.put(p.getKategori(), p.getJumlah()));

        List<DashboardDtos.TechnicianPerformanceItem> performaTeknisi = ticketRepository
                .findTechnicianPerformance(fromDate, toDate)
                .stream()
                .map(p -> new DashboardDtos.TechnicianPerformanceItem(
                        p.getNama(),
                        p.getTotalSelesai(),
                        p.getRataRataHari() == null ? 0.0 : p.getRataRataHari()
                ))
                .toList();

        List<DashboardDtos.OldUnresolvedTicketItem> tiketLama = ticketRepository
                .findOldestUnresolvedTickets(UNRESOLVED_STATUSES, fromDate, toDate, PageRequest.of(0, 10))
                .stream()
                .map(t -> new DashboardDtos.OldUnresolvedTicketItem(
                        t.getId(),
                        t.getJudul(),
                        t.getTanggalDibuat()
                ))
                .toList();

        return new DashboardDtos.SupervisorDashboardResponse(
                totalTiket,
                perStatus,
                perKategori,
                performaTeknisi,
                tiketLama
        );
    }
}