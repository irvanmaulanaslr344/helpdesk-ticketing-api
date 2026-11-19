package com.company.helpdesk.service;

import com.company.helpdesk.dto.AuthDtos;
import com.company.helpdesk.exception.ApiException;
import com.company.helpdesk.exception.ErrorCode;
import com.company.helpdesk.model.Role;
import com.company.helpdesk.model.User;
import com.company.helpdesk.repository.UserRepository;
import com.company.helpdesk.security.JwtService;
import com.company.helpdesk.security.TotpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TotpService totpService;

    @Transactional
    public AuthDtos.LoginResponse register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Email sudah terdaftar");
        }

        User user = userRepository.save(User.builder()
                .nama(request.nama().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.KARYAWAN)
                .aktif(true)
                .totpEnabled(false)
                .build());

        String token = jwtService.generateToken(user);
        return new AuthDtos.LoginResponse(
                token,
                new AuthDtos.AuthUserSummary(user.getId(), user.getNama(), user.getRole())
        );
    }

    @Transactional(readOnly = true)
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Email atau password salah"));

        if (!Boolean.TRUE.equals(user.getAktif())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Akun dinonaktifkan, hubungi Admin");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Email atau password salah");
        }

        if (Boolean.TRUE.equals(user.getTotpEnabled())) {
            if (request.totpCode() == null || request.totpCode().isBlank()) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "Kode Authenticator (2FA) wajib diisi");
            }
            if (!totpService.verifyCode(user.getTotpSecret(), request.totpCode())) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "Kode Authenticator (2FA) tidak valid");
            }
        }

        String token = jwtService.generateToken(user);
        return new AuthDtos.LoginResponse(
                token,
                new AuthDtos.AuthUserSummary(user.getId(), user.getNama(), user.getRole())
        );
    }

    @Transactional(readOnly = true)
    public AuthDtos.ProfileResponse getProfile(Long userId) {
        User user = findUserOrThrow(userId);
        return new AuthDtos.ProfileResponse(
                user.getId(),
                user.getNama(),
                user.getEmail(),
                user.getRole(),
                Boolean.TRUE.equals(user.getTotpEnabled())
        );
    }

    @Transactional
    public void changePassword(Long userId, AuthDtos.ChangePasswordRequest request) {
        User user = findUserOrThrow(userId);
        if (!passwordEncoder.matches(request.passwordLama(), user.getPassword())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Password lama tidak sesuai");
        }
        user.setPassword(passwordEncoder.encode(request.passwordBaru()));
        userRepository.save(user);
    }

    @Transactional
    public AuthDtos.TwoFactorSetupResponse setupTwoFactor(Long userId) {
        User user = findUserOrThrow(userId);
        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        user.setTotpEnabled(false);
        userRepository.save(user);

        String uri = totpService.buildOtpAuthUri(user.getEmail(), secret);
        return new AuthDtos.TwoFactorSetupResponse(secret, uri);
    }

    @Transactional
    public void enableTwoFactor(Long userId, AuthDtos.TwoFactorCodeRequest request) {
        User user = findUserOrThrow(userId);
        if (user.getTotpSecret() == null || user.getTotpSecret().isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Lakukan setup 2FA terlebih dahulu");
        }
        if (!totpService.verifyCode(user.getTotpSecret(), request.code())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Kode Authenticator tidak valid");
        }
        user.setTotpEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void disableTwoFactor(Long userId, AuthDtos.TwoFactorDisableRequest request) {
        User user = findUserOrThrow(userId);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Password tidak sesuai");
        }
        if (Boolean.TRUE.equals(user.getTotpEnabled()) && !totpService.verifyCode(user.getTotpSecret(), request.code())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Kode Authenticator tidak valid");
        }
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User tidak ditemukan"));
    }
}