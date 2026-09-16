package com.pathstudy.service;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.Lesson;
import com.pathstudy.domain.Material;
import com.pathstudy.domain.MaterialType;
import com.pathstudy.repo.LessonRepository;
import com.pathstudy.repo.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MaterialService {

    private final MaterialRepository materials;
    private final LessonRepository lessons;

    public MaterialService(MaterialRepository materials, LessonRepository lessons) {
        this.materials = materials;
        this.lessons = lessons;
    }

    @Transactional(readOnly = true)
    public Optional<Lesson> lessonOf(CourseModule module) {
        return lessons.findFirstByModuleOrderByOrderIndexAsc(module);
    }

    @Transactional(readOnly = true)
    public List<Material> listForModule(CourseModule module) {
        return lessonOf(module)
                .map(materials::findByLessonOrderByOrderIndexAscIdAsc)
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public List<Material> listForLesson(Lesson lesson) {
        return materials.findByLessonOrderByOrderIndexAscIdAsc(lesson);
    }

    @Transactional
    public void add(CourseModule module, MaterialType type, String title, String url, String createdByEmail) {
        Lesson lesson = lessonOf(module).orElseThrow(
                () -> new IllegalStateException("Module chưa có bài học để gắn tài liệu"));
        Material m = new Material();
        m.setLesson(lesson);
        m.setType(type);
        m.setTitle(title.strip());
        m.setUrl(url.strip());
        m.setCreatedByEmail(createdByEmail);
        m.setOrderIndex((int) materials.countByLesson(lesson));
        materials.save(m);
    }

    @Transactional
    public void delete(Long id) {
        materials.deleteById(id);
    }
}
