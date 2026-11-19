package com.company.helpdesk.dto;

import com.company.helpdesk.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class UserDtos {

    private UserDtos() {}

    public record CreateUserRequest(
            @NotBlank(message = "Nama wajib diisi")
            @Size(max = 100)
            String nama,

            @NotBlank(message = "Email wajib diisi")
            @Email(message = "Format email tidak valid")
            @Size(max = 150)
            String email,

            @NotBlank(message = "Password wajib diisi")
            @Size(min = 8, max = 72, message = "Password minimal 8 karakter")
            String password,

            @NotNull(message = "Role wajib diisi")
            Role role
    ) {}

    public record CreateUserResponse(Long id) {}

    public record UpdateUserRequest(
            @Size(max = 100)
            String nama,
            Role role,
            Boolean aktif
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Password baru wajib diisi")
            @Size(min = 6, max = 72, message = "Password baru minimal 6 karakter")
            String passwordBaru
    ) {}

    public record UserItem(
            Long id,
            String nama,
            String email,
            Role role,
            Boolean aktif,
            Boolean totpEnabled,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record UserListResponse(
            List<UserItem> data,
            TicketDtos.PageMeta meta
    ) {}
}