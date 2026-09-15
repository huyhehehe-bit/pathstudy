package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A saved snippet in the student's personal library ("Kho lưu trữ của tôi").
 * Denormalized title/snippet so it survives even if the source content changes.
 */
@Entity
@Table(name = "bookmarks")
@Getter
@Setter
@NoArgsConstructor
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private User user;

    /** Optional link back to the source section. */
    @ManyToOne(fetch = FetchType.EAGER)
    private LessonSection section;

    /** e.g. "Tác phẩm", "Quan trọng", "Công cụ", "Từ vựng". */
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String snippet;

    private String sourceLabel;

    private Long moduleId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
