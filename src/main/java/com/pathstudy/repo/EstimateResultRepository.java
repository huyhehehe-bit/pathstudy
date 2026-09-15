package com.pathstudy.repo;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.EstimateResult;
import com.pathstudy.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstimateResultRepository extends JpaRepository<EstimateResult, Long> {
    Optional<EstimateResult> findTopByUserAndModuleOrderByCreatedAtDesc(User user, CourseModule module);
}
