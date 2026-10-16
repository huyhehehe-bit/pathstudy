/*
 * Chọn giao diện: Sáng / Tối / Hồng.
 *
 * Web có HAI trục độc lập, đừng nhầm:
 *   data-theme = pastel | kawai   (phong cách — pixel xanh hay hồng dễ thương)
 *   data-mode  = light  | dark    (ngày hay đêm)
 * Cả bốn tổ hợp đều có CSS thật (xem kawai.css mục 3 và 4). Nhưng bắt học sinh
 * hiểu hai trục là quá nhiều, nên phần chọn chỉ đưa ra BA template:
 *   Sáng = pastel+ngày · Tối = pastel+đêm · Hồng = kawai+ngày
 * Nút mặt trời/mặt trăng vẫn đổi ngày↔đêm trong phong cách đang dùng, nên
 * bản Hồng ban đêm vẫn tới được — không phí bộ ảnh ban-dem.
 *
 * Việc đặt thuộc tính LẦN ĐẦU nằm ở script nội tuyến trong <head>
 * (fragments/layout.html) vì nó phải chạy trước khi trang vẽ.
 *
 *   localStorage['pathstudy-theme'] = 'pastel' | 'kawai'
 *   localStorage['pathstudy-mode']  = 'light'  | 'dark'
 *   <html data-mode-pending>  = chưa chọn bao giờ → hiện hộp chọn
 */
(function () {
    'use strict';

    var KEY_MODE = 'pathstudy-mode';
    var KEY_THEME = 'pathstudy-theme';
    var root = document.documentElement;

    var MAU = {
        sang: { theme: 'pastel', mode: 'light', ten: 'Sáng' },
        toi: { theme: 'pastel', mode: 'dark', ten: 'Tối' },
        hong: { theme: 'kawai', mode: 'light', ten: 'Hồng' }
    };

    function choPhepHong() {
        return root.hasAttribute('data-can-kawai');
    }

    function luu(key, value) {
        try {
            localStorage.setItem(key, value);
        } catch (e) {
            /* chế độ riêng tư chặn localStorage — vẫn đổi được cho phiên này */
        }
    }

    function apDung(theme, mode) {
        luu(KEY_THEME, theme);
        luu(KEY_MODE, mode);
        root.setAttribute('data-theme', theme);
        root.setAttribute('data-mode', mode);
        root.removeAttribute('data-mode-pending');
        document.dispatchEvent(new CustomEvent('pathstudy:mode', { detail: mode }));
    }

    function dangDung() {
        if (root.getAttribute('data-theme') === 'kawai') {
            return 'hong';
        }
        return root.getAttribute('data-mode') === 'dark' ? 'toi' : 'sang';
    }

    function theMot(ma) {
        return '<button type="button" class="mode-opt" data-pick="' + ma + '">' +
            '<span class="mode-prev"><span class="pv-side"></span>' +
            '<span class="pv-main"><span class="pv-bar"></span><span class="pv-body"></span></span>' +
            '</span>' + MAU[ma].ten + '</button>';
    }

    function moHopChon(lanDau) {
        var cu = document.querySelector('.mode-gate');
        if (cu) {
            cu.remove();
        }

        var truoc = { theme: root.getAttribute('data-theme'), mode: root.getAttribute('data-mode') };
        var danh = ['sang', 'toi'];
        if (choPhepHong()) {
            danh.push('hong');
        }

        var gate = document.createElement('div');
        gate.className = 'mode-gate';
        gate.setAttribute('role', 'dialog');
        gate.setAttribute('aria-modal', 'true');
        gate.setAttribute('aria-labelledby', 'modeGateTitle');
        gate.innerHTML =
            '<div class="mode-gate-box">' +
            '  <h2 id="modeGateTitle">Bạn thích nhìn kiểu nào?</h2>' +
            '  <p>Chọn giao diện cho dễ học. Đổi lại bất cứ lúc nào bằng nút ở thanh trên cùng.</p>' +
            '  <div class="mode-opts" data-so="' + danh.length + '">' +
            danh.map(theMot).join('') +
            '  </div>' +
            '  <p class="mode-gate-note">' +
            (lanDau ? 'Chỉ hỏi một lần.' : '<button type="button" class="mode-gate-huy">Để nguyên</button>') +
            '  </p>' +
            '</div>';

        function dong(khoiPhuc) {
            if (khoiPhuc && truoc.theme) {
                root.setAttribute('data-theme', truoc.theme);
                if (truoc.mode) {
                    root.setAttribute('data-mode', truoc.mode);
                }
            }
            gate.remove();
        }

        gate.addEventListener('click', function (ev) {
            if (!ev.target.closest) {
                return;
            }
            if (ev.target.closest('.mode-gate-huy')) {
                dong(true);
                return;
            }
            var nut = ev.target.closest('.mode-opt');
            if (!nut) {
                return;
            }
            var chon = MAU[nut.getAttribute('data-pick')];
            apDung(chon.theme, chon.mode);
            gate.remove();
        });

        // Rê chuột là xem thử ngay trên trang thật; bấm mới là chốt.
        gate.addEventListener('mouseover', function (ev) {
            var nut = ev.target.closest ? ev.target.closest('.mode-opt') : null;
            if (nut) {
                var xem = MAU[nut.getAttribute('data-pick')];
                root.setAttribute('data-theme', xem.theme);
                root.setAttribute('data-mode', xem.mode);
            }
        });

        document.body.appendChild(gate);
        var hienTai = gate.querySelector('.mode-opt[data-pick="' + dangDung() + '"]');
        if (hienTai) {
            hienTai.classList.add('dang-dung');
            hienTai.focus();
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        if (root.hasAttribute('data-mode-pending')) {
            moHopChon(true);
        }

        // Nút "Đổi giao diện": mở lại hộp 3 template.
        var btnTheme = document.getElementById('btnThemeSwitch');
        if (btnTheme) {
            btnTheme.addEventListener('click', function () {
                moHopChon(false);
            });
        }

        // Nút mặt trời/mặt trăng: đổi ngày↔đêm, GIỮ nguyên phong cách đang dùng
        // (đang ở Hồng thì ra Hồng ban đêm, không nhảy về pastel).
        var btnMode = document.getElementById('btnModeSwitch');
        if (btnMode) {
            btnMode.addEventListener('click', function () {
                var theme = root.getAttribute('data-theme') || 'pastel';
                apDung(theme, root.getAttribute('data-mode') === 'dark' ? 'light' : 'dark');
            });
        }
    });
})();
