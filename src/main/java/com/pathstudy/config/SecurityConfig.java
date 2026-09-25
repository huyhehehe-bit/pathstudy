package com.pathstudy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.rememberme.key:pathstudy-remember-me-key}")
    private String rememberMeKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Ép CSRF token nạp SỚM (eager) trong CsrfFilter thay vì trễ (deferred).
        // Nếu để deferred, token chỉ được tạo khi render <form th:action>, lúc đó
        // response có thể đã commit (vd trang có nhiều nội dung ở <head>) → lỗi
        // "Cannot create a session after the response has been committed" → 500.
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/register", "/login",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                "/h2-console/**", "/error", "/payment/webhook").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/teacher/**").hasAnyRole("TEACHER", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/start", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?loggedOut")
                        .deleteCookies("remember-me")
                        .permitAll())
                // Keep users signed in across server restarts / free-tier spin-down.
                .rememberMe(rm -> rm
                        .key(rememberMeKey)
                        .alwaysRemember(true)
                        .tokenValiditySeconds(60 * 60 * 24 * 14))
                // H2 console and the payment webhook post without a CSRF token.
                .csrf(csrf -> csrf
                        .csrfTokenRequestHandler(csrfHandler)
                        .ignoringRequestMatchers("/h2-console/**", "/payment/webhook"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}
