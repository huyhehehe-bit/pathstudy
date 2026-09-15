package com.pathstudy.repo;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {
    List<CourseModule> findBySubjectOrderByOrderIndexAsc(Subject subject);
    List<CourseModule> findBySubjectCodeOrderByOrderIndexAsc(String code);
}
