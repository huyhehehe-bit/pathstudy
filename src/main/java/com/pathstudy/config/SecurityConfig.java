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
                                "/css/**", "/js/**", "/images/**", "/theme-kawai/**", "/favicon.ico",
                                "/h2-console/**", "/error", "/payment/webhook").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/teacher/**").hasAnyRole("TEACHER", "ADMIN")
                        // Khu vực HỌC SINH. Giáo viên và quản trị KHÔNG làm bài
                        // kiểm tra, không ghi tiến độ, không mua gói — họ có
                        // /teacher/** và /admin/** riêng. Chặn ở đây (không chỉ
                        // ẩn nút trên giao diện) vì gõ thẳng địa chỉ vẫn vào được.
                        .requestMatchers(
                                "/placement/**",   // kiểm tra đầu vào
                                "/module/**",      // bài học + bài tự đánh giá
                                "/english/**",     // hub Tiếng Anh của học sinh
                                "/home",           // trang chu sau dang nhap cua hoc sinh
                                "/path", "/progress", "/library/**",
                                "/subjects", "/subjects/**",
                                "/upgrade/**").hasRole("STUDENT")
                        // Đề thi thì KHÁC: giáo viên là người soạn đề nên phải
                        // xem và làm thử được (ExamController cho họ qua cổng
                        // Premium và không lọc theo khối). /ai/** đi kèm vì
                        // trang kết quả có nút "Vì sao sai?".
                        .requestMatchers("/exams", "/exam/**", "/ai/**").authenticated()
                        .anyRequest().authenticated())
                // Bị chặn thì đẩy về đúng nhà của vai trò đó kèm lời giải thích,
                // thay vì quăng trang 403 trắng không ai hiểu.
                .exceptionHandling(ex -> ex.accessDeniedHandler((req, res, denied) ->
                        res.sendRedirect(req.getContextPath() + "/start?denied")))
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
