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
        return localStorage.getItem(STORAGE_THEME_KEY) || 'pastel';
    }

    function getMode() {
        return localStorage.getItem(STORAGE_MODE_KEY) || 'light';
    }

    function setTheme(theme) {
        localStorage.setItem(STORAGE_THEME_KEY, theme);
        document.documentElement.setAttribute('data-theme', theme);
        updateCardCharacters();
    }

    function setMode(mode) {
        localStorage.setItem(STORAGE_MODE_KEY, mode);
        document.documentElement.setAttribute('data-mode', mode);
        updateCardCharacters();
    }

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

        // Nếu không có nút chuyển (trang admin / manager), cố định theme cũ và không chạy
        if (!btnTheme) {
            document.documentElement.setAttribute('data-theme', 'pastel');
            document.documentElement.setAttribute('data-mode', 'light');
            return;
        }

        function updateModeTooltip(mode) {
            if (btnMode) {
                var isDark = (mode === 'dark');
                btnMode.title = isDark ? 'Chế độ Ban đêm (Nhấn để chuyển sang Ban ngày)' : 'Chế độ Ban ngày (Nhấn để chuyển sang Ban đêm)';
                btnMode.setAttribute('aria-label', isDark ? 'Chuyển sang Ban ngày' : 'Chuyển sang Ban đêm');
            }
        }

        // Đồng bộ thuộc tính hiện tại
        var currentTheme = getTheme();
        var currentMode = getMode();
        document.documentElement.setAttribute('data-theme', currentTheme);
        document.documentElement.setAttribute('data-mode', currentMode);
        updateModeTooltip(currentMode);

        btnTheme.addEventListener('click', function () {
            var current = getTheme();
            var nextTheme = (current === 'kawai') ? 'pastel' : 'kawai';
            setTheme(nextTheme);
        });

        btnMode.addEventListener('click', function () {
            var current = getMode();
            var nextMode = (current === 'dark') ? 'light' : 'dark';
            setMode(nextMode);
            updateModeTooltip(nextMode);
        });

        updateCardCharacters();
    });
})();
