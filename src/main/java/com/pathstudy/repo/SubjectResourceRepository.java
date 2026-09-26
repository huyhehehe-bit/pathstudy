package com.pathstudy.repo;

import com.pathstudy.domain.Subject;
import com.pathstudy.domain.SubjectResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectResourceRepository extends JpaRepository<SubjectResource, Long> {
    List<SubjectResource> findBySubjectOrderByCreatedAtDesc(Subject subject);
}
