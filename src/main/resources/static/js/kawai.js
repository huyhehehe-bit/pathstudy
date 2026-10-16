(function () {
    'use strict';

    var STORAGE_THEME_KEY = 'pathstudy-theme';
    var STORAGE_MODE_KEY = 'pathstudy-mode';

    var CHARACTERS_DAY = [
        '/theme-kawai/ban-ngay/characters/cat_girl.png',
        '/theme-kawai/ban-ngay/characters/archer.png',
        '/theme-kawai/ban-ngay/characters/knight.png',
        '/theme-kawai/ban-ngay/characters/boxer.png'
    ];

    var CHARACTERS_NIGHT = [
        '/theme-kawai/ban-dem/characters/cat_girl.png',
        '/theme-kawai/ban-dem/characters/archer.png',
        '/theme-kawai/ban-dem/characters/knight.png',
        '/theme-kawai/ban-dem/characters/boxer.png'
    ];

    function getTheme() {
        return document.documentElement.getAttribute('data-theme')
            || localStorage.getItem(STORAGE_THEME_KEY) || 'pastel';
    }

    // Tone Sáng/Tối do js/theme.js quản. Đọc từ thuộc tính trên <html> chứ
    // KHÔNG đọc thẳng localStorage: người chưa chọn bao giờ thì chưa có gì
    // trong localStorage mà trang vẫn đang hiển thị theo cài đặt của máy.
    function getMode() {
        var m = document.documentElement.getAttribute('data-mode');
        if (m === 'light' || m === 'dark') {
            return m;
        }
        return (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches)
            ? 'dark' : 'light';
    }

    // setTheme()/setMode() đã bỏ: việc chọn giao diện nằm trọn trong
    // js/theme.js, để chỉ MỘT nơi ghi localStorage. Trước đây hai file cùng
    // bắt sự kiện bấm nút thì một cú bấm đổi hai lượt = nhìn như không đổi.

    function updateCardCharacters() {
        var theme = getTheme();
        var mode = getMode();
        var chars = (mode === 'dark') ? CHARACTERS_NIGHT : CHARACTERS_DAY;

        var lessonCards = document.querySelectorAll('.content .card.between');
        lessonCards.forEach(function (card, idx) {
            var charEl = card.querySelector('.kawai-card-char');
            if (!charEl) {
                charEl = document.createElement('span');
                charEl.className = 'kawai-card-char';
                charEl.setAttribute('aria-hidden', 'true');
                var rightGroup = card.querySelector('.center:last-child');
                if (rightGroup) {
                    rightGroup.insertBefore(charEl, rightGroup.firstChild);
                }
            }
            if (charEl) {
                var charImg = chars[idx % chars.length];
                charEl.style.backgroundImage = 'url("' + charImg + '")';
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        var btnTheme = document.getElementById('btnThemeSwitch');
        var btnMode = document.getElementById('btnModeSwitch');

        function updateModeTooltip(mode) {
            if (btnMode) {
                var isDark = (mode === 'dark');
                btnMode.title = isDark ? 'Chế độ Ban đêm (Nhấn để chuyển sang Ban ngày)' : 'Chế độ Ban ngày (Nhấn để chuyển sang Ban đêm)';
                btnMode.setAttribute('aria-label', isDark ? 'Chuyển sang Ban ngày' : 'Chuyển sang Ban đêm');
            }
        }

        updateModeTooltip(getMode());

        // theme.js đổi tone → đổi theo bộ nhân vật ngày/đêm và chữ gợi ý.
        document.addEventListener('pathstudy:mode', function (ev) {
            updateModeTooltip(ev.detail);
            updateCardCharacters();
        });

        // Việc chọn phong cách (pastel/kawai) và tone (ngày/đêm) nay nằm trọn
        // trong js/theme.js — một nơi duy nhất ghi localStorage. File này chỉ
        // còn lo phần trang trí: thay bộ nhân vật theo ngày/đêm.
        if (!btnTheme) {
            return;
        }

        updateCardCharacters();
    });
})();
