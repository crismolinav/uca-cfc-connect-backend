(function () {
    'use strict';

    const api = '/api/v1/diplomados';
    const body = document.getElementById('diplomas-body');
    const listStatus = document.getElementById('list-status');
    const actionStatus = document.getElementById('action-status');
    const pagination = document.getElementById('pagination');
    const formDialog = document.getElementById('diploma-dialog');
    const form = document.getElementById('diploma-form');
    const sessionsDialog = document.getElementById('sessions-dialog');
    const sessionForm = document.getElementById('session-form');
    const statusDialog = document.getElementById('status-dialog');
    const deleteDialog = document.getElementById('delete-dialog');
    const search = document.getElementById('diploma-search');
    const categoryFilter = document.getElementById('category-filter');
    const modalityFilter = document.getElementById('modality-filter');
    const statusFilter = document.getElementById('status-filter');
    const sortFilter = document.getElementById('sort-filter');
    const teacherAssignments = window.crearGestorDocentes('diplomados');
    let page = 0;
    let size = 10;
    let editingId = null;
    let originalStartDate = null;
    let selectedDiploma = null;
    let editingSessionId = null;
    let sessionsCache = [];
    let dialogTrigger = null;
    let csrfToken;
    let loadNumber = 0;

    async function getCsrf() {
        if (csrfToken) return csrfToken;
        const response = await fetch('/api/v1/auth/csrf', {credentials: 'same-origin'});
        if (!response.ok) throw new Error('No se pudo iniciar una solicitud segura.');
        csrfToken = (await response.json()).token;
        return csrfToken;
    }

    async function logout() {
        try {
            const csrf = await getCsrf();
            await fetch('/api/v1/auth/logout', {
                method: 'POST', credentials: 'same-origin', headers: {'X-CSRF-TOKEN': csrf}
            });
        } finally {
            window.location.assign('/auth/login.html');
        }
    }

    function errorMessage(data) {
        if (data.message || data.mensaje) return data.message || data.mensaje;
        const first = Object.values(data.fieldErrors || data).find((value) => typeof value === 'string');
        return first || 'No se pudo completar la operación.';
    }

    async function request(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const headers = {...options.headers};
        if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) headers['X-CSRF-TOKEN'] = await getCsrf();
        const response = await fetch(url, {...options, credentials: 'same-origin', headers});
        if (response.status === 401) {
            await logout();
            throw new Error('Tu sesión venció.');
        }
        const data = response.status === 204 ? null : await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(errorMessage(data || {}));
        return data;
    }

    function localDate() {
        const now = new Date();
        return now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-'
            + String(now.getDate()).padStart(2, '0');
    }

    function formatDate(value) {
        return new Intl.DateTimeFormat('es-SV', {
            day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC'
        }).format(new Date(value + 'T00:00:00Z'));
    }

    function money(value) {
        return new Intl.NumberFormat('es-SV', {style: 'currency', currency: 'USD'}).format(value);
    }

    function cell(row, value, className) {
        const td = document.createElement('td');
        td.textContent = value ?? '—';
        if (className) td.className = className;
        row.append(td);
        return td;
    }

    function button(label, handler, ariaLabel, extraClass = '') {
        const control = document.createElement('button');
        control.type = 'button';
        control.className = 'row-button ' + extraClass;
        control.textContent = label;
        control.setAttribute('aria-label', ariaLabel);
        control.addEventListener('click', handler);
        return control;
    }

    function badge(diploma) {
        const span = document.createElement('span');
        span.className = 'state-pill ' + (diploma.activo ? 'state-active' : 'state-inactive');
        span.textContent = diploma.activo ? 'Publicado' : 'Borrador';
        return span;
    }

    function actions(diploma) {
        const wrap = document.createElement('div');
        wrap.className = 'row-actions';
        wrap.append(
            button('Sesiones', (event) => openSessions(diploma, event.currentTarget), 'Gestionar sesiones de ' + diploma.nombre),
            button('Docentes', (event) => teacherAssignments.open(diploma, event.currentTarget),
                'Asignar docentes a ' + diploma.nombre),
            button('Editar', () => openForm(diploma), 'Editar ' + diploma.nombre),
            button(diploma.activo ? 'Retirar' : 'Publicar', (event) => openStatus(diploma, event.currentTarget),
                (diploma.activo ? 'Retirar ' : 'Publicar ') + diploma.nombre),
            button('Eliminar', (event) => openDelete(diploma, event.currentTarget), 'Eliminar ' + diploma.nombre, 'delete-row-button')
        );
        return wrap;
    }

    function row(diploma, number) {
        const tr = document.createElement('tr');
        cell(tr, String(number));
        const title = cell(tr, '', 'course-title-cell');
        const strong = document.createElement('strong');
        strong.textContent = diploma.nombre;
        const description = document.createElement('span');
        description.className = 'course-description';
        description.textContent = diploma.descripcion;
        title.append(strong, description);
        cell(tr, diploma.categoria);
        cell(tr, diploma.modalidad);
        cell(tr, formatDate(diploma.fechaInicio) + ' - ' + formatDate(diploma.fechaFin), 'date-cell');
        cell(tr, diploma.duracionHoras + ' h');
        cell(tr, money(diploma.costo));
        const state = document.createElement('td');
        state.append(badge(diploma));
        tr.append(state);
        const actionCell = document.createElement('td');
        actionCell.append(actions(diploma));
        tr.append(actionCell);
        return tr;
    }

    function addOptions(select, items, placeholder) {
        select.replaceChildren(new Option(placeholder, ''));
        items.forEach((item) => select.append(new Option(item.nombre, String(item.id))));
    }

    async function loadCatalogs() {
        const [categories, modalities] = await Promise.all([
            request('/api/v1/cursos/catalogos/categorias'),
            request('/api/v1/cursos/catalogos/modalidades')
        ]);
        if (!categories.length || !modalities.length) {
            throw new Error('Primero debe registrar al menos una categoría y una modalidad.');
        }
        addOptions(categoryFilter, categories, 'Todas');
        addOptions(modalityFilter, modalities, 'Todas');
        addOptions(form.elements.namedItem('idCategoria'), categories, 'Selecciona una categoría');
        addOptions(form.elements.namedItem('idModalidad'), modalities, 'Selecciona una modalidad');
    }

    function query() {
        const [sort, direction] = sortFilter.value.split(',');
        const params = new URLSearchParams({
            pagina: String(page), tamano: String(size), ordenarPor: sort, direccion: direction
        });
        if (search.value.trim()) params.set('texto', search.value.trim());
        if (categoryFilter.value) params.set('idCategoria', categoryFilter.value);
        if (modalityFilter.value) params.set('idModalidad', modalityFilter.value);
        if (statusFilter.value) params.set('activo', statusFilter.value);
        return params;
    }

    async function load() {
        const thisLoad = ++loadNumber;
        listStatus.textContent = 'Cargando diplomados...';
        body.replaceChildren();
        pagination.hidden = true;
        try {
            const data = await request(api + '?' + query());
            if (thisLoad !== loadNumber) return;
            data.contenido.forEach((diploma, index) => body.append(row(diploma, page * size + index + 1)));
            listStatus.textContent = data.totalElementos
                ? data.totalElementos + (data.totalElementos === 1 ? ' diplomado encontrado.' : ' diplomados encontrados.')
                : 'No hay diplomados que coincidan con los filtros.';
            pagination.hidden = data.totalElementos === 0;
            document.getElementById('previous-page').disabled = data.primera;
            document.getElementById('next-page').disabled = data.ultima;
            document.getElementById('page-indicator').textContent =
                'Página ' + (data.paginaActual + 1) + ' de ' + Math.max(data.totalPaginas, 1);
            document.getElementById('total-count').textContent = data.totalElementos + ' diplomados';
        } catch (error) {
            if (thisLoad === loadNumber) listStatus.textContent = error.message;
        }
    }

    function updateEndMin() {
        form.elements.namedItem('fechaFin').min = form.elements.namedItem('fechaInicio').value || '';
    }

    function openForm(diploma = null) {
        form.reset();
        document.getElementById('dialog-error').textContent = '';
        editingId = diploma?.idDiplomado ?? null;
        originalStartDate = diploma?.fechaInicio ?? null;
        ['nombre', 'descripcion', 'idCategoria', 'idModalidad', 'fechaInicio', 'fechaFin', 'duracionHoras', 'costo']
            .forEach((name) => { form.elements.namedItem(name).value = diploma?.[name] ?? ''; });
        document.getElementById('dialog-title').textContent = diploma ? 'Editar diplomado' : 'Nuevo diplomado';
        document.getElementById('dialog-description').textContent = diploma
            ? 'Actualiza los datos generales del programa.'
            : 'Se guardará como borrador. Después podrás programar sus sesiones.';
        document.getElementById('dialog-save').textContent = diploma ? 'Guardar cambios' : 'Guardar diplomado';
        form.elements.namedItem('fechaInicio').min = diploma ? '' : localDate();
        updateEndMin();
        formDialog.showModal();
    }

    function validateForm() {
        const error = document.getElementById('dialog-error');
        if (!form.reportValidity()) return false;
        const start = form.elements.namedItem('fechaInicio');
        const end = form.elements.namedItem('fechaFin');
        if (start.value < localDate() && (editingId === null || start.value !== originalStartDate)) {
            error.textContent = 'La fecha de inicio no puede estar en el pasado.';
            start.focus();
            return false;
        }
        if (end.value < start.value) {
            error.textContent = 'La fecha de fin no puede ser anterior al inicio.';
            end.focus();
            return false;
        }
        const duration = Number(form.elements.namedItem('duracionHoras').value);
        if (!Number.isInteger(duration) || duration <= 0) {
            error.textContent = 'La duración debe ser un número entero mayor que cero.';
            return false;
        }
        const cost = form.elements.namedItem('costo').value;
        if (Number(cost) <= 0 || !/^\d+(?:\.\d{1,2})?$/.test(cost)) {
            error.textContent = 'El costo debe ser mayor que cero y tener máximo dos decimales.';
            return false;
        }
        return true;
    }

    async function saveDiploma(event) {
        event.preventDefault();
        document.getElementById('dialog-error').textContent = '';
        if (!validateForm()) return;
        const save = document.getElementById('dialog-save');
        const label = save.textContent;
        save.disabled = true;
        save.textContent = 'Guardando...';
        const data = {
            nombre: form.elements.namedItem('nombre').value.trim(),
            descripcion: form.elements.namedItem('descripcion').value.trim(),
            idCategoria: Number(form.elements.namedItem('idCategoria').value),
            idModalidad: Number(form.elements.namedItem('idModalidad').value),
            fechaInicio: form.elements.namedItem('fechaInicio').value,
            fechaFin: form.elements.namedItem('fechaFin').value,
            duracionHoras: Number(form.elements.namedItem('duracionHoras').value),
            costo: Number(form.elements.namedItem('costo').value)
        };
        try {
            await request(editingId ? api + '/' + editingId : api, {
                method: editingId ? 'PUT' : 'POST',
                headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data)
            });
            formDialog.close();
            await load();
            actionStatus.textContent = 'El diplomado «' + data.nombre + '» fue guardado correctamente.';
        } catch (error) {
            document.getElementById('dialog-error').textContent = error.message;
        } finally {
            save.disabled = false;
            save.textContent = label;
        }
    }

    function minutesFromTime(value) {
        const [hours, minutes] = value.split(':').map(Number);
        return hours * 60 + minutes;
    }

    function durationText(minutes) {
        const hours = Math.floor(minutes / 60);
        const rest = minutes % 60;
        return hours + (hours === 1 ? ' hora' : ' horas') + (rest ? ' y ' + rest + ' minutos' : '');
    }

    function sessionMinutes(session) {
        return minutesFromTime(session.horaFin) - minutesFromTime(session.horaInicio);
    }

    function sessionRow(session) {
        const tr = document.createElement('tr');
        cell(tr, formatDate(session.fecha));
        cell(tr, session.titulo);
        cell(tr, session.horaInicio.slice(0, 5) + ' - ' + session.horaFin.slice(0, 5));
        cell(tr, session.cupo ?? '—');
        const actionsCell = document.createElement('td');
        const wrap = document.createElement('div');
        wrap.className = 'row-actions';
        wrap.append(
            button('Editar', () => showSessionForm(session), 'Editar ' + session.titulo),
            button('Eliminar', () => deleteSession(session), 'Eliminar ' + session.titulo, 'delete-row-button')
        );
        actionsCell.append(wrap);
        tr.append(actionsCell);
        return tr;
    }

    async function openSessions(diploma, trigger) {
        selectedDiploma = diploma;
        dialogTrigger = trigger;
        sessionForm.hidden = true;
        document.getElementById('sessions-title').textContent = 'Sesiones de ' + diploma.nombre;
        document.getElementById('sessions-description').textContent =
            'Programa actividades sin superar ' + diploma.duracionHoras + ' horas.';
        sessionsDialog.showModal();
        await loadSessions();
    }

    async function loadSessions() {
        const diploma = selectedDiploma;
        if (!diploma) return;
        const sessionsBody = document.getElementById('sessions-body');
        const sessionsStatus = document.getElementById('sessions-status');
        sessionsStatus.textContent = 'Cargando sesiones...';
        sessionsBody.replaceChildren();
        try {
            const sessions = await request(api + '/' + diploma.idDiplomado + '/actividades');
            if (selectedDiploma?.idDiplomado !== diploma.idDiplomado) return;
            sessionsCache = sessions;
            sessions.forEach((session) => sessionsBody.append(sessionRow(session)));
            const programmed = sessions.reduce((total, session) => total + sessionMinutes(session), 0);
            const declared = diploma.duracionHoras * 60;
            document.getElementById('session-progress-text').textContent =
                durationText(programmed) + ' de ' + diploma.duracionHoras + ' horas';
            const progress = document.getElementById('session-progress');
            progress.max = declared;
            progress.value = Math.min(programmed, declared);
            sessionsStatus.textContent = sessions.length
                ? sessions.length + (sessions.length === 1 ? ' sesión programada.' : ' sesiones programadas.')
                : 'Aún no hay sesiones. Programa las actividades antes de publicar el diplomado.';
        } catch (error) {
            sessionsStatus.textContent = error.message;
        }
    }

    function showSessionForm(session = null) {
        editingSessionId = session?.idActividad ?? null;
        sessionForm.reset();
        document.getElementById('session-form-error').textContent = '';
        document.getElementById('session-form-title').textContent = session ? 'Editar sesión' : 'Nueva sesión';
        ['titulo', 'fecha', 'horaInicio', 'horaFin', 'cupo'].forEach((name) => {
            sessionForm.elements.namedItem(name).value = session?.[name] ?? '';
        });
        const date = sessionForm.elements.namedItem('fecha');
        date.min = selectedDiploma.fechaInicio > localDate() ? selectedDiploma.fechaInicio : localDate();
        if (session && session.fecha < date.min) date.min = session.fecha;
        date.max = selectedDiploma.fechaFin;
        sessionForm.hidden = false;
        sessionForm.elements.namedItem('titulo').focus();
    }

    async function saveSession(event) {
        event.preventDefault();
        const error = document.getElementById('session-form-error');
        error.textContent = '';
        if (!sessionForm.reportValidity()) return;
        const start = sessionForm.elements.namedItem('horaInicio');
        const end = sessionForm.elements.namedItem('horaFin');
        if (end.value <= start.value) {
            error.textContent = 'La hora de fin debe ser posterior a la hora de inicio.';
            end.focus();
            return;
        }
        const capacityControl = sessionForm.elements.namedItem('cupo');
        if (capacityControl.value && (!Number.isInteger(Number(capacityControl.value)) || Number(capacityControl.value) <= 0)) {
            error.textContent = 'El cupo debe ser un número entero mayor que cero.';
            capacityControl.focus();
            return;
        }
        const minutesWithoutEditing = sessionsCache
            .filter((session) => session.idActividad !== editingSessionId)
            .reduce((total, session) => total + sessionMinutes(session), 0);
        const newMinutes = minutesFromTime(end.value) - minutesFromTime(start.value);
        const declaredMinutes = selectedDiploma.duracionHoras * 60;
        if (minutesWithoutEditing + newMinutes > declaredMinutes) {
            error.textContent = 'La sesión supera la duración del diplomado. Quedan '
                + durationText(Math.max(declaredMinutes - minutesWithoutEditing, 0)) + ' por programar.';
            end.focus();
            return;
        }
        const save = document.getElementById('session-save');
        save.disabled = true;
        const data = {
            titulo: sessionForm.elements.namedItem('titulo').value.trim(),
            fecha: sessionForm.elements.namedItem('fecha').value,
            horaInicio: start.value,
            horaFin: end.value,
            cupo: capacityControl.value ? Number(capacityControl.value) : null
        };
        try {
            const base = api + '/' + selectedDiploma.idDiplomado + '/actividades';
            await request(editingSessionId ? base + '/' + editingSessionId : base, {
                method: editingSessionId ? 'PUT' : 'POST',
                headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data)
            });
            sessionForm.hidden = true;
            await loadSessions();
            await load();
        } catch (failure) {
            error.textContent = failure.message;
        } finally {
            save.disabled = false;
        }
    }

    async function deleteSession(session) {
        if (!window.confirm('¿Eliminar la sesión «' + session.titulo + '»?')) return;
        try {
            await request(api + '/' + selectedDiploma.idDiplomado + '/actividades/' + session.idActividad, {
                method: 'DELETE'
            });
            await loadSessions();
            await load();
        } catch (error) {
            document.getElementById('sessions-status').textContent = error.message;
        }
    }

    function openStatus(diploma, trigger) {
        selectedDiploma = diploma;
        dialogTrigger = trigger;
        const action = diploma.activo ? 'Retirar' : 'Publicar';
        document.getElementById('status-dialog-title').textContent = action + ' diplomado';
        document.getElementById('status-dialog-description').textContent = diploma.activo
            ? '¿Retirar «' + diploma.nombre + '» del catálogo de clientes?'
            : '¿Publicar «' + diploma.nombre + '»? Sus sesiones deben completar exactamente ' + diploma.duracionHoras + ' horas.';
        document.getElementById('status-dialog-error').textContent = '';
        document.getElementById('status-confirm').textContent = action;
        statusDialog.showModal();
    }

    async function changeStatus() {
        const confirm = document.getElementById('status-confirm');
        confirm.disabled = true;
        try {
            await request(api + '/' + selectedDiploma.idDiplomado + '/estado', {
                method: 'PATCH', headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({activo: !selectedDiploma.activo})
            });
            statusDialog.close();
            await load();
        } catch (error) {
            document.getElementById('status-dialog-error').textContent = error.message;
        } finally {
            confirm.disabled = false;
        }
    }

    function openDelete(diploma, trigger) {
        selectedDiploma = diploma;
        dialogTrigger = trigger;
        document.getElementById('delete-dialog-description').textContent =
            '¿Eliminar definitivamente «' + diploma.nombre + '»? Esta acción no se puede deshacer.';
        document.getElementById('delete-dialog-error').textContent = '';
        deleteDialog.showModal();
    }

    async function deleteDiploma() {
        const confirm = document.getElementById('delete-confirm');
        confirm.disabled = true;
        try {
            await request(api + '/' + selectedDiploma.idDiplomado, {method: 'DELETE'});
            deleteDialog.close();
            await load();
        } catch (error) {
            document.getElementById('delete-dialog-error').textContent = error.message;
        } finally {
            confirm.disabled = false;
        }
    }

    function clearAllFilters() {
        search.value = '';
        categoryFilter.value = '';
        modalityFilter.value = '';
        statusFilter.value = '';
        sortFilter.value = 'nombre,asc';
        page = 0;
        load();
    }

    async function init() {
        try {
            const user = await request('/api/v1/auth/me');
            if (user.rol !== 'ADMIN') {
                window.location.replace(user.rol === 'RECEPCIONISTA'
                    ? '/recepcion/index.html' : '/cliente/index.html');
                return;
            }
            document.getElementById('admin-name').textContent = user.nombre;
            await loadCatalogs();
            await load();
        } catch (error) {
            listStatus.textContent = error.message || 'No se pudo verificar el acceso.';
        }
    }

    document.getElementById('logout-button').addEventListener('click', logout);
    document.getElementById('new-diploma-button').addEventListener('click', () => openForm());
    document.getElementById('clear-filters').addEventListener('click', clearAllFilters);
    document.getElementById('dialog-close').addEventListener('click', () => formDialog.close());
    document.getElementById('dialog-cancel').addEventListener('click', () => formDialog.close());
    form.addEventListener('submit', saveDiploma);
    form.elements.namedItem('fechaInicio').addEventListener('change', updateEndMin);
    document.getElementById('sessions-close').addEventListener('click', () => sessionsDialog.close());
    document.getElementById('new-session-button').addEventListener('click', () => showSessionForm());
    document.getElementById('session-cancel').addEventListener('click', () => { sessionForm.hidden = true; });
    sessionForm.addEventListener('submit', saveSession);
    sessionForm.elements.namedItem('horaInicio').addEventListener('change', () => {
        sessionForm.elements.namedItem('horaFin').min = sessionForm.elements.namedItem('horaInicio').value;
    });
    document.getElementById('status-cancel').addEventListener('click', () => statusDialog.close());
    document.getElementById('status-confirm').addEventListener('click', changeStatus);
    document.getElementById('delete-cancel').addEventListener('click', () => deleteDialog.close());
    document.getElementById('delete-confirm').addEventListener('click', deleteDiploma);
    document.getElementById('previous-page').addEventListener('click', () => { page -= 1; load(); });
    document.getElementById('next-page').addEventListener('click', () => { page += 1; load(); });
    document.getElementById('page-size').addEventListener('change', (event) => {
        size = Number(event.target.value); page = 0; load();
    });
    [categoryFilter, modalityFilter, statusFilter, sortFilter].forEach((control) => {
        control.addEventListener('change', () => { page = 0; load(); });
    });
    let searchTimer;
    search.addEventListener('input', () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => { page = 0; load(); }, 250);
    });
    [statusDialog, deleteDialog, sessionsDialog].forEach((dialog) => dialog.addEventListener('close', () => {
        if (dialogTrigger?.isConnected) dialogTrigger.focus();
        dialogTrigger = null;
        selectedDiploma = null;
        if (dialog === sessionsDialog) {
            sessionForm.hidden = true;
            editingSessionId = null;
            sessionsCache = [];
        }
    }));

    const sidebar = document.getElementById('sidebar');
    const menu = document.getElementById('menu-button');
    const main = document.getElementById('main-content');
    function setMenu(open, restore = true) {
        sidebar.classList.toggle('is-open', open);
        main.inert = open;
        menu.setAttribute('aria-expanded', String(open));
        if (open) document.getElementById('sidebar-close').focus();
        else if (restore) menu.focus();
    }
    menu.addEventListener('click', () => setMenu(!sidebar.classList.contains('is-open')));
    document.getElementById('sidebar-close').addEventListener('click', () => setMenu(false));
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && sidebar.classList.contains('is-open')) setMenu(false);
    });

    init();
})();
