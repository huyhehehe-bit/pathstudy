package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A stage in a subject's study path, e.g. "Phân tích tác phẩm".
 * Named CourseModule to avoid clashing with the Java 'module' keyword.
 */
@Entity
@Table(name = "modules")
@Getter
@Setter
@NoArgsConstructor
public class CourseModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Subject subject;

    @Column(nullable = false)
    private int orderIndex;

    /** Group label in the study path: "Nền tảng", "Lớp 10", "Lớp 11", "Lớp 12".
     *  Nullable at the DB level so ddl-auto=update can add it to existing tables. */
    @Column(length = 32)
    private String grade;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String subtitle;

    private String iconKey;

    /** Minimum total score (percent) to pass this module's estimate test. */
    @Column(nullable = false)
    private int passThreshold = 70;

    /** Whether this module contains real content or is a placeholder stage. */
    @Column(nullable = false)
    private boolean hasContent = false;
}
