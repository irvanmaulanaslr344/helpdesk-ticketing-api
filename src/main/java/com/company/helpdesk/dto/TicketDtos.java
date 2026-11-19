package com.company.helpdesk.dto;

import com.company.helpdesk.model.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class TicketDtos {

    private TicketDtos() {}

    public record CreateTicketRequest(
            @NotBlank(message = "Judul wajib diisi")
            @Size(max = 200, message = "Judul maksimal 200 karakter")
            String judul,

            @NotBlank(message = "Deskripsi wajib diisi")
            String deskripsi,

            @NotNull(message = "Kategori wajib dipilih")
            Long kategoriId
    ) {}

    public record CreateTicketResponse(
            Long id,
            TicketStatus status,
            LocalDateTime tanggalDibuat
    ) {}

    public record UserRef(
            Long id,
            String nama
    ) {}

    public record CategoryRef(
            Long id,
            String nama
    ) {}

    public record TicketListItem(
            Long id,
            String judul,
            TicketStatus status,
            String kategori,
            UserRef dibuatOleh,
            LocalDateTime tanggalDibuat
    ) {}

    public record PageMeta(
            int page,
            int limit,
            long total
    ) {}

    public record TicketListResponse(
            List<TicketListItem> data,
            PageMeta meta
    ) {}

    public record TicketHistoryItem(
            TicketStatus statusLama,
            TicketStatus statusBaru,
            String diubahOleh,
            String catatan,
            LocalDateTime tanggal
    ) {}

    public record TicketDetailResponse(
            Long id,
            String judul,
            String deskripsi,
            TicketStatus status,
            CategoryRef kategori,
            UserRef dibuatOleh,
            UserRef ditugaskanKe,
            LocalDateTime tanggalDibuat,
            LocalDateTime tanggalSelesai,
            List<TicketHistoryItem> history
    ) {}

    public record UpdateStatusRequest(
            @NotNull(message = "Status baru wajib diisi")
            TicketStatus status,

            @Size(max = 2000, message = "Catatan maksimal 2000 karakter")
            String catatan
    ) {}

    public record StatusResponse(
            Long id,
            TicketStatus status
    ) {}

    public record AssignTicketRequest(
            @NotNull(message = "ID teknisi tujuan wajib diisi")
            Long ditugaskanKe
    ) {}

    public record AssignTicketResponse(
            Long id,
            Long ditugaskanKe
    ) {}

    public record UpdateCategoryRequest(
            @NotNull(message = "ID kategori wajib diisi")
            Long kategoriId
    ) {}

    public record UpdateCategoryResponse(
            Long id,
            Long kategoriId
    ) {}

    public record NoteRequest(
            @NotBlank(message = "Catatan wajib diisi")
            @Size(max = 2000, message = "Catatan maksimal 2000 karakter")
            String catatan
    ) {}
}