(function () {
    'use strict';

    const cards = Array.from(document.querySelectorAll('.catalog-card'));
    const interests = Array.from(document.querySelectorAll('[data-interest]'));
    const showMore = document.getElementById('show-more-client');
    const searchForm = document.getElementById('client-search');
    const searchInput = document.getElementById('client-search-input');
    const searchStatus = document.getElementById('search-status');
    const interestStatus = document.getElementById('interest-status');
    let selected = 'todos';
    let query = '';
    let expanded = false;

    document.querySelectorAll('.catalog-card .card-body a').forEach((link) => {
        const label = document.createElement('span');
        label.className = 'card-action-disabled';
        label.textContent = link.textContent + ' · próximamente';
        link.replaceWith(label);
    });

    function matches(card) {
        if (query) {
            const text = card.dataset.category + ' ' + card.querySelector('h3').textContent + ' ' +
                card.querySelector('.card-body p').textContent + ' ' + card.querySelector('.card-type').textContent;
            return text.toLocaleLowerCase('es').includes(query);
        }
        return selected === 'todos' || card.dataset.category === selected;
    }

    function render() {
        const matching = cards.filter(matches);
        cards.forEach((card) => {
            const index = matching.indexOf(card);
            card.hidden = index < 0 || (!expanded && index >= 4);
        });
        showMore.hidden = matching.length <= 4;
        showMore.setAttribute('aria-expanded', String(expanded));
        showMore.textContent = expanded ? 'Mostrar menos' : 'Mostrar ' + (matching.length - 4) + ' más';
        if (query) {
            searchStatus.textContent = matching.length
                ? matching.length + ' opciones relacionadas con «' + query + '».'
                : 'No encontramos ese formato. Los programas específicos aún no están publicados.';
        }
    }

    function selectInterest(category) {
        selected = category;
        query = '';
        expanded = false;
        searchInput.value = '';
        searchStatus.textContent = '';
        interests.forEach((button) => button.setAttribute('aria-pressed', String(button.dataset.interest === category)));
        interestStatus.textContent = category === 'todos'
            ? 'Mostrando todos los formatos y servicios.'
            : 'Mostrando opciones de ' + interests.find((button) => button.dataset.interest === category).textContent.toLocaleLowerCase('es') + '.';
        render();
    }

    interests.forEach((button) => button.addEventListener('click', () => {
        selectInterest(button.dataset.interest);
        document.getElementById('explorar').scrollIntoView({behavior: 'smooth', block: 'start'});
    }));
    document.querySelectorAll('[data-jump]').forEach((link) => link.addEventListener('click', () => {
        selectInterest(link.dataset.jump);
    }));
    showMore.addEventListener('click', () => {
        expanded = !expanded;
        render();
    });
    searchForm.addEventListener('submit', (event) => {
        event.preventDefault();
        query = searchInput.value.trim().toLocaleLowerCase('es');
        if (!query) {
            searchStatus.textContent = 'Escribe una palabra para buscar formatos en esta página.';
            searchInput.focus();
            return;
        }
        selected = 'todos';
        expanded = true;
        interests.forEach((button) => button.setAttribute('aria-pressed', String(button.dataset.interest === 'todos')));
        render();
        document.getElementById('explorar').scrollIntoView({behavior: 'smooth', block: 'start'});
    });
    const initialQuery = new URLSearchParams(window.location.search).get('q');
    if (initialQuery?.trim()) {
        searchInput.value = initialQuery.trim();
        query = initialQuery.trim().toLocaleLowerCase('es');
        expanded = true;
    }
    render();
})();
