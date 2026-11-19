package com.company.helpdesk.service;

import com.company.helpdesk.dto.TicketDtos;
import com.company.helpdesk.dto.UserDtos;
import com.company.helpdesk.exception.ApiException;
import com.company.helpdesk.exception.ErrorCode;
import com.company.helpdesk.model.Role;
import com.company.helpdesk.model.User;
import com.company.helpdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserDtos.UserListResponse listUsers(int page, int limit, Role role, String search) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.clamp(limit, 1, 100);
        String cleanSearch = (search == null || search.isBlank()) ? null : search.trim();

        Page<User> result = userRepository.searchUsers(
                role,
                cleanSearch,
                PageRequest.of(safePage - 1, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<UserDtos.UserItem> items = result.getContent()
                .stream()
                .map(this::toUserItem)
                .toList();

        return new UserDtos.UserListResponse(
                items,
                new TicketDtos.PageMeta(safePage, safeLimit, result.getTotalElements())
        );
    }

    @Transactional
    public UserDtos.CreateUserResponse createUser(UserDtos.CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Email sudah terdaftar");
        }

        User user = userRepository.save(User.builder()
                .nama(request.nama().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .aktif(true)
                .totpEnabled(false)
                .build());

        return new UserDtos.CreateUserResponse(user.getId());
    }

    @Transactional
    public UserDtos.UserItem updateUser(Long id, UserDtos.UpdateUserRequest request) {
        User user = findUserOrThrow(id);
        if (request.nama() != null && !request.nama().isBlank()) {
            user.setNama(request.nama().trim());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.aktif() != null) {
            user.setAktif(request.aktif());
        }
        return toUserItem(userRepository.save(user));
    }

    @Transactional
    public void resetPassword(Long id, UserDtos.ResetPasswordRequest request) {
        User user = findUserOrThrow(id);
        user.setPassword(passwordEncoder.encode(request.passwordBaru()));
        userRepository.save(user);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User tidak ditemukan"));
    }

    private UserDtos.UserItem toUserItem(User user) {
        return new UserDtos.UserItem(
                user.getId(),
                user.getNama(),
                user.getEmail(),
                user.getRole(),
                user.getAktif(),
                user.getTotpEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}