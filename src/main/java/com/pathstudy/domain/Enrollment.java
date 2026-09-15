package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A student's personalized study path for one subject.
 */
@Entity
@Table(name = "enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "subject_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Subject subject;

    /** The first subject the student chose. */
    @Column(nullable = false)
    private boolean primarySubject = false;

    /** Level determined by the placement test, e.g. "Khá". Null until tested. */
    private String level;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
