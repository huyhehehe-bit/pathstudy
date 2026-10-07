package com.pathstudy.service;

import com.pathstudy.domain.Exam;
import com.pathstudy.domain.Question;
import com.pathstudy.repo.ExamRepository;
import com.pathstudy.repo.FeedbackRepository;
import com.pathstudy.repo.PaymentOrderRepository;
import com.pathstudy.repo.PlacementResultRepository;
import com.pathstudy.repo.QuestionRepository;
import com.pathstudy.repo.ReferenceMaterialRepository;
import com.pathstudy.repo.SubjectResourceRepository;
import com.pathstudy.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Xuất BẢN SAO dữ liệu KHÔNG tái tạo được (tài khoản, đơn thanh toán, nội dung do
 * giáo viên tạo, feedback, kết quả kiểm tra) ra một Map để tải về dạng JSON.
 * Nội dung seed (bài học, đề mẫu) không xuất vì có thể seed lại.
 */
@Service
public class BackupService {

    private final UserRepository users;
    private final PaymentOrderRepository orders;
    private final ExamRepository exams;
    private final QuestionRepository questions;
    private final SubjectResourceRepository subjectResources;
    private final ReferenceMaterialRepository referenceMaterials;
    private final FeedbackRepository feedbacks;
    private final PlacementResultRepository placementResults;

    public BackupService(UserRepository users, PaymentOrderRepository orders, ExamRepository exams,
                         QuestionRepository questions, SubjectResourceRepository subjectResources,
                         ReferenceMaterialRepository referenceMaterials, FeedbackRepository feedbacks,
                         PlacementResultRepository placementResults) {
        this.users = users;
        this.orders = orders;
        this.exams = exams;
        this.questions = questions;
        this.subjectResources = subjectResources;
        this.referenceMaterials = referenceMaterials;
        this.feedbacks = feedbacks;
        this.placementResults = placementResults;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("exportedAt", LocalDateTime.now().toString());
        root.put("note", "Bản sao dữ liệu không tái tạo được của PathStudy. Chứa hash mật khẩu — giữ kín.");

        // Tài khoản (gồm hash mật khẩu để khôi phục được; file này nhạy cảm).
        List<Map<String, Object>> userList = new ArrayList<>();
        users.findAll().forEach(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("fullName", u.getFullName());
            m.put("email", u.getEmail());
            m.put("passwordHash", u.getPasswordHash());
            m.put("role", u.getRole());
            m.put("grade", u.getGrade());
            m.put("createdAt", str(u.getCreatedAt()));
            userList.add(m);
        });
        root.put("users", userList);

        // Đơn thanh toán.
        List<Map<String, Object>> orderList = new ArrayList<>();
        orders.findAll().forEach(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userEmail", o.getUser() != null ? o.getUser().getEmail() : null);
            m.put("planCode", o.getPlanCode());
            m.put("amount", o.getAmount());
            m.put("memoCode", o.getMemoCode());
            m.put("status", o.getStatus() != null ? o.getStatus().name() : null);
            m.put("createdAt", str(o.getCreatedAt()));
            m.put("paidAt", str(o.getPaidAt()));
            m.put("expiresAt", str(o.getExpiresAt()));
            orderList.add(m);
        });
        root.put("paymentOrders", orderList);

        // Đề do giáo viên tạo + câu hỏi của đề.
        List<Map<String, Object>> examList = new ArrayList<>();
        for (Exam e : exams.findAll()) {
            if (e.getCreatedByEmail() == null) {
                continue; // bỏ đề seed
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subjectCode", e.getSubject() != null ? e.getSubject().getCode() : null);
            m.put("title", e.getTitle());
            m.put("level", e.getLevel());
            m.put("grade", e.getGrade());
            m.put("description", e.getDescription());
            m.put("premium", e.isPremium());
            m.put("category", e.getCategory());
            m.put("createdByEmail", e.getCreatedByEmail());
            List<Map<String, Object>> qs = new ArrayList<>();
            for (Question q : questions.findByExamOrderByOrderIndexAsc(e)) {
                Map<String, Object> qm = new LinkedHashMap<>();
                qm.put("text", q.getText());
                qm.put("options", q.getOptions());
                qm.put("correctIndex", q.getCorrectIndex());
                qm.put("competency", q.getCompetency() != null ? q.getCompetency().name() : null);
                qm.put("topic", q.getTopic());
                qm.put("passage", q.getPassage());
                qm.put("orderIndex", q.getOrderIndex());
                qs.add(qm);
            }
            m.put("questions", qs);
            examList.add(m);
        }
        root.put("teacherExams", examList);

        // Kho tài liệu (toàn bộ do giáo viên nhập).
        List<Map<String, Object>> resList = new ArrayList<>();
        subjectResources.findAll().forEach(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subjectCode", r.getSubject() != null ? r.getSubject().getCode() : null);
            m.put("title", r.getTitle());
            m.put("url", r.getUrl());
            m.put("category", r.getCategory());
            m.put("description", r.getDescription());
            m.put("createdByEmail", r.getCreatedByEmail());
            m.put("createdAt", str(r.getCreatedAt()));
            resList.add(m);
        });
        root.put("subjectResources", resList);

        // Tài liệu nguồn do giáo viên nhập (bỏ seed).
        List<Map<String, Object>> refList = new ArrayList<>();
        referenceMaterials.findAll().forEach(r -> {
            if (r.getCreatedByEmail() == null) {
                return;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subjectCode", r.getSubject() != null ? r.getSubject().getCode() : null);
            m.put("title", r.getTitle());
            m.put("content", r.getContent());
            m.put("createdByEmail", r.getCreatedByEmail());
            refList.add(m);
        });
        root.put("referenceMaterials", refList);

        // Feedback.
        List<Map<String, Object>> fbList = new ArrayList<>();
        feedbacks.findAllByOrderByCreatedAtDesc().forEach(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userLabel", f.getUserLabel());
            m.put("rating", f.getRating());
            m.put("pros", f.getPros());
            m.put("cons", f.getCons());
            m.put("comment", f.getComment());
            m.put("createdAt", str(f.getCreatedAt()));
            fbList.add(m);
        });
        root.put("feedback", fbList);

        // Kết quả kiểm tra đầu vào (tiến độ/trình độ học sinh).
        List<Map<String, Object>> prList = new ArrayList<>();
        placementResults.findAll().forEach(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userEmail", p.getUser() != null ? p.getUser().getEmail() : null);
            m.put("subjectCode", p.getSubject() != null ? p.getSubject().getCode() : null);
            m.put("grade", p.getGrade());
            m.put("attemptNo", p.getAttemptNo());
            m.put("score", p.getScore());
            m.put("level", p.getLevel());
            m.put("weakTopics", p.getWeakTopics());
            m.put("createdAt", str(p.getCreatedAt()));
            prList.add(m);
        });
        root.put("placementResults", prList);

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("users", userList.size());
        counts.put("paymentOrders", orderList.size());
        counts.put("teacherExams", examList.size());
        counts.put("subjectResources", resList.size());
        counts.put("referenceMaterials", refList.size());
        counts.put("feedback", fbList.size());
        counts.put("placementResults", prList.size());
        root.put("counts", counts);

        return root;
    }

    private static String str(LocalDateTime t) {
        return t == null ? null : t.toString();
    }
}
