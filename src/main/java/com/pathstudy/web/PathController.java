package com.pathstudy.web;

import com.pathstudy.domain.Enrollment;
import com.pathstudy.domain.Subject;
import com.pathstudy.domain.User;
import com.pathstudy.repo.EnrollmentRepository;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.service.CurrentUserService;
import com.pathstudy.service.StudyPathService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class PathController {

    private final SubjectRepository subjects;
    private final EnrollmentRepository enrollments;
    private final CurrentUserService currentUser;
    private final StudyPathService studyPath;

    public PathController(SubjectRepository subjects, EnrollmentRepository enrollments,
                          CurrentUserService currentUser, StudyPathService studyPath) {
        this.subjects = subjects;
        this.enrollments = enrollments;
        this.currentUser = currentUser;
        this.studyPath = studyPath;
    }

    @GetMapping("/path")
    public String path(@RequestParam(required = false) String subject, Model model) {
        User user = currentUser.require();
        List<Enrollment> myEnrollments = enrollments.findByUserOrderByCreatedAtAsc(user);
        if (myEnrollments.isEmpty()) {
            return "redirect:/subjects";
        }

        Subject subj = (subject != null)
                ? subjects.findByCode(subject).orElse(myEnrollments.get(0).getSubject())
                : myEnrollments.get(0).getSubject();

        // English uses its own hub (choose: study curriculum or take exams).
        if ("anh".equals(subj.getCode())) {
            return "redirect:/english";
        }

        model.addAttribute("summary", studyPath.getPathSummary(user, subj));
        model.addAttribute("enrollments", myEnrollments);
        return "path/index";
    }
}
