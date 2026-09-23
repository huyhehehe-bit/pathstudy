package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String role = "STUDENT";

    /** Khối lớp của học sinh: "Lớp 10"/"Lớp 11"/"Lớp 12". Null cho GV/Admin hoặc tài khoản cũ. */
    @Column(length = 20)
    private String grade;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
