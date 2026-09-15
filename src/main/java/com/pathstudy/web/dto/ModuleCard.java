package com.pathstudy.web.dto;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.ProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ModuleCard {
    private CourseModule module;
    private ProgressStatus status;
    private int percent;
    private boolean current;
    private boolean locked;
    private String lessonTitle;

    public String getStepLabel() {
        return String.format("%02d", module.getOrderIndex());
    }
}
