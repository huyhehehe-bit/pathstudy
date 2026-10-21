/*
 * Lời mời khảo sát trải nghiệm.
 *
 * Hiện MỘT LẦN, sau khi học sinh đã dùng web đủ lâu (mặc định 5 phút, cộng dồn
 * qua nhiều trang chứ không phải 5 phút liên tục trên một trang — người ta đọc
 * bài rồi chuyển trang liên tục, đếm theo từng trang thì gần như không bao giờ
 * tới ngưỡng).
 *
 * TUYỆT ĐỐI không hiện khi đang làm bài: cắt ngang lúc học sinh đang tính toán
 * là vừa mất bài vừa mất thiện cảm. Nhận diện bằng thẻ .q-card — mọi trang làm
 * bài (kiểm tra đầu vào, đề thi, tự đánh giá, đề Tiếng Anh) đều render class
 * này. Nếu đến hẹn mà đang làm bài thì im lặng, để trang sau mời.
 *
 * localStorage:
 *   pathstudy-khaosat-batdau = mốc thời gian lần đầu vào web (ms)
 *   pathstudy-khaosat-xong   = 'roi' khi đã mở form hoặc đã từ chối
 */
(function () {
    'use strict';

    var KEY_BAT_DAU = 'pathstudy-khaosat-batdau';
    var KEY_XONG = 'pathstudy-khaosat-xong';
    var root = document.documentElement;

    function doc(k) {
        try {
            return localStorage.getItem(k);
        } catch (e) {
            return null;
        }
    }

    function ghi(k, v) {
        try {
            localStorage.setItem(k, v);
        } catch (e) {
            /* chế độ riêng tư chặn localStorage — coi như chưa từng mời */
        }
    }

    function dangLamBai() {
        return !!document.querySelector('.q-card');
    }

    function dungThe(link) {
        var hop = document.createElement('div');
        hop.className = 'ks-moi';
        hop.setAttribute('role', 'dialog');
        hop.setAttribute('aria-label', 'Lời mời khảo sát');
        hop.innerHTML =
            '<button type="button" class="ks-dong" aria-label="Đóng">×</button>' +
            '<div class="ks-tieu-de">Bạn giúp mình một việc nhỏ nhé?</div>' +
            '<p class="ks-than">PathStudy đang trong giai đoạn hoàn thiện. Bạn dành 2 phút ' +
            'nói về trải nghiệm vừa rồi được không? Góp ý của bạn quyết định web sẽ sửa gì tiếp theo.</p>' +
            '<div class="ks-nut">' +
            '  <a class="btn btn-primary ks-mo" href="' + link + '" target="_blank" rel="noopener">Làm khảo sát</a>' +
            '  <button type="button" class="btn btn-ghost ks-thoi">Để sau</button>' +
            '</div>';

        function dong(danhDauXong) {
            if (danhDauXong) {
                ghi(KEY_XONG, 'roi');
            }
            hop.classList.remove('hien');
            setTimeout(function () {
                if (hop.parentNode) {
                    hop.parentNode.removeChild(hop);
                }
            }, 200);
        }

        hop.addEventListener('click', function (ev) {
            if (!ev.target.closest) {
                return;
            }
            if (ev.target.closest('.ks-mo')) {
                dong(true);           // mở form rồi thì thôi, không mời lại
            } else if (ev.target.closest('.ks-thoi') || ev.target.closest('.ks-dong')) {
                dong(true);           // từ chối cũng là một câu trả lời, đừng hỏi nữa
            }
        });

        document.body.appendChild(hop);
        // đợi một nhịp để trình duyệt kịp vẽ rồi mới chạy hiệu ứng trượt lên
        requestAnimationFrame(function () {
            hop.classList.add('hien');
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        var link = root.getAttribute('data-khaosat');
        if (!link || doc(KEY_XONG)) {
            return;
        }

        var batDau = parseInt(doc(KEY_BAT_DAU), 10);
        if (!batDau || isNaN(batDau)) {
            ghi(KEY_BAT_DAU, String(Date.now()));
            return;                   // lần đầu vào web thì chỉ bấm giờ
        }

        var nguong = parseInt(root.getAttribute('data-khaosat-sau'), 10) || 300000;
        var conLai = batDau + nguong - Date.now();

        if (conLai > 0) {
            // Chưa đủ giờ: hẹn đúng lúc còn thiếu, phòng khi học sinh ở lì một trang
            setTimeout(function () {
                if (!dangLamBai() && !doc(KEY_XONG)) {
                    dungThe(link);
                }
            }, conLai);
            return;
        }
        if (dangLamBai()) {
            return;                   // đang làm bài: im lặng, để trang sau mời
        }
        dungThe(link);
    });
})();
