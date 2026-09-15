package com.pathstudy.repo;

import com.pathstudy.domain.Bookmark;
import com.pathstudy.domain.LessonSection;
import com.pathstudy.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserOrderByCreatedAtDesc(User user);
    long countByUser(User user);
    boolean existsByUserAndSection(User user, LessonSection section);
}
