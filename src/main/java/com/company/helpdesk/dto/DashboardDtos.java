package com.company.helpdesk.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {

    private DashboardDtos() {}

    public record KaryawanDashboardResponse(
            long totalTiket,
            long open,
            long selesai
    ) {}

    public record CategoryCountItem(
            String kategori,
            long jumlah
    ) {}

    public record AdminDashboardResponse(
            long masukHariIni,
            long belumSelesai,
            List<CategoryCountItem> grafikKategori
    ) {}

    public record TechnicianPerformanceItem(
            String nama,
            long totalSelesai,
            double rataRataHari
    ) {}

    public record OldUnresolvedTicketItem(
            Long id,
            String judul,
            LocalDateTime tanggalDibuat
    ) {}

    public record SupervisorDashboardResponse(
            long totalTiket,
            Map<String, Long> perStatus,
            Map<String, Long> perKategori,
            List<TechnicianPerformanceItem> performaTeknisi,
            List<OldUnresolvedTicketItem> tiketLamaBelumSelesai
    ) {}
}