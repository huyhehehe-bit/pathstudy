package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String grade = "THPT";

    private String iconKey;

    private String colorKey;

    @Column(length = 500)
    private String description;

    /** Whether this subject has real learning content yet. */
    @Column(nullable = false)
    private boolean active = false;

    @Column(nullable = false)
    private int orderIndex = 0;
}
