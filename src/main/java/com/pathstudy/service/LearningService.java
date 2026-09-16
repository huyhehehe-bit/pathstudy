package com.pathstudy.service;

import com.pathstudy.domain.*;
import com.pathstudy.repo.*;
import com.pathstudy.web.dto.ModuleDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class LearningService {

    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final LessonSectionRepository sections;
    private final BookmarkRepository bookmarks;
    private final ModuleProgressRepository progress;
    private final EstimateService estimateService;
    private final StudyPathService studyPath;

    public LearningService(CourseModuleRepository modules, LessonRepository lessons,
                           LessonSectionRepository sections, BookmarkRepository bookmarks,
                           ModuleProgressRepository progress, EstimateService estimateService,
                           StudyPathService studyPath) {
        this.modules = modules;
        this.lessons = lessons;
        this.sections = sections;
        this.bookmarks = bookmarks;
        this.progress = progress;
        this.estimateService = estimateService;
        this.studyPath = studyPath;
    }

    @Transactional(readOnly = true)
    public CourseModule module(Long id) {
        return modules.findById(id).orElseThrow();
    }

    @Transactional
    public ModuleDetail openModule(User user, Long moduleId) {
        CourseModule module = modules.findById(moduleId).orElseThrow();

        Optional<Lesson> lessonOpt = lessons.findFirstByModuleOrderByOrderIndexAsc(module);
        List<LessonSection> secs = lessonOpt
                .map(sections::findByLessonOrderByOrderIndexAsc)
                .orElseGet(List::of);

        Set<Long> bookmarkedIds = new HashSet<>();
        for (Bookmark b : bookmarks.findByUserOrderByCreatedAtDesc(user)) {
            if (b.getSection() != null) {
                bookmarkedIds.add(b.getSection().getId());
            }
        }

        // Read-only: opening a lesson never fabricates progress.
        // Real progress comes from the estimate test (see EstimateService).
        ProgressStatus status = progress.findByUserAndModule(user, module)
                .map(ModuleProgress::getStatus).orElse(ProgressStatus.LOCKED);

        boolean hasEstimate = estimateService.hasEstimate(module);
        EstimateResult last = estimateService.latest(user, module).orElse(null);
        CourseModule next = studyPath.nextModule(module);

        return new ModuleDetail(module, lessonOpt.orElse(null), secs, bookmarkedIds,
                hasEstimate, status, last, next);
    }
}
