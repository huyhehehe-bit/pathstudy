package com.pathstudy.repo;

import com.pathstudy.domain.Enrollment;
import com.pathstudy.domain.Subject;
import com.pathstudy.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByUserOrderByCreatedAtAsc(User user);
    Optional<Enrollment> findByUserAndSubject(User user, Subject subject);
    boolean existsByUserAndSubject(User user, Subject subject);
    long countByUser(User user);
}
