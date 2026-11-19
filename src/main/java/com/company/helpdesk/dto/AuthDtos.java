package com.company.helpdesk.dto;

import com.company.helpdesk.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank(message = "Nama wajib diisi")
            @Size(max = 100)
            String nama,

            @NotBlank(message = "Email wajib diisi")
            @Email(message = "Format email tidak valid")
            @Size(max = 150)
            String email,

            @NotBlank(message = "Password wajib diisi")
            @Size(min = 8, max = 72, message = "Password minimal 8 karakter")
            String password
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email wajib diisi")
            @Email(message = "Format email tidak valid")
            String email,

            @NotBlank(message = "Password wajib diisi")
            String password,

            @Pattern(regexp = "^$|^[0-9]{6}$", message = "Kode authenticator harus 6 digit angka")
            String totpCode
    ) {}

    public record AuthUserSummary(
            Long id,
            String nama,
            Role role
    ) {}

    public record LoginResponse(
            String token,
            AuthUserSummary user
    ) {}

    public record ProfileResponse(
            Long id,
            String nama,
            String email,
            Role role,
            boolean totpEnabled
    ) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "Password lama wajib diisi")
            String passwordLama,

            @NotBlank(message = "Password baru wajib diisi")
            @Size(min = 8, max = 72, message = "Password baru minimal 8 karakter")
            String passwordBaru
    ) {}

    public record TwoFactorSetupResponse(
            String secret,
            String otpauthUri
    ) {}

    public record TwoFactorCodeRequest(
            @NotBlank(message = "Kode authenticator wajib diisi")
            @Pattern(regexp = "^[0-9]{6}$", message = "Kode authenticator harus 6 digit angka")
            String code
    ) {}

    public record TwoFactorDisableRequest(
            @NotBlank(message = "Password wajib diisi")
            String password,

            @NotBlank(message = "Kode authenticator wajib diisi")
            @Pattern(regexp = "^[0-9]{6}$", message = "Kode authenticator harus 6 digit angka")
            String code
    ) {}

    public record MessageResponse(String message) {}
}