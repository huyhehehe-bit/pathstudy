package com.pathstudy.service;

import com.pathstudy.domain.*;
import com.pathstudy.repo.*;
import com.pathstudy.web.dto.PathSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:adaptive-path-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
})
class AdaptiveStudyPathTest {

    @Autowired StudyPathService studyPathService;
    @Autowired UserRepository users;
    @Autowired SubjectRepository subjects;
    @Autowired CourseModuleRepository modules;
    @Autowired PlacementResultRepository placementResults;
    @Autowired ModuleProgressRepository moduleProgress;

    @Test
    @Transactional
    void lo_trinh_phan_hoa_ro_ret_giua_hai_tai_khoan() {
        Subject toan = subjects.findByCode("toan").orElseThrow();

        // 1. Tài khoản Yen: 20% Cần cải thiện
        User yen = new User();
        yen.setEmail("yen@test.com");
        yen.setFullName("Yen");
        yen.setPasswordHash("pass");
        yen.setRole("STUDENT");
        yen.setGrade("Lớp 10");
        users.save(yen);

        PlacementResult yenResult = new PlacementResult();
        yenResult.setUser(yen);
        yenResult.setSubject(toan);
        yenResult.setGrade("Lớp 10");
        yenResult.setAttemptNo(1);
        yenResult.setScore(20);
        yenResult.setLevel("Cần cải thiện");
        yenResult.setWeakTopics("Mệnh đề, Tập hợp, Bất phương trình, Vectơ, Hàm số bậc hai, Toạ độ phẳng");
        yenResult.setStudyPlan("Ôn lại từ đầu các kiến thức cơ bản...");
        placementResults.save(yenResult);

        studyPathService.adaptPathFromPlacement(yen, toan, "Lớp 10", yenResult);
        PathSummary yenSummary = studyPathService.getPathSummary(yen, toan);

        // Kiểm tra tài khoản Yen: Bắt đầu từ chương 1, hoàn thành 0 module
        assertThat(yenSummary.getCompletedCount()).isEqualTo(0);
        assertThat(yenSummary.getCurrentModule().getOrderIndex()).isEqualTo(1);
        assertThat(yenSummary.getCurrentModule().getTitle()).contains("Mệnh đề");
        assertThat(yenSummary.getAiRoadmapHeadline()).contains("nền tảng");

        // 2. Tài khoản Tran Van A: 79% Khá
        User tranVanA = new User();
        tranVanA.setEmail("tranvana@test.com");
        tranVanA.setFullName("Tran Van A");
        tranVanA.setPasswordHash("pass");
        tranVanA.setRole("STUDENT");
        tranVanA.setGrade("Lớp 10");
        users.save(tranVanA);

        PlacementResult tranVanAResult = new PlacementResult();
        tranVanAResult.setUser(tranVanA);
        tranVanAResult.setSubject(toan);
        tranVanAResult.setGrade("Lớp 10");
        tranVanAResult.setAttemptNo(1);
        tranVanAResult.setScore(79);
        tranVanAResult.setLevel("Khá");
        tranVanAResult.setWeakTopics("Hàm số bậc hai, Toạ độ phẳng");
        tranVanAResult.setStudyPlan("Tập trung ôn các chủ đề còn yếu: Hàm số bậc hai, Toạ độ phẳng...");
        placementResults.save(tranVanAResult);

        studyPathService.adaptPathFromPlacement(tranVanA, toan, "Lớp 10", tranVanAResult);
        PathSummary aSummary = studyPathService.getPathSummary(tranVanA, toan);

        // Kiểm tra tài khoản Tran Van A:
        // Đã được tự động miễn giảm các chương đã nắm vững (> 0 module)
        assertThat(aSummary.getCompletedCount()).isGreaterThan(0);
        assertThat(aSummary.getFastTrackedCount()).isGreaterThan(0);
        // Mục tiêu đang học được đưa thẳng tới chương yếu đầu tiên (Hàm số và đồ thị) thay vì bắt học lại Chương 1
        assertThat(aSummary.getCurrentModule().getTitle()).contains("Hàm số");
        assertThat(aSummary.getAiRoadmapHeadline()).contains("bứt phá");

        // KHÁC BIỆT RÕ RỆT GIỮA 2 TÀI KHOẢN:
        assertThat(aSummary.getCompletedCount()).isNotEqualTo(yenSummary.getCompletedCount());
        assertThat(aSummary.getCurrentModule().getId()).isNotEqualTo(yenSummary.getCurrentModule().getId());
        assertThat(aSummary.getOverallPercent()).isGreaterThan(yenSummary.getOverallPercent());
    }
}
