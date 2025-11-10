package com.mpfc.securebankingsystem.config;

import com.mpfc.securebankingsystem.security.CustomUserDetailsService;
import com.mpfc.securebankingsystem.security.LockoutService;
import com.mpfc.securebankingsystem.service.AuditLogService;
import com.mpfc.securebankingsystem.service.IncidentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

@Configuration
public class SecurityConfig {

    private final AuditLogService auditLogService;
    private final IncidentService incidentService;
    private final LockoutService lockoutService;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(AuditLogService auditLogService,
                          IncidentService incidentService,
                          LockoutService lockoutService,
                          CustomUserDetailsService userDetailsService) {
        this.auditLogService = auditLogService;
        this.incidentService = incidentService;
        this.lockoutService = lockoutService;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .userDetailsService(userDetailsService)
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/h2-console/**", "/error/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/upload/**").hasRole("RECORDS_OFFICER")
                .anyRequest().authenticated()
            )
            .formLogin(f -> f
                .loginPage("/login")
                .successHandler((req, res, auth) -> {
                    // reset attempts
                    lockoutService.reset(auth.getName());
                    SavedRequest saved = new HttpSessionRequestCache().getRequest(req, res);
                    if (saved != null) {
                        String target = saved.getRedirectUrl();
                        if (!target.endsWith("/") && !target.equals(req.getContextPath() + "/")) {
                            res.sendRedirect(target);
                            return;
                        }
                    }
                    boolean isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    res.sendRedirect(isAdmin ? "/admin" : "/upload");
                })
                .failureHandler((req, res, ex) -> {
                    String user = req.getParameter("username") != null ? req.getParameter("username").toLowerCase() : "unknown";
                    boolean nowLocked = lockoutService.recordFailure(user);
                    incidentService.recordIncident(user, "FAILED_LOGIN", ex.getClass().getSimpleName());
                    auditLogService.log(user, "LOGIN_FAILED", null, ex.getMessage());
                    if (nowLocked) {
                        incidentService.recordIncident(user, "ACCOUNT_LOCKED", "Too many failed login attempts");
                        auditLogService.log(user, "ACCOUNT_LOCKED", null, "Locked for 2 minutes");
                    }
                    String redirect = "/login?error";
                    if (lockoutService.isLocked(user)) redirect += "&locked";
                    res.sendRedirect(redirect);
                })
                .permitAll()
            )
            .logout(l -> l
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((req, res, ex2) -> {
                    incidentService.recordIncident(
                        req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "anonymous",
                        "UNAUTHORIZED_ACCESS",
                        req.getRequestURI()
                    );
                    res.sendRedirect("/403");
                })
            );
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @EventListener
    public void onAuthSuccess(InteractiveAuthenticationSuccessEvent evt) {
        Authentication auth = evt.getAuthentication();
        auditLogService.log(auth.getName(), "LOGIN_SUCCESS", null, "User logged in");
    }

    @EventListener
    public void onAuthFailure(AbstractAuthenticationFailureEvent evt) {
        // Redundant with failureHandler, kept for audit completeness
        String user = (evt.getAuthentication() != null) ? String.valueOf(evt.getAuthentication().getPrincipal()) : "unknown";
        auditLogService.log(user, "LOGIN_FAILURE_EVENT", null, evt.getException().getMessage());
    }
}
