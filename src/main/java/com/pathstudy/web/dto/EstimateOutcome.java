package com.pathstudy.web.dto;

import com.pathstudy.domain.CourseModule;
import com.pathstudy.domain.EstimateResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class EstimateOutcome {
    private EstimateResult result;
    private CourseModule module;
    private CourseModule nextModule;
    private boolean passed;
    private int threshold;
    private List<String> weakAreas;
}
