(function () {
    'use strict';

    const button = document.getElementById('account-menu-button');
    const menu = document.getElementById('account-menu');
    if (!button || !menu) return;

    function closeMenu(returnFocus) {
        menu.hidden = true;
        button.setAttribute('aria-expanded', 'false');
        button.setAttribute('aria-label', 'Abrir menú de cuenta');
        if (returnFocus) button.focus();
    }

    button.addEventListener('click', () => {
        if (!menu.hidden) {
            closeMenu(false);
            return;
        }
        menu.hidden = false;
        button.setAttribute('aria-expanded', 'true');
        button.setAttribute('aria-label', 'Cerrar menú de cuenta');
        menu.querySelector('a').focus();
    });
    document.addEventListener('click', (event) => {
        if (!menu.hidden && !event.target.closest('.account-control')) closeMenu(false);
    });
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && !menu.hidden) closeMenu(true);
    });
    document.addEventListener('focusin', (event) => {
        if (!menu.hidden && !event.target.closest('.account-control')) closeMenu(false);
    });
})();
