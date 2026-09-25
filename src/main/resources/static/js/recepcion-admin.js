(function () {
    'use strict';
    const endpoint = '/api/v1/admin/recepcionistas';
    const form = document.getElementById('staff-form');
    const list = document.getElementById('staff-list');
    const formStatus = document.getElementById('form-status');
    const listStatus = document.getElementById('list-status');
    let csrfToken;

    async function csrf() {
        if (csrfToken) return csrfToken;
        const response = await fetch('/api/v1/auth/csrf', {credentials: 'same-origin'});
        if (!response.ok) throw new Error('No se pudo iniciar una solicitud segura.');
        csrfToken = (await response.json()).token;
        return csrfToken;
    }

    async function request(url, options = {}) {
        const method = options.method || 'GET';
        const headers = {...options.headers};
        if (method !== 'GET') headers['X-CSRF-TOKEN'] = await csrf();
        const response = await fetch(url, {...options, credentials: 'same-origin', headers});
        if (response.status === 401) {
            window.location.replace('/auth/login.html');
            throw new Error('La sesión terminó. Inicia sesión nuevamente.');
        }
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.mensaje || Object.values(data)[0] || 'No se pudo guardar.');
        return data;
    }

    function mostrarEstado(elemento, mensaje, error = false) {
        elemento.textContent = mensaje;
        elemento.classList.toggle('is-error', error);
    }

    function fila(usuario) {
        const row = document.createElement('div');
        row.className = 'staff-row';
        const info = document.createElement('div');
        const name = document.createElement('strong');
        name.textContent = usuario.nombre;
        const email = document.createElement('small');
        email.textContent = usuario.correo;
        info.append(name, email);
        const actions = document.createElement('div');
        actions.className = 'staff-actions';
        const state = document.createElement('span');
        state.className = 'state-pill ' + (usuario.activo ? 'state-active' : 'state-inactive');
        state.textContent = usuario.activo ? 'Activo' : 'Inactivo';
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = usuario.activo ? 'Desactivar' : 'Activar';
        button.setAttribute('aria-label', button.textContent + ' cuenta de ' + usuario.nombre);
        button.addEventListener('click', async () => {
            if (!window.confirm((usuario.activo ? '¿Desactivar' : '¿Activar') + ' la cuenta de ' + usuario.nombre + '?')) return;
            button.disabled = true;
            try {
                await request(endpoint + '/' + usuario.id + '/estado', {
                    method: 'PATCH', headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({activo: !usuario.activo})
                });
                await cargar();
                mostrarEstado(listStatus, 'Acceso actualizado.');
            } catch (error) {
                mostrarEstado(listStatus, error.message, true);
                button.disabled = false;
            }
        });
        actions.append(state, button);
        row.append(info, actions);
        return row;
    }

    async function cargar() {
        const users = await request(endpoint);
        list.replaceChildren(...users.map(fila));
        mostrarEstado(listStatus, users.length ? users.length + ' cuentas registradas.' : 'Todavía no hay recepcionistas.');
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!form.reportValidity()) return;
        const button = form.querySelector('button[type="submit"]');
        button.disabled = true;
        mostrarEstado(formStatus, 'Creando cuenta...');
        try {
            await request(endpoint, {
                method: 'POST', headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    nombre: form.elements.namedItem('nombre').value.trim(),
                    correo: form.elements.namedItem('correo').value.trim(),
                    password: form.elements.namedItem('password').value
                })
            });
            form.reset();
            mostrarEstado(formStatus, 'Cuenta creada. Entrega el correo y la contraseña inicial a la recepcionista.');
            await cargar();
        } catch (error) {
            mostrarEstado(formStatus, error.message, true);
        } finally {
            button.disabled = false;
        }
    });

    document.getElementById('logout-button').addEventListener('click', async () => {
        try {
            await request('/api/v1/auth/logout', {method: 'POST'});
        } finally {
            window.location.assign('/');
        }
    });

    const menu = document.getElementById('menu-button');
    const sidebar = document.getElementById('sidebar');
    function setMenu(open) {
        sidebar.classList.toggle('is-open', open);
        menu.setAttribute('aria-expanded', String(open));
    }
    menu.addEventListener('click', () => setMenu(!sidebar.classList.contains('is-open')));
    document.getElementById('sidebar-close').addEventListener('click', () => setMenu(false));

    (async () => {
        try {
            const user = await request('/api/v1/auth/me');
            if (user.rol !== 'ADMIN') {
                window.location.replace('/cuenta/perfil.html');
                return;
            }
            document.getElementById('admin-name').textContent = user.nombre;
            await cargar();
        } catch (error) {
            mostrarEstado(listStatus, error.message, true);
        }
    })();
})();
