package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A ready-made practice exam for a subject (the "làm đề" mode). Its questions
 * are graded and analysed for weaknesses like the entrance test.
 */
@Entity
@Table(name = "exams")
@Getter
@Setter
@NoArgsConstructor
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Subject subject;

    @Column(nullable = false)
    private String title;

    /** e.g. "Cơ bản", "Lớp 10 – Cuối kì", "Tốt nghiệp THPT". */
    private String level;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean premium = true;

    @Column(nullable = false)
    private int orderIndex = 0;
}
