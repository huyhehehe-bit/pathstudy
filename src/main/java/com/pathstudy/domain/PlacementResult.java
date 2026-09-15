package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "placement_results")
@Getter
@Setter
@NoArgsConstructor
public class PlacementResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Subject subject;

    @Column(nullable = false)
    private int attemptNo;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private String level;

    /** Comma-separated competency labels the student is strong in. */
    private String strengths;

    /** Comma-separated competency labels to improve. */
    private String weaknesses;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
