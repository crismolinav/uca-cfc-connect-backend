(function () {
    'use strict';

    const api = '/api/v1/cursos';
    const body = document.getElementById('courses-body');
    const listStatus = document.getElementById('list-status');
    const actionStatus = document.getElementById('action-status');
    const pagination = document.getElementById('pagination');
    const formDialog = document.getElementById('course-dialog');
    const form = document.getElementById('course-form');
    const statusDialog = document.getElementById('status-dialog');
    const deleteDialog = document.getElementById('delete-dialog');
    const search = document.getElementById('course-search');
    const categoryFilter = document.getElementById('category-filter');
    const modalityFilter = document.getElementById('modality-filter');
    const statusFilter = document.getElementById('status-filter');
    const sortFilter = document.getElementById('sort-filter');
    const formFields = [
        'titulo', 'descripcion', 'idCategoria', 'idModalidad', 'fechaInicio',
        'fechaFin', 'duracionHoras', 'cupoMaximo', 'costo', 'horario'
    ];

    let page = 0;
    let size = 10;
    let editingId = null;
    let selectedCourse = null;
    let dialogTrigger = null;
    let csrfToken;
    let loadNumber = 0;

    async function obtenerCsrf() {
        if (csrfToken) return csrfToken;
        const response = await fetch('/api/v1/auth/csrf', {credentials: 'same-origin'});
        if (!response.ok) throw new Error('No se pudo iniciar una solicitud segura.');
        csrfToken = (await response.json()).token;
        return csrfToken;
    }

    async function salir() {
        try {
            const csrf = await obtenerCsrf();
            await fetch('/api/v1/auth/logout', {
                method: 'POST',
                credentials: 'same-origin',
                headers: {'X-CSRF-TOKEN': csrf}
            });
        } finally {
            localStorage.removeItem('uca_cfc_token');
            sessionStorage.removeItem('uca_cfc_token');
            window.location.assign('/auth/login.html');
        }
    }

    function mensajeError(data) {
        if (data.mensaje || data.message) return data.mensaje || data.message;
        const errores = data.fieldErrors || data;
        const primero = Object.values(errores).find((valor) => typeof valor === 'string');
        return primero || 'No se pudo completar la operación. Intenta nuevamente.';
    }

    async function request(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const headers = {...options.headers};
        if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
            headers['X-CSRF-TOKEN'] = await obtenerCsrf();
        }
        const response = await fetch(url, {...options, credentials: 'same-origin', headers});
        if (response.status === 401) {
            await salir();
            throw new Error('Tu sesión venció. Inicia sesión nuevamente.');
        }
        const data = response.status === 204 ? null : await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(mensajeError(data || {}));
        return data;
    }

    function celda(row, value, className) {
        const cell = document.createElement('td');
        cell.textContent = value ?? '—';
        if (className) cell.className = className;
        row.append(cell);
        return cell;
    }

    function moneda(value) {
        return new Intl.NumberFormat('es-SV', {style: 'currency', currency: 'USD'}).format(value);
    }

    function fecha(value) {
        if (!value) return '—';
        return new Intl.DateTimeFormat('es-SV', {day: '2-digit', month: 'short', year: 'numeric'})
            .format(new Date(value + 'T00:00:00'));
    }

    function estado(curso, extraClass = '') {
        const badge = document.createElement('span');
        badge.className = 'state-pill ' + extraClass + (curso.activo ? ' state-active' : ' state-inactive');
        badge.textContent = curso.activo ? 'Activo' : 'Inactivo';
        return badge;
    }

    function boton(label, action, ariaLabel, className = '') {
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = label;
        button.className = className;
        button.setAttribute('aria-label', ariaLabel);
        button.addEventListener('click', action);
        return button;
    }

    function acciones(curso, extraClass = '') {
        const wrap = document.createElement('span');
        wrap.className = 'row-actions ' + extraClass;
        wrap.append(
            boton('Ver', () => abrirFormulario('view', curso), 'Ver ' + curso.titulo),
            boton('Editar', () => abrirFormulario('edit', curso), 'Editar ' + curso.titulo),
            boton(curso.activo ? 'Inactivar' : 'Activar', (event) => abrirEstado(curso, event.currentTarget),
                (curso.activo ? 'Inactivar ' : 'Activar ') + curso.titulo),
            boton('Eliminar', (event) => abrirEliminar(curso, event.currentTarget), 'Eliminar ' + curso.titulo,
                'delete-row-button')
        );
        return wrap;
    }

    function fila(curso, numero) {
        const row = document.createElement('tr');
        celda(row, String(numero));

        const titleCell = document.createElement('td');
        titleCell.className = 'course-title-cell';
        const title = document.createElement('strong');
        title.textContent = curso.titulo;
        const description = document.createElement('span');
        description.textContent = curso.descripcion;
        description.className = 'course-description';
        const mobileMeta = document.createElement('span');
        mobileMeta.className = 'course-mobile-meta';
        mobileMeta.textContent = curso.categoria + ' · ' + curso.modalidad + ' · ' + moneda(curso.costo);
        titleCell.append(title, description, mobileMeta, estado(curso, 'mobile-state'), acciones(curso, 'mobile-actions'));
        row.append(titleCell);

        celda(row, curso.categoria);
        celda(row, curso.modalidad);
        celda(row, fecha(curso.fechaInicio) + ' - ' + fecha(curso.fechaFin), 'date-cell');
        celda(row, moneda(curso.costo));
        celda(row, String(curso.cupoMaximo));
        const stateCell = document.createElement('td');
        stateCell.append(estado(curso, 'desktop-state'));
        row.append(stateCell);
        const actionsCell = document.createElement('td');
        actionsCell.append(acciones(curso, 'desktop-actions'));
        row.append(actionsCell);
        return row;
    }

    function llenarFormulario(curso) {
        formFields.forEach((field) => {
            const control = form.elements.namedItem(field);
            const value = curso?.[field];
            control.value = value ?? '';
        });
    }

    function abrirFormulario(mode, curso = null) {
        form.reset();
        document.getElementById('dialog-error').textContent = '';
        llenarFormulario(curso);
        editingId = mode === 'new' ? null : curso.idCurso;
        const readOnly = mode === 'view';
        document.getElementById('dialog-title').textContent =
            mode === 'new' ? 'Nuevo curso' : mode === 'edit' ? 'Editar curso' : 'Detalle del curso';
        document.getElementById('dialog-description').textContent =
            mode === 'new' ? 'Completa la información de la oferta académica.' :
                mode === 'edit' ? 'Actualiza los datos académicos y comerciales.' :
                    'Información registrada en el sistema.';
        formFields.forEach((field) => {
            form.elements.namedItem(field).disabled = readOnly;
        });
        document.getElementById('dialog-save').hidden = readOnly;
        document.getElementById('dialog-cancel').textContent = readOnly ? 'Cerrar' : 'Cancelar';
        document.getElementById('dialog-save').textContent = mode === 'new' ? 'Guardar curso' : 'Guardar cambios';
        formDialog.showModal();
        (readOnly ? document.getElementById('dialog-cancel') : form.elements.namedItem('titulo')).focus();
    }

    function abrirEstado(curso, trigger) {
        selectedCourse = curso;
        dialogTrigger = trigger;
        const accion = curso.activo ? 'Inactivar' : 'Activar';
        document.getElementById('status-dialog-title').textContent = accion + ' curso';
        document.getElementById('status-dialog-description').textContent =
            '¿' + accion + ' «' + curso.titulo + '»? ' + (curso.activo
                ? 'Dejará de mostrarse como oferta disponible.'
                : 'Volverá a estar disponible en la oferta académica.');
        document.getElementById('status-confirm').textContent = accion + ' curso';
        document.getElementById('status-confirm').classList.toggle('danger-button', curso.activo);
        document.getElementById('status-dialog-error').textContent = '';
        statusDialog.showModal();
        document.getElementById('status-cancel').focus();
    }

    function abrirEliminar(curso, trigger) {
        selectedCourse = curso;
        dialogTrigger = trigger;
        document.getElementById('delete-dialog-description').textContent =
            '¿Eliminar definitivamente «' + curso.titulo + '»? Esta acción no se puede deshacer.';
        document.getElementById('delete-dialog-error').textContent = '';
        deleteDialog.showModal();
        document.getElementById('delete-cancel').focus();
    }

    async function cambiarEstado() {
        if (!selectedCourse) return;
        const curso = selectedCourse;
        const activo = !curso.activo;
        const confirm = document.getElementById('status-confirm');
        const label = confirm.textContent;
        confirm.disabled = true;
        confirm.textContent = 'Guardando...';
        try {
            await request(api + '/' + curso.idCurso + '/estado', {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({activo})
            });
            statusDialog.close();
            await cargar();
            actionStatus.textContent = '«' + curso.titulo + '» ahora está ' + (activo ? 'activo.' : 'inactivo.');
        } catch (error) {
            document.getElementById('status-dialog-error').textContent = error.message;
        } finally {
            confirm.disabled = false;
            confirm.textContent = label;
        }
    }

    async function eliminar() {
        if (!selectedCourse) return;
        const curso = selectedCourse;
        const confirm = document.getElementById('delete-confirm');
        confirm.disabled = true;
        confirm.textContent = 'Eliminando...';
        try {
            await request(api + '/' + curso.idCurso, {method: 'DELETE'});
            deleteDialog.close();
            await cargar();
            actionStatus.textContent = 'El curso «' + curso.titulo + '» fue eliminado.';
        } catch (error) {
            document.getElementById('delete-dialog-error').textContent = error.message;
        } finally {
            confirm.disabled = false;
            confirm.textContent = 'Eliminar curso';
        }
    }

    function agregarOpciones(select, items, placeholder) {
        select.replaceChildren();
        const first = document.createElement('option');
        first.value = '';
        first.textContent = placeholder;
        select.append(first);
        items.forEach((item) => {
            const option = document.createElement('option');
            option.value = String(item.id);
            option.textContent = item.nombre;
            select.append(option);
        });
    }

    async function cargarCatalogos() {
        const [categorias, modalidades] = await Promise.all([
            request(api + '/catalogos/categorias'),
            request(api + '/catalogos/modalidades')
        ]);
        agregarOpciones(categoryFilter, categorias, 'Todas');
        agregarOpciones(modalityFilter, modalidades, 'Todas');
        agregarOpciones(form.elements.namedItem('idCategoria'), categorias, 'Selecciona una categoría');
        agregarOpciones(form.elements.namedItem('idModalidad'), modalidades, 'Selecciona una modalidad');

        if (!categorias.length || !modalidades.length) {
            document.getElementById('new-course-button').disabled = true;
            actionStatus.textContent = 'Agrega al menos una categoría y una modalidad antes de crear cursos.';
        }
    }

    async function cargar() {
        const thisLoad = ++loadNumber;
        listStatus.hidden = false;
        listStatus.classList.remove('visually-hidden');
        listStatus.textContent = 'Cargando cursos...';
        body.replaceChildren();
        pagination.hidden = true;
        try {
            const [ordenarPor, direccion] = sortFilter.value.split(',');
            const params = new URLSearchParams({
                pagina: String(page), tamano: String(size), ordenarPor, direccion
            });
            if (search.value.trim()) params.set('texto', search.value.trim());
            if (categoryFilter.value) params.set('idCategoria', categoryFilter.value);
            if (modalityFilter.value) params.set('idModalidad', modalityFilter.value);
            if (statusFilter.value) params.set('activo', statusFilter.value);

            const data = await request(api + '?' + params);
            if (thisLoad !== loadNumber) return;
            if (data.contenido.length === 0 && page > 0 && data.totalElementos > 0) {
                page -= 1;
                await cargar();
                return;
            }
            data.contenido.forEach((curso, index) => body.append(fila(curso, page * size + index + 1)));
            listStatus.classList.toggle('visually-hidden', data.contenido.length > 0);
            listStatus.textContent = data.contenido.length
                ? data.totalElementos + (data.totalElementos === 1 ? ' curso encontrado.' : ' cursos encontrados.')
                : 'No hay cursos que coincidan con los filtros seleccionados.';
            pagination.hidden = data.totalElementos === 0;
            document.getElementById('previous-page').disabled = data.primera;
            document.getElementById('next-page').disabled = data.ultima;
            document.getElementById('page-indicator').textContent =
                'Página ' + (data.paginaActual + 1) + ' de ' + Math.max(data.totalPaginas, 1);
            document.getElementById('total-count').textContent = data.totalElementos + ' cursos';
        } catch (error) {
            if (thisLoad !== loadNumber) return;
            listStatus.textContent = error.message || 'No se pudo cargar la lista de cursos.';
        }
    }

    async function guardar(event) {
        event.preventDefault();
        const error = document.getElementById('dialog-error');
        error.textContent = '';
        if (!form.reportValidity()) return;
        if (form.elements.namedItem('fechaFin').value < form.elements.namedItem('fechaInicio').value) {
            error.textContent = 'La fecha de fin no puede ser anterior a la fecha de inicio.';
            form.elements.namedItem('fechaFin').focus();
            return;
        }

        const save = document.getElementById('dialog-save');
        const label = save.textContent;
        save.disabled = true;
        save.textContent = 'Guardando...';
        try {
            const data = {
                titulo: form.elements.namedItem('titulo').value.trim(),
                descripcion: form.elements.namedItem('descripcion').value.trim(),
                idCategoria: Number(form.elements.namedItem('idCategoria').value),
                idModalidad: Number(form.elements.namedItem('idModalidad').value),
                fechaInicio: form.elements.namedItem('fechaInicio').value,
                fechaFin: form.elements.namedItem('fechaFin').value,
                duracionHoras: Number(form.elements.namedItem('duracionHoras').value),
                cupoMaximo: Number(form.elements.namedItem('cupoMaximo').value),
                costo: Number(form.elements.namedItem('costo').value),
                horario: form.elements.namedItem('horario').value.trim()
            };
            await request(editingId ? api + '/' + editingId : api, {
                method: editingId ? 'PUT' : 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(data)
            });
            const action = editingId ? 'actualizado' : 'creado';
            formDialog.close();
            await cargar();
            actionStatus.textContent = 'El curso «' + data.titulo + '» fue ' + action + ' correctamente.';
        } catch (failure) {
            error.textContent = failure.message || 'No se pudo guardar el curso.';
        } finally {
            save.disabled = false;
            save.textContent = label;
        }
    }

    function limpiarFiltros() {
        search.value = '';
        categoryFilter.value = '';
        modalityFilter.value = '';
        statusFilter.value = '';
        sortFilter.value = 'titulo,asc';
        page = 0;
        cargar();
    }

    async function iniciar() {
        try {
            const user = await request('/api/v1/auth/me');
            if (user.rol !== 'ADMIN') {
                listStatus.textContent = 'Esta sección es exclusiva para administradores.';
                document.getElementById('new-course-button').hidden = true;
                return;
            }
            document.getElementById('admin-name').textContent = user.nombre;
            await cargarCatalogos();
            await cargar();
        } catch (error) {
            listStatus.textContent = error.message || 'No se pudo verificar el acceso.';
        }
    }

    document.getElementById('logout-button').addEventListener('click', salir);
    document.getElementById('new-course-button').addEventListener('click', () => abrirFormulario('new'));
    document.getElementById('clear-filters').addEventListener('click', limpiarFiltros);
    document.getElementById('dialog-close').addEventListener('click', () => formDialog.close());
    document.getElementById('dialog-cancel').addEventListener('click', () => formDialog.close());
    document.getElementById('status-cancel').addEventListener('click', () => statusDialog.close());
    document.getElementById('status-confirm').addEventListener('click', cambiarEstado);
    document.getElementById('delete-cancel').addEventListener('click', () => deleteDialog.close());
    document.getElementById('delete-confirm').addEventListener('click', eliminar);
    document.getElementById('previous-page').addEventListener('click', () => { page -= 1; cargar(); });
    document.getElementById('next-page').addEventListener('click', () => { page += 1; cargar(); });
    document.getElementById('page-size').addEventListener('change', (event) => {
        size = Number(event.target.value);
        page = 0;
        cargar();
    });
    [categoryFilter, modalityFilter, statusFilter, sortFilter].forEach((control) => {
        control.addEventListener('change', () => { page = 0; cargar(); });
    });
    let searchTimer;
    search.addEventListener('input', () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => { page = 0; cargar(); }, 250);
    });
    form.addEventListener('submit', guardar);
    [statusDialog, deleteDialog].forEach((modal) => modal.addEventListener('close', () => {
        if (dialogTrigger?.isConnected) dialogTrigger.focus();
        selectedCourse = null;
        dialogTrigger = null;
    }));

    const sidebar = document.getElementById('sidebar');
    const menu = document.getElementById('menu-button');
    const main = document.getElementById('main-content');
    function setMenu(open, restoreFocus = true) {
        sidebar.classList.toggle('is-open', open);
        main.inert = open;
        menu.setAttribute('aria-expanded', String(open));
        menu.setAttribute('aria-label', open ? 'Cerrar menú' : 'Abrir menú');
        if (open) document.getElementById('sidebar-close').focus();
        else if (restoreFocus) menu.focus();
    }
    menu.addEventListener('click', () => setMenu(!sidebar.classList.contains('is-open')));
    document.getElementById('sidebar-close').addEventListener('click', () => setMenu(false));
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && sidebar.classList.contains('is-open')) setMenu(false);
    });
    window.matchMedia('(max-width: 950px)').addEventListener('change', (event) => {
        if (!event.matches && sidebar.classList.contains('is-open')) setMenu(false, false);
    });

    localStorage.removeItem('uca_cfc_token');
    sessionStorage.removeItem('uca_cfc_token');
    iniciar();
})();
