package com.pathstudy.service;

import com.pathstudy.domain.*;
import com.pathstudy.repo.*;
import com.pathstudy.web.dto.ModuleCard;
import com.pathstudy.web.dto.PathSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudyPathService {

    private final EnrollmentRepository enrollments;
    private final CourseModuleRepository modules;
    private final ModuleProgressRepository progress;
    private final LessonRepository lessons;

    public StudyPathService(EnrollmentRepository enrollments, CourseModuleRepository modules,
                            ModuleProgressRepository progress, LessonRepository lessons) {
        this.enrollments = enrollments;
        this.modules = modules;
        this.progress = progress;
        this.lessons = lessons;
    }

    @Transactional
    public Enrollment enroll(User user, Subject subject, boolean primary) {
        return enrollments.findByUserAndSubject(user, subject).orElseGet(() -> {
            Enrollment e = new Enrollment();
            e.setUser(user);
            e.setSubject(subject);
            e.setPrimarySubject(primary);
            return enrollments.save(e);
        });
    }

    /** Create module-progress rows for a subject, unlocking the first module. */
    @Transactional
    public void ensurePathInitialized(User user, Subject subject) {
        List<CourseModule> mods = modules.findBySubjectOrderByOrderIndexAsc(subject);
        for (int i = 0; i < mods.size(); i++) {
            final int idx = i;
            CourseModule m = mods.get(i);
            progress.findByUserAndModule(user, m).orElseGet(() -> {
                ModuleProgress mp = new ModuleProgress();
                mp.setUser(user);
                mp.setModule(m);
                mp.setStatus(idx == 0 ? ProgressStatus.IN_PROGRESS : ProgressStatus.LOCKED);
                mp.setPercent(0);
                return progress.save(mp);
            });
        }
    }

    @Transactional(readOnly = true)
    public PathSummary getPathSummary(User user, Subject subject) {
        List<CourseModule> mods = modules.findBySubjectOrderByOrderIndexAsc(subject);
        Map<Long, ModuleProgress> byModule = new HashMap<>();
        for (ModuleProgress mp : progress.findByUser(user)) {
            byModule.put(mp.getModule().getId(), mp);
        }

        List<ModuleCard> cards = new ArrayList<>();
        int sumPercent = 0;
        int completed = 0;
        CourseModule current = null;
        String currentLesson = null;

        for (CourseModule m : mods) {
            ModuleProgress mp = byModule.get(m.getId());
            ProgressStatus status = mp != null ? mp.getStatus() : ProgressStatus.LOCKED;
            int pct = mp != null ? mp.getPercent() : 0;
            boolean locked = status == ProgressStatus.LOCKED;
            boolean isCurrent = status == ProgressStatus.IN_PROGRESS;
            String lessonTitle = lessons.findFirstByModuleOrderByOrderIndexAsc(m)
                    .map(Lesson::getTitle).orElse(null);

            if (isCurrent && current == null) {
                current = m;
                currentLesson = lessonTitle;
            }
            if (status == ProgressStatus.COMPLETED) {
                completed++;
                sumPercent += 100;
            } else {
                sumPercent += pct;
            }
            cards.add(new ModuleCard(m, status, pct, isCurrent, locked, lessonTitle));
        }

        int overall = mods.isEmpty() ? 0 : Math.round(sumPercent / (float) mods.size());
        Enrollment e = enrollments.findByUserAndSubject(user, subject).orElse(null);
        String level = e != null ? e.getLevel() : null;
        boolean hasPlacement = level != null;

        return new PathSummary(subject, level, hasPlacement, overall, completed,
                mods.size(), cards, current, currentLesson);
    }

    @Transactional
    public CourseModule completeModule(User user, CourseModule module) {
        ModuleProgress mp = getOrCreate(user, module);
        mp.setStatus(ProgressStatus.COMPLETED);
        mp.setPercent(100);
        progress.save(mp);

        CourseModule next = nextModule(module);
        if (next != null) {
            ModuleProgress np = getOrCreate(user, next);
            if (np.getStatus() == ProgressStatus.LOCKED) {
                np.setStatus(ProgressStatus.IN_PROGRESS);
                progress.save(np);
            }
        }
        return next;
    }

    @Transactional
    public void touchModuleProgress(User user, CourseModule module, int percent) {
        ModuleProgress mp = getOrCreate(user, module);
        if (mp.getStatus() == ProgressStatus.COMPLETED) {
            return;
        }
        if (mp.getStatus() == ProgressStatus.LOCKED) {
            mp.setStatus(ProgressStatus.IN_PROGRESS);
        }
        if (percent > mp.getPercent()) {
            mp.setPercent(Math.min(percent, 99));
        }
        progress.save(mp);
    }

    @Transactional(readOnly = true)
    public CourseModule nextModule(CourseModule module) {
        List<CourseModule> mods = modules.findBySubjectOrderByOrderIndexAsc(module.getSubject());
        for (int i = 0; i < mods.size(); i++) {
            if (mods.get(i).getId().equals(module.getId()) && i + 1 < mods.size()) {
                return mods.get(i + 1);
            }
        }
        return null;
    }

    @Transactional
    public ModuleProgress getOrCreate(User user, CourseModule module) {
        return progress.findByUserAndModule(user, module).orElseGet(() -> {
            ModuleProgress mp = new ModuleProgress();
            mp.setUser(user);
            mp.setModule(module);
            mp.setStatus(ProgressStatus.LOCKED);
            mp.setPercent(0);
            return progress.save(mp);
        });
    }
}
