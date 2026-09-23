package com.pathstudy.config;

import com.pathstudy.domain.*;
import com.pathstudy.repo.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Seeds demo content. Keyed on {@link #SEED_VERSION}: when the version changes,
 * existing content + learning progress is wiped and re-seeded (registered user
 * accounts are kept). This lets deployed environments pick up new curriculum
 * content on the next start. Bump SEED_VERSION whenever seeded content changes.
 *
 * Ngữ văn is organised by grade (Lớp 10 → 11 → 12); each work is one stage of
 * the personalized study path.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String SEED_VERSION = "2026-09-23-exams-hk-10-11";

    @PersistenceContext
    private EntityManager entityManager;

    private final SubjectRepository subjects;
    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final LessonSectionRepository sections;
    private final QuestionRepository questions;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AppSettingRepository appSettings;
    private final EnrollmentRepository enrollments;
    private final ModuleProgressRepository moduleProgress;
    private final PlacementResultRepository placementResults;
    private final EstimateResultRepository estimateResults;
    private final BookmarkRepository bookmarks;
    private final MaterialRepository materials;
    private final ReferenceMaterialRepository referenceMaterials;
    private final ExamRepository exams;
    private final EnglishLessonRepository englishLessons;

    private int order = 0; // running module order within a subject

    public DataSeeder(SubjectRepository subjects, CourseModuleRepository modules,
                      LessonRepository lessons, LessonSectionRepository sections,
                      QuestionRepository questions, UserRepository users,
                      PasswordEncoder passwordEncoder, AppSettingRepository appSettings,
                      EnrollmentRepository enrollments, ModuleProgressRepository moduleProgress,
                      PlacementResultRepository placementResults, EstimateResultRepository estimateResults,
                      BookmarkRepository bookmarks, MaterialRepository materials,
                      ReferenceMaterialRepository referenceMaterials, ExamRepository exams,
                      EnglishLessonRepository englishLessons) {
        this.subjects = subjects;
        this.modules = modules;
        this.lessons = lessons;
        this.sections = sections;
        this.questions = questions;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.appSettings = appSettings;
        this.enrollments = enrollments;
        this.moduleProgress = moduleProgress;
        this.placementResults = placementResults;
        this.estimateResults = estimateResults;
        this.bookmarks = bookmarks;
        this.materials = materials;
        this.referenceMaterials = referenceMaterials;
        this.exams = exams;
        this.englishLessons = englishLessons;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String current = appSettings.findById("seedVersion").map(AppSetting::getValue).orElse(null);
        if (SEED_VERSION.equals(current)) {
            return;
        }
        wipeContent();
        seedSubjects();
        seedUsers();
        seedVanContent(subjects.findByCode("van").orElseThrow());
        seedEnglish(subjects.findByCode("anh").orElseThrow());
        appSettings.save(new AppSetting("seedVersion", SEED_VERSION));
    }

    /** Removes seeded content and learning progress (FK-safe order). Keeps user accounts. */
    private void wipeContent() {
        bookmarks.deleteAll();
        estimateResults.deleteAll();
        placementResults.deleteAll();
        moduleProgress.deleteAll();
        enrollments.deleteAll();
        materials.deleteAll();
        referenceMaterials.deleteAll();
        questions.deleteAll();
        exams.deleteAll();
        englishLessons.deleteAll();
        sections.deleteAll();
        lessons.deleteAll();
        modules.deleteAll();
        subjects.deleteAll();
        // Force the deletes to hit the DB now. Otherwise Hibernate defers them and,
        // within one transaction, executes the seed INSERTs before these DELETEs,
        // causing a duplicate-key error when data already exists (e.g. on redeploy).
        entityManager.flush();
    }

    private void seedSubjects() {
        subject("van", "Ngữ văn", "book", "indigo", 1, true,
                "Đọc hiểu, phân tích tác phẩm và nghị luận theo chương trình THPT (lớp 10–11–12).");
        subject("anh", "Tiếng Anh", "flag", "rose", 2, true,
                "Kiểm tra chẩn đoán, phân tích điểm yếu và lộ trình ôn tập cá nhân hoá (Lớp 10–11–12).");
        subject("toan", "Toán", "calculator", "sky", 3, false, "Đại số, hình học và luyện đề THPT. Sắp ra mắt.");
        subject("ly", "Vật lý", "atom", "violet", 4, false, "Cơ, điện, quang và luyện đề. Sắp ra mắt.");
        subject("hoa", "Hóa học", "flask", "amber", 5, false, "Hoá vô cơ, hữu cơ và bài tập. Sắp ra mắt.");
        subject("sinh", "Sinh học", "leaf", "green", 6, false, "Di truyền, sinh thái và luyện đề. Sắp ra mắt.");
    }

    private void seedUsers() {
        user("Nguyễn An", "demo@pathstudy.vn", "123456", "STUDENT", "Lớp 10");
        user("Cô Lan (Giáo viên)", "teacher@pathstudy.vn", "teacher123", "TEACHER", null);
        user("Quản trị viên", "admin@pathstudy.vn", "admin123", "ADMIN", null);
    }

    private void user(String name, String email, String rawPassword, String role, String grade) {
        User u = users.findByEmail(email).orElse(null);
        if (u == null) {
            u = new User();
            u.setFullName(name);
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(rawPassword));
            u.setRole(role);
        }
        // Gán khối cho tài khoản demo (kể cả tài khoản cũ đã tồn tại) nếu chưa có.
        if (grade != null && u.getGrade() == null) {
            u.setGrade(grade);
        }
        users.save(u);
    }

    private void seedVanContent(Subject van) {
        order = 0;

        // ---------- Nền tảng ----------
        CourseModule base = module(van, "Nền tảng", "Nền tảng",
                "Đọc hiểu · Thể loại · Nghệ thuật cơ bản", "layers", 60);
        Lesson bl = lesson(base, "Kỹ năng đọc hiểu văn bản", null, "Kỹ năng", 30,
                "Đọc hiểu là bước đầu tiên: nắm nội dung, nhận diện thể loại và các tín hiệu nghệ thuật trước khi phân tích sâu.",
                "Trang bị kỹ năng đọc hiểu cốt lõi làm nền cho việc phân tích tác phẩm.");
        section(bl, 1, SectionType.READING, "Đọc & hiểu văn bản", """
                Đọc hiểu gồm ba lớp: nghĩa bề mặt (văn bản nói gì), nghĩa hàm ẩn (tác giả gửi gắm điều gì) \
                và tín hiệu nghệ thuật (từ ngữ, hình ảnh, nhịp điệu tạo ra ý nghĩa đó). Khi đọc, hãy gạch chân \
                từ khoá, xác định mạch cảm xúc và luôn hỏi "vì sao tác giả viết như vậy".""");
        section(bl, 2, SectionType.VOCAB, "Thuật ngữ cơ bản", """
                • Hình ảnh: sự vật, hiện tượng được gợi tả. • Biểu tượng: hình ảnh mang nghĩa khái quát, vượt nghĩa đen.
                • Nhịp điệu: cách ngắt nhịp, gieo vần tạo âm hưởng. • Mạch cảm xúc: dòng chảy tình cảm xuyên suốt tác phẩm.""");
        section(bl, 3, SectionType.GUIDE, "Cách tiếp cận một tác phẩm", """
                B1: Đọc toàn bộ, nắm chủ đề. B2: Xác định thể loại và đặc trưng. B3: Chia bố cục, đặt tên từng phần.
                B4: Tìm hình ảnh/biện pháp nổi bật và giải mã ý nghĩa. B5: Khái quát tư tưởng, tình cảm của tác giả.""");
        eq(base, 1, "Đọc hiểu một văn bản gồm mấy lớp nghĩa cơ bản?", Competency.KNOWLEDGE, 2,
                "Một", "Hai", "Ba", "Bốn");
        eq(base, 2, "\"Biểu tượng\" khác \"hình ảnh\" ở điểm nào?", Competency.KNOWLEDGE, 1,
                "Không khác gì", "Biểu tượng mang nghĩa khái quát, vượt nghĩa đen", "Biểu tượng ngắn hơn", "Hình ảnh chỉ có trong thơ");
        eq(base, 3, "Khi phân tích một khổ thơ, bước hợp lý đầu tiên là:", Competency.ANALYSIS, 1,
                "Chép lại toàn bộ khổ thơ", "Nêu câu chủ đề khái quát nội dung khổ", "Kể tiểu sử tác giả", "Viết cảm nghĩ dài dòng");
        eq(base, 4, "Để nắm mạch cảm xúc của bài thơ, người đọc nên:", Competency.APPLICATION, 0,
                "Theo dõi sự thay đổi tình cảm qua từng khổ", "Đếm số câu", "Chỉ đọc khổ cuối", "Bỏ qua các hình ảnh");

        // ==================== LỚP 10 ====================
        // --- Bình Ngô đại cáo ---
        CourseModule m1 = module(van, "Lớp 10", "Bình Ngô đại cáo", "Nguyễn Trãi · Nghị luận trung đại", "pen", 60);
        Lesson l1 = lesson(m1, "Bình Ngô đại cáo", "Nguyễn Trãi", "Cáo (nghị luận)", 45, """
                Việc nhân nghĩa cốt ở yên dân,
                Quân điếu phạt trước lo trừ bạo.
                                        — Nguyễn Trãi, trích "Bình Ngô đại cáo" (1428)""",
                "Áng \"thiên cổ hùng văn\" tổng kết cuộc kháng chiến chống Minh và tuyên bố nền độc lập.");
        section(l1, 1, SectionType.READING, "Đọc & hiểu tác phẩm", """
                Bình Ngô đại cáo do Nguyễn Trãi thừa lệnh Lê Lợi viết đầu năm 1428 sau đại thắng quân Minh. \
                Bài cáo nêu tư tưởng nhân nghĩa, khẳng định chủ quyền dân tộc, tố cáo tội ác giặc, thuật lại quá \
                trình kháng chiến gian khổ mà anh dũng và trịnh trọng tuyên bố chiến thắng, mở ra kỉ nguyên độc lập.""");
        section(l1, 2, SectionType.THEME, "Chủ đề & nội dung", """
                Tư tưởng cốt lõi là nhân nghĩa gắn với "yên dân" và độc lập dân tộc. Tác phẩm khẳng định Đại Việt \
                là quốc gia có nền văn hiến, lãnh thổ, phong tục, lịch sử và hào kiệt riêng, bình đẳng với phương Bắc.""");
        section(l1, 3, SectionType.TECHNIQUE, "Nghệ thuật", """
                Thể cáo với lối văn biền ngẫu, giọng điệu hùng hồn, đanh thép. Nghệ thuật liệt kê, đối lập ta – địch, \
                kết hợp lí lẽ sắc bén với dẫn chứng lịch sử, giàu cảm xúc và sức thuyết phục.""");
        section(l1, 4, SectionType.CONTEXT, "Bối cảnh", """
                Nguyễn Trãi (1380–1442) là anh hùng dân tộc, danh nhân văn hoá. Ông là quân sư của Lê Lợi trong \
                khởi nghĩa Lam Sơn; Bình Ngô đại cáo được xem là bản "tuyên ngôn độc lập" thứ hai của dân tộc.""");
        eq(m1, 1, "\"Bình Ngô đại cáo\" thuộc thể loại nào?", Competency.KNOWLEDGE, 1, "Hịch", "Cáo", "Chiếu", "Phú");
        eq(m1, 2, "Tác giả của tác phẩm là ai?", Competency.KNOWLEDGE, 2, "Nguyễn Du", "Nguyễn Dữ", "Nguyễn Trãi", "Lê Lợi");
        eq(m1, 3, "Tư tưởng chủ đạo xuyên suốt tác phẩm là:", Competency.ANALYSIS, 0,
                "Nhân nghĩa, yên dân", "Cầu tài", "Ẩn dật", "Hưởng lạc");
        eq(m1, 4, "Vì sao Bình Ngô đại cáo được coi là bản tuyên ngôn độc lập?", Competency.APPLICATION, 1,
                "Vì kể chuyện chiến trận", "Vì khẳng định chủ quyền, văn hiến và độc lập của Đại Việt", "Vì ca ngợi vua", "Vì tả cảnh");

        // --- Truyện Kiều: Trao duyên ---
        CourseModule m2 = module(van, "Lớp 10", "Truyện Kiều – Trao duyên", "Nguyễn Du · Thơ Nôm (lục bát)", "book", 60);
        Lesson l2 = lesson(m2, "Truyện Kiều – Trao duyên", "Nguyễn Du", "Thơ Nôm", 45, """
                Cậy em em có chịu lời,
                Ngồi lên cho chị lạy rồi sẽ thưa.
                                        — Nguyễn Du, trích "Trao duyên" (Truyện Kiều)""",
                "Thúy Kiều nhờ em Thúy Vân thay mình trả nghĩa cho Kim Trọng — bi kịch tình yêu tan vỡ.");
        section(l2, 1, SectionType.READING, "Đọc & hiểu tác phẩm", """
                Đoạn trích thuộc phần "Gia biến và lưu lạc". Trước khi bán mình chuộc cha, Kiều cậy nhờ Thúy Vân \
                nối duyên với Kim Trọng. Đoạn thơ diễn tả tâm trạng giằng xé giữa lí trí (trả nghĩa) và tình cảm \
                (không nỡ dứt tình) của Kiều trong giờ phút trao duyên đầy nước mắt.""");
        section(l2, 2, SectionType.THEME, "Chủ đề & nội dung", """
                Bi kịch tình yêu và thân phận người phụ nữ trong xã hội phong kiến. Qua đó, Nguyễn Du thể hiện \
                tấm lòng nhân đạo sâu sắc: trân trọng khát vọng hạnh phúc và xót thương cho số phận con người.""");
        section(l2, 3, SectionType.TECHNIQUE, "Nghệ thuật", """
                Thể thơ lục bát điêu luyện, ngôn ngữ độc thoại nội tâm tinh tế. Nghệ thuật miêu tả tâm lí bậc thầy; \
                cách dùng từ đắt giá ("cậy", "chịu", "lạy") diễn tả vừa nỗi đau vừa sự nài xin trân trọng.""");
        section(l2, 4, SectionType.CONTEXT, "Bối cảnh", """
                Nguyễn Du (1765–1820) là đại thi hào dân tộc, danh nhân văn hoá thế giới. Truyện Kiều \
                (Đoạn trường tân thanh) gồm 3254 câu lục bát, là kiệt tác của văn học trung đại Việt Nam.""");
        eq(m2, 1, "Đoạn \"Trao duyên\" được viết theo thể thơ nào?", Competency.KNOWLEDGE, 1,
                "Song thất lục bát", "Lục bát", "Thất ngôn", "Tự do");
        eq(m2, 2, "Kiều trao duyên cho ai?", Competency.KNOWLEDGE, 2, "Kim Trọng", "Thúc Sinh", "Thúy Vân", "Từ Hải");
        eq(m2, 3, "Từ \"cậy\", \"lạy\" thể hiện điều gì trong tâm trạng Kiều?", Competency.ANALYSIS, 0,
                "Sự nài xin trân trọng và đau đớn", "Sự giận dữ", "Sự vui mừng", "Sự thờ ơ");
        eq(m2, 4, "Giá trị nổi bật nhất của đoạn trích là:", Competency.APPLICATION, 1,
                "Tả cảnh thiên nhiên", "Giá trị nhân đạo và nghệ thuật miêu tả tâm lí", "Kể chuyện chiến trận", "Bàn về đạo lí");

        // --- Chuyện chức phán sự đền Tản Viên ---
        CourseModule m3 = module(van, "Lớp 10", "Chuyện chức phán sự đền Tản Viên", "Nguyễn Dữ · Truyện truyền kì", "sparkles", 60);
        Lesson l3 = lesson(m3, "Chuyện chức phán sự đền Tản Viên", "Nguyễn Dữ", "Truyền kì", 40,
                "Ngô Tử Văn — kẻ sĩ cương trực, dám đốt đền tà trừ hại cho dân, được minh oan và nhận chức phán sự.",
                "Truyện truyền kì ca ngợi tinh thần khảng khái, chính nghĩa của kẻ sĩ nước Việt.");
        section(l3, 1, SectionType.READING, "Đọc & hiểu tác phẩm", """
                Ngô Tử Văn là kẻ sĩ nóng nảy nhưng cương trực. Chàng đốt ngôi đền bị hồn tên tướng giặc bại trận \
                chiếm giữ, tác quái hại dân. Bị kiện xuống âm phủ, Tử Văn vẫn cứng cỏi vạch tội, cuối cùng được \
                minh oan và được tiến cử giữ chức phán sự đền Tản Viên.""");
        section(l3, 2, SectionType.THEME, "Chủ đề & nội dung", """
                Đề cao tinh thần chính nghĩa, sự cứng cỏi, dám đấu tranh trừ gian tà của kẻ sĩ. Truyện gửi gắm \
                niềm tin "ở hiền gặp lành", chính nghĩa nhất định thắng gian tà.""");
        section(l3, 3, SectionType.TECHNIQUE, "Nghệ thuật", """
                Thể loại truyền kì với yếu tố kì ảo (ma quỷ, âm phủ) đan xen hiện thực. Cốt truyện giàu kịch tính, \
                nhân vật được khắc hoạ nổi bật qua hành động và lời nói khảng khái.""");
        section(l3, 4, SectionType.CONTEXT, "Bối cảnh", """
                Nguyễn Dữ (thế kỉ XVI) với "Truyền kì mạn lục" — được Vũ Khâm Lân khen là "thiên cổ kì bút", \
                mượn chuyện kì ảo để phản ánh hiện thực và gửi gắm quan niệm đạo đức.""");
        eq(m3, 1, "Tác phẩm thuộc thể loại nào?", Competency.KNOWLEDGE, 2, "Kí", "Tiểu thuyết", "Truyền kì", "Sử thi");
        eq(m3, 2, "Nhân vật chính của truyện là ai?", Competency.KNOWLEDGE, 1, "Tản Viên", "Ngô Tử Văn", "Nguyễn Dữ", "Diêm Vương");
        eq(m3, 3, "Hành động đốt đền của Tử Văn thể hiện điều gì?", Competency.ANALYSIS, 0,
                "Sự cương trực, dám trừ gian tà", "Sự liều lĩnh vô nghĩa", "Lòng tham", "Sự sợ hãi");
        eq(m3, 4, "Truyện được rút từ tập nào?", Competency.APPLICATION, 1,
                "Vang bóng một thời", "Truyền kì mạn lục", "Thơ thơ", "Nhật kí trong tù");

        // ==================== LỚP 11 ====================
        // --- Chữ người tử tù ---
        CourseModule m4 = module(van, "Lớp 11", "Chữ người tử tù", "Nguyễn Tuân · Truyện ngắn", "file", 60);
        Lesson l4 = lesson(m4, "Chữ người tử tù", "Nguyễn Tuân", "Truyện ngắn", 45,
                "Huấn Cao cho chữ viên quản ngục trong ngục tối — \"một cảnh tượng xưa nay chưa từng có\".",
                "Cuộc gặp gỡ giữa Huấn Cao và quản ngục — sự chiến thắng của cái đẹp và thiên lương.");
        section(l4, 1, SectionType.READING, "Đọc & hiểu tác phẩm", """
                Truyện xoay quanh Huấn Cao — người tử tù tài hoa, khí phách, viết chữ đẹp nổi tiếng — và viên quản \
                ngục yêu cái đẹp. Trong ngục tối, cảm mến tấm lòng "biệt nhỡn liên tài" của quản ngục, Huấn Cao \
                đồng ý cho chữ; cảnh cho chữ được Nguyễn Tuân gọi là "một cảnh tượng xưa nay chưa từng có".""");
        section(l4, 2, SectionType.THEME, "Chủ đề & nội dung", """
                Ngợi ca cái đẹp, cái tài và "thiên lương" con người; khẳng định cái đẹp có thể chiến thắng và cảm hoá \
                cái xấu, cái ác ngay giữa chốn ngục tù tăm tối.""");
        section(l4, 3, SectionType.TECHNIQUE, "Nghệ thuật", """
                Nghệ thuật tạo tình huống truyện độc đáo, thủ pháp đối lập (ánh sáng – bóng tối, cái đẹp – cái xấu), \
                bút pháp lãng mạn, ngôn ngữ cổ kính, giàu chất tạo hình và cảm hứng.""");
        section(l4, 4, SectionType.CONTEXT, "Bối cảnh", """
                Nguyễn Tuân (1910–1987) là bậc thầy tuỳ bút và ngôn ngữ. "Chữ người tử tù" in trong tập \
                "Vang bóng một thời" (1940), hướng về vẻ đẹp của một thời đã qua.""");
        eq(m4, 1, "Nhân vật trung tâm của truyện là ai?", Competency.KNOWLEDGE, 0, "Huấn Cao", "Chí Phèo", "Tràng", "Ngô Tử Văn");
        eq(m4, 2, "Truyện được in trong tập nào?", Competency.KNOWLEDGE, 1, "Thơ thơ", "Vang bóng một thời", "Truyền kì mạn lục", "Nhật kí trong tù");
        eq(m4, 3, "Chủ đề nổi bật của tác phẩm là:", Competency.ANALYSIS, 2,
                "Tình yêu đôi lứa", "Chiến tranh", "Sự chiến thắng của cái đẹp và thiên lương", "Cảnh nghèo đói");
        eq(m4, 4, "Cảnh nào được xem là \"xưa nay chưa từng có\"?", Competency.APPLICATION, 1,
                "Cảnh xử án", "Cảnh cho chữ trong ngục", "Cảnh chia tay", "Cảnh chợ");

        // --- Chí Phèo ---
        CourseModule m5 = module(van, "Lớp 11", "Chí Phèo", "Nam Cao · Truyện ngắn", "file", 60);
        Lesson l5 = lesson(m5, "Chí Phèo", "Nam Cao", "Truyện ngắn", 45,
                "\"Ai cho tao lương thiện?\" — bi kịch của người nông dân bị tha hoá và bị cự tuyệt quyền làm người.",
                "Bi kịch bị tha hoá và bị cự tuyệt quyền làm người của người nông dân trong xã hội cũ.");
        section(l5, 1, SectionType.READING, "Đọc & hiểu tác phẩm", """
                Chí Phèo vốn là anh nông dân lương thiện, bị Bá Kiến đẩy vào tù; ra tù thì tha hoá thành "con quỷ dữ \
                của làng Vũ Đại". Gặp Thị Nở, khát vọng lương thiện thức tỉnh, nhưng bị cự tuyệt. Tuyệt vọng, Chí giết \
                Bá Kiến rồi tự sát — cái chết trên ngưỡng cửa trở về làm người.""");
        section(l5, 2, SectionType.THEME, "Chủ đề & nội dung", """
                Tố cáo xã hội thực dân nửa phong kiến đẩy người nông dân vào bi kịch tha hoá; đồng thời khẳng định \
                bản chất lương thiện và khát vọng làm người qua câu hỏi nhức nhối "Ai cho tao lương thiện?".""");
        section(l5, 3, SectionType.TECHNIQUE, "Nghệ thuật", """
                Nghệ thuật điển hình hoá nhân vật, miêu tả và phân tích tâm lí sắc sảo, kết cấu vòng tròn/đảo, \
                ngôn ngữ sống động, giọng trần thuật đa thanh linh hoạt.""");
        section(l5, 4, SectionType.CONTEXT, "Bối cảnh", """
                Nam Cao (1917–1951) là nhà văn hiện thực xuất sắc. "Chí Phèo" (1941) là kiệt tác về đề tài người \
                nông dân Việt Nam trước Cách mạng tháng Tám.""");
        eq(m5, 1, "Tác giả của \"Chí Phèo\" là ai?", Competency.KNOWLEDGE, 1, "Ngô Tất Tố", "Nam Cao", "Kim Lân", "Vũ Trọng Phụng");
        eq(m5, 2, "Chí Phèo bị ai đẩy vào con đường tha hoá?", Competency.KNOWLEDGE, 0, "Bá Kiến", "Thị Nở", "Lí Cường", "Đội Tảo");
        eq(m5, 3, "Câu \"Ai cho tao lương thiện?\" thể hiện điều gì?", Competency.ANALYSIS, 2,
                "Sự say rượu", "Lời chửi bới", "Bi kịch bị cự tuyệt quyền làm người", "Niềm vui");
        eq(m5, 4, "Tác phẩm thuộc trào lưu văn học nào?", Competency.APPLICATION, 1,
                "Lãng mạn", "Hiện thực phê phán", "Thơ mới", "Trung đại");

        // --- Vội vàng ---
        CourseModule m6 = module(van, "Lớp 11", "Vội vàng", "Xuân Diệu · Thơ mới", "book", 60);
        Lesson l6 = lesson(m6, "Vội vàng", "Xuân Diệu", "Thơ mới", 40, """
                Tôi muốn tắt nắng đi
                Cho màu đừng nhạt mất;
                                        — Xuân Diệu, trích "Vội vàng" (1938)""",
                "Khát vọng sống mãnh liệt và quan niệm mới mẻ về thời gian, tuổi trẻ của \"nhà thơ mới nhất\".");
        section(l6, 1, SectionType.READING, "Đọc & hiểu tác phẩm", """
                Bài thơ bộc lộ khát khao giao cảm với đời, tình yêu cuộc sống trần thế đến cuồng nhiệt. Nhà thơ ý \
                thức thời gian trôi nhanh, tuổi trẻ ngắn ngủi nên giục giã sống "vội vàng", tận hưởng trọn vẹn mọi \
                vẻ đẹp của mùa xuân và tuổi trẻ.""");
        section(l6, 2, SectionType.THEME, "Chủ đề & nội dung", """
                Quan niệm nhân sinh tích cực: sống hết mình, trân quý từng khoảnh khắc. Thời gian là tuyến tính, \
                một đi không trở lại, nên phải chạy đua để tận hưởng và cống hiến.""");
        section(l6, 3, SectionType.TECHNIQUE, "Nghệ thuật", """
                Thơ mới với cái tôi cá nhân mạnh mẽ; hình ảnh tươi mới, táo bạo; nhịp điệu dồn dập; điệp ngữ, \
                động từ mạnh và những so sánh, ẩn dụ đầy cảm giác.""");
        section(l6, 4, SectionType.CONTEXT, "Bối cảnh", """
                Xuân Diệu (1916–1985) được mệnh danh "nhà thơ mới nhất trong các nhà thơ mới". "Vội vàng" in trong \
                tập "Thơ thơ" (1938), tiêu biểu cho phong trào Thơ mới 1932–1945.""");
        eq(m6, 1, "Tác giả bài thơ là ai?", Competency.KNOWLEDGE, 1, "Huy Cận", "Xuân Diệu", "Hàn Mặc Tử", "Chế Lan Viên");
        eq(m6, 2, "\"Vội vàng\" in trong tập thơ nào?", Competency.KNOWLEDGE, 0, "Thơ thơ", "Lửa thiêng", "Điêu tàn", "Gái quê");
        eq(m6, 3, "Cảm hứng chủ đạo của bài thơ là:", Competency.ANALYSIS, 2,
                "Nỗi buồn chiến tranh", "Tình quê", "Khát vọng sống và chạy đua với thời gian", "Nỗi cô đơn");
        eq(m6, 4, "Bài thơ tiêu biểu cho phong trào nào?", Competency.APPLICATION, 1,
                "Văn học trung đại", "Thơ mới", "Văn học cách mạng", "Hiện thực phê phán");

        // ==================== LỚP 12 ====================
        // --- Tây Tiến ---
        CourseModule m7 = module(van, "Lớp 12", "Tây Tiến", "Quang Dũng · Thơ", "book", 60);
        Lesson l7 = lesson(m7, "Tây Tiến", "Quang Dũng", "Thơ", 45, """
                Sông Mã xa rồi Tây Tiến ơi!
                Nhớ về rừng núi nhớ chơi vơi
                                        — Quang Dũng, trích "Tây Tiến" (1948)""",
                "Nỗi nhớ về đoàn quân Tây Tiến và thiên nhiên miền Tây hùng vĩ; người lính hào hoa, bi tráng.");
        section(l7, 1, SectionType.READING, "Đọc & hiểu tác phẩm", """
                Bài thơ là nỗi nhớ của Quang Dũng về đoàn binh Tây Tiến và những ngày hành quân gian khổ nơi núi \
                rừng miền Tây. Hiện lên bức tranh thiên nhiên vừa hùng vĩ dữ dội vừa thơ mộng, cùng hình tượng người \
                lính hào hoa, kiêu dũng và sự hi sinh bi tráng.""");
        section(l7, 2, SectionType.THEME, "Chủ đề & nội dung", """
                Khắc hoạ vẻ đẹp bi tráng, lãng mạn của người lính Tây Tiến — sẵn sàng hi sinh vì Tổ quốc mà vẫn hào \
                hoa, mộng mơ. Bài thơ thấm đượm nỗi nhớ và niềm tự hào về đồng đội một thời.""");
        section(l7, 3, SectionType.TECHNIQUE, "Nghệ thuật", """
                Bút pháp lãng mạn kết hợp hiện thực, cảm hứng bi tráng. Hình ảnh giàu chất nhạc, chất hoạ; nghệ thuật \
                đối lập giữa dữ dội và trữ tình; ngôn ngữ tinh tế, gợi cảm.""");
        section(l7, 4, SectionType.CONTEXT, "Bối cảnh", """
                Quang Dũng (1921–1988) là nghệ sĩ đa tài. "Tây Tiến" (1948) viết tại Phù Lưu Chanh khi ông rời đơn \
                vị, là một trong những bài thơ hay nhất về người lính trong kháng chiến chống Pháp.""");
        eq(m7, 1, "Tác giả bài thơ \"Tây Tiến\" là ai?", Competency.KNOWLEDGE, 1, "Tố Hữu", "Quang Dũng", "Chính Hữu", "Nguyễn Đình Thi");
        eq(m7, 2, "Bài thơ ra đời trong kháng chiến chống:", Competency.KNOWLEDGE, 0, "Pháp", "Mỹ", "Minh", "Nguyên Mông");
        eq(m7, 3, "Cảm hứng bao trùm bài thơ là:", Competency.ANALYSIS, 2,
                "Hài hước", "Phê phán", "Lãng mạn và bi tráng", "Trào phúng");
        eq(m7, 4, "Hình tượng trung tâm của bài thơ là:", Competency.APPLICATION, 1,
                "Người nông dân", "Người lính Tây Tiến", "Người mẹ", "Dòng sông");

        // --- Việt Bắc ---
        CourseModule m8 = module(van, "Lớp 12", "Việt Bắc", "Tố Hữu · Thơ (lục bát)", "book", 60);
        Lesson l8 = lesson(m8, "Việt Bắc", "Tố Hữu", "Thơ", 45, """
                Mình về mình có nhớ ta
                Mười lăm năm ấy thiết tha mặn nồng.
                                        — Tố Hữu, trích "Việt Bắc" (1954)""",
                "Khúc tình ca và hùng ca về nghĩa tình cách mạng giữa cán bộ kháng chiến và nhân dân Việt Bắc.");
        section(l8, 1, SectionType.READING, "Đọc & hiểu tác phẩm", """
                Bài thơ ra đời tháng 10/1954 khi Trung ương Đảng và Chính phủ rời chiến khu Việt Bắc về Hà Nội. \
                Qua lối đối đáp "mình – ta", tác phẩm tái hiện kỉ niệm kháng chiến gian khổ mà nghĩa tình và khẳng \
                định lòng thuỷ chung son sắt.""");
        section(l8, 2, SectionType.THEME, "Chủ đề & nội dung", """
                Ân nghĩa cách mạng, lòng biết ơn và thuỷ chung giữa người cán bộ về xuôi và nhân dân Việt Bắc; \
                niềm tự hào về cuộc kháng chiến và con người kháng chiến anh hùng.""");
        section(l8, 3, SectionType.TECHNIQUE, "Nghệ thuật", """
                Thể lục bát đậm đà tính dân tộc; kết cấu đối đáp giao duyên; cặp đại từ "mình – ta"; giọng thơ tâm \
                tình ngọt ngào; hình ảnh và nhạc điệu mang màu sắc ca dao.""");
        section(l8, 4, SectionType.CONTEXT, "Bối cảnh", """
                Tố Hữu (1920–2002) là "lá cờ đầu" của thơ ca cách mạng Việt Nam. "Việt Bắc" in trong tập cùng tên, \
                được coi là đỉnh cao của thơ Tố Hữu.""");
        eq(m8, 1, "Tác giả của \"Việt Bắc\" là ai?", Competency.KNOWLEDGE, 2, "Quang Dũng", "Xuân Diệu", "Tố Hữu", "Chế Lan Viên");
        eq(m8, 2, "Bài thơ được viết chủ yếu theo thể thơ nào?", Competency.KNOWLEDGE, 1, "Bảy chữ", "Lục bát", "Tự do", "Năm chữ");
        eq(m8, 3, "Cặp đại từ \"mình – ta\" trong bài có tác dụng gì?", Competency.ANALYSIS, 0,
                "Tạo lối đối đáp, bộc lộ nghĩa tình gắn bó", "Chỉ thời gian", "Chỉ địa danh", "Không có ý nghĩa");
        eq(m8, 4, "Bài thơ ra đời gắn với sự kiện nào?", Competency.APPLICATION, 1,
                "Cách mạng tháng Tám 1945", "Rời chiến khu Việt Bắc năm 1954", "Chiến dịch Hồ Chí Minh 1975", "Phong trào Thơ mới");

        // --- Sóng (bản đầy đủ, giữ nguyên bộ công cụ phong phú) ---
        CourseModule m9 = module(van, "Lớp 12", "Sóng", "Xuân Quỳnh · Thơ", "book", 70);
        Lesson song = lesson(m9, "Sóng – Xuân Quỳnh", "Xuân Quỳnh", "Thơ", 45, """
                Dữ dội và dịu êm
                Ồn ào và lặng lẽ
                Sông không hiểu nổi mình
                Sóng tìm ra tận bể
                                        — Xuân Quỳnh, trích "Sóng" (1967)""",
                "Hình tượng sóng như ẩn dụ cho tâm hồn và tình yêu của người phụ nữ.");
        section(song, 1, SectionType.READING, "Đọc & hiểu tác phẩm", """
                "Sóng" được Xuân Quỳnh viết năm 1967 tại biển Diêm Điền. Toàn bài xây dựng trên hai hình tượng \
                song hành: "sóng" và "em". Sóng là hình ảnh thiên nhiên, còn em là chủ thể trữ tình; hai hình tượng \
                lúc phân đôi để soi chiếu, lúc hoà nhập làm một để nói về khát vọng tình yêu.""");
        section(song, 2, SectionType.THEME, "Chủ đề & nội dung", """
                Chủ đề: vẻ đẹp tâm hồn và khát vọng tình yêu của người phụ nữ — vừa mãnh liệt vừa dịu dàng, vừa lo \
                âu vừa tin tưởng; khát khao một tình yêu chân thành, thuỷ chung và được hoà vào cái lớn lao, vĩnh hằng.""");
        section(song, 3, SectionType.IMAGERY, "Hình ảnh, biểu tượng", """
                • Sóng: biểu tượng cho tâm hồn người phụ nữ đang yêu — nhiều cung bậc, không yên định.
                • Biển/bể: cái lớn lao, vô tận mà tình yêu khát khao vươn tới.
                • "Con sóng dưới lòng sâu / Con sóng trên mặt nước": nỗi nhớ bao trùm cả không gian.""");
        section(song, 4, SectionType.TECHNIQUE, "Biện pháp nghệ thuật", """
                • Thể thơ năm chữ, nhịp ngắn đều đặn gợi âm hưởng những con sóng nối nhau.
                • Phép tương phản: "Dữ dội - dịu êm", "Ồn ào - lặng lẽ" diễn tả các trạng thái đối lập mà thống nhất.
                • Điệp từ, điệp cấu trúc tạo nhịp trùng điệp; ẩn dụ xuyên suốt: sóng ẩn dụ cho tình yêu.""");
        section(song, 5, SectionType.CONTEXT, "Bối cảnh", """
                Xuân Quỳnh (1942–1988) là gương mặt tiêu biểu của thế hệ nhà thơ trẻ trưởng thành trong kháng chiến \
                chống Mỹ. Thơ bà giàu nữ tính, khao khát yêu thương. "Sóng" in trong tập "Hoa dọc chiến hào".""");
        section(song, 6, SectionType.GUIDE, "Hướng dẫn phân tích", """
                1. Mở bài: giới thiệu tác giả, tác phẩm, hình tượng sóng – em. 2. Thân bài theo mạch: bản tính người \
                phụ nữ đang yêu; trăn trở về nguồn gốc tình yêu; nỗi nhớ và lòng thuỷ chung; khát vọng bất tử hoá \
                tình yêu. 3. Kết bài: khẳng định giá trị nhân văn và nét riêng của hồn thơ Xuân Quỳnh.""");
        section(song, 7, SectionType.FRAMEWORK, "Framework viết đoạn văn", """
                Mô hình: Câu chủ đề → phân tích từ ngữ, hình ảnh, biện pháp nghệ thuật → trích dẫn chính xác câu thơ \
                → đánh giá ý nghĩa của chi tiết với chủ đề và với tâm hồn tác giả.""");
        section(song, 8, SectionType.EXAMPLE, "Ví dụ bài làm", """
                (Đoạn mẫu) Mở đầu bài thơ, Xuân Quỳnh diễn tả bản tính phong phú của người phụ nữ đang yêu qua hai \
                câu "Dữ dội và dịu êm / Ồn ào và lặng lẽ". Phép tương phản đặt những trạng thái đối lập cạnh nhau cho \
                thấy một tâm hồn nhiều cung bậc nhưng thống nhất, từ đó khát khao "tìm ra tận bể" — vươn tới tình yêu lớn lao.""");
        section(song, 9, SectionType.VOCAB, "Từ vựng & khái niệm", """
                • Ẩn dụ: gọi tên sự vật này bằng sự vật khác có nét tương đồng (sóng ↔ tình yêu).
                • Tương phản: đặt hai yếu tố trái ngược để làm nổi bật ý. • Điệp cấu trúc: lặp mô hình câu để tạo nhịp.
                • Chủ thể trữ tình: "cái tôi" bộc lộ cảm xúc trong tác phẩm ("em").""");
        eq(m9, 1, "\"Sóng\" được viết theo thể thơ nào?", Competency.KNOWLEDGE, 1, "Lục bát", "Năm chữ (ngũ ngôn)", "Bảy chữ", "Tự do");
        eq(m9, 2, "Bài thơ ra đời năm nào?", Competency.KNOWLEDGE, 2, "1945", "1954", "1967", "1975");
        eq(m9, 3, "Hình tượng \"sóng\" chủ yếu tượng trưng cho:", Competency.ANALYSIS, 1,
                "Thiên nhiên biển cả đơn thuần", "Tâm hồn và tình yêu của người phụ nữ", "Dòng chảy thời gian", "Nỗi cô đơn");
        eq(m9, 4, "Quan hệ giữa \"sóng\" và \"em\" trong bài là:", Competency.ANALYSIS, 1,
                "Hoàn toàn đối lập", "Song hành, soi chiếu và hoà nhập", "Không liên quan", "Nhân quả");
        eq(m9, 5, "Khi viết đoạn phân tích một khổ thơ, câu đầu tiên nên là:", Competency.APPLICATION, 1,
                "Chép lại khổ thơ", "Câu chủ đề khái quát nội dung khổ", "Tiểu sử tác giả", "Cảm nghĩ cá nhân");
        eq(m9, 6, "Dẫn chứng thuyết phục khi phân tích thơ cần:", Competency.APPLICATION, 0,
                "Trích đúng câu thơ và phân tích từ ngữ, hình ảnh", "Chỉ nêu cảm xúc", "Kể lại nội dung", "Chép văn mẫu");
        material(song, MaterialType.DOCUMENT, "Bài thơ Sóng — Wikipedia",
                "https://vi.wikipedia.org/wiki/Sóng_(bài_thơ)");

        // --- Vợ nhặt ---
        CourseModule m10 = module(van, "Lớp 12", "Vợ nhặt", "Kim Lân · Truyện ngắn", "file", 60);
        Lesson l10 = lesson(m10, "Vợ nhặt", "Kim Lân", "Truyện ngắn", 45,
                "Giữa nạn đói 1945, Tràng \"nhặt\" được vợ — khát vọng sống và tổ ấm bừng lên bên bờ vực cái chết.",
                "Giữa nạn đói khủng khiếp 1945, con người vẫn khát khao sống, yêu thương và hi vọng.");
        section(l10, 1, SectionType.READING, "Đọc & hiểu tác phẩm", """
                Truyện kể anh Tràng nghèo khổ, xấu xí bỗng "nhặt" được vợ chỉ qua vài câu bông đùa và bốn bát bánh \
                đúc giữa nạn đói năm 1945. Trong cái đói và cái chết cận kề, mái ấm gia đình và niềm hi vọng vẫn nhen \
                lên; kết truyện là hình ảnh lá cờ đỏ và đoàn người phá kho thóc trong óc Tràng.""");
        section(l10, 2, SectionType.THEME, "Chủ đề & nội dung", """
                Giá trị nhân đạo sâu sắc: dù bị đẩy tới bờ vực cái chết, con người vẫn hướng về sự sống, khát khao \
                hạnh phúc và tin vào tương lai. Tác phẩm đồng thời tố cáo tội ác gây ra nạn đói.""");
        section(l10, 3, SectionType.TECHNIQUE, "Nghệ thuật", """
                Tình huống truyện độc đáo ("nhặt" được vợ); nghệ thuật miêu tả tâm lí tinh tế; ngôn ngữ mộc mạc, \
                đậm chất nông thôn Bắc Bộ; giọng văn ấm áp tình người.""");
        section(l10, 4, SectionType.CONTEXT, "Bối cảnh", """
                Kim Lân (1920–2007) là nhà văn của nông thôn và người nông dân. "Vợ nhặt" lấy bối cảnh nạn đói Ất Dậu \
                1945, in trong tập "Con chó xấu xí" (1962).""");
        eq(m10, 1, "Tác giả của \"Vợ nhặt\" là ai?", Competency.KNOWLEDGE, 2, "Nam Cao", "Ngô Tất Tố", "Kim Lân", "Nguyễn Tuân");
        eq(m10, 2, "Truyện lấy bối cảnh sự kiện nào?", Competency.KNOWLEDGE, 1, "Cách mạng tháng Tám", "Nạn đói năm 1945", "Chống Pháp", "Chống Mỹ");
        eq(m10, 3, "Giá trị nổi bật của tác phẩm là:", Competency.ANALYSIS, 0,
                "Nhân đạo — khát vọng sống và hạnh phúc", "Phê phán tôn giáo", "Ca ngợi thiên nhiên", "Trào phúng");
        eq(m10, 4, "Tình huống truyện độc đáo của tác phẩm là:", Competency.APPLICATION, 1,
                "Đi lính", "Tràng \"nhặt\" được vợ giữa nạn đói", "Đòi nợ", "Thi cử");

        // ---------- Placement test cho Ngữ văn (đánh giá kỹ năng chung) ----------
        pq(van, 1, "Biện pháp tu từ chủ đạo trong \"Dữ dội và dịu êm / Ồn ào và lặng lẽ\" là:",
                Competency.COMPREHENSION, 1, "Ẩn dụ", "Tương phản (đối lập)", "So sánh", "Nói quá");
        pq(van, 2, "Thể thơ lục bát có đặc điểm gì?", Competency.KNOWLEDGE, 0,
                "Cặp câu 6 – 8 tiếng, gieo vần lưng", "Mỗi dòng 5 tiếng", "Mỗi dòng 7 tiếng", "Không có vần");
        pq(van, 3, "Tác phẩm nào sau đây thuộc văn học trung đại?", Competency.KNOWLEDGE, 2,
                "Chí Phèo", "Vội vàng", "Bình Ngô đại cáo", "Tây Tiến");
        pq(van, 4, "Hình tượng \"sóng\" trong thơ Xuân Quỳnh chủ yếu là ẩn dụ cho:",
                Competency.ANALYSIS, 1, "Cảnh biển", "Tâm hồn người phụ nữ đang yêu", "Cuộc kháng chiến", "Thời gian");
        pq(van, 5, "Để làm nổi bật chủ đề, hai hình tượng \"sóng\" và \"em\":",
                Competency.ANALYSIS, 1, "Tách rời hoàn toàn", "Soi chiếu, cộng hưởng và hoà nhập", "Mâu thuẫn nhau", "Không liên hệ");
        pq(van, 6, "Bước đầu tiên nên làm khi viết đoạn văn nghị luận văn học là:",
                Competency.APPLICATION, 1, "Chép lại đề bài", "Xác định luận điểm và câu chủ đề", "Kể tiểu sử tác giả", "Viết kết bài trước");
    }

    // Chủ đề ngữ pháp SGK Tiếng Anh 12 (Global Success) — dùng CHUNG cho câu hỏi
    // chẩn đoán và bài học, để điểm yếu ánh xạ thẳng sang bài học cần ôn.
    private static final String G12 = "Lớp 12";
    private static final String T_PAST = "Quá khứ đơn & Quá khứ tiếp diễn";
    private static final String T_ART = "Mạo từ a/an/the";
    private static final String T_VPREP = "Động từ đi với giới từ";
    private static final String T_WHICH = "Mệnh đề quan hệ với \"which\"";
    private static final String T_PRESPERF = "Thì hiện tại hoàn thành";
    private static final String T_DBLCOMP = "So sánh kép (càng... càng)";
    // Không dùng dấu phẩy trong tên chủ đề: weakTopics được lưu dạng phân tách bằng dấu phẩy.
    private static final String T_SENTENCE = "Câu đơn / câu ghép / câu phức";

    // Chủ đề ngữ pháp SGK Tiếng Anh 10 (Global Success) — Unit 1–5.
    private static final String G10 = "Lớp 10";
    private static final String T10_PRES = "Hiện tại đơn & Hiện tại tiếp diễn";
    private static final String T10_FUTURE = "Tương lai: will & be going to";
    private static final String T10_PASSIVE = "Câu bị động";
    private static final String T10_COMPOUND = "Câu ghép";
    private static final String T10_INF = "To-infinitive & bare infinitive";
    private static final String T10_PAST = "Quá khứ đơn & Quá khứ tiếp diễn (when/while)";
    private static final String T10_PRESPERF = "Thì hiện tại hoàn thành (lớp 10)";
    private static final String T10_GERUND = "Danh động từ & to-infinitive";
    private static final String T10_PASSMODAL = "Câu bị động với động từ khiếm khuyết";
    private static final String T10_COMPSUP = "So sánh hơn & so sánh nhất (tính từ)";
    private static final String T10_RELCLAUSE = "Mệnh đề quan hệ (who/which/that/whose)";
    private static final String T10_REPORTED = "Câu tường thuật (lớp 10)";
    private static final String T10_CONDITIONAL = "Câu điều kiện loại 1 & loại 2";

    // Chủ đề ngữ pháp SGK Tiếng Anh 11 (Global Success) — Unit 1–10.
    private static final String G11 = "Lớp 11";
    private static final String T11_PASTPERF = "Quá khứ đơn & Hiện tại hoàn thành";
    private static final String T11_MODAL = "Động từ khiếm khuyết: must, have to, should";
    private static final String T11_STATIVE = "Động từ trạng thái ở dạng tiếp diễn & Linking verbs";
    private static final String T11_GERSO = "Danh động từ làm chủ ngữ & tân ngữ";
    private static final String T11_PARTICIPLE = "Mệnh đề phân từ (hiện tại & quá khứ phân từ)";
    private static final String T11_TOINF = "Mệnh đề to-infinitive";
    private static final String T11_PERFGER = "Danh động từ hoàn thành & phân từ hoàn thành";
    private static final String T11_CLEFT = "Câu chẻ (cleft) với It is/was ... that/who";
    private static final String T11_LINKING = "Từ nối & cụm từ nối (linking words)";
    private static final String T11_COMPNOUN = "Danh từ ghép (compound nouns)";

    // Chủ đề ngữ pháp SGK Tiếng Anh 12 (Global Success) — Unit 6–10.
    private static final String T12_CAUSATIVE = "Thể sai khiến chủ động & bị động (causatives)";
    private static final String T12_ADVMANNER = "Mệnh đề trạng ngữ chỉ cách thức & kết quả";
    private static final String T12_ADVCOND = "Mệnh đề trạng ngữ chỉ điều kiện & so sánh";
    private static final String T12_PHRASAL3 = "Cụm động từ ba thành phần (phrasal verbs)";
    private static final String T12_REPORTED = "Câu tường thuật: mệnh lệnh, yêu cầu, đề nghị, lời khuyên";

    private void seedEnglish(Subject anh) {
        // ---- Đề khảo sát ĐẦU VÀO 30 câu, đủ dạng như đề KSCL đầu năm (mỗi lớp một đề) ----
        seedEnglish10Placement(anh);
        seedEnglish11Placement(anh);
        seedEnglish12Placement(anh);

        // Tài liệu nguồn cho AI (giáo viên gửi) — AI dựa vào đây để soạn giáo trình + bài tập.
        String grammar = readClasspath("materials/tieng-anh-thanh-phan-cau.txt");
        if (grammar != null && !grammar.isBlank()) {
            ReferenceMaterial rm = new ReferenceMaterial();
            rm.setSubject(anh);
            rm.setTitle("Các thành phần cơ bản trong câu tiếng Anh");
            rm.setContent(grammar);
            rm.setCreatedByEmail("admin@pathstudy.vn");
            referenceMaterials.save(rm);
        }

        // ---- Giáo trình theo lớp (SGK Global Success 10/11/12) ----
        seedEnglish10Lessons();
        seedEnglish11Lessons();
        seedEnglish12Lessons();

        // Tài liệu nguồn cho AI (Gemini bám vào để soạn giáo trình) — theo từng lớp.
        seedGradeReference(anh, G10, "SGK Tiếng Anh 10 – Global Success (Ngữ pháp & từ vựng)");
        seedGradeReference(anh, G11, "SGK Tiếng Anh 11 – Global Success (Ngữ pháp & từ vựng)");
        seedGradeReference(anh, G12, "SGK Tiếng Anh 12 – Global Success (Ngữ pháp & từ vựng)");

        // ---- Đề luyện tập (làm đề) — chấm điểm + chỉ ra điểm yếu ----
        Exam ex1 = exam(anh, "Đề luyện tập số 1 — Ngữ pháp cơ bản", "Cơ bản",
                "Thành phần câu, từ loại và các thì cơ bản", 1);
        eqExam(ex1, 1, "Trong câu \"She reads books\", đâu là Chủ ngữ (Subject)?",
                Competency.KNOWLEDGE, "Thành phần câu (Chủ ngữ)", 0, "She", "reads", "books", "a book");
        eqExam(ex1, 2, "Đâu là Động từ (Verb) trong \"He is running fast\"?",
                Competency.KNOWLEDGE, "Từ loại (Động từ)", 1, "He", "is running", "fast", "run");
        eqExam(ex1, 3, "Đâu là Tính từ (Adjective) trong \"The beautiful flower bloomed\"?",
                Competency.KNOWLEDGE, "Từ loại (Tính từ)", 1, "The", "beautiful", "flower", "bloomed");
        eqExam(ex1, 4, "She ___ chocolate.", Competency.KNOWLEDGE, "Thì hiện tại đơn", 1,
                "love", "loves", "loving", "loved");
        eqExam(ex1, 5, "The book is ___ the table.", Competency.KNOWLEDGE, "Giới từ", 1,
                "in", "on", "at", "of");
        eqExam(ex1, 6, "I ___ Paris once.", Competency.APPLICATION, "Thì hiện tại hoàn thành", 2,
                "visit", "visited", "have visited", "visiting");

        Exam ex2 = exam(anh, "Đề luyện tập số 2 — Các thì", "Cơ bản",
                "Ôn tập và phân biệt các thì trong tiếng Anh", 2);
        eqExam(ex2, 1, "They ___ soccer now.", Competency.KNOWLEDGE, "Thì hiện tại tiếp diễn", 2,
                "play", "plays", "are playing", "played");
        eqExam(ex2, 2, "The sun ___ in the east.", Competency.KNOWLEDGE, "Thì hiện tại đơn", 1,
                "rise", "rises", "is rising", "rose");
        eqExam(ex2, 3, "We ___ at 5 PM tomorrow (đã sắp xếp).", Competency.APPLICATION, "Thì hiện tại tiếp diễn", 2,
                "meet", "met", "are meeting", "will met");
        eqExam(ex2, 4, "She ___ TV when I called.", Competency.APPLICATION, "Thì quá khứ tiếp diễn", 1,
                "watched", "was watching", "watches", "is watching");
        eqExam(ex2, 5, "He ___ here since 2010.", Competency.APPLICATION, "Thì hiện tại hoàn thành", 2,
                "lives", "lived", "has lived", "living");
        eqExam(ex2, 6, "If it ___ , we will stay home.", Competency.APPLICATION, "Câu điều kiện loại 1", 0,
                "rains", "rained", "will rain", "raining");

        // Đề tổng hợp cuối kì theo lớp (từ đề cương ôn tập / đề thi của user)
        seedEnglish10ExamHK1(anh);
        seedEnglish10ExamHK2(anh);
        seedEnglish11ExamHK1(anh);
        seedEnglish11ExamHK2(anh);
    }

    private void seedEnglish10ExamHK1(Subject anh) {
        Exam ex = exam(anh, G10, "Đề tổng hợp Học kì 1 — Lớp 10", "Tổng hợp HK1",
                "Ôn tập Unit 1–5 (các thì, bị động, câu ghép, động từ nguyên mẫu/V-ing) — chấm điểm & chỉ điểm yếu.", 10);
        eqExam(ex, 1, "My sister ___ her homework at the moment.", Competency.APPLICATION, T10_PRES, 2,
                "do", "does", "is doing", "did");
        eqExam(ex, 2, "Water ___ at 100 degrees Celsius.", Competency.KNOWLEDGE, T10_PRES, 1,
                "boil", "boils", "is boiling", "boiled");
        eqExam(ex, 3, "I promise I ___ you tomorrow.", Competency.APPLICATION, T10_FUTURE, 1,
                "am", "will call", "call", "calling");
        eqExam(ex, 4, "Look at the timetable. The train ___ at 6 p.m. (kế hoạch chắc chắn).", Competency.APPLICATION, T10_FUTURE, 2,
                "will leave", "leave", "is going to leave", "left");
        eqExam(ex, 5, "The letter ___ yesterday.", Competency.APPLICATION, T10_PASSIVE, 1,
                "sent", "was sent", "is sent", "sends");
        eqExam(ex, 6, "I was tired, ___ I went to bed early.", Competency.KNOWLEDGE, T10_COMPOUND, 3,
                "but", "or", "and", "so");
        eqExam(ex, 7, "She agreed ___ us with the project.", Competency.APPLICATION, T10_INF, 1,
                "help", "to help", "helping", "helped");
        eqExam(ex, 8, "My mother made me ___ the dishes.", Competency.APPLICATION, T10_INF, 0,
                "wash", "to wash", "washing", "washed");
        eqExam(ex, 9, "While they ___ dinner, the doorbell rang.", Competency.APPLICATION, T10_PAST, 2,
                "have", "had", "were having", "are having");
        eqExam(ex, 10, "I ___ never ___ to Japan.", Competency.APPLICATION, T10_PRESPERF, 1,
                "did / go", "have / been", "am / being", "was / gone");
        eqExam(ex, 11, "He avoids ___ junk food.", Competency.KNOWLEDGE, T10_GERUND, 2,
                "eat", "to eat", "eating", "eaten");
        eqExam(ex, 12, "___ a new language takes time and effort.", Competency.APPLICATION, T10_GERUND, 1,
                "Learn", "Learning", "To learning", "Learned");
        eqExam(ex, 13, "Choose the word CLOSEST in meaning to \"talented\".", Competency.COMPREHENSION, "Từ vựng", 1,
                "lazy", "gifted", "weak", "ordinary");
        eqExam(ex, 14, "We should ___ electricity to protect the environment.", Competency.KNOWLEDGE, "Từ vựng", 0,
                "save", "waste", "spend", "throw");
        String reading = """
                Đọc đoạn văn sau và trả lời các câu hỏi (15–20).

                Music plays an important part in our lives. Many young people listen to music every day
                to relax after school or to feel more energetic. Some students say that listening to soft
                music while studying helps them concentrate better. Music can also bring people together;
                for example, at concerts and festivals, thousands of fans sing along to their favourite
                songs. In addition, learning to play a musical instrument, such as the guitar or the piano,
                can improve memory and reduce stress. For these reasons, music is much more than just
                entertainment.""";
        eqExam(ex, 15, reading, "Why do many young people listen to music every day?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "To earn money", "To relax or feel energetic", "To do homework", "To sleep in class");
        eqExam(ex, 16, reading, "According to some students, soft music while studying helps them ___.", Competency.COMPREHENSION, "Đọc hiểu", 0,
                "concentrate better", "sleep", "eat more", "talk louder");
        eqExam(ex, 17, reading, "Where do thousands of fans sing along to songs?", Competency.COMPREHENSION, "Đọc hiểu", 2,
                "At school", "At home", "At concerts and festivals", "In libraries");
        eqExam(ex, 18, reading, "What can learning a musical instrument improve?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Eyesight", "Memory and stress reduction", "Height", "Speed");
        eqExam(ex, 19, reading, "The word \"entertainment\" is closest in meaning to ___.", Competency.COMPREHENSION, "Từ vựng", 1,
                "work", "amusement", "study", "sport");
        eqExam(ex, 20, reading, "What is the main idea of the passage?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Music is a waste of time", "Music is important and useful in many ways", "Only singers like music", "Music is only for concerts");
    }

    private void seedEnglish10ExamHK2(Subject anh) {
        Exam ex = exam(anh, G10, "Đề tổng hợp Học kì 2 — Lớp 10", "Tổng hợp HK2",
                "Ôn tập Unit 6–10 (bị động+modal, so sánh, mệnh đề quan hệ, tường thuật, câu điều kiện) — chấm điểm & chỉ điểm yếu.", 11);
        eqExam(ex, 1, "This problem must ___ immediately.", Competency.APPLICATION, T10_PASSMODAL, 1,
                "solve", "be solved", "solved", "be solving");
        eqExam(ex, 2, "Gender equality should ___ in every country.", Competency.APPLICATION, T10_PASSMODAL, 1,
                "promote", "be promoted", "promoted", "promoting");
        eqExam(ex, 3, "This building is ___ than that one.", Competency.KNOWLEDGE, T10_COMPSUP, 1,
                "tall", "taller", "tallest", "more tall");
        eqExam(ex, 4, "She is ___ student in her class.", Competency.KNOWLEDGE, T10_COMPSUP, 2,
                "cleverer", "more clever", "the cleverest", "clever");
        eqExam(ex, 5, "The book ___ I borrowed is very interesting.", Competency.KNOWLEDGE, T10_RELCLAUSE, 2,
                "who", "whose", "which", "where");
        eqExam(ex, 6, "This is the town ___ I was born.", Competency.APPLICATION, T10_RELCLAUSE, 3,
                "which", "who", "that", "where");
        eqExam(ex, 7, "He said that he ___ busy at that moment.", Competency.APPLICATION, T10_REPORTED, 1,
                "is", "was", "will be", "be");
        eqExam(ex, 8, "She told me ___ the window.", Competency.APPLICATION, T10_REPORTED, 1,
                "close", "to close", "closing", "closed");
        eqExam(ex, 9, "If you heat ice, it ___.", Competency.APPLICATION, T10_CONDITIONAL, 1,
                "melted", "melts", "will melt", "would melt");
        eqExam(ex, 10, "If we don't protect nature, animals ___ their habitats.", Competency.APPLICATION, T10_CONDITIONAL, 2,
                "lost", "would lose", "will lose", "lose");
        eqExam(ex, 11, "If I had more money, I ___ around the world.", Competency.APPLICATION, T10_CONDITIONAL, 1,
                "will travel", "would travel", "travel", "travelled");
        eqExam(ex, 12, "Choose the word OPPOSITE in meaning to \"protect\".", Competency.COMPREHENSION, "Từ vựng", 2,
                "guard", "defend", "harm", "save");
        eqExam(ex, 13, "Ecotourism helps ___ the natural environment.", Competency.KNOWLEDGE, "Từ vựng", 0,
                "preserve", "destroy", "pollute", "waste");
        eqExam(ex, 14, "A person who does a job without being paid is a ___.", Competency.KNOWLEDGE, "Từ vựng", 1,
                "manager", "volunteer", "customer", "tourist");
        String reading = """
                Đọc đoạn văn sau và trả lời các câu hỏi (15–20).

                Ecotourism is a form of tourism that focuses on protecting the environment and supporting
                local people. Instead of staying in big hotels, ecotourists often stay in small guesthouses
                run by local families. They enjoy activities such as hiking, bird-watching and visiting
                national parks. Ecotourism helps local communities earn money while keeping their traditions
                alive. It also raises people's awareness of the importance of protecting nature. However,
                if it is not managed carefully, too many visitors can damage the environment. Therefore,
                both tourists and local authorities must work together to keep ecotourism sustainable.""";
        eqExam(ex, 15, reading, "What does ecotourism focus on?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Building big hotels", "Protecting the environment and supporting local people", "Making profit only", "Fast travel");
        eqExam(ex, 16, reading, "Where do ecotourists often stay?", Competency.COMPREHENSION, "Đọc hiểu", 2,
                "In big hotels", "On cruise ships", "In small local guesthouses", "In airports");
        eqExam(ex, 17, reading, "Which activity is NOT mentioned?", Competency.COMPREHENSION, "Đọc hiểu", 3,
                "Hiking", "Bird-watching", "Visiting national parks", "Shopping in malls");
        eqExam(ex, 18, reading, "What can happen if ecotourism is not managed carefully?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Local people become rich", "Too many visitors can damage the environment", "Nature improves", "Traditions disappear");
        eqExam(ex, 19, reading, "The word \"sustainable\" is closest in meaning to ___.", Competency.COMPREHENSION, "Từ vựng", 1,
                "temporary", "long-lasting", "expensive", "dangerous");
        eqExam(ex, 20, reading, "Who must work together to keep ecotourism sustainable?", Competency.COMPREHENSION, "Đọc hiểu", 2,
                "Only tourists", "Only the government", "Tourists and local authorities", "Only hotel owners");
    }

    private void seedEnglish11ExamHK2(Subject anh) {
        Exam ex = exam(anh, G11, "Đề tổng hợp Học kì 2 — Lớp 11", "Tổng hợp HK2",
                "Ôn tập Unit 6–10 (mệnh đề to-V, danh động từ/phân từ hoàn thành, câu chẻ, từ nối, danh từ ghép) — chấm điểm & chỉ điểm yếu.", 11);
        eqExam(ex, 1, "We went to the museum ___ about our national heritage.", Competency.APPLICATION, T11_TOINF, 1,
                "learn", "to learn", "learning", "learned");
        eqExam(ex, 2, "It is important ___ our cultural heritage.", Competency.APPLICATION, T11_TOINF, 1,
                "preserve", "to preserve", "preserving", "preserved");
        eqExam(ex, 3, "___ his homework, he went out to play.", Competency.APPLICATION, T11_PERFGER, 2,
                "Finish", "Finishing", "Having finished", "To finish");
        eqExam(ex, 4, "She apologised for ___ the meeting.", Competency.APPLICATION, T11_PERFGER, 2,
                "miss", "missing", "having missed", "to miss");
        eqExam(ex, 5, "It was my teacher ___ inspired me to study hard.", Competency.APPLICATION, T11_CLEFT, 1,
                "which", "who", "whom", "whose");
        eqExam(ex, 6, "It was in 2020 ___ she became independent.", Competency.APPLICATION, T11_CLEFT, 2,
                "who", "which", "that", "when");
        eqExam(ex, 7, "He studied hard; ___, he passed the exam.", Competency.KNOWLEDGE, T11_LINKING, 2,
                "however", "although", "therefore", "because");
        eqExam(ex, 8, "___ the heavy rain, they still went camping.", Competency.APPLICATION, T11_LINKING, 1,
                "Because of", "Despite", "Therefore", "So");
        eqExam(ex, 9, "A place where many species live together is called an ___.", Competency.KNOWLEDGE, T11_COMPNOUN, 1,
                "ecology", "ecosystem", "economy", "ecotour");
        eqExam(ex, 10, "The ___ keeps the balance of nature in a forest.", Competency.KNOWLEDGE, T11_COMPNOUN, 0,
                "food chain", "food shop", "food court", "fast food");
        eqExam(ex, 11, "Choose the word CLOSEST in meaning to \"independent\".", Competency.COMPREHENSION, "Từ vựng", 1,
                "dependent", "self-reliant", "weak", "shy");
        eqExam(ex, 12, "We should ___ our cultural traditions for future generations.", Competency.KNOWLEDGE, "Từ vựng", 0,
                "preserve", "destroy", "forget", "ignore");
        eqExam(ex, 13, "Peer ___ can make teenagers do things they don't want to do.", Competency.KNOWLEDGE, "Từ vựng", 1,
                "power", "pressure", "energy", "force");
        eqExam(ex, 14, "Biodiversity means the ___ of living things in a place.", Competency.KNOWLEDGE, "Từ vựng", 0,
                "variety", "shortage", "lack", "absence");
        String reading = """
                Đọc đoạn văn sau và trả lời các câu hỏi (15–20).

                Becoming independent is an important step in a young person's life. Independent teenagers
                are able to make their own decisions, manage their time and take care of themselves. To
                become more independent, students can start with simple tasks, such as doing their own
                laundry, cooking simple meals and managing their pocket money. Learning these life skills
                helps them build confidence and prepares them for the future. However, being independent
                does not mean doing everything alone. It also means knowing when to ask for help and
                learning from the advice of parents and teachers.""";
        eqExam(ex, 15, reading, "What can independent teenagers do?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Only play games", "Make decisions and take care of themselves", "Depend on others", "Avoid all tasks");
        eqExam(ex, 16, reading, "Which is a simple task mentioned to become independent?", Competency.COMPREHENSION, "Đọc hiểu", 0,
                "Doing their own laundry", "Buying a car", "Building a house", "Running a company");
        eqExam(ex, 17, reading, "What do life skills help teenagers build?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Weakness", "Confidence", "Fear", "Laziness");
        eqExam(ex, 18, reading, "According to the passage, being independent does NOT mean ___.", Competency.COMPREHENSION, "Đọc hiểu", 2,
                "making decisions", "managing time", "doing everything alone", "taking care of oneself");
        eqExam(ex, 19, reading, "The word \"confidence\" is closest in meaning to ___.", Competency.COMPREHENSION, "Từ vựng", 1,
                "doubt", "self-belief", "worry", "fear");
        eqExam(ex, 20, reading, "What should teenagers still do, according to the passage?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Never ask for help", "Ask for help and learn from advice", "Ignore their parents", "Stop studying");
    }

    private void seedEnglish11ExamHK1(Subject anh) {
        Exam ex = exam(anh, G11, "Đề tổng hợp Học kì 1 — Lớp 11", "Tổng hợp HK1",
                "Ôn tập tổng hợp Unit 1–5 (ngữ pháp, từ vựng, đọc hiểu) — chấm điểm & chỉ điểm yếu.", 10);
        // Ngữ pháp Unit 1–5 (tag topic Lớp 11 để chỉ đúng bài yếu)
        eqExam(ex, 1, "I ___ him since we were children.", Competency.APPLICATION, T11_PASTPERF, 1,
                "knew", "have known", "know", "had known");
        eqExam(ex, 2, "She ___ to Da Nang last summer.", Competency.APPLICATION, T11_PASTPERF, 1,
                "goes", "went", "has gone", "going");
        eqExam(ex, 3, "The deadline is tomorrow, so you ___ finish it today.", Competency.APPLICATION, T11_MODAL, 3,
                "must", "have to", "should", "don't have to");
        eqExam(ex, 4, "Students ___ wear uniforms at this school. It is a rule.", Competency.KNOWLEDGE, T11_MODAL, 0,
                "have to", "should", "might", "would");
        eqExam(ex, 5, "I ___ about my future career these days.", Competency.APPLICATION, T11_STATIVE, 1,
                "think", "am thinking", "thinks", "thought");
        eqExam(ex, 6, "This cake ___ delicious.", Competency.KNOWLEDGE, T11_STATIVE, 1,
                "is smelling", "smells", "smell", "smelt");
        eqExam(ex, 7, "___ smart cities requires modern technology.", Competency.APPLICATION, T11_GERSO, 1,
                "Build", "Building", "To building", "Built");
        eqExam(ex, 8, "They discussed ___ the ASEAN summit next year.", Competency.APPLICATION, T11_GERSO, 2,
                "organise", "to organise", "organising", "organised");
        eqExam(ex, 9, "The gases ___ by factories cause global warming.", Competency.APPLICATION, T11_PARTICIPLE, 2,
                "produce", "producing", "produced", "to produce");
        eqExam(ex, 10, "The woman ___ to the students is our new teacher.", Competency.APPLICATION, T11_PARTICIPLE, 1,
                "talk", "talking", "talked", "to talk");
        // Từ vựng theo chủ điểm Unit 1–5
        eqExam(ex, 11, "A balanced ___ is important for good health.", Competency.KNOWLEDGE, "Từ vựng", 0,
                "diet", "traffic", "summit", "gas");
        eqExam(ex, 12, "There is often a generation ___ between parents and children.", Competency.KNOWLEDGE, "Từ vựng", 0,
                "gap", "bridge", "hole", "space");
        eqExam(ex, 13, "A smart city uses technology to become more ___.", Competency.KNOWLEDGE, "Từ vựng", 0,
                "efficient", "lazy", "crowded", "dirty");
        eqExam(ex, 14, "Greenhouse ___ trap heat and warm the Earth.", Competency.KNOWLEDGE, "Từ vựng", 0,
                "gases", "cities", "parents", "diets");
        // Cloze (đoạn quảng cáo)
        String cloze = """
                Đọc đoạn văn sau và chọn đáp án đúng cho mỗi chỗ trống (15–19).

                VIETNAM AND ASIA
                Asia is home to many diverse cultures, and Vietnam, with its rich history and vibrant
                traditions, is one of the (15)____ countries in the region. Its capital, Ha Noi, and its
                largest city, Ho Chi Minh City, (16)____ millions of tourists every year. Vietnam is also
                an active (17)____ of ASEAN, working closely with other nations to (18)____ peace and
                development. Thanks to its efforts, Vietnam (19)____ much stronger over the past decades.""";
        eqExam(ex, 15, cloze, "Chỗ trống (15):", Competency.COMPREHENSION, "Từ vựng", 1,
                "fast-growing", "fastest-growing", "more fast", "most fast");
        eqExam(ex, 16, cloze, "Chỗ trống (16):", Competency.APPLICATION, "Từ vựng", 0,
                "attract", "attracts", "attracting", "to attract");
        eqExam(ex, 17, cloze, "Chỗ trống (17):", Competency.KNOWLEDGE, "Từ vựng", 0,
                "member", "memory", "number", "manager");
        eqExam(ex, 18, cloze, "Chỗ trống (18):", Competency.APPLICATION, "Từ vựng", 0,
                "promote", "promotes", "promoting", "promoted");
        eqExam(ex, 19, cloze, "Chỗ trống (19):", Competency.APPLICATION, T11_PASTPERF, 2,
                "becomes", "became", "has become", "becoming");
        // Đọc hiểu
        String reading = """
                Đọc đoạn văn sau và trả lời các câu hỏi (20–24).

                Global warming is one of the most serious problems facing our planet today. It is mainly
                caused by greenhouse gases, such as carbon dioxide, which are released when we burn coal,
                oil and gas. As the Earth gets warmer, ice at the poles melts and sea levels rise,
                threatening many coastal cities. In addition, extreme weather events like storms and
                droughts are becoming more common. To fight global warming, people should use renewable
                energy, plant more trees and reduce waste. Governments around the world are also working
                together to cut emissions and protect the environment.""";
        eqExam(ex, 20, reading, "What mainly causes global warming?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Planting trees", "Greenhouse gases", "Renewable energy", "Rivers");
        eqExam(ex, 21, reading, "What happens when the Earth gets warmer?", Competency.COMPREHENSION, "Đọc hiểu", 0,
                "Ice melts and sea levels rise", "Cities get colder", "Trees grow faster", "It stops raining");
        eqExam(ex, 22, reading, "Which is NOT mentioned as a way to fight global warming?", Competency.COMPREHENSION, "Đọc hiểu", 2,
                "Using renewable energy", "Planting trees", "Burning more coal", "Reducing waste");
        eqExam(ex, 23, reading, "What are governments around the world doing?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Ignoring the problem", "Working together to cut emissions", "Building more factories", "Cutting down forests");
        eqExam(ex, 24, reading, "What is the passage mainly about?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "How to travel cheaply", "Global warming and how to fight it", "Famous Asian cities", "Healthy eating habits");
    }

    private Exam exam(Subject subject, String title, String level, String description, int idx) {
        return exam(subject, null, title, level, description, idx);
    }

    private Exam exam(Subject subject, String grade, String title, String level, String description, int idx) {
        Exam e = new Exam();
        e.setSubject(subject);
        e.setGrade(grade);
        e.setTitle(title);
        e.setLevel(level);
        e.setDescription(description);
        e.setPremium(true);
        e.setOrderIndex(idx);
        return exams.save(e);
    }

    private Question eqExam(Exam exam, int idx, String text, Competency competency,
                            String topic, int correct, String... opts) {
        return eqExam(exam, idx, null, text, competency, topic, correct, opts);
    }

    private Question eqExam(Exam exam, int idx, String passage, String text, Competency competency,
                            String topic, int correct, String... opts) {
        Question q = new Question();
        // Reuse ESTIMATE scope (an allowed enum value) — exam questions are
        // identified by their exam link, and have no module/subject, so they never
        // leak into the placement or module-estimate queries.
        q.setScope(QuizScope.ESTIMATE);
        q.setExam(exam);
        q.setPassage(passage);
        q.setOrderIndex(idx);
        q.setText(text);
        q.setCompetency(competency);
        q.setCorrectIndex(correct);
        q.setTopic(topic);
        q.setOptions(Arrays.asList(opts));
        return questions.save(q);
    }

    private void seedEnglish12Lessons() {
        englishLesson(1, 1, "Life Stories We Admire", T_PAST, "Past simple vs. Past continuous",
                "Nguyên âm đôi /eɪ/ (age, saved) và /əʊ/ (soldier, wrote, hero, shows).",
                """
                • attend school/college — đi học ở trường/đại học
                • have a happy/difficult childhood — có tuổi thơ hạnh phúc/khó khăn
                • be admired for (something) — được ngưỡng mộ vì điều gì
                • have a long marriage — có cuộc hôn nhân dài lâu
                • impressive achievement — thành tựu ấn tượng""",
                """
                Quá khứ đơn (Past simple) dùng để diễn tả:
                • một hành động ĐÃ HOÀN THÀNH trong quá khứ.
                • các sự kiện chính trong một câu chuyện.

                Quá khứ tiếp diễn (Past continuous — was/were + V-ing) dùng để diễn tả:
                • một hành động ĐANG diễn ra tại một thời điểm cụ thể trong quá khứ.
                • bối cảnh (settings) của câu chuyện.

                Kết hợp hai thì:
                • Khi một hành động xảy ra GIỮA một hành động khác: dùng quá khứ đơn cho hành động NGẮN, quá khứ tiếp diễn cho hành động DÀI.
                • Khi hai hay nhiều hành động cùng xảy ra MỘT LÚC: dùng quá khứ tiếp diễn cho tất cả.""",
                """
                • I read a good book last night. (hành động đã hoàn thành)
                • Mary read a few pages of her book and went to bed. (các sự kiện chính)
                • I was reading a good book at 10 p.m. last night. (đang diễn ra tại một thời điểm)
                • It was raining heavily outside. (bối cảnh câu chuyện)
                • I was reading a book when the phone rang. (hành động dài bị hành động ngắn cắt ngang)
                • While I was reading a book, my mother was watching TV. (hai hành động song song)""");

        englishLesson(2, 2, "A Multicultural World", T_ART, "Articles (review and extension)",
                "Nguyên âm đôi /ɔɪ/, /aɪ/ và /aʊ/.",
                """
                • origin (n) — nguồn gốc
                • popularity (n) — sự phổ biến, được ưa chuộng
                • identity (n) — bản sắc, danh tính
                • festivities (n) — các hoạt động lễ hội
                • trend (n) — xu hướng""",
                """
                Có hai loại mạo từ: không xác định (a/an) và xác định (the).

                Dùng a/an trước danh từ SỐ ÍT, ĐẾM ĐƯỢC khi người nghe/đọc chưa biết cụ thể vật nào (a trước phụ âm, an trước nguyên âm).

                Dùng the khi người nghe/đọc đã biết rõ vật đang nói tới, vì:
                • chỉ có một (duy nhất nói chung, hoặc duy nhất trong ngữ cảnh đó).
                • vật đó đã được nhắc đến trước đó.
                • nói về một loại nhạc cụ.

                Cũng dùng the với:
                • tên nước chứa từ kingdom/state, hoặc tên nước ở dạng số nhiều: the UK, the US, the Philippines.
                • đại dương, biển, dãy núi: the Pacific.

                KHÔNG dùng mạo từ với danh từ số nhiều đếm được hoặc danh từ không đếm được mang nghĩa CHUNG CHUNG.""",
                """
                • I want to buy a souvenir.
                • The sun rises in the east.
                • A boy lost a watch. A woman found the watch and returned it to the boy.
                • I'm learning to play the piano.
                • the UK, the US, the Philippines — The Pacific is the largest of all oceans.
                • Tigers are endangered animals. (nghĩa chung → không mạo từ)""");

        englishLesson(3, 3, "Green Living", T_VPREP, "Verbs with prepositions",
                "Nguyên âm đôi /ɪə/, /eə/ và /ʊə/.",
                """
                • waste (n) — rác thải; sự lãng phí
                • landfill (n) — bãi chôn lấp rác
                • reuse (v) — tái sử dụng
                • packaging (n) — bao bì đóng gói
                • container (n) — hộp/thùng đựng""",
                """
                Nhiều động từ đi kèm một GIỚI TỪ + tân ngữ; nghĩa của cụm vẫn gần với nghĩa gốc của động từ:
                • với about: ask about, care about, talk about, think about, learn about
                • với for: ask for, apply for, apologise for, wait for, prepare for
                • với on: agree on, base on, depend on, rely on
                • với to: introduce to, refer to, respond to, listen to, explain to

                Đôi khi động từ kết hợp với giới từ/trạng từ tạo thành CỤM ĐỘNG TỪ (phrasal verb) có nghĩa KHÁC HẲN nghĩa gốc của động từ chính:
                work out, carry out, turn on, turn off, look for, look after, look up.""",
                """
                • Many people have now started to care about the environment.
                • The future of our planet depends on how we deal with climate change.
                • We should work out some solutions to reducing plastic pollution.
                • My sister is responsible for looking after the plants at home.""");

        englishLesson(4, 3, "Green Living", T_WHICH, "Relative clauses referring to a whole sentence",
                "Nguyên âm đôi /ɪə/, /eə/ và /ʊə/.",
                "(Chung từ vựng với bài Unit 3 — Green living.)",
                """
                Ta có thể dùng một MỆNH ĐỀ QUAN HỆ KHÔNG XÁC ĐỊNH để nói về TOÀN BỘ thông tin ở (các) mệnh đề đứng trước.

                • Mệnh đề này bắt đầu bằng đại từ quan hệ "which".
                • Luôn thêm DẤU PHẨY trước "which".
                • "which" ở đây mang nghĩa "điều đó / việc đó".""",
                """
                • More and more people are interested in recycling nowadays, which is good for the environment.
                  (= Việc ngày càng nhiều người quan tâm tái chế là điều tốt cho môi trường.)
                • Plastic takes hundreds of years to decompose, which is harmful to the environment.
                • I always turn off the fans when I leave the room, which helps save energy.""");

        englishLesson(5, 4, "Urbanisation", T_PRESPERF, "Present perfect (review and extension)",
                "Lược âm của các từ không mang trọng âm trong lời nói nối (connected speech).",
                """
                • afford (v) — đủ khả năng chi trả
                • housing (n) — nhà ở
                • expand (v) — mở rộng, phát triển
                • seek (v) — tìm kiếm
                • unemployment (n) — tình trạng thất nghiệp""",
                """
                Thì hiện tại hoàn thành (have/has + V3/-ed) dùng để diễn tả việc bắt đầu trong quá khứ và VẪN đang tiếp diễn tới hiện tại, hoặc việc vừa hoàn thành trong quá khứ RẤT gần.

                Nói việc gì đó xảy ra BAO NHIÊU LẦN:
                It/This/That + be + the first/the second time + S + have/has (done)...

                Nói về một TRẢI NGHIỆM DUY NHẤT:
                It/This/That/Noun hoặc Gerund + be + the best/the worst/the only/the most... + S + have/has (ever done)...""",
                """
                • A lot of young people have moved to big cities to work or study.
                • This is the second time I have visited this city.
                • It is not the first time I have heard about urbanisation.
                • That is the worst meal I have ever had in this city.
                • Moving to the city is the best decision my parents have ever made in their life.""");

        englishLesson(6, 4, "Urbanisation", T_DBLCOMP, "Double comparatives to show change",
                "Lược âm của các từ không mang trọng âm trong lời nói nối (connected speech).",
                "(Chung từ vựng với bài Unit 4 — Urban life.)",
                """
                Dùng SO SÁNH KÉP để diễn tả sự THAY ĐỔI (tăng/giảm dần):
                • "...er and ...er" với tính từ ngắn (bigger and bigger).
                • "more and more + adj" với tính từ dài (more and more polluted).

                Cũng dùng so sánh kép để nói HAI điều thay đổi CÙNG nhau (càng... càng...):
                • The + so sánh hơn..., the + so sánh hơn...""",
                """
                • Towns are getting bigger and bigger.
                • The air is becoming more and more polluted.
                • There are more and more high-rise buildings in the city.
                • The bigger the city gets, the more crowded it becomes.
                • The more we invest in rural areas, the more we can help people there.""");

        englishLesson(7, 5, "The World of Work", T_SENTENCE,
                "Simple, compound, and complex sentences (review and extension)",
                "Nhấn trọng âm ở trợ động từ (auxiliary) và động từ khiếm khuyết (modal verbs).",
                """
                • challenging (adj) — đầy thử thách (một cách thú vị)
                • relevant (adj) — liên quan, phù hợp
                • bonus (n) — tiền thưởng
                • employ (v) — thuê, tuyển dụng
                • rewarding (adj) — đáng làm, mang lại sự hài lòng""",
                """
                Câu ĐƠN (simple sentence): chỉ có MỘT mệnh đề độc lập.

                Câu GHÉP (compound sentence): có từ HAI mệnh đề độc lập trở lên, được nối bằng:
                • liên từ kết hợp: and, but, or, nor, yet, so
                • liên từ tương liên: not only ... but also
                • trạng từ liên kết: as a result, moreover, in fact, on the other hand

                Câu PHỨC (complex sentence): có MỘT mệnh đề độc lập + ít nhất MỘT mệnh đề phụ thuộc, nối bằng liên từ phụ thuộc: when, while, because, although, if, so that.""",
                """
                • My brother didn't apply for the job. (câu đơn)
                • My brother didn't apply for the job, but he was offered an apprenticeship. (câu ghép)
                • Being a nurse is a very tiring job; moreover, you don't earn a high salary. (câu ghép)
                • When I was younger, I wanted to become a driver. (câu phức)
                • Because my brother is often late for work, he is never promoted. (câu phức)""");

        englishLesson(G12, 8, 6, "Artificial Intelligence", T12_CAUSATIVE,
                "Active and passive causatives",
                "Từ đồng âm (homophones).",
                """
                - artificial intelligence (AI): trí tuệ nhân tạo
                - technology (n): công nghệ
                - application (n): ứng dụng
                - automate (v): tự động hoá
                - device (n): thiết bị""",
                """
                Thể SAI KHIẾN (causative): nhờ/khiến ai đó làm việc gì.

                Chủ động: have + O (người) + V (nguyên mẫu); get + O (người) + to V.
                Ví dụ: I had the technician fix my computer. / I got the technician to fix my computer.

                Bị động: have/get + O (vật) + V3/-ed (nhờ ai làm gì cho vật đó).
                Ví dụ: I had my computer fixed. / I got my phone repaired.""",
                """
                - We had a robot assemble the parts. (chủ động: have + O + V)
                - She got her assistant to write the report. (chủ động: get + O + to V)
                - They had the software updated by AI. (bị động: have + O + V3)
                - I get my data backed up automatically. (bị động: get + O + V3)""");

        englishLesson(G12, 9, 7, "The World of Mass Media", T12_ADVMANNER,
                "Adverbial clauses of manner and result",
                "Nối âm /r/ giữa hai nguyên âm.",
                """
                - mass media (n): truyền thông đại chúng
                - broadcast (v): phát sóng
                - influence (v/n): ảnh hưởng
                - digital (adj): kỹ thuật số
                - traditional (adj): truyền thống""",
                """
                Mệnh đề trạng ngữ chỉ CÁCH THỨC (manner): as, as if, as though (như thể).
                Ví dụ: He talks as if he knew everything.

                Mệnh đề trạng ngữ chỉ KẾT QUẢ (result): so + adj/adv + that; such + (a/an) + adj + N + that (đến nỗi mà).
                Ví dụ: The news was so shocking that everyone talked about it.""",
                """
                - She reports the news as a professional does. (cách thức)
                - He acts as if he were a famous reporter. (cách thức, as if)
                - The article was so interesting that it went viral. (kết quả, so ... that)
                - It was such a powerful story that many people shared it. (kết quả, such ... that)""");

        englishLesson(G12, 10, 8, "Wildlife Conservation", T12_ADVCOND,
                "Adverbial clauses of condition and comparison",
                "Đồng hoá âm (assimilation).",
                """
                - wildlife (n): động vật hoang dã
                - conservation (n): sự bảo tồn
                - endangered (adj): có nguy cơ tuyệt chủng
                - habitat (n): môi trường sống
                - extinct (adj): tuyệt chủng""",
                """
                Mệnh đề trạng ngữ chỉ ĐIỀU KIỆN: if, unless (trừ khi), as long as, provided that (miễn là).
                Ví dụ: Unless we act now, many species will disappear.

                Mệnh đề trạng ngữ chỉ SO SÁNH: than, as ... as, the + so sánh hơn ..., the + so sánh hơn (càng... càng).
                Ví dụ: The more forests we protect, the safer wildlife becomes.""",
                """
                - We can save animals as long as we protect their habitats. (điều kiện)
                - Unless people stop hunting, tigers will become extinct. (điều kiện)
                - This species is more endangered than that one. (so sánh)
                - The more we destroy nature, the faster animals disappear. (so sánh kép)""");

        englishLesson(G12, 11, 9, "Career Paths", T12_PHRASAL3,
                "Three-word phrasal verbs",
                "Trọng âm và nhịp điệu câu.",
                """
                - career (n): sự nghiệp
                - qualification (n): bằng cấp, trình độ
                - apply (v): ứng tuyển
                - opportunity (n): cơ hội
                - promotion (n): sự thăng chức""",
                """
                Cụm động từ BA THÀNH PHẦN (three-word phrasal verbs) = động từ + trạng từ + giới từ, mang nghĩa cố định, thường có tân ngữ theo sau.

                Ví dụ hay gặp: look forward to (mong đợi), come up with (nghĩ ra), catch up with (theo kịp), keep up with (bắt kịp), look up to (kính trọng), get on with (hoà hợp/tiếp tục), run out of (cạn kiệt).""",
                """
                - I look forward to starting my new career.
                - She came up with a brilliant business idea.
                - You must keep up with new skills in your field.
                - He looks up to his mentor at work.""");

        englishLesson(G12, 12, 10, "Lifelong Learning", T12_REPORTED,
                "Reported speech: orders, requests, offers, and advice",
                "Ngữ điệu trong câu hỏi (ôn tập).",
                """
                - lifelong learning: học tập suốt đời
                - skill (n): kỹ năng
                - improve (v): cải thiện
                - knowledge (n): kiến thức
                - motivate (v): tạo động lực""",
                """
                Tường thuật MỆNH LỆNH, YÊU CẦU, ĐỀ NGHỊ, LỜI KHUYÊN dùng: động từ tường thuật + O + (not) to V.

                - Mệnh lệnh: tell somebody to do. (order)
                - Yêu cầu: ask somebody to do. (request)
                - Đề nghị: offer to do. (offer)
                - Lời khuyên: advise somebody to do. (advice)""",
                """
                - "Study hard!" → The teacher told us to study hard.
                - "Please help me." → She asked me to help her.
                - "I'll carry your bag." → He offered to carry my bag.
                - "You should keep learning." → My mentor advised me to keep learning.""");
    }

    private void englishLesson(int order, int unitNo, String unitTitle, String topic,
                               String grammarName, String pronunciation, String vocabulary,
                               String theory, String examples) {
        englishLesson(G12, order, unitNo, unitTitle, topic, grammarName, pronunciation,
                vocabulary, theory, examples);
    }

    private void englishLesson(String grade, int order, int unitNo, String unitTitle, String topic,
                               String grammarName, String pronunciation, String vocabulary,
                               String theory, String examples) {
        EnglishLesson l = new EnglishLesson();
        l.setGrade(grade);
        l.setUnitNo(unitNo);
        l.setUnitTitle(unitTitle);
        l.setTopic(topic);
        l.setGrammarName(grammarName);
        l.setPronunciation(pronunciation);
        l.setVocabulary(vocabulary);
        l.setTheory(theory);
        l.setExamples(examples);
        l.setOrderIndex(order);
        englishLessons.save(l);
    }

    /** Gom nội dung bài học của 1 lớp thành tài liệu nguồn để AI (Gemini) bám vào soạn giáo trình. */
    private void seedGradeReference(Subject anh, String grade, String title) {
        List<EnglishLesson> ls = englishLessons.findByGradeOrderByOrderIndexAsc(grade);
        if (ls.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder(
                "GIÁO TRÌNH TIẾNG ANH " + grade.replace("Lớp ", "")
                        + " — SGK Global Success (Ngữ pháp, từ vựng, phát âm).\n\n");
        for (EnglishLesson l : ls) {
            sb.append("== UNIT ").append(l.getUnitNo()).append(": ").append(l.getUnitTitle())
                    .append(" — ").append(l.getGrammarName()).append(" ==\n");
            sb.append("Phát âm: ").append(l.getPronunciation()).append('\n');
            sb.append("Ngữ pháp:\n").append(l.getTheory()).append('\n');
            sb.append("Ví dụ:\n").append(l.getExamples()).append('\n');
            sb.append("Từ vựng:\n").append(l.getVocabulary()).append("\n\n");
        }
        ReferenceMaterial rm = new ReferenceMaterial();
        rm.setSubject(anh);
        rm.setTitle(title);
        rm.setContent(sb.toString());
        rm.setCreatedByEmail("admin@pathstudy.vn");
        referenceMaterials.save(rm);
    }

    private void seedEnglish10Lessons() {
        englishLesson(G10, 1, 1, "Family Life", T10_PRES, "Present simple vs. present continuous",
                "Tổ hợp phụ âm đầu (consonant blends): /br/, /kr/, /tr/.",
                """
                - breadwinner (n): người trụ cột kiếm tiền nuôi gia đình
                - housework (n): việc nhà (nấu ăn, dọn dẹp, giặt giũ)
                - groceries (n): thực phẩm và đồ dùng mua ở cửa hàng/siêu thị
                - homemaker (n): người nội trợ, lo việc nhà
                - heavy lifting (n): việc nhấc/khiêng vật nặng""",
                """
                Hiện tại đơn (Present simple) dùng để nói về THÓI QUEN hoặc việc làm THƯỜNG XUYÊN.

                Hiện tại tiếp diễn (Present continuous = am/is/are + V-ing) dùng để nói về việc đang xảy ra NGAY TẠI THỜI ĐIỂM NÓI.

                Lưu ý: KHÔNG dùng hiện tại tiếp diễn với động từ chỉ trạng thái (stative verbs) như like, love, need, want, know, agree.""",
                """
                - My mother cooks every day. (thói quen → hiện tại đơn)
                - My mother isn't cooking now. She's working in her office. (đang xảy ra → hiện tại tiếp diễn)
                - I love this song. (KHÔNG nói "I am loving")""");

        englishLesson(G10, 2, 2, "Humans and the Environment", T10_FUTURE,
                "The future with will and be going to",
                "Tổ hợp phụ âm đầu: /kl/, /pl/, /gr/, /pr/.",
                """
                - household appliances: thiết bị gia dụng (tủ lạnh, TV...)
                - energy (n): năng lượng
                - carbon footprint: dấu chân carbon (lượng CO2 thải ra)
                - litter (n): rác vứt bừa nơi công cộng
                - eco-friendly (adj): thân thiện với môi trường""",
                """
                Dùng "will" để nói về:
                - quyết định NGAY LÚC NÓI. Ví dụ: This shirt looks beautiful. I will buy it.
                - dự đoán dựa trên SUY NGHĨ/NIỀM TIN. Ví dụ: I think our team will win the competition.

                Dùng "be going to" để nói về:
                - kế hoạch đã ĐỊNH TRƯỚC lúc nói. Ví dụ: I have made a reservation. We are going to have dinner at the Chinese restaurant.
                - dự đoán dựa trên BẰNG CHỨNG nhìn thấy/biết. Ví dụ: Look at the dark clouds. It is going to rain soon.""",
                """
                - I will help you with your bags. (quyết định lúc nói)
                - I think it will be sunny tomorrow. (dự đoán theo suy nghĩ)
                - We are going to build a new school. (kế hoạch định trước)
                - Look at the dark clouds. It is going to rain. (dự đoán theo bằng chứng)""");

        englishLesson(G10, 3, 2, "Humans and the Environment", T10_PASSIVE, "Passive voice",
                "Tổ hợp phụ âm đầu: /kl/, /pl/, /gr/, /pr/.",
                "(Chung từ vựng với bài Unit 2 — The environment.)",
                """
                Dùng câu BỊ ĐỘNG (passive voice) khi người/vật gây ra hành động KHÔNG quan trọng, KHÔNG biết, hoặc ta muốn NHẤN MẠNH vào hành động chứ không phải người làm.

                Cấu trúc: be (chia theo thì) + V3/-ed (+ by + tác nhân, nếu cần).""",
                """
                - The school playground is cleaned up every day (by students). (hiện tại đơn bị động)
                - More trees will be planted in the neighbourhood. (tương lai bị động)
                - Important environmental issues were discussed at the meeting. (quá khứ bị động)""");

        englishLesson(G10, 4, 3, "Music", T10_COMPOUND, "Compound sentences",
                "Trọng âm của từ có hai âm tiết.",
                """
                - perform (v): biểu diễn
                - judge (n): giám khảo
                - audience (n): khán giả
                - talented (adj): tài năng
                - single (n): đĩa đơn (bản thu một bài hát)""",
                """
                Câu GHÉP (compound sentence) gồm HAI mệnh đề độc lập trở lên, nối với nhau bằng LIÊN TỪ KẾT HỢP (coordinating conjunction): and, or, but, so.""",
                """
                - It was raining, but they still went to the outdoor show.
                - I am a jazz fan, and my favourite style is from the late 1960s.
                - He has a maths exam that day, so he can't go to the festival.""");

        englishLesson(G10, 5, 3, "Music", T10_INF, "To-infinitives and bare infinitives",
                "Trọng âm của từ có hai âm tiết.",
                "(Chung từ vựng với bài Unit 3 — Music.)",
                """
                Một số động từ theo sau bởi TO-INFINITIVE (to + V): decide, expect, plan, want, promise, agree, hope, hesitate, ask...

                Một số động từ theo sau bởi BARE INFINITIVE (V nguyên mẫu không "to"): make, let, hear, notice... (thường sau tân ngữ).""",
                """
                - Her fans planned to send her a surprise present. (plan + to V)
                - The band decided to delay their live concert. (decide + to V)
                - Their performance made us fall asleep. (make + O + V nguyên mẫu)
                - Her parents won't let her watch such TV shows. (let + O + V nguyên mẫu)""");

        englishLesson(G10, 6, 4, "For a Better Community", T10_PAST,
                "Past simple vs. past continuous with when and while",
                "Trọng âm của từ hai âm tiết viết giống nhau (danh từ/động từ).",
                """
                - donate (v): quyên góp (tiền, đồ...)
                - volunteer (n): tình nguyện viên
                - generous (adj): hào phóng
                - remote (adj): xa xôi, hẻo lánh
                - benefit (v): mang lại lợi ích
                - Hậu tố tính từ: -ed vs -ing (interested/interesting), -ful vs -less (hopeful/hopeless)""",
                """
                Dùng QUÁ KHỨ TIẾP DIỄN (was/were + V-ing) cho hành động DÀI đang diễn ra trong quá khứ.
                Dùng QUÁ KHỨ ĐƠN cho hành động NGẮN xen vào, cắt ngang hành động dài đó.

                - "when" thường đứng trước hành động NGẮN (quá khứ đơn).
                - "while" thường đứng trước hành động DÀI (quá khứ tiếp diễn).""",
                """
                - I was reading an article when she called.
                - While I was reading an article, she called.
                - While Lan was working as a volunteer, she met an old friend.
                - We saw many unhappy children while we were helping people in remote areas.""");

        englishLesson(G10, 7, 5, "Inventions", T10_PRESPERF, "Present perfect",
                "Trọng âm của danh từ có ba âm tiết.",
                """
                - experiment (n): thí nghiệm
                - device (n): thiết bị
                - laboratory (n): phòng thí nghiệm
                - hardware (n): phần cứng
                - software (n): phần mềm
                - equipment (n): trang thiết bị""",
                """
                Thì HIỆN TẠI HOÀN THÀNH (have/has + V3/-ed) dùng để nói về:
                - việc xảy ra trong quá khứ nhưng VẪN đúng/quan trọng ở hiện tại. Ví dụ: I have lost my key. Now I can't open the door.
                - việc bắt đầu trong quá khứ và VẪN đang tiếp diễn (thường với since/for). Ví dụ: They have lived here for a year.
                - việc vừa hoàn thành trong quá khứ RẤT gần (thường với just/recently). Ví dụ: He has just finished his homework.""",
                """
                - They have just found a suitable solution to the problem.
                - Since people invented the first computer, they have created many more inventions.
                - The woman is very angry because her son has lost his smartphone.""");

        englishLesson(G10, 8, 5, "Inventions", T10_GERUND, "Gerunds and to-infinitives",
                "Trọng âm của danh từ có ba âm tiết.",
                "(Chung từ vựng với bài Unit 5 — Inventions.)",
                """
                Dùng DANH ĐỘNG TỪ (gerund = V-ing):
                - sau các động từ như avoid, enjoy, finish. Ví dụ: I enjoy cooking.
                - làm CHỦ NGỮ của câu. Ví dụ: Learning English is fun.

                Dùng TO-INFINITIVE (to + V):
                - sau các động từ như want, decide, allow. Ví dụ: My parents don't allow me to use a smartphone.
                - sau tính từ để nêu ý kiến, bắt đầu bằng "It's...". Ví dụ: It's fun to learn English.
                - làm chủ ngữ. Ví dụ: To learn English is fun.

                Lưu ý: like, love, hate có thể theo sau bởi CẢ HAI. Ví dụ: I like playing / to play computer games.""",
                """
                - Many children enjoy using modern devices nowadays. (enjoy + V-ing)
                - I decided to study computer science at university. (decide + to V)
                - Playing language games on a smartphone is fun. (V-ing làm chủ ngữ)
                - It is very convenient to study with a smartphone. (It's + adj + to V)""");

        englishLesson(G10, 9, 6, "Gender Equality", T10_PASSMODAL, "Passive voice with modals",
                "Trọng âm của tính từ và động từ có ba âm tiết.",
                """
                - gender equality (n): bình đẳng giới
                - discrimination (n): sự phân biệt đối xử
                - right (n): quyền
                - responsibility (n): trách nhiệm
                - equal (adj): bình đẳng, ngang bằng""",
                """
                Câu bị động với ĐỘNG TỪ KHIẾM KHUYẾT (modal verbs: can, could, should, must, will...).

                Cấu trúc: modal + be + V3/-ed (+ by + tác nhân).

                Dùng khi nhấn mạnh vào hành động và kèm ý nghĩa của modal (khả năng, lời khuyên, bắt buộc...).""",
                """
                - Women must be treated equally at work. (must be + V3)
                - Gender discrimination should be eliminated. (should be + V3)
                - This problem can be solved with better laws. (can be + V3)
                - Equal rights will be given to everyone. (will be + V3)""");

        englishLesson(G10, 10, 7, "Viet Nam and International Organisations", T10_COMPSUP,
                "Comparative and superlative adjectives",
                "Trọng âm của từ có hơn ba âm tiết.",
                """
                - organisation (n): tổ chức
                - member (n): thành viên
                - cooperation (n): sự hợp tác
                - develop (v): phát triển
                - support (v/n): hỗ trợ; sự hỗ trợ""",
                """
                So sánh HƠN (comparative) và so sánh NHẤT (superlative) của tính từ:
                - Tính từ ngắn: thêm -er / -est. Ví dụ: tall → taller → the tallest.
                - Tính từ dài: dùng more / the most. Ví dụ: important → more important → the most important.
                - Bất quy tắc: good → better → the best; bad → worse → the worst.

                So sánh hơn thường đi với "than"; so sánh nhất thường đi với "the".""",
                """
                - Viet Nam is becoming stronger than before.
                - ASEAN is one of the most important organisations in the region.
                - This is the best solution for both countries.
                - Cooperation is more effective than competition.""");

        englishLesson(G10, 11, 8, "New Ways to Learn", T10_RELCLAUSE,
                "Relative clauses (defining and non-defining)",
                "Trọng âm câu (sentence stress).",
                """
                - blended learning: học kết hợp (trực tuyến + trực tiếp)
                - device (n): thiết bị
                - online (adj/adv): trực tuyến
                - flexible (adj): linh hoạt
                - access (v/n): truy cập""",
                """
                Mệnh đề quan hệ dùng đại từ quan hệ: who (người), which (vật), that (người/vật), whose (sở hữu).

                - Mệnh đề quan hệ XÁC ĐỊNH (defining): cần thiết để xác định danh từ, KHÔNG có dấu phẩy.
                - Mệnh đề quan hệ KHÔNG XÁC ĐỊNH (non-defining): bổ sung thông tin thêm, CÓ dấu phẩy, KHÔNG dùng "that".""",
                """
                - The app which/that helps students learn is very popular. (xác định)
                - Students who study online can learn anytime. (xác định)
                - My teacher, who is very kind, uses online tools. (không xác định, có phẩy)
                - This is the girl whose laptop was broken. (whose - sở hữu)""");

        englishLesson(G10, 12, 9, "Protecting the Environment", T10_REPORTED, "Reported speech",
                "Nhịp điệu (rhythm).",
                """
                - pollution (n): sự ô nhiễm
                - protect (v): bảo vệ
                - reduce (v): giảm bớt
                - waste (n): rác thải
                - solution (n): giải pháp""",
                """
                Câu TƯỜNG THUẬT (reported speech): thuật lại lời người khác nói.

                Khi lùi thì (động từ tường thuật ở quá khứ): hiện tại đơn → quá khứ đơn; hiện tại tiếp diễn → quá khứ tiếp diễn; will → would; can → could...
                Đổi đại từ và trạng từ chỉ thời gian/nơi chốn cho phù hợp (now → then, today → that day, here → there...).""",
                """
                - "I recycle every day." → She said (that) she recycled every day.
                - "We will plant trees." → They said they would plant trees.
                - "I am cleaning the beach." → He said he was cleaning the beach.
                - "You should save water." → She told me I should save water.""");

        englishLesson(G10, 13, 10, "Ecotourism", T10_CONDITIONAL,
                "Conditional sentences Type 1 and Type 2",
                "Ngữ điệu (intonation).",
                """
                - ecotourism (n): du lịch sinh thái
                - attraction (n): điểm thu hút
                - preserve (v): bảo tồn
                - local (adj): địa phương
                - sustainable (adj): bền vững""",
                """
                Câu điều kiện LOẠI 1 (có thật ở hiện tại/tương lai):
                If + hiện tại đơn, ... will + V. Ví dụ: If we protect nature, tourists will come.

                Câu điều kiện LOẠI 2 (không có thật/giả định ở hiện tại):
                If + quá khứ đơn, ... would + V. (be → were cho mọi ngôi). Ví dụ: If I were rich, I would travel the world.""",
                """
                - If you visit the Mekong Delta, you will enjoy ecotours. (loại 1)
                - If people don't pollute, nature will recover. (loại 1)
                - If I were a tour guide, I would protect the environment. (loại 2)
                - If we had more trees, the air would be cleaner. (loại 2)""");
    }

    private void seedEnglish11Lessons() {
        englishLesson(G11, 1, 1, "A Long and Healthy Life", T11_PASTPERF,
                "Past simple vs. Present perfect",
                "Dạng nhấn và dạng yếu của trợ động từ (strong/weak forms).",
                """
                - fitness (n): sự khoẻ khoắn, thể lực
                - well-being (n): sự khoẻ mạnh (thể chất & tinh thần)
                - nutrition (n): dinh dưỡng
                - lifestyle (n): lối sống
                - immune system: hệ miễn dịch""",
                """
                QUÁ KHỨ ĐƠN: hành động đã kết thúc, có MỐC thời gian xác định trong quá khứ (yesterday, last year, in 2010, ago).

                HIỆN TẠI HOÀN THÀNH (have/has + V3): việc bắt đầu trong quá khứ và còn liên quan hiện tại, hoặc chưa nêu mốc thời gian (already, yet, just, ever, never, since, for).""",
                """
                - I visited the doctor yesterday. (quá khứ đơn - có mốc)
                - I have visited the doctor twice this month. (HTHT - còn liên quan hiện tại)
                - She started exercising in 2020. (quá khứ đơn)
                - She has exercised regularly since 2020. (HTHT - since)""");

        englishLesson(G11, 2, 2, "The Generation Gap", T11_MODAL,
                "Modal verbs: must, have to, should",
                "Dạng rút gọn (contracted forms).",
                """
                - generation gap: khoảng cách thế hệ
                - conflict (n): xung đột
                - viewpoint (n): quan điểm
                - respect (v/n): tôn trọng
                - traditional (adj): truyền thống""",
                """
                MUST: sự bắt buộc mạnh, thường do người nói tự thấy cần. Ví dụ: You must respect your parents.
                HAVE TO: sự bắt buộc do quy định/hoàn cảnh bên ngoài. Ví dụ: Students have to wear uniforms.
                SHOULD: lời khuyên, nên làm. Ví dụ: You should listen to different opinions.

                Phủ định: mustn't (cấm) khác don't have to (không cần thiết).""",
                """
                - Children must obey the family rules. (bắt buộc)
                - I have to be home before 10 p.m. (quy định)
                - You shouldn't argue with your parents. (lời khuyên)
                - You don't have to agree, but you should respect them. (không bắt buộc + lời khuyên)""");

        englishLesson(G11, 3, 3, "Cities of the Future", T11_STATIVE,
                "Stative verbs in the continuous form; Linking verbs",
                "Nối phụ âm cuối với nguyên âm đầu.",
                """
                - smart city: thành phố thông minh
                - infrastructure (n): cơ sở hạ tầng
                - sustainable (adj): bền vững
                - efficient (adj): hiệu quả
                - liveable (adj): đáng sống""",
                """
                Động từ CHỈ TRẠNG THÁI (stative verbs: think, feel, taste, look, have...) thường KHÔNG dùng tiếp diễn. Nhưng khi chuyển sang nghĩa HÀNH ĐỘNG, có thể dùng tiếp diễn.
                Ví dụ: I think it's good. (quan điểm) — I'm thinking about the future. (đang suy nghĩ).

                LINKING VERBS (be, become, seem, look, feel, taste, smell, sound...) nối chủ ngữ với tính từ (bổ ngữ), không dùng trạng từ.
                Ví dụ: The city looks modern. (không nói "looks modernly").""",
                """
                - I'm having lunch now. (have = hành động → tiếp diễn được)
                - This city seems very liveable. (linking verb + adj)
                - The air feels fresh in the smart city. (linking verb + adj)
                - She is being very helpful today. (trạng thái tạm thời)""");

        englishLesson(G11, 4, 4, "ASEAN and Viet Nam", T11_GERSO,
                "Gerunds as subjects and objects",
                "Lược bỏ nguyên âm (elision).",
                """
                - ASEAN: Hiệp hội các quốc gia Đông Nam Á
                - cooperation (n): sự hợp tác
                - integration (n): sự hội nhập
                - summit (n): hội nghị thượng đỉnh
                - member state: quốc gia thành viên""",
                """
                DANH ĐỘNG TỪ (gerund = V-ing) có thể làm:
                - CHỦ NGỮ của câu. Ví dụ: Joining ASEAN benefits Viet Nam.
                - TÂN NGỮ sau động từ (enjoy, avoid, consider, suggest, mind...). Ví dụ: They suggested holding a summit.
                - TÂN NGỮ sau giới từ. Ví dụ: They are interested in cooperating.""",
                """
                - Cooperating with other countries is important. (chủ ngữ)
                - Viet Nam considers joining more organisations. (tân ngữ sau động từ)
                - They talked about improving the economy. (sau giới từ)
                - Promoting peace is ASEAN's main goal. (chủ ngữ)""");

        englishLesson(G11, 5, 5, "Global Warming", T11_PARTICIPLE,
                "Present participle and past participle clauses",
                "Trọng âm và nhịp điệu câu.",
                """
                - global warming: sự nóng lên toàn cầu
                - greenhouse gas: khí nhà kính
                - emission (n): sự phát thải
                - climate change: biến đổi khí hậu
                - carbon dioxide (CO2): khí cacbonic""",
                """
                Mệnh đề PHÂN TỪ giúp rút gọn mệnh đề quan hệ/trạng ngữ:
                - Hiện tại phân từ (V-ing): mang nghĩa CHỦ ĐỘNG. Ví dụ: The factory producing gas is huge. (= which produces).
                - Quá khứ phân từ (V3/-ed): mang nghĩa BỊ ĐỘNG. Ví dụ: Gases produced by cars pollute the air. (= which are produced).""",
                """
                - Countries emitting more CO2 should act first. (V-ing, chủ động)
                - The heat trapped by gases warms the Earth. (V3, bị động)
                - Feeling worried, scientists warned the world. (V-ing chỉ nguyên nhân)
                - Affected by droughts, many farms failed. (V3, bị động)""");

        englishLesson(G11, 6, 6, "Preserving Our Heritage", T11_TOINF,
                "To-infinitive clauses",
                "Ngữ điệu trong câu kể, câu mệnh lệnh và liệt kê.",
                """
                - heritage (n): di sản
                - preserve (v): bảo tồn
                - monument (n): di tích, tượng đài
                - restore (v): trùng tu, phục hồi
                - cultural (adj): thuộc văn hoá""",
                """
                Mệnh đề TO-INFINITIVE (to + V) dùng để nêu MỤC ĐÍCH hoặc bổ nghĩa:
                - Chỉ mục đích (= in order to / so as to). Ví dụ: We work hard to preserve our heritage.
                - Sau tính từ: It's important to protect monuments.
                - Sau danh từ: There is a lot to do.""",
                """
                - People donate money to restore the old temple. (mục đích)
                - It is necessary to preserve cultural values. (sau tính từ)
                - She was happy to join the heritage project. (sau tính từ)
                - We have a duty to protect our heritage. (sau danh từ)""");

        englishLesson(G11, 7, 7, "Education Options for School-Leavers", T11_PERFGER,
                "Perfect gerunds and perfect participle clauses",
                "Ngữ điệu trong câu hỏi Wh- và Yes/No.",
                """
                - school-leaver (n): người vừa rời trường
                - vocational (adj): thuộc dạy nghề
                - apprenticeship (n): việc học nghề
                - qualification (n): bằng cấp
                - career (n): sự nghiệp""",
                """
                DANH ĐỘNG TỪ HOÀN THÀNH (perfect gerund = having + V3) và PHÂN TỪ HOÀN THÀNH (perfect participle = having + V3) diễn tả hành động XẢY RA TRƯỚC hành động chính.

                - Perfect gerund: sau động từ/giới từ. Ví dụ: She admitted having cheated.
                - Perfect participle: rút gọn, nhấn mạnh việc xảy ra trước. Ví dụ: Having finished school, he applied for a job.""",
                """
                - Having left school, she took a vocational course. (phân từ hoàn thành)
                - He thanked me for having helped him choose a career.
                - Having studied hard, they passed the exam.
                - She regretted not having applied earlier. (danh động từ hoàn thành, phủ định)""");

        englishLesson(G11, 8, 8, "Becoming Independent", T11_CLEFT,
                "Cleft sentences with It is/was ... that/who ...",
                "Ngữ điệu trong lời mời, gợi ý và yêu cầu lịch sự.",
                """
                - independent (adj): độc lập, tự lập
                - responsible (adj): có trách nhiệm
                - budget (v/n): lập ngân sách; ngân sách
                - decision (n): quyết định
                - confident (adj): tự tin""",
                """
                Câu CHẺ (cleft sentence) dùng để NHẤN MẠNH một thành phần của câu:
                It + is/was + (thành phần nhấn mạnh) + that/who + phần còn lại.

                - Nhấn mạnh người: dùng who hoặc that.
                - Nhấn mạnh vật/thời gian/nơi chốn: dùng that.""",
                """
                - It is teenagers who need to learn life skills. (nhấn mạnh người)
                - It was my mother that taught me to cook. (nhấn mạnh người)
                - It is self-confidence that helps you succeed. (nhấn mạnh vật)
                - It was last year that I started living on my own. (nhấn mạnh thời gian)""");

        englishLesson(G11, 9, 9, "Social Issues", T11_LINKING,
                "Linking words and phrases",
                "Ngữ điệu trong câu hỏi lựa chọn.",
                """
                - social issue: vấn đề xã hội
                - peer pressure: áp lực đồng trang lứa
                - poverty (n): sự nghèo đói
                - discrimination (n): sự phân biệt đối xử
                - awareness (n): sự nhận thức""",
                """
                TỪ NỐI & CỤM TỪ NỐI giúp liên kết ý:
                - Thêm ý: in addition, moreover, furthermore.
                - Tương phản: however, nevertheless, on the other hand, despite/in spite of + N.
                - Nguyên nhân/kết quả: therefore, as a result, because of + N, due to + N.
                - Ví dụ: for example, for instance.""",
                """
                - Poverty is serious; however, we can help. (tương phản)
                - Many teens feel peer pressure. Therefore, they need support. (kết quả)
                - In addition, education raises awareness. (thêm ý)
                - Despite the difficulties, they kept trying. (tương phản + N)""");

        englishLesson(G11, 10, 10, "The Ecosystem", T11_COMPNOUN,
                "Compound nouns",
                "Ngữ điệu trong câu hỏi đuôi.",
                """
                - ecosystem (n): hệ sinh thái
                - biodiversity (n): đa dạng sinh học
                - species (n): loài
                - habitat (n): môi trường sống
                - food chain: chuỗi thức ăn""",
                """
                DANH TỪ GHÉP (compound noun) = hai (hoặc nhiều) từ ghép lại thành một danh từ có nghĩa mới.
                - Danh từ + danh từ: food chain, rainforest, wildlife.
                - Tính từ + danh từ: greenhouse.
                - Có thể viết liền (rainforest), có gạch nối (well-being) hoặc tách rời (food chain).
                Trọng âm thường rơi vào từ ĐẦU tiên.""",
                """
                - A national park protects many species. (national park)
                - The food chain keeps the ecosystem balanced. (food chain)
                - Rainforests are home to great biodiversity. (rainforest)
                - Climate change threatens many habitats. (climate change)""");
    }

    private void seedEnglish10Placement(Subject anh) {
        // 1–4: Phát âm & trọng âm
        pqTopic(anh, G10, 1, "Choose the word whose underlined part is pronounced differently: bre_a_d, h_ea_d, br_ea_k, r_ea_dy.",
                Competency.KNOWLEDGE, "Phát âm", 2, "bread", "head", "break", "ready");
        pqTopic(anh, G10, 2, "Choose the word whose underlined \"ch\" is pronounced differently.",
                Competency.KNOWLEDGE, "Phát âm", 2, "child", "chair", "chemistry", "church");
        pqTopic(anh, G10, 3, "Choose the word that has a different stress pattern.",
                Competency.KNOWLEDGE, "Phát âm", 3, "exercise", "benefit", "different", "contribute");
        pqTopic(anh, G10, 4, "Choose the word that has a different stress pattern.",
                Competency.KNOWLEDGE, "Phát âm", 2, "happy", "pretty", "relax", "busy");

        // 5–17: Ngữ pháp (mỗi câu 1 chủ đề Lớp 10 để map bài học)
        pqTopic(anh, G10, 5, "Look! The children ___ football in the yard.", Competency.KNOWLEDGE, T10_PRES, 2,
                "play", "plays", "are playing", "played");
        pqTopic(anh, G10, 6, "While we ___ TV, the lights suddenly went out.", Competency.APPLICATION, T10_PAST, 1,
                "watched", "were watching", "watch", "are watching");
        pqTopic(anh, G10, 7, "I ___ this film already, so let's watch another one.", Competency.APPLICATION, T10_PRESPERF, 1,
                "saw", "have seen", "see", "seeing");
        pqTopic(anh, G10, 8, "Look at those dark clouds! It ___ rain soon.", Competency.APPLICATION, T10_FUTURE, 1,
                "will", "is going to", "goes to", "would");
        pqTopic(anh, G10, 9, "This bridge ___ in 1995.", Competency.KNOWLEDGE, T10_PASSIVE, 1,
                "built", "was built", "is built", "has built");
        pqTopic(anh, G10, 10, "The report must ___ before Monday.", Competency.APPLICATION, T10_PASSMODAL, 1,
                "finish", "be finished", "finished", "be finish");
        pqTopic(anh, G10, 11, "He was very tired, ___ he kept working until midnight.", Competency.KNOWLEDGE, T10_COMPOUND, 1,
                "and", "but", "or", "because");
        pqTopic(anh, G10, 12, "My parents let me ___ TV after dinner.", Competency.KNOWLEDGE, T10_INF, 0,
                "watch", "to watch", "watching", "watched");
        pqTopic(anh, G10, 13, "I enjoy ___ books in my free time.", Competency.KNOWLEDGE, T10_GERUND, 2,
                "read", "to read", "reading", "reads");
        pqTopic(anh, G10, 14, "Mount Everest is ___ mountain in the world.", Competency.KNOWLEDGE, T10_COMPSUP, 1,
                "higher", "the highest", "high", "more high");
        pqTopic(anh, G10, 15, "The woman ___ lives next door is a doctor.", Competency.KNOWLEDGE, T10_RELCLAUSE, 1,
                "which", "who", "whose", "where");
        pqTopic(anh, G10, 16, "She said that she ___ very tired that day.", Competency.APPLICATION, T10_REPORTED, 1,
                "is", "was", "will be", "be");
        pqTopic(anh, G10, 17, "If I ___ you, I would say sorry to her.", Competency.APPLICATION, T10_CONDITIONAL, 2,
                "am", "was", "were", "be");

        // 18–20: Từ vựng (đồng nghĩa / trái nghĩa / chọn từ)
        pqTopic(anh, G10, 18, "Choose the word CLOSEST in meaning to \"enormous\".",
                Competency.COMPREHENSION, "Từ vựng", 1, "tiny", "huge", "narrow", "weak");
        pqTopic(anh, G10, 19, "Choose the word OPPOSITE in meaning to \"sociable\".",
                Competency.COMPREHENSION, "Từ vựng", 2, "friendly", "talkative", "unfriendly", "kind");
        pqTopic(anh, G10, 20, "She is very ___; she always helps other people.",
                Competency.KNOWLEDGE, "Từ vựng", 1, "selfish", "generous", "lazy", "rude");

        // 21–25: Điền vào đoạn văn (cloze)
        String cloze = """
                Đọc đoạn văn sau và chọn đáp án đúng cho mỗi chỗ trống (21–25).

                In many families today, both parents (21)____. As a result, children are often asked
                to help (22)____ the housework. Doing chores (23)____ children become more responsible
                and independent. For example, a child who cooks dinner learns a useful (24)____.
                Although some children think housework is boring, it (25)____ them important life lessons.""";
        pqTopic(anh, G10, 21, cloze, "Chỗ trống (21):", Competency.APPLICATION, T10_PRES, 1,
                "works", "work", "working", "worked");
        pqTopic(anh, G10, 22, cloze, "Chỗ trống (22):", Competency.KNOWLEDGE, "Từ vựng", 0,
                "with", "for", "on", "of");
        pqTopic(anh, G10, 23, cloze, "Chỗ trống (23):", Competency.APPLICATION, T10_GERUND, 1,
                "help", "helps", "helping", "to help");
        pqTopic(anh, G10, 24, cloze, "Chỗ trống (24):", Competency.KNOWLEDGE, "Từ vựng", 0,
                "skill", "skills", "skilful", "skilfully");
        pqTopic(anh, G10, 25, cloze, "Chỗ trống (25):", Competency.APPLICATION, T10_PRES, 1,
                "teach", "teaches", "taught", "teaching");

        // 26–30: Đọc hiểu
        String reading = """
                Đọc đoạn văn sau và trả lời các câu hỏi (26–30).

                Ha Long Bay is one of the most famous tourist attractions in Viet Nam. Located in
                Quang Ninh Province, it has thousands of limestone islands of different shapes and
                sizes. In 1994, Ha Long Bay was recognised by UNESCO as a World Heritage Site.
                Every year, millions of tourists visit the bay to enjoy its beautiful scenery,
                explore the caves, and take boat trips around the islands. However, the increasing
                number of visitors has caused some environmental problems, such as water pollution.
                To protect the bay, local authorities have asked tourists not to throw rubbish into
                the water and have organised regular clean-up activities.""";
        pqTopic(anh, G10, 26, reading, "Where is Ha Long Bay located?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "In Ha Noi", "In Quang Ninh Province", "In Da Nang", "In Hue");
        pqTopic(anh, G10, 27, reading, "When was Ha Long Bay recognised by UNESCO?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "In 1990", "In 1994", "In 2000", "In 2004");
        pqTopic(anh, G10, 28, reading, "Which activity is NOT mentioned in the passage?", Competency.COMPREHENSION, "Đọc hiểu", 2,
                "Exploring the caves", "Taking boat trips", "Climbing mountains", "Enjoying the scenery");
        pqTopic(anh, G10, 29, reading, "What problem has the growing number of tourists caused?", Competency.COMPREHENSION, "Đọc hiểu", 0,
                "Water pollution", "Traffic jams", "Noise pollution", "Deforestation");
        pqTopic(anh, G10, 30, reading, "What have local authorities done to protect the bay?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Built more hotels", "Organised clean-up activities", "Banned all tourists", "Closed the bay");
    }

    private void seedEnglish11Placement(Subject anh) {
        // 1–4: Phát âm & trọng âm
        pqTopic(anh, G11, 1, "Choose the word whose underlined \"c\" is pronounced differently: con_c_ert, _c_ity, re_c_ent, _c_ircle.",
                Competency.KNOWLEDGE, "Phát âm", 0, "concert", "city", "recent", "circle");
        pqTopic(anh, G11, 2, "Choose the word whose underlined \"i\" is pronounced differently.",
                Competency.KNOWLEDGE, "Phát âm", 0, "fine", "fit", "sick", "little");
        pqTopic(anh, G11, 3, "Choose the word that has a different stress pattern.",
                Competency.KNOWLEDGE, "Phát âm", 0, "adapt", "carry", "enter", "happen");
        pqTopic(anh, G11, 4, "Choose the word that has a different stress pattern.",
                Competency.KNOWLEDGE, "Phát âm", 0, "wonderful", "unhealthy", "domestic", "fantastic");

        // 5–17: Ngữ pháp (mỗi câu 1 chủ đề Lớp 11 để map bài học)
        pqTopic(anh, G11, 5, "I ___ a writer since 2010.", Competency.APPLICATION, T11_PASTPERF, 0,
                "have been", "was", "am", "had been");
        pqTopic(anh, G11, 6, "You look ill. You ___ see a doctor.", Competency.KNOWLEDGE, T11_MODAL, 2,
                "must", "have to", "should", "would");
        pqTopic(anh, G11, 7, "This soup ___ delicious.", Competency.KNOWLEDGE, T11_STATIVE, 1,
                "is tasting", "tastes", "taste", "tasted");
        pqTopic(anh, G11, 8, "___ English every day improves your skills.", Competency.APPLICATION, T11_GERSO, 1,
                "Practise", "Practising", "To practising", "Practised");
        pqTopic(anh, G11, 9, "The man ___ over there is my teacher.", Competency.APPLICATION, T11_PARTICIPLE, 1,
                "stand", "standing", "stood", "to stand");
        pqTopic(anh, G11, 10, "She stayed up late ___ for the exam.", Competency.APPLICATION, T11_TOINF, 1,
                "prepare", "to prepare", "preparing", "prepared");
        pqTopic(anh, G11, 11, "He thanked me for ___ him with his homework.", Competency.APPLICATION, T11_PERFGER, 2,
                "help", "helping", "having helped", "to help");
        pqTopic(anh, G11, 12, "It was John ___ broke the window.", Competency.APPLICATION, T11_CLEFT, 1,
                "which", "who", "whom", "whose");
        pqTopic(anh, G11, 13, "It was raining heavily; ___, we decided to go out.", Competency.KNOWLEDGE, T11_LINKING, 0,
                "however", "because", "so", "therefore");
        pqTopic(anh, G11, 14, "A ___ protects many kinds of wild animals.", Competency.KNOWLEDGE, T11_COMPNOUN, 1,
                "nation park", "national park", "nationally park", "nation's park");
        pqTopic(anh, G11, 15, "My family ___ to Ha Noi last year.", Competency.APPLICATION, T11_PASTPERF, 1,
                "moves", "moved", "has moved", "move");
        pqTopic(anh, G11, 16, "You ___ smoke here. It is strictly forbidden.", Competency.APPLICATION, T11_MODAL, 0,
                "mustn't", "don't have to", "should", "needn't");
        pqTopic(anh, G11, 17, "I avoid ___ fast food because it is unhealthy.", Competency.KNOWLEDGE, T11_GERSO, 2,
                "eat", "to eat", "eating", "eaten");

        // 18–20: Từ vựng
        pqTopic(anh, G11, 18, "Choose the word CLOSEST in meaning to \"fantastic\".",
                Competency.COMPREHENSION, "Từ vựng", 1, "terrible", "wonderful", "ordinary", "boring");
        pqTopic(anh, G11, 19, "Who is going to ___ the children while you are away?",
                Competency.KNOWLEDGE, "Từ vựng", 3, "come in", "break up", "go for", "look after");
        pqTopic(anh, G11, 20, "She was very ___ with the excellent service at the hotel.",
                Competency.KNOWLEDGE, "Từ vựng", 2, "satisfy", "satisfactorily", "satisfied", "satisfaction");

        // 21–25: Điền vào đoạn văn (cloze)
        String cloze = """
                Đọc mẩu quảng cáo sau và chọn đáp án đúng cho mỗi chỗ trống (21–25).

                REVIEWS WANTED
                Have you visited (21)____ wonderful cafe recently? Now is your chance to write about it.
                We (22)____ for reviews of cafes in your area. Describe your experience at the cafe that
                you (23)____ last week. Say why you were (24)____ or dissatisfied with it.
                We will publish the (25)____ interesting reviews on our website.""";
        pqTopic(anh, G11, 21, cloze, "Chỗ trống (21):", Competency.KNOWLEDGE, "Từ vựng", 0,
                "a", "an", "the", "(không cần mạo từ)");
        pqTopic(anh, G11, 22, cloze, "Chỗ trống (22):", Competency.APPLICATION, T11_STATIVE, 1,
                "look", "are looking", "looked", "looking");
        pqTopic(anh, G11, 23, cloze, "Chỗ trống (23):", Competency.APPLICATION, T11_PASTPERF, 1,
                "visit", "visited", "have visited", "visiting");
        pqTopic(anh, G11, 24, cloze, "Chỗ trống (24):", Competency.KNOWLEDGE, "Từ vựng", 1,
                "satisfy", "satisfied", "satisfaction", "satisfying");
        pqTopic(anh, G11, 25, cloze, "Chỗ trống (25):", Competency.KNOWLEDGE, "Từ vựng", 1,
                "more", "most", "much", "many");

        // 26–30: Đọc hiểu
        String reading = """
                Đọc đoạn văn sau và trả lời các câu hỏi (26–30).

                Living a long and healthy life is something everyone wants. To stay healthy, people
                should eat a balanced diet with plenty of fruit and vegetables. Regular exercise is
                also important because it keeps the heart strong and reduces stress. Doctors recommend
                at least thirty minutes of physical activity every day. In addition, getting enough
                sleep helps the body recover and improves memory. On the other hand, bad habits such
                as smoking and eating too much fast food can cause serious diseases. Therefore, making
                small positive changes to our daily routine can help us live longer and feel better.""";
        pqTopic(anh, G11, 26, reading, "What should people eat to stay healthy?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Only meat", "A balanced diet with fruit and vegetables", "Fast food", "Sweets");
        pqTopic(anh, G11, 27, reading, "Why is regular exercise important?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "It causes stress", "It keeps the heart strong and reduces stress", "It makes people tired", "It wastes time");
        pqTopic(anh, G11, 28, reading, "How much daily physical activity do doctors recommend?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Ten minutes", "At least thirty minutes", "Two hours", "None at all");
        pqTopic(anh, G11, 29, reading, "Which of the following is a BAD habit mentioned in the passage?", Competency.COMPREHENSION, "Đọc hiểu", 2,
                "Getting enough sleep", "Eating vegetables", "Smoking", "Doing exercise");
        pqTopic(anh, G11, 30, reading, "What is the main idea of the passage?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Fast food is good for health", "Small positive changes help us live longer and healthier",
                "Sleep is not important", "Exercise is dangerous");
    }

    private void seedEnglish12Placement(Subject anh) {
        // 1–4: Phát âm & trọng âm
        pqTopic(anh, G12, 1, "Choose the word whose underlined \"a\" is pronounced differently: m_a_rk, f_a_ce, b_a_ke, p_a_ge.",
                Competency.KNOWLEDGE, "Phát âm", 0, "mark", "face", "bake", "page");
        pqTopic(anh, G12, 2, "Choose the word whose underlined part is pronounced differently.",
                Competency.KNOWLEDGE, "Phát âm", 1, "picture", "cartoon", "practice", "climbed");
        pqTopic(anh, G12, 3, "Choose the word that has a different stress pattern.",
                Competency.KNOWLEDGE, "Phát âm", 3, "army", "beauty", "money", "account");
        pqTopic(anh, G12, 4, "Choose the word that has a different stress pattern.",
                Competency.KNOWLEDGE, "Phát âm", 0, "concentrate", "vacation", "effective", "experience");

        // 5–17: Ngữ pháp (mỗi câu 1 chủ đề Lớp 12 để map bài học)
        pqTopic(anh, G12, 5, "While I ___ dinner, the phone suddenly rang.", Competency.APPLICATION, T_PAST, 1,
                "cooked", "was cooking", "cook", "am cooking");
        pqTopic(anh, G12, 6, "___ Pacific is the largest of all oceans.", Competency.KNOWLEDGE, T_ART, 2,
                "A", "An", "The", "(không cần mạo từ)");
        pqTopic(anh, G12, 7, "The future of our planet depends ___ how we treat it.", Competency.KNOWLEDGE, T_VPREP, 1,
                "in", "on", "at", "for");
        pqTopic(anh, G12, 8, "More and more people are recycling now, ___ is good for the environment.",
                Competency.APPLICATION, T_WHICH, 1, "that", "which", "what", "who");
        pqTopic(anh, G12, 9, "This is the second time I ___ this beautiful city.", Competency.APPLICATION, T_PRESPERF, 2,
                "visit", "visited", "have visited", "am visiting");
        pqTopic(anh, G12, 10, "The bigger the city gets, ___ it becomes.", Competency.APPLICATION, T_DBLCOMP, 1,
                "more crowded", "the more crowded", "the most crowded", "crowded");
        pqTopic(anh, G12, 11, "___ my brother is often late for work, he is never promoted.",
                Competency.APPLICATION, T_SENTENCE, 0, "Because", "But", "So", "And");
        pqTopic(anh, G12, 12, "I had my computer ___ yesterday.", Competency.APPLICATION, T12_CAUSATIVE, 1,
                "repair", "repaired", "to repair", "repairing");
        pqTopic(anh, G12, 13, "The story was ___ interesting that it went viral in a day.", Competency.APPLICATION, T12_ADVMANNER, 0,
                "so", "such", "very", "too");
        pqTopic(anh, G12, 14, "___ we act now, many species will disappear forever.", Competency.APPLICATION, T12_ADVCOND, 1,
                "If", "Unless", "Because", "Although");
        pqTopic(anh, G12, 15, "I look ___ to seeing you again next week.", Competency.KNOWLEDGE, T12_PHRASAL3, 0,
                "forward", "after", "up", "for");
        pqTopic(anh, G12, 16, "The teacher told the students ___ hard for the exam.", Competency.APPLICATION, T12_REPORTED, 1,
                "study", "to study", "studying", "studied");
        pqTopic(anh, G12, 17, "She ___ in Ha Noi since 2015.", Competency.APPLICATION, T_PRESPERF, 2,
                "lives", "lived", "has lived", "living");

        // 18–20: Từ vựng
        pqTopic(anh, G12, 18, "Choose the word CLOSEST in meaning to \"maintain\".",
                Competency.COMPREHENSION, "Từ vựng", 1, "lose", "keep", "waste", "break");
        pqTopic(anh, G12, 19, "The technology company decided to ___ a brand-new smartphone.",
                Competency.KNOWLEDGE, "Từ vựng", 0, "launch", "close", "lose", "forget");
        pqTopic(anh, G12, 20, "Choose the word OPPOSITE in meaning to \"artificial\".",
                Competency.COMPREHENSION, "Từ vựng", 2, "fake", "synthetic", "natural", "man-made");

        // 21–25: Điền vào đoạn văn (cloze)
        String cloze = """
                Đọc thông báo sau và chọn đáp án đúng cho mỗi chỗ trống (21–25).

                ANNOUNCEMENT — INTERNATIONAL CULTURAL FESTIVAL
                The School Youth Union would like to announce: The festival (21)____ at 7 p.m. on
                September 2 at our school. This is a perfect event for students to learn about cultural
                diversity, (22)____ by tasting food from different countries. This programme brings
                cultures from all over the world to one (23)____! (24)____ you have any questions,
                please contact your class monitor. We hope (25)____ you there.""";
        pqTopic(anh, G12, 21, cloze, "Chỗ trống (21):", Competency.APPLICATION, "Từ vựng", 0,
                "will be held", "held", "hold", "was holding");
        pqTopic(anh, G12, 22, cloze, "Chỗ trống (22):", Competency.KNOWLEDGE, "Từ vựng", 1,
                "particular", "particularly", "particularity", "particulars");
        pqTopic(anh, G12, 23, cloze, "Chỗ trống (23):", Competency.KNOWLEDGE, "Từ vựng", 0,
                "location", "culture", "position", "land");
        pqTopic(anh, G12, 24, cloze, "Chỗ trống (24):", Competency.APPLICATION, "Từ vựng", 0,
                "If", "Were", "Should", "Do");
        pqTopic(anh, G12, 25, cloze, "Chỗ trống (25):", Competency.APPLICATION, "Từ vựng", 1,
                "see", "to see", "seeing", "saw");

        // 26–30: Đọc hiểu
        String reading = """
                Đọc đoạn văn sau và trả lời các câu hỏi (26–30).

                Artificial intelligence (AI) is changing the way we live and work. Today, AI is used in
                many fields, from healthcare to education. In hospitals, AI helps doctors diagnose
                diseases more quickly and accurately. In schools, AI-powered apps can give students
                personalised lessons based on their strengths and weaknesses. However, some people
                worry that AI may replace human workers and cause unemployment. Others believe that AI
                will create new jobs and make our lives easier. Whatever happens, it is clear that
                understanding AI will be an important skill in the future.""";
        pqTopic(anh, G12, 26, reading, "According to the passage, where is AI used?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Only in games", "In many fields such as healthcare and education", "Only in factories", "Nowhere yet");
        pqTopic(anh, G12, 27, reading, "How does AI help doctors in hospitals?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "By cooking food", "By diagnosing diseases quickly and accurately", "By cleaning rooms", "By replacing patients");
        pqTopic(anh, G12, 28, reading, "What can AI-powered apps do in schools?", Competency.COMPREHENSION, "Đọc hiểu", 0,
                "Give students personalised lessons", "Cook meals", "Drive buses", "Build classrooms");
        pqTopic(anh, G12, 29, reading, "What do some people worry about?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "AI is too slow", "AI may replace workers and cause unemployment", "AI is too cheap", "AI cannot learn");
        pqTopic(anh, G12, 30, reading, "What will be an important skill in the future?", Competency.COMPREHENSION, "Đọc hiểu", 1,
                "Cooking well", "Understanding AI", "Driving fast", "Singing loudly");
    }

    private String readClasspath(String path) {
        try {
            org.springframework.core.io.ClassPathResource res =
                    new org.springframework.core.io.ClassPathResource(path);
            try (java.io.InputStream in = res.getInputStream()) {
                String s = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                return s.startsWith("﻿") ? s.substring(1) : s; // strip BOM
            }
        } catch (Exception e) {
            return null;
        }
    }

    // ---------- helpers ----------

    private Subject subject(String code, String name, String icon, String color,
                            int idx, boolean active, String desc) {
        Subject s = new Subject();
        s.setCode(code);
        s.setName(name);
        s.setIconKey(icon);
        s.setColorKey(color);
        s.setOrderIndex(idx);
        s.setActive(active);
        s.setDescription(desc);
        return subjects.save(s);
    }

    private CourseModule module(Subject subject, String grade, String title, String subtitle,
                                String icon, int passThreshold) {
        CourseModule m = new CourseModule();
        m.setSubject(subject);
        m.setOrderIndex(++order);
        m.setGrade(grade);
        m.setTitle(title);
        m.setSubtitle(subtitle);
        m.setIconKey(icon);
        m.setHasContent(true);
        m.setPassThreshold(passThreshold);
        return modules.save(m);
    }

    private Lesson lesson(CourseModule module, String title, String author, String genre,
                          int duration, String excerpt, String summary) {
        Lesson l = new Lesson();
        l.setModule(module);
        l.setOrderIndex(1);
        l.setTitle(title);
        l.setAuthor(author);
        l.setGenre(genre);
        l.setDurationMinutes(duration);
        l.setHeroExcerpt(excerpt);
        l.setSummary(summary);
        return lessons.save(l);
    }

    private LessonSection section(Lesson lesson, int idx, SectionType type, String title, String body) {
        LessonSection s = new LessonSection();
        s.setLesson(lesson);
        s.setOrderIndex(idx);
        s.setType(type);
        s.setTitle(title);
        s.setBody(body);
        return sections.save(s);
    }

    private Question pq(Subject subject, int idx, String text, Competency competency,
                        int correct, String... opts) {
        Question q = new Question();
        q.setScope(QuizScope.PLACEMENT);
        q.setSubject(subject);
        q.setOrderIndex(idx);
        q.setText(text);
        q.setCompetency(competency);
        q.setCorrectIndex(correct);
        q.setOptions(Arrays.asList(opts));
        return questions.save(q);
    }

    private Material material(Lesson lesson, MaterialType type, String title, String url) {
        Material m = new Material();
        m.setLesson(lesson);
        m.setType(type);
        m.setTitle(title);
        m.setUrl(url);
        m.setCreatedByEmail("admin@pathstudy.vn");
        m.setOrderIndex(0);
        return materials.save(m);
    }

    /** English placement question. The no-grade variant defaults to Lớp 12 (legacy calls). */
    private Question pqTopic(Subject subject, int idx, String text, Competency competency,
                             String topic, int correct, String... opts) {
        return pqTopic(subject, G12, idx, text, competency, topic, correct, opts);
    }

    private Question pqTopic(Subject subject, String grade, int idx, String text, Competency competency,
                             String topic, int correct, String... opts) {
        return pqTopic(subject, grade, idx, null, text, competency, topic, correct, opts);
    }

    private Question pqTopic(Subject subject, String grade, int idx, String passage, String text,
                             Competency competency, String topic, int correct, String... opts) {
        Question q = new Question();
        q.setScope(QuizScope.PLACEMENT);
        q.setSubject(subject);
        q.setGrade(grade);
        q.setPassage(passage);
        q.setOrderIndex(idx);
        q.setText(text);
        q.setCompetency(competency);
        q.setCorrectIndex(correct);
        q.setTopic(topic);
        q.setOptions(Arrays.asList(opts));
        return questions.save(q);
    }

    private Question eq(CourseModule module, int idx, String text, Competency competency,
                        int correct, String... opts) {
        Question q = new Question();
        q.setScope(QuizScope.ESTIMATE);
        q.setModule(module);
        q.setOrderIndex(idx);
        q.setText(text);
        q.setCompetency(competency);
        q.setCorrectIndex(correct);
        q.setOptions(Arrays.asList(opts));
        return questions.save(q);
    }
}
