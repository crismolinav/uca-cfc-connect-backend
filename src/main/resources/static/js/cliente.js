(function () {
    'use strict';

    const courseApi = '/api/v1/cursos';
    const diplomaApi = '/api/v1/diplomados';
    const spaceApi = '/api/v1/espacios';
    const rentalApi = '/api/v1/alquileres';
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
    const diplomaDetailDialog = document.getElementById('diploma-detail-dialog');
    const size = 6;
    let coursePage = 0;
    let diplomaPage = 0;
    let courseLoadNumber = 0;
    let diplomaLoadNumber = 0;
    let spaceLoadNumber = 0;
    let diplomaDetailLoadNumber = 0;
    let detailTrigger = null;
    let spacePage = 0;
    let activeCatalogView = 'courses';
    let selectedSpace = null;
    let csrfToken = null;
    let enrolledCourseIds = new Set();
    let clientEnrollments = [];
    let enrollmentListLoaded = false;

    function setCatalogView(view, scroll = false) {
        const sections = {courses: 'explorar', diplomas: 'diplomados', spaces: 'espacios'};
        if (!sections[view]) return;
        activeCatalogView = view;
        Object.entries(sections).forEach(([key, id]) => {
            document.getElementById(id).hidden = key !== view;
            const button = document.querySelector('[data-catalog-view="' + key + '"]');
            button.classList.toggle('is-active', key === view);
            button.setAttribute('aria-pressed', String(key === view));
        });
        document.getElementById('catalog-filters').hidden = view === 'spaces';
        if (scroll) scrollToSection(sections[view]);
    }

    async function request(url) {
        const response = await fetch(url, {credentials: 'same-origin'});
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            const error = new Error(data.message || data.mensaje || 'No se pudo cargar la información.');
            error.status = response.status;
            throw error;
        }
        return data;
    }

    async function secureRequest(url, options = {}) {
        if (!csrfToken) {
            const csrfResponse = await fetch('/api/v1/auth/csrf', {credentials: 'same-origin'});
            if (!csrfResponse.ok) throw new Error('No se pudo iniciar una solicitud segura.');
            csrfToken = (await csrfResponse.json()).token;
        }
        const response = await fetch(url, {
            ...options, credentials: 'same-origin',
            headers: {...options.headers, 'X-CSRF-TOKEN': csrfToken}
        });
        const data = response.status === 204 ? null : await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.mensaje || data.message || 'No se pudo completar la solicitud.');
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

    function updateOverviewCount(id, value) {
        const counter = document.getElementById(id);
        if (counter) counter.textContent = new Intl.NumberFormat('es-SV').format(value);
    }

    function setGridLoading(grid, loading) {
        grid.classList.toggle('is-loading', loading);
        grid.setAttribute('aria-busy', String(loading));
    }

    function replaceGridCards(grid, cards) {
        grid.replaceChildren(...cards);
        setGridLoading(grid, false);
    }

    function preparePagination(container) {
        container.querySelectorAll('button').forEach((button) => { button.disabled = true; });
    }

    function scrollToSection(id) {
        const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth';
        document.getElementById(id).scrollIntoView({behavior, block: 'start'});
    }

    function fact(label, value) {
        const item = document.createElement('li');
        item.append(element('span', 'course-fact-label', label), element('strong', '', value));
        return item;
    }

    function tone(id) {
        return 'course-cover-tone-' + ((Number(id) || 0) % 4 + 1);
    }

    function categoryIcon(category) {
        const value = String(category || '').toLocaleLowerCase('es');
        if (value.includes('tecnolog') || value.includes('inform')) return 'laptop';
        if (value.includes('administr') || value.includes('negocio') || value.includes('finanz')) return 'chart';
        if (value.includes('arte') || value.includes('diseñ') || value.includes('manga')) return 'palette';
        if (value.includes('idioma') || value.includes('inglés')) return 'globe';
        if (value.includes('salud')) return 'health';
        if (value.includes('educ')) return 'book';
        return 'sparkle';
    }

    function icon(name, className) {
        const paths = {
            laptop: ['M4 5h16v11H4z', 'M2 19h20l-2 2H4z'],
            chart: ['M4 19V10h4v9z', 'M10 19V5h4v14z', 'M16 19v-7h4v7z'],
            palette: ['M12 3a9 9 0 1 0 0 18h1a2 2 0 0 0 1.5-3.3c-.8-.9-.2-2.2 1-2.2H18a3 3 0 0 0 3-3c0-5.2-4-9.5-9-9.5z', 'M7.5 11h.01', 'M10 7.5h.01', 'M15 8h.01', 'M17 11.5h.01'],
            globe: ['M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z', 'M3 12h18', 'M12 3a15 15 0 0 1 0 18', 'M12 3a15 15 0 0 0 0 18'],
            health: ['M12 21s-8-4.5-8-11a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 10c0 6.5-8 11-8 11z', 'M12 8v7', 'M8.5 11.5h7'],
            book: ['M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21z', 'M4 5.5v15', 'M8 7h8', 'M8 11h8'],
            sparkle: ['m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z', 'm19 15 .8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z']
        };
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('viewBox', '0 0 24 24');
        svg.setAttribute('aria-hidden', 'true');
        svg.setAttribute('focusable', 'false');
        svg.classList.add(className);
        (paths[name] || paths.sparkle).forEach((d) => {
            const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            path.setAttribute('d', d);
            svg.append(path);
        });
        return svg;
    }

    function spaceCard(space) {
        const card = element('article', 'catalog-card course-card space-card');
        const cover = element('div', 'space-cover ' + tone(space.idEspacio));
        cover.append(element('span', 'space-type', space.tipo), element('span', 'course-cover-mark', 'ESPACIO · UCA CFC'));
        const content = element('div', 'card-body');
        const limit = space.duracionMaximaHoras == null ? 'horario por confirmar' : 'máximo ' + Number(space.duracionMaximaHoras) + ' h';
        content.append(element('span', 'card-provider', space.capacidad + ' personas · ' + limit), element('h3', '', space.nombre),
            element('p', 'course-card-description', space.equipamiento || 'Espacio sin equipamiento especificado.'));
        const footer = element('div', 'course-card-footer'); footer.append(element('strong', 'course-price', money(space.precio)));
        const reserve = element('button', 'course-detail-button', 'Solicitar reserva'); reserve.type = 'button'; reserve.addEventListener('click', event => openReservation(space, event.currentTarget)); footer.append(reserve); content.append(footer); card.append(cover, content); return card;
    }

    async function loadSpaces() {
        const thisLoad = ++spaceLoadNumber;
        const status = document.getElementById('space-list-status'), grid = document.getElementById('space-grid'), pagination = document.getElementById('space-pagination');
        status.textContent = 'Cargando espacios disponibles...'; status.classList.remove('is-error'); setGridLoading(grid, true); preparePagination(pagination);
        try {
            const params = new URLSearchParams({disponible: 'true', pagina: String(spacePage), tamano: String(size), ordenarPor: 'capacidad', direccion: 'asc'});
            const data = await request(spaceApi + '?' + params);
            if (thisLoad !== spaceLoadNumber) return;
            if (data.totalPaginas > 0 && spacePage >= data.totalPaginas) { spacePage = data.totalPaginas - 1; return loadSpaces(); }
            replaceGridCards(grid, data.contenido.map(spaceCard));
            updateOverviewCount('overview-space-count', data.totalElementos);
            status.textContent = data.totalElementos ? data.contenido.length + (data.contenido.length === 1 ? ' espacio en esta página.' : ' espacios en esta página.') : 'Por ahora no hay espacios disponibles para reservar.';
            renderPagination(data, document.getElementById('space-previous-page'), document.getElementById('space-next-page'), document.getElementById('space-page-indicator'), pagination);
        } catch (error) {
            if (thisLoad !== spaceLoadNumber) return;
            setGridLoading(grid, false); status.classList.add('is-error'); status.textContent = error.message;
        }
    }

    function rentalBadge(state) { return element('span', 'rental-state state-' + state.toLowerCase(), ({PENDIENTE:'Pendiente',CONFIRMADO:'Confirmada',CANCELADO:'Cancelada',FINALIZADO:'Finalizada'}[state] || state)); }

    async function loadMyRentals() {
        const status = document.getElementById('rental-list-status'), list = document.getElementById('rental-list'); list.replaceChildren();
        try {
            const rentals = await request(rentalApi + '/mios');
            rentals.forEach(rental => {
                const item = element('article', 'rental-item'); const copy = element('div', 'rental-copy');
                copy.append(element('strong', '', rental.nombreEspacio), element('span', '', date(rental.fecha) + ' · ' + rental.horaInicio.slice(0,5) + '–' + rental.horaFin.slice(0,5)), element('small', '', rental.motivo || 'Sin motivo especificado'));
                const controls = element('div', 'rental-controls'); controls.append(rentalBadge(rental.estado));
                if (['PENDIENTE','CONFIRMADO'].includes(rental.estado)) { const cancel = element('button', 'rental-cancel', 'Cancelar'); cancel.type = 'button'; cancel.addEventListener('click', () => cancelRental(rental, cancel)); controls.append(cancel); }
                item.append(copy, controls); list.append(item);
            });
            status.textContent = rentals.length ? rentals.length + (rentals.length === 1 ? ' reserva registrada.' : ' reservas registradas.') : 'Aún no has solicitado ningún espacio. Elige uno del catálogo para comenzar.';
        } catch (error) { status.classList.add('is-error'); status.textContent = error.message; }
    }

    function openReservation(space, trigger) {
        selectedSpace = space; detailTrigger = trigger; const form = document.getElementById('reservation-form'); form.reset();
        document.getElementById('reservation-date').min = new Date().toISOString().slice(0,10); document.getElementById('reservation-space-name').textContent = space.nombre;
        document.getElementById('reservation-space-meta').textContent = space.tipo + ' · ' + space.capacidad + ' personas · máximo ' + Number(space.duracionMaximaHoras) + ' h · ' + money(space.precio);
        document.getElementById('reservation-limit').textContent = 'Puedes reservar este espacio por un máximo de ' + Number(space.duracionMaximaHoras) + ' horas continuas.';
        document.getElementById('reservation-error').textContent = ''; document.getElementById('space-reservation-dialog').showModal();
    }

    async function submitReservation(event) {
        event.preventDefault(); const form = event.currentTarget; if (!form.reportValidity()) return;
        if (form.horaFin.value <= form.horaInicio.value) { document.getElementById('reservation-error').textContent = 'La hora de fin debe ser posterior a la hora de inicio.'; return; }
        const [startHour, startMinute] = form.horaInicio.value.split(':').map(Number), [endHour, endMinute] = form.horaFin.value.split(':').map(Number);
        const requestedMinutes = endHour * 60 + endMinute - startHour * 60 - startMinute;
        if (requestedMinutes > Number(selectedSpace.duracionMaximaHoras) * 60) {
            document.getElementById('reservation-error').textContent = 'Este espacio admite reservas de máximo ' + Number(selectedSpace.duracionMaximaHoras) + ' horas.';
            return;
        }
        const button = document.getElementById('reservation-submit'); button.disabled = true; button.textContent = 'Enviando...'; document.getElementById('reservation-error').textContent = '';
        try {
            await secureRequest(rentalApi + '/solicitudes', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({idEspacio:selectedSpace.idEspacio,fecha:form.fecha.value,horaInicio:form.horaInicio.value,horaFin:form.horaFin.value,motivo:form.motivo.value.trim()})});
            document.getElementById('space-reservation-dialog').close(); await loadMyRentals(); document.getElementById('mis-reservas').scrollIntoView({behavior:'smooth'});
        } catch (error) { document.getElementById('reservation-error').textContent = error.message; }
        finally { button.disabled = false; button.textContent = button.dataset.idleLabel; }
    }

    async function cancelRental(rental, button) {
        if (!window.confirm('¿Cancelar la reserva de ' + rental.nombreEspacio + '?')) return; button.disabled = true;
        try { await secureRequest(rentalApi + '/' + rental.idAlquiler + '/cancelar', {method:'PATCH'}); await loadMyRentals(); }
        catch (error) { document.getElementById('rental-list-status').textContent = error.message; button.disabled = false; }
    }

    function catalogCard(item, type) {
        const isDiploma = type === 'diploma';
        const title = isDiploma ? item.nombre : item.titulo;
        const card = element('article', 'catalog-card course-card');
        const cover = element('div', 'course-cover ' + tone(item.idCategoria + (isDiploma ? 1 : 0)));
        if (!isDiploma && item.imagenUrl) {
            const image = element('img', 'course-cover-image');
            image.src = item.imagenUrl;
            image.alt = '';
            image.loading = 'lazy';
            cover.append(image);
        }
        const category = element('span', 'course-cover-category');
        const categoryIconElement = icon(categoryIcon(item.categoria), 'course-category-icon');
        category.append(categoryIconElement, element('span', '', item.categoria || 'Formación continua'));
        cover.append(
            category,
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
            else window.location.assign('/cliente/curso.html?id=' + encodeURIComponent(item.idCurso));
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
        container.hidden = data.totalPaginas <= 1;
        container.dataset.totalPages = String(data.totalPaginas || 0);
        previous.disabled = Boolean(data.primera) || data.paginaActual <= 0;
        next.disabled = Boolean(data.ultima) || data.paginaActual >= data.totalPaginas - 1;
        indicator.textContent = 'Página ' + (data.paginaActual + 1) + ' de ' + Math.max(data.totalPaginas || 0, 1);
    }

    async function loadCourses() {
        const thisLoad = ++courseLoadNumber;
        courseStatus.textContent = 'Cargando cursos disponibles...';
        courseStatus.classList.remove('is-error');
        setGridLoading(courseGrid, true);
        preparePagination(coursePagination);
        try {
            const [firstPage, enrollments] = await Promise.all([
                request(courseApi + '?' + query(0)),
                enrollmentListLoaded ? Promise.resolve(null) : request('/api/v1/cliente/inscripciones').then((result) => {
                    if (!Array.isArray(result)) throw new Error('No se pudo comprobar el estado de tus inscripciones. Recarga la página.');
                    return result;
                }).catch((error) => {
                    if (error.status === 401 || error.status === 403) return [];
                    throw error;
                })
            ]);
            if (Array.isArray(enrollments)) {
                clientEnrollments = enrollments;
                enrolledCourseIds = new Set(enrollments
                    .map((enrollment) => enrollment.idCurso == null ? null : String(enrollment.idCurso))
                    .filter((id) => id != null));
                enrollmentListLoaded = true;
            }
            if (thisLoad !== courseLoadNumber) return;
            const inicio = coursePage * size;
            const cursosDisponibles = [];
            let paginaActual = firstPage;
            let paginaCruda = 0;
            while (paginaActual && paginaCruda < paginaActual.totalPaginas && cursosDisponibles.length <= inicio + size) {
                if (paginaCruda > 0) paginaActual = await request(courseApi + '?' + query(paginaCruda));
                if (thisLoad !== courseLoadNumber) return;
                cursosDisponibles.push(...paginaActual.contenido.filter((course) => !enrolledCourseIds.has(String(course.idCurso))));
                paginaCruda += 1;
            }
            updateOverviewCount('overview-course-count', firstPage.totalElementos);
            updateOverviewCount('overview-enrollment-count', clientEnrollments.length);
            if (coursePage > 0 && cursosDisponibles.length <= inicio) {
                coursePage = Math.max(0, Math.ceil(cursosDisponibles.length / size) - 1);
                return loadCourses();
            }
            const availableCourses = cursosDisponibles.slice(inicio, inicio + size);
            replaceGridCards(courseGrid, availableCourses.map((course) => catalogCard(course, 'course')));
            courseStatus.textContent = availableCourses.length
                ? 'Mostrando ' + availableCourses.length + (availableCourses.length === 1 ? ' curso disponible.' : ' cursos disponibles.')
                : firstPage.totalElementos
                    ? 'Ya estás inscrito en todos los cursos que coinciden con estos filtros.'
                    : 'No hay cursos activos que coincidan con los filtros seleccionados.';
            const previous = document.getElementById('course-previous-page');
            const next = document.getElementById('course-next-page');
            const indicator = document.getElementById('course-page-indicator');
            const hasPrevious = coursePage > 0;
            const hasNext = cursosDisponibles.length > inicio + size;
            coursePagination.hidden = !hasPrevious && !hasNext;
            previous.disabled = !hasPrevious;
            next.disabled = !hasNext;
            indicator.textContent = 'Página ' + (coursePage + 1);
        } catch (error) {
            if (thisLoad !== courseLoadNumber) return;
            setGridLoading(courseGrid, false);
            courseStatus.classList.add('is-error');
            courseStatus.textContent = error.message || 'No se pudo cargar el catálogo de cursos.';
        }
    }

    async function loadDiplomas() {
        const thisLoad = ++diplomaLoadNumber;
        diplomaStatus.textContent = 'Cargando diplomados disponibles...';
        diplomaStatus.classList.remove('is-error');
        setGridLoading(diplomaGrid, true);
        preparePagination(diplomaPagination);
        try {
            const data = await request(diplomaApi + '?' + query(diplomaPage));
            if (thisLoad !== diplomaLoadNumber) return;
            if (data.totalPaginas > 0 && diplomaPage >= data.totalPaginas) { diplomaPage = data.totalPaginas - 1; return loadDiplomas(); }
            if (!data.totalPaginas) diplomaPage = 0;
            updateOverviewCount('overview-diploma-count', data.totalElementos);
            replaceGridCards(diplomaGrid, data.contenido.map((diploma) => catalogCard(diploma, 'diploma')));
            diplomaStatus.textContent = data.totalElementos
                ? data.contenido.length + (data.contenido.length === 1 ? ' diplomado en esta página.' : ' diplomados en esta página.')
                : 'No hay diplomados activos que coincidan con los filtros seleccionados.';
            renderPagination(data,
                document.getElementById('diploma-previous-page'), document.getElementById('diploma-next-page'),
                document.getElementById('diploma-page-indicator'), diplomaPagination);
        } catch (error) {
            if (thisLoad !== diplomaLoadNumber) return;
            setGridLoading(diplomaGrid, false);
            diplomaStatus.classList.add('is-error');
            diplomaStatus.textContent = error.message || 'No se pudo cargar el catálogo de diplomados.';
        }
    }

    async function loadAll(scroll = false) {
        await Promise.all([loadCourses(), loadDiplomas()]);
        if (scroll) scrollToSection({courses: 'explorar', diplomas: 'diplomados', spaces: 'espacios'}[activeCatalogView]);
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
        setCatalogView('courses');
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
    document.querySelectorAll('[data-catalog-view]').forEach((button) => {
        button.addEventListener('click', () => setCatalogView(button.dataset.catalogView, true));
    });
    document.addEventListener('click', (event) => {
        const link = event.target.closest('a[href="#explorar"], a[href="#diplomados"], a[href="#espacios"]');
        if (!link) return;
        event.preventDefault();
        const view = {'#explorar': 'courses', '#diplomados': 'diplomas', '#espacios': 'spaces'}[link.getAttribute('href')];
        setCatalogView(view, true);
    });
    document.getElementById('course-previous-page').addEventListener('click', (event) => { if (!event.currentTarget.disabled) { coursePage = Math.max(0, coursePage - 1); scrollToSection('explorar'); loadCourses(); } });
    document.getElementById('course-next-page').addEventListener('click', (event) => { if (!event.currentTarget.disabled) { coursePage += 1; scrollToSection('explorar'); loadCourses(); } });
    document.getElementById('diploma-previous-page').addEventListener('click', (event) => { if (!event.currentTarget.disabled) { diplomaPage = Math.max(0, diplomaPage - 1); scrollToSection('diplomados'); loadDiplomas(); } });
    document.getElementById('diploma-next-page').addEventListener('click', (event) => { if (!event.currentTarget.disabled) { diplomaPage += 1; scrollToSection('diplomados'); loadDiplomas(); } });
    document.getElementById('space-previous-page').addEventListener('click', (event) => { if (!event.currentTarget.disabled) { spacePage = Math.max(0, spacePage - 1); scrollToSection('espacios'); loadSpaces(); } });
    document.getElementById('space-next-page').addEventListener('click', (event) => { if (!event.currentTarget.disabled) { spacePage += 1; scrollToSection('espacios'); loadSpaces(); } });
    document.getElementById('diploma-detail-close').addEventListener('click', () => diplomaDetailDialog.close());
    diplomaDetailDialog.addEventListener('close', restoreDetailFocus);
    document.getElementById('reservation-form').addEventListener('submit', submitReservation);
    document.getElementById('reservation-close').addEventListener('click', () => document.getElementById('space-reservation-dialog').close());
    document.getElementById('reservation-cancel').addEventListener('click', () => document.getElementById('space-reservation-dialog').close());
    document.getElementById('space-reservation-dialog').addEventListener('close', restoreDetailFocus);

    const initialQuery = new URLSearchParams(window.location.search).get('q');
    if (initialQuery?.trim()) searchInput.value = initialQuery.trim();
    const initialView = {'#diplomados': 'diplomas', '#espacios': 'spaces'}[window.location.hash] || 'courses';
    setCatalogView(initialView);
    loadCatalogs();
    loadAll(Boolean(initialQuery?.trim()));
    loadSpaces();
    loadMyRentals();
})();
