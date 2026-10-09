(function () {
    // Auto-hide toast
    document.addEventListener('DOMContentLoaded', function () {
        var toast = document.querySelector('.toast');
        if (toast) {
            setTimeout(function () {
                toast.style.transition = 'opacity .4s ease, transform .4s ease';
                toast.style.opacity = '0';
                toast.style.transform = 'translate(-50%, 10px)';
                setTimeout(function () { toast.remove(); }, 420);
            }, 3200);
        }

        // Quiz option highlight
        document.querySelectorAll('.q-card').forEach(function (card) {
            card.querySelectorAll('.option').forEach(function (opt) {
                var input = opt.querySelector('input[type=radio]');
                if (!input) return;
                var sync = function () {
                    card.querySelectorAll('.option').forEach(function (o) { o.classList.remove('selected'); });
                    if (input.checked) opt.classList.add('selected');
                };
                input.addEventListener('change', sync);
                if (input.checked) opt.classList.add('selected');
            });
        });

        // Subject picker highlight (không phụ thuộc CSS :has — tránh bug invalidation)
        var pickRadios = document.querySelectorAll('.pick input[type=radio]');
        if (pickRadios.length) {
            var syncPicks = function () {
                pickRadios.forEach(function (r) {
                    var label = r.closest('.pick');
                    if (label) label.classList.toggle('is-picked', r.checked);
                });
            };
            pickRadios.forEach(function (r) { r.addEventListener('change', syncPicks); });
            syncPicks();
        }

        // Smooth-scroll to a bookmarked section anchor
        if (window.location.hash) {
            var el = document.querySelector(window.location.hash);
            if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }

        // Bottom-nav mobile: nút "Thêm" mở tấm chứa các mục phụ + đăng xuất.
        setupMobileNav();

        // Câu nhận xét/động viên sau khi xem kết quả (theo % điểm, phong cách Gen Z).
        renderEncourage();
    });

    function setupMobileNav() {
        var btn = document.querySelector('.nav-more');
        var sidebar = document.querySelector('.sidebar');
        if (!btn || !sidebar) return;

        var lbl = btn.querySelector('.lbl');
        var close = function () {
            document.body.classList.remove('nav-open');
            btn.setAttribute('aria-expanded', 'false');
            if (lbl) lbl.textContent = 'Thêm';
        };

        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            var open = !document.body.classList.contains('nav-open');
            document.body.classList.toggle('nav-open', open);
            btn.setAttribute('aria-expanded', open ? 'true' : 'false');
            if (lbl) lbl.textContent = open ? 'Đóng' : 'Thêm';
        });

        // Bấm vào vùng tối bên ngoài (chính là ::before của .sidebar) thì đóng.
        sidebar.addEventListener('click', function (e) {
            if (e.target === sidebar) close();
        });

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') close();
        });

        // Quay về desktop thì bỏ trạng thái mở để sidebar hiện bình thường.
        window.addEventListener('resize', function () {
            if (window.innerWidth > 900) close();
        });
    }

    function renderEncourage() {
        var box = document.querySelector('.encourage[data-score]');
        if (!box) return;
        var score = parseInt(box.getAttribute('data-score'), 10);
        if (isNaN(score)) return;

        var MSGS = {
            excellent: [ // 90–100
                'Uầy đỉnh thật sự 😳 Ai cho phép giỏi vậy trời!',
                'Slay quá trời quá đất luôn 💅✨',
                'Điểm này mà không flex thì hơi phí đó nha 😎',
                'Xuất sắc! Xin vía điểm này cho lần sau với ạ 🙏',
                'Quá dữ dằn 🔥 Giữ phong độ này nhaaa!',
                'Top của top rồi đó 👑 Tự thưởng đi là vừa!',
                'Học kiểu gì mà điểm xinh dữ vậy 😭✨'
            ],
            great: [ // 80–89
                'Ngon lành cành đào 👏 Giữ phong độ nha!',
                'Điểm đẹp đó 👀✨ Thêm xíu nữa là chạm đỉnh luôn!',
                'Làm tốt lắm nè 🫶 Đà này là lên hạng rồi!',
                'Xịn đấy 😎 Cố thêm chút là full combo!',
                'Quá ổn áp 💪 Tiếp tục phát huy nhaaa!',
                'Giỏi ghê 😳 Sắp thành cao thủ rồi đó!'
            ],
            good: [ // 65–79
                'Khá ổn rồi nè 👍 Luyện thêm tí là bứt phá liền!',
                'Đang lên tay đó 📈 Cố thêm chút nữa nha!',
                'Không tệ đâu 🫶 Sửa vài chỗ là điểm bay cao liền!',
                'Tiến bộ thấy rõ luôn 😌 Chiến tiếp nào!',
                'Ổn áp phết 💪 Ôn lại phần yếu là xịn ngay!',
                'Gần tới rồi 👀 Thêm xíu quyết tâm là đẹp!'
            ],
            ok: [ // 50–64
                'Qua được rồi nè 🫶 Luyện thêm là tiến bộ liền thôi!',
                'Không sao hết á 🌱 Mỗi lần làm là một lần giỏi hơn!',
                'Nền có rồi đó 💪 Giờ xây lên thôi nào!',
                'Tạm ổn nha 😌 Ôn lại tí là lần sau khác liền!',
                'Đừng lo nhaaa 🥺 Bạn đang tiến lên từng chút đó!',
                'Cũng ngon mà 👍 Thêm chút luyện tập là bứt tốc!'
            ],
            low: [ // < 50
                'Não hôm nay hơi lag thôi 😭 Lần sau load nhanh hơn nha!',
                'Một cú flop nhẹ để chuẩn bị màn comeback 😌🔥',
                'Không sao hết á 🫶 Lần sau mình gỡ lại nhaaa!',
                'Đừng buồn nhaaa 🥺 Điểm số chỉ là con số thui mà!',
                'Chưa ổn lần này thì lần sau mình chiến tiếp 💪',
                'Hôm nay đề hơi báo thôi 😭 Bạn vẫn ổn mà, thử lại nha!',
                'Comeback mới là thứ đáng mong chờ 😎 Cố lên nào!'
            ]
        };

        var band = score >= 90 ? 'excellent'
                 : score >= 80 ? 'great'
                 : score >= 65 ? 'good'
                 : score >= 50 ? 'ok' : 'low';
        var list = MSGS[band];
        if (!list || !list.length) return;

        var idx = Math.floor(Math.random() * list.length);
        try {
            var key = 'enc_last_' + band;
            var last = parseInt(localStorage.getItem(key), 10);
            if (list.length > 1 && idx === last) idx = (idx + 1) % list.length;
            localStorage.setItem(key, String(idx));
        } catch (e) { /* localStorage có thể bị chặn — bỏ qua */ }

        // Luôn hiển thị câu (opacity do CSS = 1); 'enc-in' chỉ thêm animation nhẹ.
        box.textContent = list[idx];
        box.classList.add('encourage--' + band, 'enc-in');
    }
})();
