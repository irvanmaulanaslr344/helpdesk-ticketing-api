package com.company.helpdesk.controller;

import com.company.helpdesk.dto.AuthDtos;
import com.company.helpdesk.dto.UserDtos;
import com.company.helpdesk.model.Role;
import com.company.helpdesk.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "5. User Management", description = "Kelola user, role, status aktif, dan reset password (khusus ADMIN)")
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Daftar seluruh user dengan filter role, search, dan pagination")
    @GetMapping
    public ResponseEntity<UserDtos.UserListResponse> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(userService.listUsers(page, limit, role, search));
    }

    @Operation(summary = "Membuat user baru (KARYAWAN / ADMIN / SUPERVISOR)")
    @PostMapping
    public ResponseEntity<UserDtos.CreateUserResponse> createUser(@Valid @RequestBody UserDtos.CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @Operation(summary = "Mengubah nama, role, atau status aktif user")
    @PatchMapping("/{id}")
    public ResponseEntity<UserDtos.UserItem> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDtos.UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @Operation(summary = "Reset password user secara manual oleh Admin")
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<AuthDtos.MessageResponse> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody UserDtos.ResetPasswordRequest request
    ) {
        userService.resetPassword(id, request);
        return ResponseEntity.ok(new AuthDtos.MessageResponse("password direset"));
    }
}