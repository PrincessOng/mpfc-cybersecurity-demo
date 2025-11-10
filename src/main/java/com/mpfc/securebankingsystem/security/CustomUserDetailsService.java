package com.mpfc.securebankingsystem.security;

import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final LockoutService lockoutService;
    private final Map<String, String> users; // username -> encoded password
    private final Map<String, String[]> roles; // username -> roles

    public CustomUserDetailsService(LockoutService lockoutService) {
        this.lockoutService = lockoutService;
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        this.users = Map.of(
                "admin", encoder.encode("Admin@123"),
                "officer", encoder.encode("Officer@123")
        );
        this.roles = Map.of(
                "admin", new String[]{"ADMIN"},
                "officer", new String[]{"RECORDS_OFFICER"}
        );

        boolean adminMatches = encoder.matches("Admin@123", users.get("admin"));
        boolean officerMatches = encoder.matches("Officer@123", users.get("officer"));
        log.info("\n================ DEMO USER PASSWORD HASHES ================\n" +
                 "Admin (username=admin)   : {}\n" +
                 "Officer (username=officer): {}\n" +
                 "BCrypt match check       : admin={}, officer={}\n" +
                 "===========================================================\n",
                 users.get("admin"), users.get("officer"), adminMatches, officerMatches);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String key = username.toLowerCase();
        if (!users.containsKey(key)) throw new UsernameNotFoundException("User not found");
        boolean locked = lockoutService.isLocked(key);
        return User.withUsername(key)
                .password(users.get(key))
                .roles(roles.get(key))
                .accountLocked(locked)
                .build();
    }
}