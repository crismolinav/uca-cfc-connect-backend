(function () {
    'use strict';

    const api = '/api/v1/admin/clientes';
    const body = document.getElementById('clients-body');
    const status = document.getElementById('list-status');
    const pagination = document.getElementById('pagination');
    const dialog = document.getElementById('client-dialog');
    const form = document.getElementById('client-form');
    const statusDialog = document.getElementById('status-dialog');
    const statusMessage = document.getElementById('action-status');
    const search = document.getElementById('client-search');
    let page = 0;
    let size = 10;
    let editingId = null;
    let loadNumber = 0;
    let statusClient = null;
    let statusTrigger = null;
    let csrfToken;

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
                method: 'POST', credentials: 'same-origin', headers: {'X-CSRF-TOKEN': csrf}
            });
        } finally {
            localStorage.removeItem('uca_cfc_token');
            sessionStorage.removeItem('uca_cfc_token');
            window.location.assign('/');
        }
    }

    async function request(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const headers = {...options.headers};
        if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) headers['X-CSRF-TOKEN'] = await obtenerCsrf();
        const response = await fetch(url, {
            ...options,
            credentials: 'same-origin',
            headers
        });
        if (response.status === 401) {
            salir();
            throw new Error('Sesión vencida');
        }
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.mensaje || 'No se pudo completar la operación. Intenta nuevamente.');
        }
        return data;
    }

    function celda(row, value, className) {
        const cell = document.createElement('td');
        cell.textContent = value || '—';
        if (className) cell.className = className;
        row.append(cell);
        return cell;
    }

    function abrirDialogo(mode, cliente) {
        form.reset();
        document.getElementById('dialog-error').textContent = '';
        const campos = ['nombre', 'duiNit', 'empresa', 'correo', 'telefono', 'direccion'];
        campos.forEach((campo) => {
            form.elements.namedItem(campo).value = cliente?.[campo] || '';
        });
        editingId = mode === 'new' ? null : cliente.id;
        const soloLectura = mode === 'view';
        document.getElementById('dialog-title').textContent =
            mode === 'new' ? 'Nuevo cliente' : mode === 'edit' ? 'Editar cliente' : 'Detalle del cliente';
        document.getElementById('dialog-description').textContent =
            mode === 'new' ? 'Completa los datos para registrar un cliente.' :
                mode === 'edit' ? 'Actualiza los datos del cliente.' : 'Datos registrados en el sistema.';
        campos.forEach((campo) => {
            form.elements.namedItem(campo).disabled = soloLectura ||
                (mode === 'edit' && cliente.tieneCuenta && (campo === 'nombre' || campo === 'correo'));
        });
        const note = document.getElementById('dialog-note');
        note.hidden = !(mode === 'edit' && cliente.tieneCuenta);
        note.textContent = 'La cuenta de este cliente está vinculada. El nombre y el correo se gestionan desde usuarios.';
        document.getElementById('dialog-save').hidden = soloLectura;
        document.getElementById('dialog-cancel').textContent = soloLectura ? 'Cerrar' : 'Cancelar';
        document.getElementById('dialog-save').textContent =
            mode === 'new' ? 'Guardar cliente' : 'Guardar cambios';
        dialog.showModal();
        if (!soloLectura) form.elements.namedItem(mode === 'edit' && cliente.tieneCuenta ? 'duiNit' : 'nombre').focus();
    }

    function fila(cliente, numero) {
        const row = document.createElement('tr');
        celda(row, String(numero));
        const nameCell = celda(row, cliente.nombre, 'client-name-cell');
        nameCell.append(estado(cliente, 'mobile-state'));
        nameCell.append(acciones(cliente, 'mobile-actions'));
        celda(row, cliente.duiNit, !cliente.duiNit ? 'cell-muted' : '');
        celda(row, cliente.empresa, !cliente.empresa ? 'cell-muted' : '');
        celda(row, cliente.correo, !cliente.correo ? 'cell-muted' : '');
        celda(row, cliente.telefono, !cliente.telefono ? 'cell-muted' : '');
        const stateCell = document.createElement('td');
        stateCell.append(estado(cliente, 'desktop-state'));
        row.append(stateCell);
        const actions = document.createElement('td');
        actions.append(acciones(cliente, 'desktop-actions'));
        row.append(actions);
        return row;
    }

    function estado(cliente, extraClass) {
        const badge = document.createElement('span');
        badge.className = 'state-pill ' + extraClass + (cliente.tieneCuenta
            ? cliente.activo ? ' state-active' : ' state-inactive' : ' state-unlinked');
        badge.textContent = cliente.tieneCuenta ? cliente.activo ? 'Activo' : 'Inactivo' : 'Sin cuenta';
        return badge;
    }

    function abrirEstado(cliente, trigger) {
        statusClient = cliente;
        statusTrigger = trigger;
        const accion = cliente.activo ? 'Desactivar' : 'Activar';
        document.getElementById('status-dialog-title').textContent = accion + ' acceso';
        document.getElementById('status-dialog-description').textContent = cliente.activo
            ? '¿Desactivar el acceso de ' + cliente.nombre + '? No podrá iniciar sesión hasta que vuelvas a activarlo.'
            : '¿Activar el acceso de ' + cliente.nombre + '? Podrá volver a iniciar sesión.';
        document.getElementById('status-confirm').textContent = accion + ' acceso';
        document.getElementById('status-confirm').classList.toggle('danger-button', cliente.activo);
        document.getElementById('status-dialog-error').textContent = '';
        statusDialog.showModal();
        document.getElementById('status-cancel').focus();
    }

    function acciones(cliente, extraClass) {
        const wrap = document.createElement('span');
        wrap.className = 'row-actions ' + extraClass;
        for (const [label, mode] of [['Ver', 'view'], ['Editar', 'edit']]) {
            const button = document.createElement('button');
            button.type = 'button';
            button.textContent = label;
            button.setAttribute('aria-label', label + ' a ' + cliente.nombre);
            button.addEventListener('click', () => abrirDialogo(mode, cliente));
            wrap.append(button);
        }
        if (cliente.tieneCuenta) {
            const button = document.createElement('button');
            button.type = 'button';
            button.dataset.statusId = String(cliente.id);
            button.textContent = cliente.activo ? 'Desactivar' : 'Activar';
            button.setAttribute('aria-label', button.textContent + ' acceso de ' + cliente.nombre);
            button.addEventListener('click', () => abrirEstado(cliente, button));
            wrap.append(button);
        }
        return wrap;
    }

    async function cambiarEstado() {
        if (!statusClient) return;
        const cliente = statusClient;
        const activo = !cliente.activo;
        const button = document.getElementById('status-confirm');
        button.disabled = true;
        const label = button.textContent;
        button.textContent = 'Guardando...';
        document.getElementById('status-dialog-error').textContent = '';
        try {
            await request(api + '/' + cliente.id + '/estado', {
                method: 'PATCH',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({activo})
            });
            statusDialog.close();
            await cargar();
            statusMessage.textContent = 'El acceso de ' + cliente.nombre + ' ahora está ' + (activo ? 'activo.' : 'inactivo.');
            const matching = [...body.querySelectorAll('[data-status-id="' + cliente.id + '"]')];
            matching.find((item) => item.getClientRects().length)?.focus();
        } catch (error) {
            document.getElementById('status-dialog-error').textContent =
                error.message || 'No se pudo cambiar el estado. Intenta nuevamente.';
        } finally {
            button.disabled = false;
            button.textContent = label;
        }
    }

    async function cargar() {
        const thisLoad = ++loadNumber;
        status.hidden = false;
        status.classList.remove('visually-hidden');
        status.textContent = 'Cargando clientes...';
        body.replaceChildren();
        pagination.hidden = true;
        try {
            const params = new URLSearchParams({q: search.value.trim(), page: String(page), size: String(size)});
            const data = await request(api + '?' + params);
            if (thisLoad !== loadNumber) return;
            if (data.content.length === 0 && page > 0 && data.totalElements > 0) {
                page -= 1;
                await cargar();
                return;
            }
            data.content.forEach((cliente, index) => body.append(fila(cliente, page * size + index + 1)));
            status.classList.toggle('visually-hidden', data.content.length > 0);
            status.textContent = data.content.length > 0
                ? data.totalElements + ' clientes encontrados.'
                : search.value.trim()
                    ? 'No encontramos clientes con esa búsqueda. Prueba con otro término.'
                    : 'Aún no hay clientes registrados. Usa «Nuevo cliente» para agregar uno.';
            pagination.hidden = data.totalElements === 0;
            document.getElementById('previous-page').disabled = page === 0;
            document.getElementById('next-page').disabled = page + 1 >= data.totalPages;
            document.getElementById('page-indicator').textContent =
                'Página ' + (page + 1) + ' de ' + data.totalPages;
            document.getElementById('total-count').textContent = data.totalElements + ' clientes';
        } catch (error) {
            if (thisLoad !== loadNumber) return;
            status.textContent = error.message || 'No se pudo cargar la lista. Actualiza la página.';
        }
    }

    async function guardar(event) {
        event.preventDefault();
        const error = document.getElementById('dialog-error');
        error.textContent = '';
        if (!form.reportValidity()) return;
        const save = document.getElementById('dialog-save');
        save.disabled = true;
        const oldText = save.textContent;
        save.textContent = 'Guardando...';
        try {
            const fields = ['nombre', 'duiNit', 'empresa', 'correo', 'telefono', 'direccion'];
            const data = Object.fromEntries(fields.map((field) => [field, form.elements.namedItem(field).value.trim()]));
            await request(editingId ? api + '/' + editingId : api, {
                method: editingId ? 'PUT' : 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(data)
            });
            dialog.close();
            await cargar();
        } catch (failure) {
            error.textContent = failure.message || 'No se pudo guardar el cliente.';
        } finally {
            save.disabled = false;
            save.textContent = oldText;
        }
    }

    async function iniciar() {
        try {
            const user = await request('/api/v1/auth/me');
            if (user.rol === 'CLIENTE') {
                window.location.replace('/cliente/index.html');
                return;
            }
            if (user.rol !== 'ADMIN') {
                status.textContent = 'Esta sección es solo para administradores. Tu cuenta no tiene acceso.';
                document.getElementById('new-client-button').hidden = true;
                return;
            }
            document.getElementById('admin-name').textContent = user.nombre;
            await cargar();
        } catch (error) {
            status.textContent = error.message || 'No se pudo verificar el acceso.';
        }
    }

    document.getElementById('logout-button').addEventListener('click', salir);
    document.getElementById('new-client-button').addEventListener('click', () => abrirDialogo('new'));
    document.getElementById('dialog-close').addEventListener('click', () => dialog.close());
    document.getElementById('dialog-cancel').addEventListener('click', () => dialog.close());
    document.getElementById('previous-page').addEventListener('click', () => { page -= 1; cargar(); });
    document.getElementById('next-page').addEventListener('click', () => { page += 1; cargar(); });
    document.getElementById('page-size').addEventListener('change', (event) => {
        size = Number(event.target.value);
        page = 0;
        cargar();
    });
    let searchTimer;
    search.addEventListener('input', () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => { page = 0; cargar(); }, 250);
    });
    form.addEventListener('submit', guardar);
    document.getElementById('status-confirm').addEventListener('click', cambiarEstado);
    document.getElementById('status-cancel').addEventListener('click', () => statusDialog.close());
    statusDialog.addEventListener('close', () => {
        if (statusTrigger?.isConnected) statusTrigger.focus();
        statusClient = null;
        statusTrigger = null;
    });
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
        if (!sidebar.classList.contains('is-open')) return;
        if (event.key === 'Escape') setMenu(false);
        if (event.key === 'Tab') {
            const focusables = [...sidebar.querySelectorAll('button, a[href]')];
            const first = focusables[0];
            const last = focusables[focusables.length - 1];
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
        }
    });
    window.matchMedia('(max-width: 950px)').addEventListener('change', (event) => {
        if (!event.matches && sidebar.classList.contains('is-open')) {
            setMenu(false, false);
            document.querySelector('.account-link').focus();
        }
    });
    localStorage.removeItem('uca_cfc_token');
    sessionStorage.removeItem('uca_cfc_token');
    iniciar();
})();
