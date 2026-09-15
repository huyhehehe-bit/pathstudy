package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "module_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "module_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ModuleProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private CourseModule module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressStatus status = ProgressStatus.LOCKED;

    @Column(nullable = false)
    private int percent = 0;
}
