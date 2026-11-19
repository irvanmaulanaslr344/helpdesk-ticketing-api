package com.company.helpdesk.controller;

import com.company.helpdesk.dto.DashboardDtos;
import com.company.helpdesk.security.CustomUserDetails;
import com.company.helpdesk.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Tag(name = "4. Dashboard", description = "Statistik dan analitik berdasarkan role (Redis + HTTP Cache)")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Dashboard ringkasan tiket milik karyawan")
    @PreAuthorize("hasRole('KARYAWAN')")
    @GetMapping("/karyawan")
    public ResponseEntity<DashboardDtos.KaryawanDashboardResponse> karyawanDashboard(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePrivate())
                .body(dashboardService.getKaryawanDashboard(principal.getId()));
    }

    @Operation(summary = "Dashboard operasional admin/teknisi")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<DashboardDtos.AdminDashboardResponse> adminDashboard() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePrivate())
                .body(dashboardService.getAdminDashboard());
    }

    @Operation(summary = "Dashboard statistik, performa teknisi, dan tiket lama belum selesai (khusus SUPERVISOR)")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/supervisor")
    public ResponseEntity<DashboardDtos.SupervisorDashboardResponse> supervisorDashboard(
            @RequestParam(name = "tanggal_dari", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggalDari,
            @RequestParam(name = "tanggal_sampai", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggalSampai
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePrivate())
                .body(dashboardService.getSupervisorDashboard(tanggalDari, tanggalSampai));
    }
}