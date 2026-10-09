package com.pathstudy.web;

import com.pathstudy.domain.User;
import com.pathstudy.repo.UserRepository;
import com.pathstudy.service.BankTransferPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final BankTransferPaymentService payments;

    /** Số ngày Premium tự cấp cho người dùng mới (0 = tắt). */
    @Value("${app.signup.auto-premium-days:0}")
    private int autoPremiumDays;

    /** Mã đăng ký giáo viên. Trống = KHÔNG cho tự đăng ký GV (chỉ admin cấp quyền). */
    @Value("${app.signup.teacher-code:}")
    private String teacherCode;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          BankTransferPaymentService payments) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.payments = payments;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    /** Có cho phép tự đăng ký giáo viên không (khi đã cấu hình mã GV). */
    @ModelAttribute("teacherSignupEnabled")
    public boolean teacherSignupEnabled() {
        return teacherCode != null && !teacherCode.isBlank();
    }

    @GetMapping("/register")
    public String registerForm(@RequestParam(required = false) String goal, Model model) {
        if (!model.containsAttribute("form")) {
            RegisterForm f = new RegisterForm();
            f.setGoal(StudyGoal.normalize(goal)); // mục tiêu chọn từ landing
            model.addAttribute("form", f);
        }
        model.addAttribute("goals", StudyGoal.ALL);
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult binding,
                           Model model,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        boolean asTeacher = "TEACHER".equals(form.getRole());
        if (users.existsByEmail(form.getEmail())) {
            binding.rejectValue("email", "exists", "Email này đã được đăng ký");
        }
        if (asTeacher) {
            if (!teacherSignupEnabled()) {
                binding.rejectValue("teacherCode", "disabled", "Đăng ký giáo viên hiện chưa mở.");
            } else if (form.getTeacherCode() == null || !teacherCode.equals(form.getTeacherCode().strip())) {
                binding.rejectValue("teacherCode", "invalid", "Mã giáo viên không đúng.");
            }
        } else if (form.getGrade() == null || form.getGrade().isBlank()) {
            binding.rejectValue("grade", "required", "Vui lòng chọn khối lớp.");
        }
        if (binding.hasErrors()) {
            model.addAttribute("goals", StudyGoal.ALL);
            return "auth/register";
        }

        User u = new User();
        u.setFullName(form.getFullName().strip());
        u.setEmail(form.getEmail().strip().toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        if (asTeacher) {
            u.setRole("TEACHER");
            u.setGrade(null);
        } else {
            u.setGrade(form.getGrade());
            u.setGoal(StudyGoal.normalize(form.getGoal()));
        }
        users.save(u);

        // Ưu đãi launch: tự mở Premium miễn phí cho HỌC SINH mới (GV không cần).
        if (!asTeacher && autoPremiumDays > 0) {
            payments.grantComp(u, autoPremiumDays);
        }

        autoLogin(u.getEmail(), form.getPassword(), request, response);
        return "redirect:/start";
    }

    private void autoLogin(String email, String rawPassword,
                           HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
