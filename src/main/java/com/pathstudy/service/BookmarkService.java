package com.pathstudy.service;

import com.pathstudy.domain.Bookmark;
import com.pathstudy.domain.LessonSection;
import com.pathstudy.domain.SectionType;
import com.pathstudy.domain.User;
import com.pathstudy.repo.BookmarkRepository;
import com.pathstudy.repo.LessonSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarks;
    private final LessonSectionRepository sections;

    public BookmarkService(BookmarkRepository bookmarks, LessonSectionRepository sections) {
        this.bookmarks = bookmarks;
        this.sections = sections;
    }

    @Transactional(readOnly = true)
    public List<Bookmark> list(User user) {
        return bookmarks.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public long count(User user) {
        return bookmarks.countByUser(user);
    }

    @Transactional
    public Bookmark saveSection(User user, Long sectionId) {
        LessonSection s = sections.findById(sectionId).orElseThrow();
        if (bookmarks.existsByUserAndSection(user, s)) {
            return bookmarks.findByUserOrderByCreatedAtDesc(user).stream()
                    .filter(b -> b.getSection() != null && b.getSection().getId().equals(sectionId))
                    .findFirst().orElse(null);
        }
        Bookmark b = new Bookmark();
        b.setUser(user);
        b.setSection(s);
        b.setCategory(categoryFor(s.getType()));
        b.setTitle(s.getTitle() != null ? s.getTitle() : s.getType().getLabel());
        b.setSnippet(truncate(s.getBody(), 220));
        b.setSourceLabel(s.getLesson().getTitle());
        b.setModuleId(s.getLesson().getModule().getId());
        return bookmarks.save(b);
    }

    @Transactional
    public Bookmark saveNote(User user, Long moduleId, String sourceLabel, String text) {
        Bookmark b = new Bookmark();
        b.setUser(user);
        b.setCategory("Ghi chú");
        b.setTitle("Ghi chú của tôi");
        b.setSnippet(truncate(text, 500));
        b.setSourceLabel(sourceLabel);
        b.setModuleId(moduleId);
        return bookmarks.save(b);
    }

    @Transactional
    public void delete(User user, Long id) {
        bookmarks.findById(id)
                .filter(b -> b.getUser().getId().equals(user.getId()))
                .ifPresent(bookmarks::delete);
    }

    private String categoryFor(SectionType type) {
        return switch (type) {
            case VOCAB -> "Từ vựng";
            case GUIDE, FRAMEWORK, EXAMPLE -> "Công cụ";
            default -> "Tác phẩm";
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        s = s.strip();
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
