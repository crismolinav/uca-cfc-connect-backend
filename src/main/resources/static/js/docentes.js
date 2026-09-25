(function () {
    'use strict';

    const api = '/api/v1/docentes';
    const body = document.getElementById('teachers-body');
    const listStatus = document.getElementById('list-status');
    const actionStatus = document.getElementById('action-status');
    const pagination = document.getElementById('pagination');
    const formDialog = document.getElementById('teacher-dialog');
    const form = document.getElementById('teacher-form');
    const deleteDialog = document.getElementById('delete-dialog');
    const search = document.getElementById('teacher-search');
    const sort = document.getElementById('sort-filter');
    let page = 0;
    let size = 10;
    let editingId = null;
    let selectedTeacher = null;
    let dialogTrigger = null;
    let csrfToken;
    let loadNumber = 0;

    async function csrf() {
        if (csrfToken) return csrfToken;
        const response = await fetch('/api/v1/auth/csrf', {credentials: 'same-origin'});
        if (!response.ok) throw new Error('No se pudo iniciar una solicitud segura.');
        csrfToken = (await response.json()).token;
        return csrfToken;
    }

    async function logout() {
        try {
            await fetch('/api/v1/auth/logout', {
                method: 'POST', credentials: 'same-origin', headers: {'X-CSRF-TOKEN': await csrf()}
            });
        } finally {
            window.location.assign('/auth/login.html');
        }
    }

    function errorMessage(data) {
        if (data.message || data.mensaje) return data.message || data.mensaje;
        return Object.values(data.fieldErrors || data).find((value) => typeof value === 'string')
            || 'No se pudo completar la operación.';
    }

    async function request(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const headers = {...options.headers};
        if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) headers['X-CSRF-TOKEN'] = await csrf();
        const response = await fetch(url, {...options, credentials: 'same-origin', headers});
        if (response.status === 401) {
            await logout();
            throw new Error('Tu sesión venció.');
        }
        const data = response.status === 204 ? null : await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(errorMessage(data || {}));
        return data;
    }

    function cell(row, value, className) {
        const td = document.createElement('td');
        td.textContent = value || '—';
        if (className) td.className = className;
        row.append(td);
        return td;
    }

    function button(label, handler, ariaLabel, className = '') {
        const control = document.createElement('button');
        control.type = 'button';
        control.textContent = label;
        control.className = className;
        control.setAttribute('aria-label', ariaLabel);
        control.addEventListener('click', handler);
        return control;
    }

    function actions(teacher, extraClass = '') {
        const wrap = document.createElement('div');
        wrap.className = 'row-actions ' + extraClass;
        wrap.append(
            button('Editar', () => openForm(teacher), 'Editar ' + teacher.nombre),
            button('Eliminar', (event) => openDelete(teacher, event.currentTarget),
                'Eliminar ' + teacher.nombre, 'delete-row-button')
        );
        return wrap;
    }

    function row(teacher, number) {
        const tr = document.createElement('tr');
        cell(tr, String(number));
        const name = cell(tr, '', 'course-title-cell');
        const strong = document.createElement('strong');
        strong.textContent = teacher.nombre;
        const mobile = document.createElement('span');
        mobile.className = 'teacher-mobile-meta';
        mobile.textContent = teacher.especialidad + ' · ' + teacher.correo;
        name.append(strong, mobile, actions(teacher, 'mobile-actions'));
        cell(tr, teacher.especialidad);
        const email = document.createElement('a');
        email.href = 'mailto:' + teacher.correo;
        email.textContent = teacher.correo;
        const emailCell = document.createElement('td');
        emailCell.append(email);
        tr.append(emailCell);
        cell(tr, teacher.telefono);
        const actionCell = document.createElement('td');
        actionCell.append(actions(teacher, 'desktop-actions'));
        tr.append(actionCell);
        return tr;
    }

    function query() {
        const [field, direction] = sort.value.split(',');
        const params = new URLSearchParams({
            pagina: String(page), tamano: String(size), ordenarPor: field, direccion: direction
        });
        if (search.value.trim()) params.set('texto', search.value.trim());
        return params;
    }

    async function load() {
        const current = ++loadNumber;
        body.replaceChildren();
        listStatus.textContent = 'Cargando docentes...';
        pagination.hidden = true;
        try {
            const data = await request(api + '?' + query());
            if (current !== loadNumber) return;
            data.contenido.forEach((teacher, index) => body.append(row(teacher, page * size + index + 1)));
            listStatus.textContent = data.totalElementos
                ? data.totalElementos + (data.totalElementos === 1 ? ' docente registrado.' : ' docentes registrados.')
                : 'No hay docentes que coincidan con la búsqueda.';
            pagination.hidden = data.totalElementos === 0;
            document.getElementById('previous-page').disabled = data.primera;
            document.getElementById('next-page').disabled = data.ultima;
            document.getElementById('page-indicator').textContent =
                'Página ' + (data.paginaActual + 1) + ' de ' + Math.max(data.totalPaginas, 1);
            document.getElementById('total-count').textContent = data.totalElementos + ' docentes';
        } catch (error) {
            if (current === loadNumber) listStatus.textContent = error.message;
        }
    }

    function openForm(teacher = null) {
        form.reset();
        editingId = teacher?.idDocente ?? null;
        document.getElementById('dialog-error').textContent = '';
        ['nombre', 'especialidad', 'correo', 'telefono'].forEach((name) => {
            form.elements.namedItem(name).value = teacher?.[name] ?? '';
        });
        document.getElementById('dialog-title').textContent = teacher ? 'Editar docente' : 'Nuevo docente';
        document.getElementById('dialog-save').textContent = teacher ? 'Guardar cambios' : 'Guardar docente';
        formDialog.showModal();
        form.elements.namedItem('nombre').focus();
    }

    async function save(event) {
        event.preventDefault();
        const error = document.getElementById('dialog-error');
        error.textContent = '';
        if (!form.reportValidity()) return;
        const data = Object.fromEntries(new FormData(form));
        Object.keys(data).forEach((key) => { data[key] = data[key].trim(); });
        const saveButton = document.getElementById('dialog-save');
        saveButton.disabled = true;
        try {
            await request(editingId ? api + '/' + editingId : api, {
                method: editingId ? 'PUT' : 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(data)
            });
            formDialog.close();
            await load();
            actionStatus.textContent = 'El docente «' + data.nombre + '» fue guardado correctamente.';
        } catch (failure) {
            error.textContent = failure.message;
        } finally {
            saveButton.disabled = false;
        }
    }

    function openDelete(teacher, trigger) {
        selectedTeacher = teacher;
        dialogTrigger = trigger;
        document.getElementById('delete-dialog-description').textContent =
            '¿Eliminar definitivamente a «' + teacher.nombre + '»?';
        document.getElementById('delete-dialog-error').textContent = '';
        deleteDialog.showModal();
    }

    async function remove() {
        const confirm = document.getElementById('delete-confirm');
        confirm.disabled = true;
        try {
            await request(api + '/' + selectedTeacher.idDocente, {method: 'DELETE'});
            const name = selectedTeacher.nombre;
            deleteDialog.close();
            await load();
            actionStatus.textContent = 'El docente «' + name + '» fue eliminado.';
        } catch (error) {
            document.getElementById('delete-dialog-error').textContent = error.message;
        } finally {
            confirm.disabled = false;
        }
    }

    async function init() {
        try {
            const user = await request('/api/v1/auth/me');
            if (user.rol !== 'ADMIN') {
                window.location.replace('/cliente/index.html');
                return;
            }
            document.getElementById('admin-name').textContent = user.nombre;
            await load();
        } catch (error) {
            listStatus.textContent = error.message || 'No se pudo verificar el acceso.';
        }
    }

    document.getElementById('logout-button').addEventListener('click', logout);
    document.getElementById('new-teacher-button').addEventListener('click', () => openForm());
    document.getElementById('clear-filters').addEventListener('click', () => { search.value = ''; sort.value = 'nombre,asc'; page = 0; load(); });
    document.getElementById('dialog-close').addEventListener('click', () => formDialog.close());
    document.getElementById('dialog-cancel').addEventListener('click', () => formDialog.close());
    form.addEventListener('submit', save);
    document.getElementById('delete-cancel').addEventListener('click', () => deleteDialog.close());
    document.getElementById('delete-confirm').addEventListener('click', remove);
    document.getElementById('previous-page').addEventListener('click', () => { page -= 1; load(); });
    document.getElementById('next-page').addEventListener('click', () => { page += 1; load(); });
    document.getElementById('page-size').addEventListener('change', (event) => { size = Number(event.target.value); page = 0; load(); });
    sort.addEventListener('change', () => { page = 0; load(); });
    let searchTimer;
    search.addEventListener('input', () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => { page = 0; load(); }, 250);
    });
    deleteDialog.addEventListener('close', () => {
        if (dialogTrigger?.isConnected) dialogTrigger.focus();
        dialogTrigger = null;
        selectedTeacher = null;
    });

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
