package com.company.helpdesk.config;

import com.company.helpdesk.model.Role;
import com.company.helpdesk.model.User;
import com.company.helpdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedUser("Admin Helpdesk", "admin@company.com", "Admin123!", Role.ADMIN);
        seedUser("Supervisor IT", "supervisor@company.com", "Supervisor123!", Role.SUPERVISOR);
        seedUser("Budi Karyawan", "karyawan@company.com", "Karyawan123!", Role.KARYAWAN);
    }

    private void seedUser(String nama, String email, String rawPassword, Role role) {
        if (!userRepository.existsByEmailIgnoreCase(email)) {
            userRepository.save(User.builder()
                    .nama(nama)
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .aktif(true)
                    .totpEnabled(false)
                    .build());
            log.info("Seeded default account: {} ({})", email, role);
        }
    }
}