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

        // Smooth-scroll to a bookmarked section anchor
        if (window.location.hash) {
            var el = document.querySelector(window.location.hash);
            if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    });
})();
