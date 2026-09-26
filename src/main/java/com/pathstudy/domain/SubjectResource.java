package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tài liệu tham khảo theo MÔN do giáo viên/admin bổ sung: một liên kết ngoài
 * (Google Drive, PDF...) kèm tiêu đề, phân loại (đề thi cuối kì, giáo trình...).
 * Khác {@link ReferenceMaterial} (nội dung text cho AI đọc) — đây là link để
 * con người mở xem lại.
 */
@Entity
@Table(name = "subject_resources")
@Getter
@Setter
@NoArgsConstructor
public class SubjectResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Subject subject;

    @Column(nullable = false)
    private String title;

    /** Liên kết ngoài (chỉ http/https), ví dụ Google Drive. */
    @Column(length = 1000)
    private String url;

    /** Phân loại: "Đề thi cuối kì", "Đề thi giữa kì", "Giáo trình", "Bài tập", "Khác". */
    @Column(length = 60)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(length = 160)
    private String createdByEmail;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
