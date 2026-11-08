package com.ga.medic.security;

import com.ga.medic.enums.UserStatusEnum;
import com.ga.medic.model.User;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@NullMarked
public record MyUserDetails(User user) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatusEnum.ACTIVE && user.getDeletedAt() == null;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (user.getDoctorProfile() != null) {
            return Boolean.TRUE.equals(user.getDoctorProfile().getIsVerified());
        }
        return true;
    }
}
