package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "estimate_results")
@Getter
@Setter
@NoArgsConstructor
public class EstimateResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private CourseModule module;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private int knowledge;

    @Column(nullable = false)
    private int analysis;

    @Column(nullable = false)
    private int application;

    @Column(nullable = false)
    private boolean passed;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
