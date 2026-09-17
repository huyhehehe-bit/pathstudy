package com.pathstudy.repo;

import com.pathstudy.domain.ReferenceMaterial;
import com.pathstudy.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferenceMaterialRepository extends JpaRepository<ReferenceMaterial, Long> {
    List<ReferenceMaterial> findBySubjectOrderByIdAsc(Subject subject);
}
