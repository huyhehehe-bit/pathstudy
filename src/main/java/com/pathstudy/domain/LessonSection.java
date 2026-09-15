package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A supporting tool attached to a lesson: reading, theme, imagery, technique,
 * context, analysis guide, writing framework, sample answer, vocabulary.
 */
@Entity
@Table(name = "lesson_sections")
@Getter
@Setter
@NoArgsConstructor
public class LessonSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Lesson lesson;

    @Column(nullable = false)
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SectionType type;

    private String title;

    @Column(length = 6000)
    private String body;

    @Column(nullable = false)
    private boolean bookmarkable = true;
}
