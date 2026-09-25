package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Đánh giá nhanh của người dùng: điểm hài lòng + ưu điểm + hạn chế + cảm nhận.
 * Lưu DB để xem trong /admin và (nếu cấu hình) đẩy sang Google Sheet qua webhook.
 * Lưu kèm snapshot email để hiển thị không cần join lazy (open-in-view=false).
 */
@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    /** Snapshot người gửi (email hoặc "Khách") để hiển thị trong admin. */
    @Column(length = 160)
    private String userLabel;

    /** Điểm hài lòng 1–5. */
    private Integer rating;

    @Column(length = 2000)
    private String pros;

    @Column(length = 2000)
    private String cons;

    @Column(length = 2000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
