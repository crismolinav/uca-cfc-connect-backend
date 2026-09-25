(function () {
    'use strict';

    const states = {
        curso: {
            title: 'Comienza a explorar',
            copy: 'Aún no hay cursos en seguimiento. Conoce los formatos de formación disponibles en el CFC.'
        },
        completado: {
            title: 'Todavía no hay actividades completadas',
            copy: 'Cuando el seguimiento de cursos esté disponible, aquí podrás ver lo que hayas completado.'
        },
        certificados: {
            title: 'Todavía no hay certificados',
            copy: 'Cuando el sistema habilite certificados, podrás consultarlos desde este espacio.'
        }
    };
    const buttons = document.querySelectorAll('[data-learning-filter]');
    const title = document.getElementById('learning-empty-title');
    const copy = document.getElementById('learning-empty-copy');
    buttons.forEach((button) => button.addEventListener('click', () => {
        const state = states[button.dataset.learningFilter];
        buttons.forEach((item) => item.setAttribute('aria-pressed', String(item === button)));
        title.textContent = state.title;
        copy.textContent = state.copy;
    }));
})();
