// ponytail: click logger — capture clicks, batch-send to backend for debugging
// Load in layout only when APP_DEBUG=true or CLICK_LOGGER_ENABLED=true

(function () {
    'use strict';

    const BATCH_SIZE = parseInt('{{ config("click_logger.batch_size", 10) }}') || 10;
    const FLUSH_INTERVAL = 5000; // ms
    const ENDPOINT = '/api/v1/logs';

    let buffer = [];
    let sessionId = sessionStorage.getItem('_cl_sid');

    if (!sessionId) {
        sessionId = 'sess-' + Date.now() + '-' + Math.random().toString(36).slice(2, 9);
        sessionStorage.setItem('_cl_sid', sessionId);
    }

    function getStableSelector(el) {
        if (el.dataset.testid) return '[data-testid="' + el.dataset.testid + '"]';
        if (el.id) return '#' + el.id;
        if (el.dataset.action) return '[data-action="' + el.dataset.action + '"]';
        // ponytail: CSS path fallback, max 4 levels deep
        var path = [], max = 4, e = el;
        while (e && e !== document.body && max-- > 0) {
            var tag = e.tagName.toLowerCase();
            if (e.id) { path.unshift('#' + e.id); break; }
            path.unshift(tag);
            e = e.parentElement;
        }
        return path.join(' > ');
    }

    function flush() {
        if (!buffer.length) return;
        var payload = buffer.splice(0, BATCH_SIZE);
        fetch(ENDPOINT, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Log-Session-Id': sessionId,
                'X-CSRF-TOKEN': document.querySelector('meta[name="csrf-token"]')?.content || '',
            },
            body: JSON.stringify({ logs: payload }),
            keepalive: true,
        }).catch(function () {
            // ponytail: silent fail — don't break the app for logging failures
        });
    }

    document.addEventListener('click', function (e) {
        if (e.target.closest('[data-no-log]')) return; // guard: exclude sensitive elements

        buffer.push({
            timestamp: new Date().toISOString(),
            action: 'click',
            selector: getStableSelector(e.target),
            url: window.location.pathname + window.location.search,
            text: (e.target.textContent || '').trim().slice(0, 255) || null,
            tag: e.target.tagName || null,
            meta: {
                viewport: { w: window.innerWidth, h: window.innerHeight },
                scroll: { x: window.scrollX, y: window.scrollY },
            },
        });

        if (buffer.length >= BATCH_SIZE) flush();
    }, true);

    setInterval(flush, FLUSH_INTERVAL);
    window.addEventListener('beforeunload', function () {
        if (buffer.length && navigator.sendBeacon) {
            navigator.sendBeacon(ENDPOINT, JSON.stringify({ logs: buffer }));
            buffer = [];
        }
    });
})();
