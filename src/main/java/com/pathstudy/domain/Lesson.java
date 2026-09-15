package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The central work/topic of a module (e.g. the poem "Sóng"), surrounded by
 * supporting {@link LessonSection}s.
 */
@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private CourseModule module;

    @Column(nullable = false)
    private int orderIndex;

    @Column(nullable = false)
    private String title;

    private String author;

    private String genre;

    private int durationMinutes;

    @Column(length = 2000)
    private String heroExcerpt;

    @Column(length = 1000)
    private String summary;
}
