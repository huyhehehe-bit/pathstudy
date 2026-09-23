package com.pathstudy.repo;

import com.pathstudy.domain.PlacementResult;
import com.pathstudy.domain.Subject;
import com.pathstudy.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlacementResultRepository extends JpaRepository<PlacementResult, Long> {
    List<PlacementResult> findByUserAndSubjectOrderByScoreDesc(User user, Subject subject);
    Optional<PlacementResult> findTopByUserAndSubjectOrderByScoreDesc(User user, Subject subject);
    long countByUserAndSubject(User user, Subject subject);

    // Grade-scoped (English): each grade has its own diagnostic + attempts.
    Optional<PlacementResult> findTopByUserAndSubjectAndGradeOrderByScoreDesc(User user, Subject subject, String grade);
    long countByUserAndSubjectAndGrade(User user, Subject subject, String grade);
}
