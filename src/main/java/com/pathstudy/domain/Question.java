package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizScope scope;

    /** Set for PLACEMENT questions. */
    @ManyToOne(fetch = FetchType.EAGER)
    private Subject subject;

    /** Set for ESTIMATE questions. */
    @ManyToOne(fetch = FetchType.EAGER)
    private CourseModule module;

    @Column(nullable = false)
    private int orderIndex;

    @Column(nullable = false, length = 1000)
    private String text;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text", length = 500)
    @OrderColumn(name = "option_index")
    private List<String> options = new ArrayList<>();

    @Column(nullable = false)
    private int correctIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Competency competency;

    /** Fine-grained weakness tag, e.g. "Thì động từ", "Từ vựng", "Đọc hiểu",
     *  "Phát âm", "Câu điều kiện". Used to analyse where the learner is weak. */
    @Column(length = 120)
    private String topic;
}
