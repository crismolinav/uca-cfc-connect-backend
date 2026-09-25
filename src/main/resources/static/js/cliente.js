(function () {
    'use strict';

    const api = '/api/v1/cursos';
    const searchForm = document.getElementById('client-search');
    const searchInput = document.getElementById('client-search-input');
    const categoryFilter = document.getElementById('course-category-filter');
    const modalityFilter = document.getElementById('course-modality-filter');
    const clearFilters = document.getElementById('clear-course-filters');
    const grid = document.getElementById('course-grid');
    const status = document.getElementById('course-list-status');
    const pagination = document.getElementById('course-pagination');
    const previousPage = document.getElementById('course-previous-page');
    const nextPage = document.getElementById('course-next-page');
    const pageIndicator = document.getElementById('course-page-indicator');
    const detailDialog = document.getElementById('course-detail-dialog');
    let page = 0;
    const size = 8;
    let loadNumber = 0;
    let detailTrigger = null;

    async function request(url) {
        const response = await fetch(url, {credentials: 'same-origin'});
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.message || data.mensaje || 'No se pudo cargar la información.');
        }
        return data;
    }

    function money(value) {
        return new Intl.NumberFormat('es-SV', {style: 'currency', currency: 'USD'}).format(value);
    }

    function date(value) {
        if (!value) return 'Por definir';
        return new Intl.DateTimeFormat('es-SV', {
            day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC'
        }).format(new Date(value + 'T00:00:00Z'));
    }

    function element(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
    }

    function fact(label, value) {
        const item = document.createElement('li');
        item.append(element('span', 'course-fact-label', label), element('strong', '', value));
        return item;
    }

    function tone(id) {
        return 'course-cover-tone-' + ((Number(id) || 0) % 4 + 1);
    }

    function courseCard(course) {
        const card = element('article', 'catalog-card course-card');
        const cover = element('div', 'course-cover ' + tone(course.idCategoria));
        cover.append(
            element('span', 'course-cover-category', course.categoria),
            element('span', 'course-cover-mark', 'UCA CFC')
        );

        const body = element('div', 'card-body');
        body.append(
            element('span', 'card-provider', course.modalidad),
            element('h3', '', course.titulo),
            element('p', 'course-card-description', course.descripcion)
        );
        const facts = element('ul', 'course-facts');
        facts.append(
            fact('Inicio', date(course.fechaInicio)),
            fact('Duración', course.duracionHoras + (course.duracionHoras === 1 ? ' hora' : ' horas')),
            fact('Horario', course.horario)
        );
        body.append(facts);

        const footer = element('div', 'course-card-footer');
        footer.append(element('strong', 'course-price', money(course.costo)));
        const detail = element('button', 'course-detail-button', 'Ver detalles');
        detail.type = 'button';
        detail.setAttribute('aria-label', 'Ver detalles de ' + course.titulo);
        detail.addEventListener('click', (event) => openDetail(course, event.currentTarget));
        footer.append(detail);
        body.append(footer);
        card.append(cover, body);
        return card;
    }

    function addOptions(select, items, placeholder) {
        const selected = select.value;
        select.replaceChildren(new Option(placeholder, ''));
        items.forEach((item) => select.append(new Option(item.nombre, String(item.id))));
        select.value = selected;
    }

    async function loadCatalogs() {
        try {
            const [categories, modalities] = await Promise.all([
                request(api + '/catalogos/categorias'),
                request(api + '/catalogos/modalidades')
            ]);
            addOptions(categoryFilter, categories, 'Todas las categorías');
            addOptions(modalityFilter, modalities, 'Todas las modalidades');
        } catch (_error) {
            categoryFilter.disabled = true;
            modalityFilter.disabled = true;
        }
    }

    function courseQuery() {
        const params = new URLSearchParams({
            activo: 'true',
            pagina: String(page),
            tamano: String(size),
            ordenarPor: 'fechaInicio',
            direccion: 'asc'
        });
        const query = searchInput.value.trim();
        if (query) params.set('texto', query);
        if (categoryFilter.value) params.set('idCategoria', categoryFilter.value);
        if (modalityFilter.value) params.set('idModalidad', modalityFilter.value);
        return params;
    }

    async function loadCourses(scroll = false) {
        const thisLoad = ++loadNumber;
        status.textContent = 'Cargando cursos disponibles...';
        status.classList.remove('is-error');
        grid.replaceChildren();
        pagination.hidden = true;
        try {
            const data = await request(api + '?' + courseQuery());
            if (thisLoad !== loadNumber) return;
            data.contenido.forEach((course) => grid.append(courseCard(course)));
            status.textContent = data.totalElementos
                ? data.totalElementos + (data.totalElementos === 1 ? ' curso disponible.' : ' cursos disponibles.')
                : 'No hay cursos activos que coincidan con los filtros seleccionados.';
            pagination.hidden = data.totalElementos === 0;
            previousPage.disabled = data.primera;
            nextPage.disabled = data.ultima;
            pageIndicator.textContent = 'Página ' + (data.paginaActual + 1) + ' de ' + Math.max(data.totalPaginas, 1);
            if (scroll) document.getElementById('explorar').scrollIntoView({behavior: 'smooth', block: 'start'});
        } catch (error) {
            if (thisLoad !== loadNumber) return;
            status.classList.add('is-error');
            status.textContent = error.message || 'No se pudo cargar el catálogo de cursos.';
        }
    }

    function openDetail(course, trigger) {
        detailTrigger = trigger;
        document.getElementById('course-detail-category').textContent = course.categoria;
        document.getElementById('course-detail-title').textContent = course.titulo;
        document.getElementById('course-detail-description').textContent = course.descripcion;
        document.getElementById('course-detail-modality').textContent = course.modalidad;
        document.getElementById('course-detail-duration').textContent =
            course.duracionHoras + (course.duracionHoras === 1 ? ' hora' : ' horas');
        document.getElementById('course-detail-dates').textContent =
            date(course.fechaInicio) + ' – ' + date(course.fechaFin);
        document.getElementById('course-detail-schedule').textContent = course.horario;
        document.getElementById('course-detail-capacity').textContent = course.cupoMaximo + ' personas';
        document.getElementById('course-detail-cost').textContent = money(course.costo);
        detailDialog.showModal();
    }

    searchForm.addEventListener('submit', (event) => {
        event.preventDefault();
        page = 0;
        loadCourses(true);
    });
    [categoryFilter, modalityFilter].forEach((filter) => filter.addEventListener('change', () => {
        page = 0;
        loadCourses(true);
    }));
    clearFilters.addEventListener('click', () => {
        searchInput.value = '';
        categoryFilter.value = '';
        modalityFilter.value = '';
        page = 0;
        loadCourses(true);
    });
    previousPage.addEventListener('click', () => {
        page -= 1;
        loadCourses(true);
    });
    nextPage.addEventListener('click', () => {
        page += 1;
        loadCourses(true);
    });
    document.getElementById('course-detail-close').addEventListener('click', () => detailDialog.close());
    detailDialog.addEventListener('close', () => {
        if (detailTrigger?.isConnected) detailTrigger.focus();
        detailTrigger = null;
    });

    const initialQuery = new URLSearchParams(window.location.search).get('q');
    if (initialQuery?.trim()) searchInput.value = initialQuery.trim();
    loadCatalogs();
    loadCourses(Boolean(initialQuery?.trim()));
})();
