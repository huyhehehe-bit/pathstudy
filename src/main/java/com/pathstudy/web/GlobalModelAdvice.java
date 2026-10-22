package com.pathstudy.web;

import com.pathstudy.domain.Enrollment;
import com.pathstudy.domain.User;
import com.pathstudy.repo.EnrollmentRepository;
import com.pathstudy.service.BankTransferPaymentService;
import com.pathstudy.service.BookmarkService;
import com.pathstudy.service.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Optional;

@ControllerAdvice
public class GlobalModelAdvice {

    private final CurrentUserService currentUser;
    private final BookmarkService bookmarks;
    private final EnrollmentRepository enrollments;
    private final BankTransferPaymentService payments;

    @Value("${app.name:PathStudy}")
    private String appName;

    /** GA4 Measurement ID (G-XXXXXXX). Trống = không nhúng Google Analytics. */
    @Value("${app.analytics.ga-id:}")
    private String gaId;

    /** Link form khao sat trai nghiem. De trong = khong hien loi moi khao sat. */
    @Value("${app.survey.url:}")
    private String surveyUrl;

    /** Dung web du bao nhieu mili-giay thi moi khao sat (cong don nhieu trang). */
    @Value("${app.survey.delay-ms:300000}")
    private long surveyDelayMs;

    public GlobalModelAdvice(CurrentUserService currentUser, BookmarkService bookmarks,
                             EnrollmentRepository enrollments, BankTransferPaymentService payments) {
        this.currentUser = currentUser;
        this.bookmarks = bookmarks;
        this.enrollments = enrollments;
        this.payments = payments;
    }

    @ModelAttribute("isPremium")
    public boolean isPremium() {
        return currentUser.current().map(payments::hasPremiumAccess).orElse(false);
    }

    @ModelAttribute("appName")
    public String appName() {
        return appName;
    }

    @ModelAttribute("gaId")
    public String gaId() {
        return gaId;
    }

    @ModelAttribute("surveyUrl")
    public String surveyUrl() {
        return surveyUrl;
    }

    @ModelAttribute("surveyDelayMs")
    public long surveyDelayMs() {
        return surveyDelayMs;
    }

    @ModelAttribute("currentUser")
    public User currentUser() {
        return currentUser.current().orElse(null);
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        return currentUser.current().map(u -> "ADMIN".equals(u.getRole())).orElse(false);
    }

    @ModelAttribute("isTeacher")
    public boolean isTeacher() {
        return currentUser.current()
                .map(u -> "TEACHER".equals(u.getRole()) || "ADMIN".equals(u.getRole()))
                .orElse(false);
    }

    @ModelAttribute("isStudent")
    public boolean isStudent() {
        return currentUser.current()
                .map(u -> !"ADMIN".equals(u.getRole()) && !"TEACHER".equals(u.getRole()))
                .orElse(true);
    }

    @ModelAttribute("bookmarkCount")
    public long bookmarkCount() {
        return currentUser.current().map(bookmarks::count).orElse(0L);
    }

    /**
     * Các môn học sinh đang học, cho thanh bên.
     * /path không có tham số luôn mở môn ĐẦU TIÊN, nên học sinh thêm môn thứ hai
     * (vd làm kiểm tra Tiếng Anh sau khi đã học Ngữ văn) không có đường nào quay
     * lại môn mới ngoài trang kết quả — phải có danh sách môn ở thanh bên.
     * Giáo viên/quản trị không học nên bỏ qua (khỏi query vô ích).
     */
    @ModelAttribute("myEnrollments")
    public List<Enrollment> myEnrollments() {
        Optional<User> u = currentUser.current();
        if (u.isEmpty() || "TEACHER".equals(u.get().getRole()) || "ADMIN".equals(u.get().getRole())) {
            return List.of();
        }
        return enrollments.findByUserOrderByCreatedAtAsc(u.get());
    }
}
