package com.ga.medic.identity.security;

import com.ga.medic.identity.enums.UserStatusEnum;
import com.ga.medic.identity.model.User;
import com.ga.medic.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@NullMarked
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User with email " + email + " not found"));

        if (user.getStatus() == UserStatusEnum.DELETED || user.getDeletedAt() != null) {
            throw new UsernameNotFoundException("User with email " + email + " not found");
        }

        return new MyUserDetails(user);
    }
}
