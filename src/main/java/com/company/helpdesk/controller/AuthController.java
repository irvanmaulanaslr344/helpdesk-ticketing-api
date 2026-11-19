package com.company.helpdesk.controller;

import com.company.helpdesk.dto.AuthDtos;
import com.company.helpdesk.security.CookieUtil;
import com.company.helpdesk.security.CustomUserDetails;
import com.company.helpdesk.security.JwtService;
import com.company.helpdesk.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1. Auth", description = "Autentikasi JWT + Cookie, Profil, Ganti Password, dan 2FA Authenticator")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtService jwtService;

    @Operation(summary = "Register akun karyawan baru")
    @PostMapping("/register")
    public ResponseEntity<AuthDtos.LoginResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        AuthDtos.LoginResponse response = authService.register(request);
        var cookie = cookieUtil.createAuthCookie(response.token(), jwtService.getExpirationSeconds());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @Operation(summary = "Login (JWT Bearer + HttpOnly Cookie + 2FA opsional jika aktif)")
    @PostMapping("/login")
    public ResponseEntity<AuthDtos.LoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthDtos.LoginResponse response = authService.login(request);
        var cookie = cookieUtil.createAuthCookie(response.token(), jwtService.getExpirationSeconds());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @Operation(summary = "Logout dan hapus auth cookie")
    @PostMapping("/logout")
    public ResponseEntity<AuthDtos.MessageResponse> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clearAuthCookie().toString())
                .body(new AuthDtos.MessageResponse("logout berhasil"));
    }

    @Operation(summary = "Melihat profil user yang sedang login")
    @GetMapping("/me")
    public ResponseEntity<AuthDtos.ProfileResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(authService.getProfile(principal.getId()));
    }

    @Operation(summary = "Mengganti password user yang sedang login")
    @PatchMapping("/password")
    public ResponseEntity<AuthDtos.MessageResponse> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AuthDtos.ChangePasswordRequest request
    ) {
        authService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(new AuthDtos.MessageResponse("password diubah"));
    }

    @Operation(summary = "Generate secret & otpauth URI untuk Two Factor Authentication (Authenticator App)")
    @PostMapping("/2fa/setup")
    public ResponseEntity<AuthDtos.TwoFactorSetupResponse> setup2fa(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(authService.setupTwoFactor(principal.getId()));
    }

    @Operation(summary = "Verifikasi kode 6 digit untuk mengaktifkan 2FA")
    @PostMapping("/2fa/verify")
    public ResponseEntity<AuthDtos.MessageResponse> enable2fa(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AuthDtos.TwoFactorCodeRequest request
    ) {
        authService.enableTwoFactor(principal.getId(), request);
        return ResponseEntity.ok(new AuthDtos.MessageResponse("2FA berhasil diaktifkan"));
    }

    @Operation(summary = "Nonaktifkan 2FA dengan konfirmasi password & kode authenticator")
    @PostMapping("/2fa/disable")
    public ResponseEntity<AuthDtos.MessageResponse> disable2fa(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AuthDtos.TwoFactorDisableRequest request
    ) {
        authService.disableTwoFactor(principal.getId(), request);
        return ResponseEntity.ok(new AuthDtos.MessageResponse("2FA berhasil dinonaktifkan"));
    }
}