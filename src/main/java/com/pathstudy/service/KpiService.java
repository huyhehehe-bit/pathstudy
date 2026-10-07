package com.pathstudy.service;

import com.pathstudy.domain.OrderStatus;
import com.pathstudy.domain.PaymentOrder;
import com.pathstudy.domain.User;
import com.pathstudy.repo.ExamRepository;
import com.pathstudy.repo.FeedbackRepository;
import com.pathstudy.repo.PaymentOrderRepository;
import com.pathstudy.repo.ReferenceMaterialRepository;
import com.pathstudy.repo.SubjectResourceRepository;
import com.pathstudy.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tổng hợp số liệu KPI nội bộ cho dashboard admin (phục vụ báo cáo/pitch):
 * người dùng, Premium, đơn thanh toán + doanh thu, nội dung, feedback.
 * (Lượng truy cập/visitor xem ở Google Analytics — không nằm trong DB.)
 */
@Service
public class KpiService {

    private final UserRepository users;
    private final PaymentOrderRepository orders;
    private final FeedbackRepository feedbacks;
    private final ExamRepository exams;
    private final SubjectResourceRepository subjectResources;
    private final ReferenceMaterialRepository referenceMaterials;

    public KpiService(UserRepository users, PaymentOrderRepository orders, FeedbackRepository feedbacks,
                      ExamRepository exams, SubjectResourceRepository subjectResources,
                      ReferenceMaterialRepository referenceMaterials) {
        this.users = users;
        this.orders = orders;
        this.feedbacks = feedbacks;
        this.exams = exams;
        this.subjectResources = subjectResources;
        this.referenceMaterials = referenceMaterials;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> metrics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);

        List<User> allUsers = users.findAll();
        long students = allUsers.stream().filter(u -> "STUDENT".equals(u.getRole())).count();
        long teachers = allUsers.stream().filter(u -> "TEACHER".equals(u.getRole())).count();
        long admins = allUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count();
        long newUsers7d = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(weekAgo)).count();

        // Thanh toán + doanh thu (chỉ tính đơn PAID có amount > 0 — bỏ đơn comp 0đ).
        long paidOrders = 0, pendingOrders = 0;
        long revenue = 0;
        Set<Long> activePremiumUsers = new HashSet<>();
        for (PaymentOrder o : orders.findAll()) {
            if (o.getStatus() == OrderStatus.PAID) {
                if (o.getAmount() > 0) {
                    paidOrders++;
                    revenue += o.getAmount();
                }
                if (o.getExpiresAt() != null && o.getExpiresAt().isAfter(now) && o.getUser() != null) {
                    activePremiumUsers.add(o.getUser().getId());
                }
            } else if (o.getStatus() == OrderStatus.PENDING) {
                pendingOrders++;
            }
        }

        long rated = feedbacks.findAll().stream().filter(f -> f.getRating() != null).count();
        double avgRating = feedbacks.findAll().stream()
                .filter(f -> f.getRating() != null).mapToInt(f -> f.getRating()).average().orElse(0);
        long teacherExams = exams.findAll().stream().filter(e -> e.getCreatedByEmail() != null).count();
        long teacherDocs = referenceMaterials.findAll().stream()
                .filter(r -> r.getCreatedByEmail() != null).count();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalUsers", allUsers.size());
        m.put("students", students);
        m.put("teachers", teachers);
        m.put("admins", admins);
        m.put("newUsers7d", newUsers7d);
        m.put("activePremium", activePremiumUsers.size());
        m.put("paidOrders", paidOrders);
        m.put("pendingOrders", pendingOrders);
        m.put("revenue", revenue);
        m.put("feedbackCount", feedbacks.count());
        m.put("ratedCount", rated);
        m.put("avgRating", Math.round(avgRating * 10) / 10.0);
        m.put("teacherExams", teacherExams);
        m.put("subjectResources", subjectResources.count());
        m.put("teacherDocs", teacherDocs);
        return m;
    }
}
