(function () {
    'use strict';

    const toggle = document.getElementById('menu-toggle');
    const navigation = document.getElementById('site-navigation');

    function closeMenu() {
        toggle.setAttribute('aria-expanded', 'false');
        navigation.classList.remove('is-open');
    }

    toggle.addEventListener('click', () => {
        const isOpen = toggle.getAttribute('aria-expanded') === 'true';
        toggle.setAttribute('aria-expanded', String(!isOpen));
        navigation.classList.toggle('is-open', !isOpen);
    });

    navigation.querySelectorAll('a').forEach((link) => {
        link.addEventListener('click', closeMenu);
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && toggle.getAttribute('aria-expanded') === 'true') {
            closeMenu();
            toggle.focus();
        }
    });

    window.matchMedia('(min-width: 801px)').addEventListener('change', (event) => {
        if (event.matches) closeMenu();
    });

    document.getElementById('announcement-close').addEventListener('click', () => {
        document.getElementById('announcement').hidden = true;
    });

    const cards = Array.from(document.querySelectorAll('.formation-card'));
    const filters = Array.from(document.querySelectorAll('[data-filter]'));
    const showMore = document.getElementById('show-more');
    let activeFilter = 'todos';
    let expanded = false;

    function renderCards() {
        const matching = cards.filter((card) => activeFilter === 'todos' || card.dataset.category === activeFilter);
        matching.forEach((card, index) => {
            card.hidden = !expanded && index >= 3;
        });
        cards.filter((card) => !matching.includes(card)).forEach((card) => {
            card.hidden = true;
        });
        showMore.hidden = matching.length <= 3;
        showMore.setAttribute('aria-expanded', String(expanded));
        showMore.textContent = expanded ? 'Mostrar menos' : 'Mostrar ' + (matching.length - 3) + ' más';
    }

    function setFilter(filter) {
        activeFilter = filter;
        expanded = false;
        filters.forEach((button) => {
            button.setAttribute('aria-pressed', String(button.dataset.filter === filter));
        });
        renderCards();
    }

    filters.forEach((button) => {
        button.addEventListener('click', () => setFilter(button.dataset.filter));
    });
    showMore.addEventListener('click', () => {
        expanded = !expanded;
        renderCards();
    });
    document.querySelectorAll('.format-pills a').forEach((link) => {
        link.addEventListener('click', () => setFilter(link.hash.slice(1)));
    });
    const initialCategory = window.location.hash.slice(1);
    if (filters.some((button) => button.dataset.filter === initialCategory)) {
        setFilter(initialCategory);
    } else {
        renderCards();
    }

    document.getElementById('site-search').addEventListener('submit', (event) => {
        event.preventDefault();
        const query = document.getElementById('search-query').value.trim().toLocaleLowerCase('es');
        const status = document.getElementById('search-status');
        if (!query) {
            status.textContent = 'Escribe una palabra para buscar en esta página.';
            document.getElementById('search-query').focus();
            return;
        }

        const match = cards.find((card) => {
            const searchable = card.querySelector('h3').textContent + ' ' + card.querySelector('p').textContent;
            return searchable.toLocaleLowerCase('es').includes(query);
        });
        if (match) {
            setFilter(match.dataset.category);
            status.textContent = 'Encontramos una sección relacionada con «' + query + '».';
            match.scrollIntoView({behavior: 'smooth', block: 'start'});
        } else {
            status.textContent = 'No hay coincidencias para «' + query + '» en esta página. El catálogo de cursos todavía está en preparación.';
            document.getElementById('formacion').scrollIntoView({behavior: 'smooth', block: 'start'});
        }
        closeMenu();
    });
})();
