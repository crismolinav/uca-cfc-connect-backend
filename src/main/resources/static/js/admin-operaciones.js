(function () {
    'use strict';

    const api = '/api/v1/admin/operaciones';
    let csrfToken;

    async function csrf() {
        if (csrfToken) return csrfToken;
        const response = await fetch('/api/v1/auth/csrf', {credentials: 'same-origin'});
        if (!response.ok) throw new Error('No se pudo iniciar una solicitud segura.');
        csrfToken = (await response.json()).token;
        return csrfToken;
    }

    async function request(path, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const headers = {...options.headers};
        if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) headers['X-CSRF-TOKEN'] = await csrf();
        const response = await fetch(path, {...options, credentials: 'same-origin', headers});
        if (response.status === 401) {
            window.location.replace('/auth/login.html');
            throw new Error('La sesión terminó. Inicia sesión nuevamente.');
        }
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.mensaje || 'No se pudo completar la operación.');
        return data;
    }

    function estado(id, mensaje, error = false) {
        const element = document.getElementById(id);
        element.textContent = mensaje;
        element.classList.toggle('is-error', error);
    }

    function moneda(valor) {
        return new Intl.NumberFormat('es-SV', {style: 'currency', currency: 'USD'}).format(Number(valor || 0));
    }

    function linea(titulo, detalle, estadoTexto, accion) {
        const row = document.createElement('div');
        row.className = 'operations-row';
        const info = document.createElement('div');
        const heading = document.createElement('strong');
        heading.textContent = titulo;
        const description = document.createElement('small');
        description.textContent = detalle;
        info.append(heading, description);
        const actions = document.createElement('div');
        actions.className = 'operations-row-actions';
        if (estadoTexto) {
            const badge = document.createElement('span');
            badge.className = 'operations-badge';
            badge.textContent = estadoTexto;
            actions.append(badge);
        }
        if (accion) actions.append(accion);
        row.append(info, actions);
        return row;
    }

    function mostrarLista(id, datos, render, vacio) {
        const list = document.getElementById(id);
        list.replaceChildren();
        if (!datos.length) {
            const message = document.createElement('p');
            message.className = 'operations-empty';
            message.textContent = vacio;
            list.append(message);
            return;
        }
        list.append(...datos.map(render));
    }

    async function cargarEspacios() {
        const items = await request(api + '/espacios');
        mostrarLista('space-list', items, item => linea(item.nombre,
            item.tipo + ' · Capacidad ' + item.capacidad + ' · ' + moneda(item.precio) +
            (item.equipamiento ? ' · ' + item.equipamiento : ''),
            item.disponible ? 'Disponible' : 'Inactivo'), 'Todavía no hay espacios registrados.');
    }

    async function cargarServicios() {
        const items = await request(api + '/servicios-catering');
        mostrarLista('service-list', items, item => linea(item.nombre,
            item.tipoServicio + ' · ' + moneda(item.precioBase) +
            (item.descripcion ? ' · ' + item.descripcion : ''), item.activo ? 'Activo' : 'Inactivo'),
        'Todavía no hay servicios de catering.');
    }

    async function cargarCotizaciones() {
        const items = await request(api + '/cotizaciones');
        mostrarLista('quote-list', items, item => {
            let button;
            if (item.estado === 'PENDIENTE') {
                button = document.createElement('button');
                button.type = 'button';
                button.className = 'secondary-button';
                button.textContent = 'Aprobar';
                button.setAttribute('aria-label', 'Aprobar cotización ' + item.id + ' de ' + item.cliente);
                button.addEventListener('click', async () => {
                    if (!window.confirm('¿Aprobar la cotización #' + item.id + ' de ' + item.cliente + '?')) return;
                    button.disabled = true;
                    estado('quote-status', 'Aprobando cotización...');
                    try {
                        await request(api + '/cotizaciones/' + item.id + '/aprobar', {method: 'PATCH'});
                        await cargarCotizaciones();
                        estado('quote-status', 'Cotización #' + item.id + ' aprobada.');
                    } catch (error) {
                        estado('quote-status', error.message, true);
                        button.disabled = false;
                    }
                });
            }
            return linea('Cotización #' + item.id + ' · ' + item.cliente,
                item.fecha + ' · ' + moneda(item.montoEstimado) +
                (item.observaciones ? ' · ' + item.observaciones : ''), item.estado, button);
        }, 'Todavía no hay cotizaciones.');
    }

    async function cargarPagos() {
        const items = await request(api + '/pagos');
        mostrarLista('payment-list', items, item => linea('Pago #' + item.id + ' · ' + item.cliente,
            item.fecha + ' · ' + moneda(item.monto) + ' · ' + item.metodo +
            (item.referencia ? ' · Ref. ' + item.referencia : ''), item.estado),
        'Todavía no hay pagos registrados.');
        estado('payment-status', items.length + ' pagos registrados.');
    }

    function dato(label, value) {
        const item = document.createElement('div');
        item.className = 'report-item';
        const name = document.createElement('span');
        name.textContent = label;
        const number = document.createElement('strong');
        number.textContent = value;
        item.append(name, number);
        return item;
    }

    async function cargarReporte() {
        const form = document.getElementById('report-form');
        const desde = form.elements.namedItem('desde').value;
        const hasta = form.elements.namedItem('hasta').value;
        if (!form.reportValidity()) return;
        if (desde > hasta) {
            estado('report-status', 'La fecha inicial debe ser anterior o igual a la final.', true);
            form.elements.namedItem('hasta').focus();
            return;
        }
        const button = form.querySelector('button[type="submit"]');
        button.disabled = true;
        estado('report-status', 'Consultando reporte...');
        try {
            const params = new URLSearchParams({desde, hasta});
            const data = await request(api + '/reportes?' + params);
            document.getElementById('report-results').replaceChildren(
                dato('Reservas activas', String(data.reservas)),
                dato('Solicitudes de catering', String(data.solicitudesCatering)),
                dato('Cotizaciones', String(data.cotizaciones)),
                dato('Pagos registrados', String(data.pagos)),
                dato('Pagos confirmados', String(data.pagosConfirmados)),
                dato('Monto confirmado', moneda(data.montoConfirmado)),
                dato('Monto pendiente', moneda(data.montoPendiente)));
            estado('report-status', 'Resultados del ' + desde + ' al ' + hasta + '.');
        } catch (error) {
            document.getElementById('report-results').replaceChildren();
            estado('report-status', error.message, true);
        } finally {
            button.disabled = false;
        }
    }

    async function crear(event, section, path, fields, reload, message) {
        event.preventDefault();
        const form = event.currentTarget;
        if (!form.reportValidity()) return;
        const button = form.querySelector('button[type="submit"]');
        button.disabled = true;
        estado(section, 'Guardando...');
        try {
            const body = Object.fromEntries(fields.map(name => {
                const control = form.elements.namedItem(name);
                return [name, control.type === 'checkbox' ? control.checked : control.value.trim()];
            }));
            await request(api + path, {method: 'POST', headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(body)});
            form.reset();
            await reload();
            estado(section, message);
        } catch (error) {
            estado(section, error.message, true);
        } finally {
            button.disabled = false;
        }
    }

    const sidebar = document.getElementById('sidebar');
    const menu = document.getElementById('menu-button');
    const main = document.getElementById('main-content');
    function setMenu(open) {
        sidebar.classList.toggle('is-open', open);
        main.inert = open;
        menu.setAttribute('aria-expanded', String(open));
        menu.setAttribute('aria-label', open ? 'Cerrar menú' : 'Abrir menú');
        (open ? document.getElementById('sidebar-close') : menu).focus();
    }
    menu.addEventListener('click', () => setMenu(!sidebar.classList.contains('is-open')));
    document.getElementById('sidebar-close').addEventListener('click', () => setMenu(false));
    document.addEventListener('keydown', event => {
        if (!sidebar.classList.contains('is-open')) return;
        if (event.key === 'Escape') setMenu(false);
        if (event.key === 'Tab') {
            const focusables = [...sidebar.querySelectorAll('button, a[href]')];
            const first = focusables[0], last = focusables[focusables.length - 1];
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
            if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
        }
    });

    document.getElementById('space-form').addEventListener('submit', event => crear(event, 'space-status',
        '/espacios', ['nombre', 'tipo', 'capacidad', 'precio', 'equipamiento', 'disponible'],
        cargarEspacios, 'Espacio creado. Ya está disponible para recepción.'));
    document.getElementById('service-form').addEventListener('submit', event => crear(event, 'service-status',
        '/servicios-catering', ['nombre', 'tipoServicio', 'precioBase', 'descripcion'],
        cargarServicios, 'Servicio creado. Ya está disponible para recepción.'));
    document.getElementById('report-form').addEventListener('submit', event => { event.preventDefault(); cargarReporte(); });
    document.getElementById('logout-button').addEventListener('click', async () => {
        try { await request('/api/v1/auth/logout', {method: 'POST'}); }
        finally { window.location.assign('/'); }
    });

    (async () => {
        try {
            const user = await request('/api/v1/auth/me');
            if (user.rol !== 'ADMIN') {
                window.location.replace(user.rol === 'RECEPCIONISTA'
                    ? '/recepcion/index.html' : '/cliente/index.html');
                return;
            }
            document.getElementById('admin-name').textContent = user.nombre;
            const today = new Date();
            const localDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
            document.querySelector('#report-form [name="hasta"]').value = localDate;
            document.querySelector('#report-form [name="desde"]').value = localDate.slice(0, 8) + '01';
            await Promise.all([cargarEspacios(), cargarServicios(), cargarCotizaciones(), cargarPagos(), cargarReporte()]);
        } catch (error) {
            estado('page-status', error.message, true);
        }
    })();
})();
