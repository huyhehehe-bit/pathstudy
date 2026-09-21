package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A grade-scoped English lesson taken from the school textbook (SGK Global
 * Success). Each lesson teaches one grammar point and is keyed by {@link #topic}
 * so the tutor flow can map a student's weak topic (from the placement test) to
 * the exact lesson that teaches it.
 *
 * <p>Lớp 10 / 11 / 12 each have their own curriculum ({@link #grade}).
 */
@Entity
@Table(name = "english_lessons")
@Getter
@Setter
@NoArgsConstructor
public class EnglishLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. "Lớp 10", "Lớp 11", "Lớp 12". */
    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private int unitNo;

    @Column(nullable = false)
    private String unitTitle;

    /**
     * Canonical weakness tag, identical to the tag used on placement questions
     * so weak topics map straight to this lesson.
     * e.g. "Quá khứ đơn &amp; Quá khứ tiếp diễn".
     */
    @Column(nullable = false, length = 160)
    private String topic;

    /** English grammar name, e.g. "Past simple vs. Past continuous". */
    @Column(nullable = false)
    private String grammarName;

    @Column(length = 500)
    private String pronunciation;

    @Column(length = 4000)
    private String vocabulary;

    /** The "Remember!" theory, in Vietnamese + English rules. */
    @Column(length = 8000)
    private String theory;

    @Column(length = 4000)
    private String examples;

    @Column(nullable = false)
    private int orderIndex;
}
