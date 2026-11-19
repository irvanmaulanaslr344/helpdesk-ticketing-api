package com.company.helpdesk.security;

import com.company.helpdesk.model.Role;
import com.company.helpdesk.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String nama;
    private final String email;
    private final String password;
    private final Role role;
    private final boolean aktif;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.nama = user.getNama();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.aktif = Boolean.TRUE.equals(user.getAktif());
    }

    public Long getId() {
        return id;
    }

    public String getNama() {
        return nama;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return aktif;
    }
}