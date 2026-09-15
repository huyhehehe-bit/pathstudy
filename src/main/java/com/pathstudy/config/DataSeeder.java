package com.pathstudy.config;

import com.pathstudy.domain.*;
import com.pathstudy.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * Seeds demo content on first run. Idempotent: skips if subjects already exist.
 * Ngữ văn is fully built (module "Phân tích tác phẩm" centres on the poem "Sóng").
 * Other subjects appear on the UI as coming-soon.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final SubjectRepository subjects;
    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final LessonSectionRepository sections;
    private final QuestionRepository questions;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(SubjectRepository subjects, CourseModuleRepository modules,
                      LessonRepository lessons, LessonSectionRepository sections,
                      QuestionRepository questions, UserRepository users,
                      PasswordEncoder passwordEncoder) {
        this.subjects = subjects;
        this.modules = modules;
        this.lessons = lessons;
        this.sections = sections;
        this.questions = questions;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (subjects.count() > 0) {
            return;
        }
        seedSubjects();
        seedDemoUser();
        Subject van = subjects.findByCode("van").orElseThrow();
        seedVanContent(van);
    }

    private void seedSubjects() {
        subject("van", "Ngữ văn", "book", "indigo", 1, true,
                "Đọc hiểu, phân tích tác phẩm và nghị luận văn học theo lộ trình cá nhân hoá.");
        subject("anh", "Tiếng Anh", "flag", "rose", 2, false,
                "Ngữ pháp, từ vựng và kỹ năng làm bài THPT. Sắp ra mắt.");
        subject("toan", "Toán", "calculator", "sky", 3, false,
                "Đại số, hình học và luyện đề THPT. Sắp ra mắt.");
        subject("ly", "Vật lý", "atom", "violet", 4, false,
                "Cơ, điện, quang và luyện đề. Sắp ra mắt.");
        subject("hoa", "Hóa học", "flask", "amber", 5, false,
                "Hoá vô cơ, hữu cơ và bài tập. Sắp ra mắt.");
        subject("sinh", "Sinh học", "leaf", "green", 6, false,
                "Di truyền, sinh thái và luyện đề. Sắp ra mắt.");
    }

    private void seedDemoUser() {
        if (users.existsByEmail("demo@pathstudy.vn")) {
            return;
        }
        User u = new User();
        u.setFullName("Nguyễn An");
        u.setEmail("demo@pathstudy.vn");
        u.setPasswordHash(passwordEncoder.encode("123456"));
        users.save(u);
    }

    private void seedVanContent(Subject van) {
        // ---- Modules (the personalized study path) ----
        CourseModule nenTang = module(van, 1, "Nền tảng",
                "Đọc hiểu · Thể loại · Nghệ thuật cơ bản", "layers", true, 60);
        CourseModule phanTich = module(van, 2, "Phân tích tác phẩm",
                "Thơ · Truyện · Nhân vật · Hình ảnh & biểu tượng", "search", true, 70);
        module(van, 3, "Nghị luận",
                "Luận điểm · Dẫn chứng · Lập luận", "pen", false, 70);
        module(van, 4, "Luyện đề",
                "Đề 1 · Đề 2 · Đề 3", "clipboard", false, 70);
        module(van, 5, "Đánh giá",
                "Estimate Test tổng hợp", "award", false, 75);

        // ---- Module 1: Nền tảng ----
        Lesson base = lesson(nenTang, 1, "Kỹ năng đọc hiểu văn bản", null, "Kỹ năng", 30,
                "Đọc hiểu là bước đầu tiên: nắm được nội dung, nhận diện thể loại và các tín hiệu nghệ thuật trước khi phân tích sâu.",
                "Trang bị kỹ năng đọc hiểu cốt lõi làm nền cho việc phân tích tác phẩm.");
        section(base, 1, SectionType.READING, "Đọc & hiểu văn bản", """
                Đọc hiểu gồm ba lớp: (1) nghĩa bề mặt — văn bản nói về cái gì; \
                (2) nghĩa hàm ẩn — tác giả gửi gắm điều gì; (3) tín hiệu nghệ thuật — \
                từ ngữ, hình ảnh, nhịp điệu tạo ra ý nghĩa đó. Khi đọc, hãy gạch chân \
                từ khoá, xác định mạch cảm xúc và ghi lại câu hỏi "vì sao tác giả viết như vậy".""");
        section(base, 2, SectionType.VOCAB, "Thuật ngữ cơ bản", """
                • Hình ảnh (image): sự vật, hiện tượng được gợi tả trong tác phẩm.
                • Biểu tượng (symbol): hình ảnh mang nghĩa khái quát, vượt lên nghĩa đen.
                • Nhịp điệu: cách ngắt nhịp, gieo vần tạo âm hưởng cho câu thơ.
                • Mạch cảm xúc: dòng chảy tình cảm xuyên suốt tác phẩm.""");
        section(base, 3, SectionType.GUIDE, "Hướng dẫn tiếp cận một tác phẩm", """
                Bước 1: Đọc toàn bộ, nắm chủ đề chung.
                Bước 2: Xác định thể loại và đặc trưng của nó.
                Bước 3: Chia bố cục, đặt tên cho từng phần.
                Bước 4: Với mỗi phần, tìm hình ảnh/biện pháp nổi bật và giải mã ý nghĩa.
                Bước 5: Khái quát tư tưởng, tình cảm của tác giả.""");

        eq(nenTang, 1, "Đọc hiểu một văn bản gồm mấy lớp nghĩa cơ bản?",
                Competency.KNOWLEDGE, 2, "Một", "Hai", "Ba", "Bốn");
        eq(nenTang, 2, "\"Biểu tượng\" khác \"hình ảnh\" ở điểm nào?",
                Competency.KNOWLEDGE, 1, "Không khác gì",
                "Biểu tượng mang nghĩa khái quát, vượt nghĩa đen", "Biểu tượng ngắn hơn",
                "Hình ảnh chỉ có trong thơ");
        eq(nenTang, 3, "Khi phân tích một khổ thơ, bước hợp lý đầu tiên là:",
                Competency.ANALYSIS, 1, "Chép lại toàn bộ khổ thơ",
                "Nêu câu chủ đề khái quát nội dung khổ", "Kể tiểu sử tác giả",
                "Viết cảm nghĩ dài dòng");
        eq(nenTang, 4, "Để nắm mạch cảm xúc của bài thơ, người đọc nên:",
                Competency.APPLICATION, 0, "Theo dõi sự thay đổi tình cảm qua từng khổ",
                "Đếm số câu", "Chỉ đọc khổ cuối", "Bỏ qua các hình ảnh");

        // ---- Module 2: Phân tích tác phẩm — "Sóng" (Xuân Quỳnh) ----
        Lesson song = lesson(phanTich, 1, "Sóng – Xuân Quỳnh", "Xuân Quỳnh", "Thơ", 45,
                """
                Dữ dội và dịu êm
                Ồn ào và lặng lẽ
                Sông không hiểu nổi mình
                Sóng tìm ra tận bể
                                        — Xuân Quỳnh, trích "Sóng" (1967)""",
                "Phân tích hình tượng sóng như ẩn dụ cho tâm hồn và tình yêu của người phụ nữ.");

        section(song, 1, SectionType.READING, "Đọc & hiểu tác phẩm", """
                "Sóng" được Xuân Quỳnh viết năm 1967 tại biển Diêm Điền. Toàn bài xây dựng \
                trên hai hình tượng song hành: "sóng" và "em". Sóng là hình ảnh thiên nhiên, \
                còn em là chủ thể trữ tình; hai hình tượng lúc phân đôi để soi chiếu, lúc \
                hoà nhập làm một để nói về khát vọng tình yêu. Đọc bài thơ, cần theo dõi cách \
                "sóng" và "em" luân phiên xuất hiện để thấy mạch cảm xúc phát triển từ nhận thức \
                về tình yêu đến khát vọng vĩnh cửu hoá tình yêu.""");
        section(song, 2, SectionType.THEME, "Chủ đề & nội dung", """
                Chủ đề: vẻ đẹp tâm hồn và khát vọng tình yêu của người phụ nữ — vừa mãnh liệt \
                vừa dịu dàng, vừa lo âu vừa tin tưởng. Bài thơ khẳng định một tình yêu chân thành, \
                thuỷ chung và mong muốn được sống hết mình, được hoà vào cái lớn lao, vĩnh hằng.""");
        section(song, 3, SectionType.IMAGERY, "Hình ảnh, biểu tượng", """
                • Sóng: biểu tượng cho tâm hồn người phụ nữ đang yêu — nhiều cung bậc, không yên định.
                • Biển/bể: cái lớn lao, vô tận mà tình yêu khát khao vươn tới.
                • "Con sóng dưới lòng sâu / Con sóng trên mặt nước": nỗi nhớ bao trùm cả không gian.
                Hình tượng sóng và em cộng hưởng, làm nổi bật khát vọng bất tử hoá tình yêu.""");
        section(song, 4, SectionType.TECHNIQUE, "Biện pháp nghệ thuật", """
                • Thể thơ năm chữ, nhịp ngắn, đều đặn gợi âm hưởng của những con sóng nối nhau.
                • Phép tương phản: "Dữ dội - dịu êm", "Ồn ào - lặng lẽ" diễn tả các trạng thái \
                đối lập nhưng thống nhất trong một tâm hồn.
                • Điệp từ, điệp cấu trúc ("Con sóng…", "Dẫu…") tạo nhịp trùng điệp như sóng vỗ.
                • Ẩn dụ xuyên suốt: sóng ẩn dụ cho tình yêu và người phụ nữ đang yêu.""");
        section(song, 5, SectionType.CONTEXT, "Bối cảnh", """
                Xuân Quỳnh (1942–1988) là gương mặt tiêu biểu của thế hệ nhà thơ trẻ trưởng thành \
                trong kháng chiến chống Mỹ. Thơ bà giàu nữ tính, thể hiện một trái tim khao khát \
                yêu thương và luôn âu lo giữ gìn hạnh phúc. "Sóng" ra đời năm 1967, in trong tập \
                "Hoa dọc chiến hào" — tiêu biểu cho hồn thơ Xuân Quỳnh.""");
        section(song, 6, SectionType.GUIDE, "Hướng dẫn phân tích", """
                1. Mở bài: giới thiệu tác giả, tác phẩm và hình tượng sóng - em.
                2. Thân bài, phân tích theo mạch: \
                (a) sóng như bản tính người phụ nữ đang yêu (khổ 1–2); \
                (b) sóng và những trăn trở về nguồn gốc tình yêu (khổ 3–4); \
                (c) nỗi nhớ và lòng thuỷ chung (khổ 5–6); \
                (d) khát vọng bất tử hoá tình yêu (khổ cuối).
                3. Kết bài: khẳng định giá trị nhân văn và nét riêng của hồn thơ Xuân Quỳnh.""");
        section(song, 7, SectionType.FRAMEWORK, "Framework viết đoạn văn", """
                Mô hình câu chủ đề → phân tích → dẫn chứng → đánh giá:
                • Câu chủ đề: nêu ý khái quát của khổ/đoạn.
                • Phân tích từ ngữ, hình ảnh, biện pháp nghệ thuật.
                • Trích dẫn chính xác câu thơ làm dẫn chứng.
                • Đánh giá: ý nghĩa của chi tiết đó với chủ đề và với tâm hồn tác giả.""");
        section(song, 8, SectionType.EXAMPLE, "Ví dụ bài làm", """
                (Đoạn mẫu) Mở đầu bài thơ, Xuân Quỳnh diễn tả bản tính phong phú của người phụ nữ \
                đang yêu qua hai câu "Dữ dội và dịu êm / Ồn ào và lặng lẽ". Phép tương phản đặt \
                những trạng thái đối lập cạnh nhau cho thấy một tâm hồn nhiều cung bậc nhưng thống \
                nhất. Từ đó, hình tượng sóng "tìm ra tận bể" thể hiện khát vọng vượt khỏi giới hạn \
                chật hẹp để đến với tình yêu lớn lao — một quan niệm tình yêu vừa nữ tính vừa hiện đại.""");
        section(song, 9, SectionType.VOCAB, "Từ vựng & khái niệm", """
                • Ẩn dụ: gọi tên sự vật này bằng sự vật khác có nét tương đồng (sóng ↔ tình yêu).
                • Tương phản (đối lập): đặt hai yếu tố trái ngược để làm nổi bật ý.
                • Điệp cấu trúc: lặp lại một mô hình câu để tạo nhịp và nhấn mạnh.
                • Chủ thể trữ tình: "cái tôi" bộc lộ cảm xúc trong tác phẩm ("em").""");

        // Estimate test for "Phân tích tác phẩm" (Sóng)
        eq(phanTich, 1, "\"Sóng\" được viết theo thể thơ nào?",
                Competency.KNOWLEDGE, 1, "Lục bát", "Năm chữ (ngũ ngôn)", "Bảy chữ", "Tự do");
        eq(phanTich, 2, "Bài thơ ra đời năm nào?",
                Competency.KNOWLEDGE, 2, "1945", "1954", "1967", "1975");
        eq(phanTich, 3, "Hình tượng \"sóng\" trong bài chủ yếu tượng trưng cho:",
                Competency.ANALYSIS, 1, "Thiên nhiên biển cả đơn thuần",
                "Tâm hồn và tình yêu của người phụ nữ", "Dòng chảy thời gian",
                "Nỗi cô đơn của con người");
        eq(phanTich, 4, "Quan hệ giữa hai hình tượng \"sóng\" và \"em\" là:",
                Competency.ANALYSIS, 1, "Hoàn toàn đối lập",
                "Song hành, soi chiếu và hoà nhập", "Không liên quan", "Quan hệ nhân quả");
        eq(phanTich, 5, "Khi viết đoạn phân tích một khổ thơ, câu đầu tiên nên là:",
                Competency.APPLICATION, 1, "Toàn bộ khổ thơ chép lại",
                "Câu chủ đề khái quát nội dung khổ", "Tiểu sử tác giả", "Cảm nghĩ cá nhân");
        eq(phanTich, 6, "Dẫn chứng thuyết phục khi phân tích thơ cần:",
                Competency.APPLICATION, 0, "Trích đúng câu thơ và phân tích từ ngữ, hình ảnh",
                "Chỉ nêu cảm xúc", "Kể lại nội dung", "Chép văn mẫu");

        // ---- Placement test for Ngữ văn ----
        pq(van, 1, "Biện pháp tu từ chủ đạo trong \"Dữ dội và dịu êm / Ồn ào và lặng lẽ\" là:",
                Competency.COMPREHENSION, 1, "Ẩn dụ", "Tương phản (đối lập)", "So sánh", "Nói quá");
        pq(van, 2, "Thể thơ năm chữ thường có đặc điểm gì?",
                Competency.KNOWLEDGE, 0, "Mỗi dòng năm tiếng, nhịp ngắn",
                "Mỗi dòng sáu tiếng", "Bắt buộc gieo vần chân", "Chỉ dùng trong ca dao");
        pq(van, 3, "Xuân Quỳnh trưởng thành trong giai đoạn văn học nào?",
                Competency.KNOWLEDGE, 2, "Trước 1945", "Kháng chiến chống Pháp",
                "Kháng chiến chống Mỹ", "Sau Đổi mới");
        pq(van, 4, "Hình tượng \"sóng\" trong thơ Xuân Quỳnh chủ yếu là ẩn dụ cho:",
                Competency.ANALYSIS, 1, "Cảnh biển", "Tâm hồn người phụ nữ đang yêu",
                "Cuộc kháng chiến", "Thời gian");
        pq(van, 5, "Để làm nổi bật chủ đề, hai hình tượng \"sóng\" và \"em\":",
                Competency.ANALYSIS, 1, "Tách rời hoàn toàn",
                "Soi chiếu, cộng hưởng và hoà nhập", "Mâu thuẫn nhau", "Không có liên hệ");
        pq(van, 6, "Bước đầu tiên nên làm khi viết đoạn văn nghị luận văn học là:",
                Competency.APPLICATION, 1, "Chép lại đề bài",
                "Xác định luận điểm và câu chủ đề", "Kể tiểu sử tác giả", "Viết kết bài trước");
    }

    // ---------- helpers ----------

    private Subject subject(String code, String name, String icon, String color,
                            int order, boolean active, String desc) {
        Subject s = new Subject();
        s.setCode(code);
        s.setName(name);
        s.setIconKey(icon);
        s.setColorKey(color);
        s.setOrderIndex(order);
        s.setActive(active);
        s.setDescription(desc);
        return subjects.save(s);
    }

    private CourseModule module(Subject subject, int order, String title, String subtitle,
                                String icon, boolean hasContent, int passThreshold) {
        CourseModule m = new CourseModule();
        m.setSubject(subject);
        m.setOrderIndex(order);
        m.setTitle(title);
        m.setSubtitle(subtitle);
        m.setIconKey(icon);
        m.setHasContent(hasContent);
        m.setPassThreshold(passThreshold);
        return modules.save(m);
    }

    private Lesson lesson(CourseModule module, int order, String title, String author,
                          String genre, int duration, String excerpt, String summary) {
        Lesson l = new Lesson();
        l.setModule(module);
        l.setOrderIndex(order);
        l.setTitle(title);
        l.setAuthor(author);
        l.setGenre(genre);
        l.setDurationMinutes(duration);
        l.setHeroExcerpt(excerpt);
        l.setSummary(summary);
        return lessons.save(l);
    }

    private LessonSection section(Lesson lesson, int order, SectionType type, String title, String body) {
        LessonSection s = new LessonSection();
        s.setLesson(lesson);
        s.setOrderIndex(order);
        s.setType(type);
        s.setTitle(title);
        s.setBody(body);
        return sections.save(s);
    }

    private Question pq(Subject subject, int order, String text, Competency competency,
                        int correct, String... opts) {
        Question q = new Question();
        q.setScope(QuizScope.PLACEMENT);
        q.setSubject(subject);
        q.setOrderIndex(order);
        q.setText(text);
        q.setCompetency(competency);
        q.setCorrectIndex(correct);
        q.setOptions(Arrays.asList(opts));
        return questions.save(q);
    }

    private Question eq(CourseModule module, int order, String text, Competency competency,
                        int correct, String... opts) {
        Question q = new Question();
        q.setScope(QuizScope.ESTIMATE);
        q.setModule(module);
        q.setOrderIndex(order);
        q.setText(text);
        q.setCompetency(competency);
        q.setCorrectIndex(correct);
        q.setOptions(Arrays.asList(opts));
        return questions.save(q);
    }
}
