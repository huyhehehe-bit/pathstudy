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

    @ModelAttribute("bookmarkCount")
    public long bookmarkCount() {
        return currentUser.current().map(bookmarks::count).orElse(0L);
    }

    /** Code of the student's primary subject, for the sidebar "Lộ trình" link. */
    @ModelAttribute("primarySubjectCode")
    public String primarySubjectCode() {
        Optional<User> u = currentUser.current();
        if (u.isEmpty()) {
            return null;
        }
        List<Enrollment> list = enrollments.findByUserOrderByCreatedAtAsc(u.get());
        return list.isEmpty() ? null : list.get(0).getSubject().getCode();
    }
}
