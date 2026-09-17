package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A teaching document (grammar/theory text) for a subject. Its content is fed to
 * the AI so it can pinpoint weaknesses, write a mini lesson and generate practice
 * questions grounded in the teacher's own material.
 */
@Entity
@Table(name = "reference_materials")
@Getter
@Setter
@NoArgsConstructor
public class ReferenceMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Subject subject;

    @Column(nullable = false)
    private String title;

    /** Plain-text content (large VARCHAR — portable across H2 & PostgreSQL). */
    @Column(nullable = false, length = 200000)
    private String content;

    private String createdByEmail;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
