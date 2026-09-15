package com.pathstudy.repo;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.ModuleProgress;
import com.pathstudy.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModuleProgressRepository extends JpaRepository<ModuleProgress, Long> {
    List<ModuleProgress> findByUser(User user);
    Optional<ModuleProgress> findByUserAndModule(User user, CourseModule module);
}
