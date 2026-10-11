package com.pathstudy.service;

import com.pathstudy.domain.PlacementResult;
import com.pathstudy.domain.Subject;
import com.pathstudy.domain.User;
import com.pathstudy.repo.PlacementResultRepository;
import com.pathstudy.repo.SubjectRepository;
import com.pathstudy.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tái hiện lỗi thật trên production: nộp bài kiểm tra đầu vào xong nhận HTTP 500.
 *
 * <p>Nguyên nhân: kế hoạch học do Gemini soạn (tổng hợp điểm yếu + giáo trình +
 * 5 bài tập kèm đáp án) thường dài hơn 4000 ký tự của cột
 * {@code PlacementResult.studyPlan}, nên lệnh insert vỡ NGAY TRONG
 * {@code PlacementService.grade()} — tức là ở bước nộp bài, trước cả khi kịp
 * hiển thị kết quả. Ở máy dev không thấy vì không có GEMINI_API_KEY, kế hoạch
 * rule-based chỉ dài vài trăm ký tự.
 *
 * <p>Test chạy trên H2 in-memory riêng, không đụng dữ liệu nào.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:placement-length-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seed.version=test-no-seed",
})
class PlacementResultLengthTest {

    @Autowired PlacementResultRepository results;
    @Autowired UserRepository users;
    @Autowired SubjectRepository subjects;

    private PlacementResult newResult(String studyPlan) {
        User u = users.findAll().stream().findFirst().orElseThrow(
                () -> new IllegalStateException("cần ít nhất 1 user từ seed"));
        Subject s = subjects.findAll().stream().findFirst().orElseThrow(
                () -> new IllegalStateException("cần ít nhất 1 subject từ seed"));

        PlacementResult r = new PlacementResult();
        r.setUser(u);
        r.setSubject(s);
        r.setAttemptNo(1);
        r.setScore(50);
        r.setLevel("Trung bình");
        r.setStrengths("");
        r.setWeaknesses("");
        r.setWeakTopics("");
        r.setStudyPlan(studyPlan);
        return r;
    }

    @Test
    @Transactional
    void ke_hoach_dai_hon_cot_thi_insert_vo() {
        // Đây chính là thứ gây 500 trên production.
        String tooLong = "x".repeat(4100);

        assertThatThrownBy(() -> results.saveAndFlush(newResult(tooLong)))
                .as("cột studyPlan là varchar(4000) nên 4100 ký tự PHẢI vỡ — "
                        + "nếu test này xanh thì cột đã được nới và có thể bỏ giới hạn cắt")
                .isInstanceOf(Exception.class);
    }

    @Test
    @Transactional
    void ke_hoach_da_cat_thi_luu_duoc() {
        // Sau khi sửa, PlacementService cắt xuống 3900 trước khi lưu.
        String clamped = "x".repeat(3899) + "…";

        PlacementResult saved = results.saveAndFlush(newResult(clamped));
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStudyPlan()).hasSize(3900);
    }
}
