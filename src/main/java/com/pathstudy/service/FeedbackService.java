package com.pathstudy.service;

import com.pathstudy.domain.Feedback;
import com.pathstudy.domain.User;
import com.pathstudy.repo.FeedbackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Lưu đánh giá nhanh vào DB và (nếu cấu hình) đẩy sang Google Sheet qua một
 * Google Apps Script web app. Việc gọi webhook chạy nền, best-effort: lỗi mạng
 * không làm hỏng việc lưu DB hay chặn request người dùng.
 */
@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FeedbackRepository feedbacks;
    private final RestClient rest = RestClient.create();

    /** URL Google Apps Script web app; trống = chỉ lưu DB. */
    @Value("${app.feedback.sheet-webhook:}")
    private String sheetWebhook;

    public FeedbackService(FeedbackRepository feedbacks) {
        this.feedbacks = feedbacks;
    }

    @Transactional
    public Feedback submit(User user, Integer rating, String pros, String cons, String comment) {
        Feedback fb = new Feedback();
        fb.setUser(user);
        fb.setUserLabel(user != null ? user.getEmail() : "Khách");
        fb.setRating(rating);
        fb.setPros(trim(pros));
        fb.setCons(trim(cons));
        fb.setComment(trim(comment));
        feedbacks.save(fb);
        forwardToSheet(fb);
        return fb;
    }

    @Transactional(readOnly = true)
    public List<Feedback> all() {
        return feedbacks.findAllByOrderByCreatedAtDesc();
    }

    /** Gửi 1 dòng sang Google Sheet (chạy nền, không chặn, nuốt mọi lỗi). */
    private void forwardToSheet(Feedback fb) {
        if (sheetWebhook == null || sheetWebhook.isBlank()) {
            return;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("time", fb.getCreatedAt().format(TS));
        row.put("user", fb.getUserLabel());
        row.put("rating", fb.getRating());
        row.put("pros", fb.getPros());
        row.put("cons", fb.getCons());
        row.put("comment", fb.getComment());
        CompletableFuture.runAsync(() -> {
            try {
                rest.post()
                        .uri(sheetWebhook)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(row)
                        .retrieve()
                        .toBodilessEntity();
            } catch (RuntimeException e) {
                log.warn("Gửi feedback sang Google Sheet thất bại: {}", e.toString());
            }
        });
    }

    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        s = s.strip();
        return s.length() > 2000 ? s.substring(0, 2000) : s;
    }
}
