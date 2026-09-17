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

    private static final String SEED_VERSION = "2026-09-17-english-v3-refdoc";

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

    private int order = 0; // running module order within a subject

    public DataSeeder(SubjectRepository subjects, CourseModuleRepository modules,
                      LessonRepository lessons, LessonSectionRepository sections,
                      QuestionRepository questions, UserRepository users,
                      PasswordEncoder passwordEncoder, AppSettingRepository appSettings,
                      EnrollmentRepository enrollments, ModuleProgressRepository moduleProgress,
                      PlacementResultRepository placementResults, EstimateResultRepository estimateResults,
                      BookmarkRepository bookmarks, MaterialRepository materials,
                      ReferenceMaterialRepository referenceMaterials) {
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
        user("Nguyễn An", "demo@pathstudy.vn", "123456", "STUDENT");
        user("Cô Lan (Giáo viên)", "teacher@pathstudy.vn", "teacher123", "TEACHER");
        user("Quản trị viên", "admin@pathstudy.vn", "admin123", "ADMIN");
    }

    private void user(String name, String email, String rawPassword, String role) {
        if (users.existsByEmail(email)) {
            return;
        }
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setRole(role);
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

    private void seedEnglish(Subject anh) {
        // Đề chẩn đoán MẪU (thay bằng đề thật khi có tài liệu). Mỗi câu gắn 1 chủ đề
        // để phân tích điểm yếu và ra lộ trình ôn tập phù hợp.
        pqTopic(anh, 1, "She ___ to school every day.", Competency.KNOWLEDGE, "Thì hiện tại đơn", 1,
                "go", "goes", "going", "gone");
        pqTopic(anh, 2, "They ___ football when it started to rain.", Competency.KNOWLEDGE, "Thì quá khứ tiếp diễn", 2,
                "play", "played", "were playing", "are playing");
        pqTopic(anh, 3, "The opposite of \"difficult\" is ___.", Competency.KNOWLEDGE, "Từ vựng", 0,
                "easy", "hard", "big", "fast");
        pqTopic(anh, 4, "Choose the word closest in meaning to \"happy\": ___.", Competency.KNOWLEDGE, "Từ vựng", 1,
                "sad", "glad", "angry", "tired");
        pqTopic(anh, 5, "Which word has a different vowel sound?", Competency.KNOWLEDGE, "Phát âm", 2,
                "cat", "hat", "car", "bat");
        pqTopic(anh, 6, "Read: \"Tom likes apples. He eats one every morning.\" What does Tom eat every morning?",
                Competency.COMPREHENSION, "Đọc hiểu", 0, "An apple", "A banana", "Bread", "Rice");
        pqTopic(anh, 7, "If it ___ tomorrow, we will stay home.", Competency.APPLICATION, "Câu điều kiện", 0,
                "rains", "rained", "will rain", "raining");
        pqTopic(anh, 8, "I'm good ___ English.", Competency.KNOWLEDGE, "Giới từ", 1,
                "in", "at", "on", "of");

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
    }

    private String readClasspath(String path) {
        try {
            org.springframework.core.io.ClassPathResource res =
                    new org.springframework.core.io.ClassPathResource(path);
            try (java.io.InputStream in = res.getInputStream()) {
                return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
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

    private Question pqTopic(Subject subject, int idx, String text, Competency competency,
                             String topic, int correct, String... opts) {
        Question q = new Question();
        q.setScope(QuizScope.PLACEMENT);
        q.setSubject(subject);
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
