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
    private final LessonSectionRepository sections;
    private final ExamRepository exams;
    private final PlacementResultRepository placementResults;

    public StudyPathService(EnrollmentRepository enrollments, CourseModuleRepository modules,
                            ModuleProgressRepository progress, LessonRepository lessons,
                            LessonSectionRepository sections, ExamRepository exams,
                            PlacementResultRepository placementResults) {
        this.enrollments = enrollments;
        this.modules = modules;
        this.progress = progress;
        this.lessons = lessons;
        this.sections = sections;
        this.exams = exams;
        this.placementResults = placementResults;
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

    /** Create module-progress rows for a subject, adapting to placement diagnostic if available. */
    @Transactional
    public void ensurePathInitialized(User user, Subject subject) {
        PlacementResult placement = placementResults
                .findTopByUserAndSubjectOrderByScoreDesc(user, subject)
                .orElse(null);
        if (placement != null) {
            adaptPathFromPlacement(user, subject, placement.getGrade(), placement);
            return;
        }

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

    public List<String> parseWeakTopics(String str) {
        if (str == null || str.isBlank()) return List.of();
        return java.util.Arrays.stream(str.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String normalize(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public boolean matchesTopic(CourseModule m, String topic) {
        if (m == null || topic == null || topic.isBlank()) return false;
        String t = normalize(topic);
        String title = normalize(m.getTitle());
        String sub = normalize(m.getSubtitle());
        if (title.contains(t) || t.contains(title)) return true;
        if (!sub.isEmpty() && (sub.contains(t) || t.contains(sub))) return true;

        if (t.contains("thong ke") && (title.contains("so dac trung") || sub.contains("thong ke"))) return true;
        if (t.contains("toa do") && title.contains("toa do")) return true;
        if (t.contains("ham so") && title.contains("ham so")) return true;
        if (t.contains("bat phuong trinh") && title.contains("bat phuong trinh")) return true;
        if (t.contains("menh de") && title.contains("menh de")) return true;
        if (t.contains("tap hop") && title.contains("tap hop")) return true;
        if (t.contains("he thuc luong") && title.contains("he thuc luong")) return true;
        if (t.contains("vecto") && title.contains("vecto")) return true;
        if (t.contains("to hop") && title.contains("to hop")) return true;
        if (t.contains("xac suat") && title.contains("xac suat")) return true;
        return false;
    }

    public boolean isModuleWeak(CourseModule m, List<String> weakTopics) {
        if (weakTopics == null || weakTopics.isEmpty() || m == null) return false;
        for (String wt : weakTopics) {
            if (matchesTopic(m, wt)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tự động tạo và điều chỉnh lộ trình học theo kết quả bài kiểm tra đầu vào:
     * - Học sinh đạt điểm Khá/Giỏi (>=65%): các chương tương ứng với chủ đề đã nắm vững
     *   sẽ được tự động công nhận hoàn thành (miễn giảm lặp lại kiến thức cơ bản),
     *   chương yếu đầu tiên được đặt làm mục tiêu đang học (IN_PROGRESS).
     * - Học sinh Cần cải thiện (<50%): giữ nguyên lộ trình bài bản từ đầu (Chương 01),
     *   đánh dấu cảnh báo các chương hổng kiến thức để học sinh chú ý ôn kỹ.
     */
    @Transactional
    public void adaptPathFromPlacement(User user, Subject subject, String grade, PlacementResult best) {
        List<CourseModule> allMods = modules.findBySubjectOrderByOrderIndexAsc(subject);
        if (allMods.isEmpty()) return;

        Map<Long, ModuleProgress> progressMap = new HashMap<>();
        for (CourseModule m : allMods) {
            ModuleProgress mp = progress.findByUserAndModule(user, m).orElseGet(() -> {
                ModuleProgress p = new ModuleProgress();
                p.setUser(user);
                p.setModule(m);
                p.setStatus(ProgressStatus.LOCKED);
                p.setPercent(0);
                return progress.save(p);
            });
            progressMap.put(m.getId(), mp);
        }

        if (best == null) {
            CourseModule first = allMods.get(0);
            ModuleProgress firstP = progressMap.get(first.getId());
            if (firstP.getStatus() == ProgressStatus.LOCKED) {
                firstP.setStatus(ProgressStatus.IN_PROGRESS);
                progress.save(firstP);
            }
            return;
        }

        int score = best.getScore();
        List<String> weakTopics = parseWeakTopics(best.getWeakTopics());
        String targetGrade = (grade != null && !grade.isBlank())
                ? grade
                : (best.getGrade() != null && !best.getGrade().isBlank()
                        ? best.getGrade()
                        : (user.getGrade() != null ? user.getGrade() : "Lớp 10"));

        List<CourseModule> targetGradeMods = allMods.stream()
                .filter(m -> m.getGrade() == null || m.getGrade().equalsIgnoreCase(targetGrade))
                .toList();

        if (score >= 65) {
            // Học sinh Khá/Giỏi: Miễn giảm các chương đã nắm vững trong khối đã kiểm tra
            for (CourseModule m : targetGradeMods) {
                ModuleProgress mp = progressMap.get(m.getId());
                boolean isWeak = isModuleWeak(m, weakTopics);
                if (!isWeak) {
                    mp.setStatus(ProgressStatus.COMPLETED);
                    mp.setPercent(100);
                    progress.save(mp);
                }
            }

            // Đặt chương chưa hoàn thành đầu tiên làm mục tiêu học hiện tại
            boolean foundActive = false;
            for (CourseModule m : allMods) {
                ModuleProgress mp = progressMap.get(m.getId());
                if (mp.getStatus() == ProgressStatus.COMPLETED) {
                    continue;
                }
                if (!foundActive) {
                    mp.setStatus(ProgressStatus.IN_PROGRESS);
                    progress.save(mp);
                    foundActive = true;
                } else {
                    if (mp.getStatus() == ProgressStatus.IN_PROGRESS) {
                        mp.setStatus(ProgressStatus.LOCKED);
                        progress.save(mp);
                    }
                }
            }
        } else if (score >= 50) {
            // Học sinh Trung bình: Miễn giảm các chương vững trước phần yếu đầu tiên
            boolean reachedWeakness = false;
            for (CourseModule m : targetGradeMods) {
                ModuleProgress mp = progressMap.get(m.getId());
                boolean isWeak = isModuleWeak(m, weakTopics);
                if (!reachedWeakness && !isWeak) {
                    mp.setStatus(ProgressStatus.COMPLETED);
                    mp.setPercent(100);
                    progress.save(mp);
                } else {
                    reachedWeakness = true;
                }
            }
            boolean foundActive = false;
            for (CourseModule m : allMods) {
                ModuleProgress mp = progressMap.get(m.getId());
                if (mp.getStatus() == ProgressStatus.COMPLETED) {
                    continue;
                }
                if (!foundActive) {
                    mp.setStatus(ProgressStatus.IN_PROGRESS);
                    progress.save(mp);
                    foundActive = true;
                } else {
                    if (mp.getStatus() == ProgressStatus.IN_PROGRESS) {
                        mp.setStatus(ProgressStatus.LOCKED);
                        progress.save(mp);
                    }
                }
            }
        } else {
            // Học sinh Cần cải thiện (<50%): Học từ đầu, tập trung nền tảng
            for (int i = 0; i < allMods.size(); i++) {
                CourseModule m = allMods.get(i);
                ModuleProgress mp = progressMap.get(m.getId());
                if (i == 0) {
                    if (mp.getStatus() != ProgressStatus.COMPLETED) {
                        mp.setStatus(ProgressStatus.IN_PROGRESS);
                        progress.save(mp);
                    }
                } else {
                    if (mp.getStatus() != ProgressStatus.COMPLETED) {
                        mp.setStatus(ProgressStatus.LOCKED);
                        progress.save(mp);
                    }
                }
            }
        }
    }

    @Transactional
    public PathSummary getPathSummary(User user, Subject subject) {
        PlacementResult placement = placementResults
                .findTopByUserAndSubjectOrderByScoreDesc(user, subject)
                .orElse(null);

        // Đảm bảo module progress đã được khởi tạo
        ensurePathInitialized(user, subject);

        // Tự động thích ứng nếu đã có bài test điểm Khá/Giỏi nhưng tiến độ chưa được miễn giảm
        if (placement != null && placement.getScore() >= 65) {
            long existingCompleted = progress.findByUser(user).stream()
                    .filter(mp -> mp.getModule().getSubject().getId().equals(subject.getId())
                            && mp.getStatus() == ProgressStatus.COMPLETED)
                    .count();
            if (existingCompleted == 0) {
                adaptPathFromPlacement(user, subject, placement.getGrade(), placement);
            }
        }

        List<CourseModule> mods = modules.findBySubjectOrderByOrderIndexAsc(subject);

        // Đề của cả môn, nạp 1 lần rồi lọc theo khối của từng chương (tránh N+1).
        // Bỏ đề THPT (category != null) vì đó là mục riêng, không gắn chương.
        List<Exam> subjectExams = exams.findBySubjectOrderByOrderIndexAsc(subject).stream()
                .filter(x -> x.getCategory() == null)
                .toList();
        Map<Long, ModuleProgress> byModule = new HashMap<>();
        for (ModuleProgress mp : progress.findByUser(user)) {
            if (mp.getModule().getSubject().getId().equals(subject.getId())) {
                byModule.put(mp.getModule().getId(), mp);
            }
        }

        List<String> weakTopics = placement != null ? parseWeakTopics(placement.getWeakTopics()) : List.of();
        List<String> masteredTopics = new ArrayList<>();

        List<ModuleCard> cards = new ArrayList<>();
        int sumPercent = 0;
        int completed = 0;
        int fastTracked = 0;
        CourseModule current = null;
        String currentLesson = null;

        for (CourseModule m : mods) {
            ModuleProgress mp = byModule.get(m.getId());
            ProgressStatus status = mp != null ? mp.getStatus() : ProgressStatus.LOCKED;
            int pct = mp != null ? mp.getPercent() : 0;
            boolean locked = status == ProgressStatus.LOCKED;
            boolean isCurrent = status == ProgressStatus.IN_PROGRESS;
            Lesson first = lessons.findFirstByModuleOrderByOrderIndexAsc(m).orElse(null);
            String lessonTitle = first != null ? first.getTitle() : null;

            List<String> sectionTitles = first == null ? List.of()
                    : sections.findByLessonOrderByOrderIndexAsc(first).stream()
                            .map(LessonSection::getTitle)
                            .filter(t -> t != null && !t.isBlank())
                            .toList();
            List<Exam> moduleExams = subjectExams.stream()
                    .filter(x -> x.getGrade() == null || x.getGrade().equals(m.getGrade()))
                    .toList();

            boolean isWeak = isModuleWeak(m, weakTopics);
            boolean isMastered = false;
            String badge = null;
            String badgeClass = null;

            if (placement != null && placement.getScore() >= 65 && !isWeak && status == ProgressStatus.COMPLETED) {
                isMastered = true;
                fastTracked++;
                badge = "✓ Nắm vững qua bài test";
                badgeClass = "pill-done";
                if (!masteredTopics.contains(m.getTitle())) {
                    masteredTopics.add(m.getTitle());
                }
            } else if (isWeak) {
                if (isCurrent) {
                    badge = (placement != null && placement.getScore() >= 65)
                            ? "🎯 Trọng tâm AI chỉ định"
                            : "🎯 Đang học (Cần ôn kỹ)";
                    badgeClass = "pill-warn";
                } else {
                    badge = (placement != null && placement.getScore() >= 65)
                            ? "⚠️ Trọng tâm cần cải thiện"
                            : "⚠️ Cần củng cố";
                    badgeClass = "pill-warn";
                }
            }

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
            cards.add(new ModuleCard(m, status, pct, isCurrent, locked, lessonTitle,
                    sectionTitles, moduleExams, isWeak, isMastered, badge, badgeClass));
        }

        // Fallback: nếu chưa có module nào IN_PROGRESS nhưng chưa hoàn thành hết lộ trình,
        // lấy module chưa hoàn thành đầu tiên làm bước tiếp theo
        if (current == null && !mods.isEmpty() && completed < mods.size()) {
            for (CourseModule m : mods) {
                ModuleProgress mp = byModule.get(m.getId());
                ProgressStatus status = mp != null ? mp.getStatus() : ProgressStatus.LOCKED;
                if (status != ProgressStatus.COMPLETED) {
                    current = m;
                    Lesson first = lessons.findFirstByModuleOrderByOrderIndexAsc(m).orElse(null);
                    currentLesson = first != null ? first.getTitle() : null;
                    break;
                }
            }
        }

        int overall = mods.isEmpty() ? 0 : Math.round(sumPercent / (float) mods.size());
        Enrollment e = enrollments.findByUserAndSubject(user, subject).orElse(null);
        String level = e != null ? e.getLevel() : (placement != null ? placement.getLevel() : null);
        boolean hasPlacement = level != null || placement != null;

        // Xây dựng thông điệp lộ trình thích ứng AI
        String headline = null;
        String subtext = null;
        if (placement != null) {
            int sc = placement.getScore();
            if (sc >= 65) {
                headline = "Lộ trình học thích ứng bứt phá (AI Optimized)";
                subtext = "Dựa trên kết quả bài kiểm tra (" + sc + "/100 · " + placement.getLevel()
                        + "), AI đã tự động xác nhận hoàn thành " + fastTracked
                        + " chương bạn đã nắm vững và định tuyến lộ trình để bạn bắt đầu ngay từ "
                        + (current != null ? current.getTitle() : "chương mục tiêu")
                        + " nhằm tối ưu hoá thời gian học tập.";
            } else if (sc >= 50) {
                headline = "Lộ trình học thích ứng tiêu chuẩn (AI Balanced)";
                subtext = "Dựa trên kết quả bài kiểm tra (" + sc + "/100 · " + placement.getLevel()
                        + "), AI đã tối ưu hoá lộ trình để củng cố các phần còn khuyết và hướng dẫn bạn chinh phục từng chương tiếp theo.";
            } else {
                headline = "Lộ trình học củng cố nền tảng (AI Foundation)";
                subtext = "Dựa trên kết quả bài kiểm tra (" + sc + "/100 · " + placement.getLevel()
                        + "), AI phát hiện bạn còn hổng nhiều kiến thức trọng tâm. Lộ trình được thiết lập bắt đầu từ "
                        + (current != null ? current.getTitle() : "Chương 01")
                        + " để bạn học chắc lý thuyết và làm bài tập cơ bản trước khi chuyển sang phần nâng cao.";
            }
        }

        Integer pScore = placement != null ? placement.getScore() : null;
        String studyPlan = placement != null ? placement.getStudyPlan() : null;

        return new PathSummary(subject, level, hasPlacement, overall, completed,
                mods.size(), cards, current, currentLesson, pScore, weakTopics,
                masteredTopics, studyPlan, headline, subtext, fastTracked);
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
    public ProgressStatus statusOf(User user, CourseModule module) {
        return progress.findByUserAndModule(user, module)
                .map(ModuleProgress::getStatus).orElse(ProgressStatus.LOCKED);
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
