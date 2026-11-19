package com.company.helpdesk.controller;

import com.company.helpdesk.dto.TicketDtos;
import com.company.helpdesk.model.TicketStatus;
import com.company.helpdesk.security.CustomUserDetails;
import com.company.helpdesk.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "3. Tickets", description = "Manajemen tiket, state machine, assign, reject, confirm, dan reopen")
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @Operation(summary = "Membuat tiket baru (khusus KARYAWAN)")
    @PreAuthorize("hasRole('KARYAWAN')")
    @PostMapping
    public ResponseEntity<TicketDtos.CreateTicketResponse> createTicket(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody TicketDtos.CreateTicketRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.createTicket(request, principal.getId()));
    }

    @Operation(summary = "Daftar tiket dengan filter, search, pagination, dan sorting")
    @PreAuthorize("hasAnyRole('KARYAWAN', 'ADMIN', 'SUPERVISOR')")
    @GetMapping
    public ResponseEntity<TicketDtos.TicketListResponse> listTickets(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "tanggal_dibuat") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(name = "kategori_id", required = false) Long kategoriId,
            @RequestParam(name = "tanggal_dari", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggalDari,
            @RequestParam(name = "tanggal_sampai", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggalSampai,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(ticketService.listTickets(
                principal, page, limit, sort, order, status, kategoriId, tanggalDari, tanggalSampai, search
        ));
    }

    @Operation(summary = "Melihat detail tiket beserta timeline riwayat perubahan")
    @PreAuthorize("hasAnyRole('KARYAWAN', 'ADMIN', 'SUPERVISOR')")
    @GetMapping("/{id}")
    public ResponseEntity<TicketDtos.TicketDetailResponse> getTicketDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(ticketService.getTicketDetail(id, principal));
    }

    @Operation(summary = "Update status tiket dan tulis solusi/catatan (khusus ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketDtos.StatusResponse> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody TicketDtos.UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(ticketService.updateStatus(id, request, principal.getId()));
    }

    @Operation(summary = "Assign tiket ke teknisi/admin secara manual (khusus ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/assign")
    public ResponseEntity<TicketDtos.AssignTicketResponse> assignTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody TicketDtos.AssignTicketRequest request
    ) {
        return ResponseEntity.ok(ticketService.assignTicket(id, request, principal.getId()));
    }

    @Operation(summary = "Memperbaiki kategori tiket saat verifikasi (khusus ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/kategori")
    public ResponseEntity<TicketDtos.UpdateCategoryResponse> updateCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody TicketDtos.UpdateCategoryRequest request
    ) {
        return ResponseEntity.ok(ticketService.updateCategory(id, request, principal.getId()));
    }

    @Operation(summary = "Menolak tiket duplikat dengan catatan alasan (khusus ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<TicketDtos.StatusResponse> rejectTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody TicketDtos.NoteRequest request
    ) {
        return ResponseEntity.ok(ticketService.rejectTicket(id, request, principal.getId()));
    }

    @Operation(summary = "Konfirmasi penyelesaian tiket RESOLVED -> CLOSED (khusus pemilik KARYAWAN)")
    @PreAuthorize("hasRole('KARYAWAN')")
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<TicketDtos.StatusResponse> confirmTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(ticketService.confirmTicket(id, principal.getId()));
    }

    @Operation(summary = "Membuka kembali tiket jika belum puas (khusus pemilik KARYAWAN)")
    @PreAuthorize("hasRole('KARYAWAN')")
    @PatchMapping("/{id}/reopen")
    public ResponseEntity<TicketDtos.StatusResponse> reopenTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody TicketDtos.NoteRequest request
    ) {
        return ResponseEntity.ok(ticketService.reopenTicket(id, request, principal.getId()));
    }
}