(function () {
    'use strict';

    const courseApi = '/api/v1/cursos';
    const diplomaApi = '/api/v1/diplomados';
    const searchForm = document.getElementById('client-search');
    const searchInput = document.getElementById('client-search-input');
    const categoryFilter = document.getElementById('course-category-filter');
    const modalityFilter = document.getElementById('course-modality-filter');
    const clearFilters = document.getElementById('clear-course-filters');
    const courseGrid = document.getElementById('course-grid');
    const courseStatus = document.getElementById('course-list-status');
    const coursePagination = document.getElementById('course-pagination');
    const diplomaGrid = document.getElementById('diploma-grid');
    const diplomaStatus = document.getElementById('diploma-list-status');
    const diplomaPagination = document.getElementById('diploma-pagination');
    const courseDetailDialog = document.getElementById('course-detail-dialog');
    const diplomaDetailDialog = document.getElementById('diploma-detail-dialog');
    const size = 8;
    let coursePage = 0;
    let diplomaPage = 0;
    let courseLoadNumber = 0;
    let diplomaLoadNumber = 0;
    let diplomaDetailLoadNumber = 0;
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

    function teacherNames(teachers) {
        return teachers?.length ? teachers.map((teacher) => teacher.nombre).join(', ') : 'Por asignar';
    }

    function date(value, weekday = false) {
        if (!value) return 'Por definir';
        return new Intl.DateTimeFormat('es-SV', {
            ...(weekday ? {weekday: 'long'} : {}),
            day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC'
        }).format(new Date(value + 'T00:00:00Z'));
    }

    function hours(value) {
        return value + (value === 1 ? ' hora' : ' horas');
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

    function catalogCard(item, type) {
        const isDiploma = type === 'diploma';
        const title = isDiploma ? item.nombre : item.titulo;
        const card = element('article', 'catalog-card course-card');
        const cover = element('div', 'course-cover ' + tone(item.idCategoria + (isDiploma ? 1 : 0)));
        cover.append(
            element('span', 'course-cover-category', item.categoria),
            element('span', 'course-cover-mark', isDiploma ? 'DIPLOMADO · UCA CFC' : 'CURSO · UCA CFC')
        );

        const body = element('div', 'card-body');
        body.append(
            element('span', 'card-provider', item.modalidad),
            element('h3', '', title),
            element('p', 'course-card-description', item.descripcion)
        );
        const facts = element('ul', 'course-facts');
        facts.append(
            fact('Inicio', date(item.fechaInicio)),
            fact('Duración', hours(item.duracionHoras)),
            isDiploma ? fact('Finaliza', date(item.fechaFin)) : fact('Horario', item.horario)
        );
        body.append(facts);

        const footer = element('div', 'course-card-footer');
        footer.append(element('strong', 'course-price', money(item.costo)));
        const detail = element('button', 'course-detail-button', 'Ver detalles');
        detail.type = 'button';
        detail.setAttribute('aria-label', 'Ver detalles de ' + title);
        detail.addEventListener('click', (event) => {
            if (isDiploma) openDiplomaDetail(item, event.currentTarget);
            else openCourseDetail(item, event.currentTarget);
        });
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
                request(courseApi + '/catalogos/categorias'), request(courseApi + '/catalogos/modalidades')
            ]);
            addOptions(categoryFilter, categories, 'Todas las categorías');
            addOptions(modalityFilter, modalities, 'Todas las modalidades');
        } catch (_error) {
            categoryFilter.disabled = true;
            modalityFilter.disabled = true;
        }
    }

    function query(pageNumber) {
        const params = new URLSearchParams({
            activo: 'true', pagina: String(pageNumber), tamano: String(size),
            ordenarPor: 'fechaInicio', direccion: 'asc'
        });
        const search = searchInput.value.trim();
        if (search) params.set('texto', search);
        if (categoryFilter.value) params.set('idCategoria', categoryFilter.value);
        if (modalityFilter.value) params.set('idModalidad', modalityFilter.value);
        return params;
    }

    function renderPagination(data, previous, next, indicator, container) {
        container.hidden = data.totalElementos === 0;
        previous.disabled = data.primera;
        next.disabled = data.ultima;
        indicator.textContent = 'Página ' + (data.paginaActual + 1) + ' de ' + Math.max(data.totalPaginas, 1);
    }

    async function loadCourses() {
        const thisLoad = ++courseLoadNumber;
        courseStatus.textContent = 'Cargando cursos disponibles...';
        courseStatus.classList.remove('is-error');
        courseGrid.replaceChildren();
        coursePagination.hidden = true;
        try {
            const data = await request(courseApi + '?' + query(coursePage));
            if (thisLoad !== courseLoadNumber) return;
            data.contenido.forEach((course) => courseGrid.append(catalogCard(course, 'course')));
            courseStatus.textContent = data.totalElementos
                ? data.totalElementos + (data.totalElementos === 1 ? ' curso disponible.' : ' cursos disponibles.')
                : 'No hay cursos activos que coincidan con los filtros seleccionados.';
            renderPagination(data,
                document.getElementById('course-previous-page'), document.getElementById('course-next-page'),
                document.getElementById('course-page-indicator'), coursePagination);
        } catch (error) {
            if (thisLoad !== courseLoadNumber) return;
            courseStatus.classList.add('is-error');
            courseStatus.textContent = error.message || 'No se pudo cargar el catálogo de cursos.';
        }
    }

    async function loadDiplomas() {
        const thisLoad = ++diplomaLoadNumber;
        diplomaStatus.textContent = 'Cargando diplomados disponibles...';
        diplomaStatus.classList.remove('is-error');
        diplomaGrid.replaceChildren();
        diplomaPagination.hidden = true;
        try {
            const data = await request(diplomaApi + '?' + query(diplomaPage));
            if (thisLoad !== diplomaLoadNumber) return;
            data.contenido.forEach((diploma) => diplomaGrid.append(catalogCard(diploma, 'diploma')));
            diplomaStatus.textContent = data.totalElementos
                ? data.totalElementos + (data.totalElementos === 1 ? ' diplomado disponible.' : ' diplomados disponibles.')
                : 'No hay diplomados activos que coincidan con los filtros seleccionados.';
            renderPagination(data,
                document.getElementById('diploma-previous-page'), document.getElementById('diploma-next-page'),
                document.getElementById('diploma-page-indicator'), diplomaPagination);
        } catch (error) {
            if (thisLoad !== diplomaLoadNumber) return;
            diplomaStatus.classList.add('is-error');
            diplomaStatus.textContent = error.message || 'No se pudo cargar el catálogo de diplomados.';
        }
    }

    async function loadAll(scroll = false) {
        await Promise.all([loadCourses(), loadDiplomas()]);
        if (scroll) document.getElementById('explorar').scrollIntoView({behavior: 'smooth', block: 'start'});
    }

    function openCourseDetail(course, trigger) {
        detailTrigger = trigger;
        document.getElementById('course-detail-category').textContent = course.categoria;
        document.getElementById('course-detail-title').textContent = course.titulo;
        document.getElementById('course-detail-description').textContent = course.descripcion;
        document.getElementById('course-detail-modality').textContent = course.modalidad;
        document.getElementById('course-detail-duration').textContent = hours(course.duracionHoras);
        document.getElementById('course-detail-dates').textContent = date(course.fechaInicio) + ' – ' + date(course.fechaFin);
        document.getElementById('course-detail-schedule').textContent = course.horario;
        document.getElementById('course-detail-capacity').textContent = course.cupoMaximo + ' personas';
        document.getElementById('course-detail-cost').textContent = money(course.costo);
        document.getElementById('course-detail-teachers').textContent = teacherNames(course.docentes);
        courseDetailDialog.showModal();
    }

    async function openDiplomaDetail(diploma, trigger) {
        detailTrigger = trigger;
        const thisLoad = ++diplomaDetailLoadNumber;
        document.getElementById('diploma-detail-category').textContent = diploma.categoria;
        document.getElementById('diploma-detail-title').textContent = diploma.nombre;
        document.getElementById('diploma-detail-description').textContent = diploma.descripcion;
        document.getElementById('diploma-detail-modality').textContent = diploma.modalidad;
        document.getElementById('diploma-detail-duration').textContent = hours(diploma.duracionHoras);
        document.getElementById('diploma-detail-dates').textContent = date(diploma.fechaInicio) + ' – ' + date(diploma.fechaFin);
        document.getElementById('diploma-detail-cost').textContent = money(diploma.costo);
        document.getElementById('diploma-detail-teachers').textContent = teacherNames(diploma.docentes);
        const sessionStatus = document.getElementById('diploma-sessions-status');
        const sessionList = document.getElementById('diploma-session-list');
        sessionStatus.textContent = 'Cargando sesiones...';
        sessionList.replaceChildren();
        diplomaDetailDialog.showModal();
        try {
            const sessions = await request(diplomaApi + '/' + diploma.idDiplomado + '/actividades');
            if (thisLoad !== diplomaDetailLoadNumber) return;
            sessions.forEach((session) => {
                const item = document.createElement('li');
                const schedule = session.horaInicio.slice(0, 5) + ' – ' + session.horaFin.slice(0, 5);
                const details = session.titulo + ' · ' + schedule + (session.cupo ? ' · Cupo: ' + session.cupo : '');
                item.append(element('strong', '', date(session.fecha, true)), element('span', '', details));
                sessionList.append(item);
            });
            sessionStatus.textContent = sessions.length
                ? sessions.length + (sessions.length === 1 ? ' sesión programada.' : ' sesiones programadas.')
                : 'No hay sesiones programadas.';
        } catch (error) {
            if (thisLoad === diplomaDetailLoadNumber) sessionStatus.textContent = error.message;
        }
    }

    function restoreDetailFocus() {
        if (detailTrigger?.isConnected) detailTrigger.focus();
        detailTrigger = null;
    }

    searchForm.addEventListener('submit', (event) => {
        event.preventDefault();
        coursePage = 0;
        diplomaPage = 0;
        loadAll(true);
    });
    [categoryFilter, modalityFilter].forEach((filter) => filter.addEventListener('change', () => {
        coursePage = 0;
        diplomaPage = 0;
        loadAll(true);
    }));
    clearFilters.addEventListener('click', () => {
        searchInput.value = '';
        categoryFilter.value = '';
        modalityFilter.value = '';
        coursePage = 0;
        diplomaPage = 0;
        loadAll(true);
    });
    document.getElementById('course-previous-page').addEventListener('click', () => { coursePage -= 1; loadCourses(); });
    document.getElementById('course-next-page').addEventListener('click', () => { coursePage += 1; loadCourses(); });
    document.getElementById('diploma-previous-page').addEventListener('click', () => { diplomaPage -= 1; loadDiplomas(); });
    document.getElementById('diploma-next-page').addEventListener('click', () => { diplomaPage += 1; loadDiplomas(); });
    document.getElementById('course-detail-close').addEventListener('click', () => courseDetailDialog.close());
    document.getElementById('diploma-detail-close').addEventListener('click', () => diplomaDetailDialog.close());
    courseDetailDialog.addEventListener('close', restoreDetailFocus);
    diplomaDetailDialog.addEventListener('close', restoreDetailFocus);

    const initialQuery = new URLSearchParams(window.location.search).get('q');
    if (initialQuery?.trim()) searchInput.value = initialQuery.trim();
    loadCatalogs();
    loadAll(Boolean(initialQuery?.trim()));
})();
