package com.mpfc.securebankingsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Root redirect: if authenticated, send to proper dashboard; else login
    @GetMapping("/")
    public String rootRedirect(org.springframework.security.core.Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return "redirect:" + (isAdmin ? "/admin" : "/upload");
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }
}
