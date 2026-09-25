(function () {
    'use strict';
    const api = '/api/v1/recepcion';
    const flash = document.getElementById('flash');
    const state = {catalogos: null, espacios: [], alquileres: [], allAlquileres: [], catering: [],
        cotizaciones: [], inscripciones: [], actividades: [], pagos: []};
    const titles = {
        agenda: ['Agenda y alquileres', 'Consulta espacios y asigna reservas sin cruces de horario.'],
        clientes: ['Clientes', 'Registra personas y empresas para los servicios del centro.'],
        participantes: ['Participantes', 'Prepara las inscripciones de cada cliente.'],
        inscripciones: ['Inscripciones', 'Asocia participantes con la oferta formativa activa.'],
        catering: ['Catering', 'Organiza solicitudes de alimentación.'],
        cotizaciones: ['Cotizaciones', 'Registra estimaciones a partir del catálogo.'],
        actividades: ['Actividades', 'Programa tareas vinculadas con los servicios.'],
        pagos: ['Pagos', 'Registra y confirma los pagos recibidos.']
    };
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
        if (!response.ok) {
            const fieldMessage = Object.entries(data).find(([key, value]) =>
                !['timestamp', 'status', 'error', 'ruta', 'mensaje'].includes(key) && typeof value === 'string');
            throw new Error(data.mensaje || fieldMessage?.[1] || 'No se pudo completar la operación.');
        }
        return data;
    }

    function aviso(message, error = false) {
        flash.textContent = message;
        flash.classList.toggle('is-error', error);
    }

    function texto(tag, value, className) {
        const element = document.createElement(tag);
        element.textContent = value == null || value === '' ? '—' : String(value);
        if (className) element.className = className;
        return element;
    }

    function fila(principal, detalle, estado, accion) {
        const row = document.createElement('div');
        row.className = 'list-row';
        const info = document.createElement('div');
        info.append(texto('strong', principal), texto('small', detalle));
        row.append(info);
        if (estado || accion) {
            const end = document.createElement('div');
            end.className = 'row-end';
            if (estado) end.append(texto('span', estado, 'status-pill' +
                (['CANCELADO', 'INACTIVO', 'PENDIENTE'].includes(estado) ? ' is-muted' : '')));
            if (accion) end.append(accion);
            row.append(end);
        }
        return row;
    }

    function mostrarLista(id, registros, render, emptyText) {
        const target = document.getElementById(id);
        target.replaceChildren(...(registros.length ? registros.map(render) : [texto('p', emptyText, 'list-empty')]));
    }

    function opciones(select, items, label) {
        const previous = select.value;
        select.replaceChildren(new Option('Seleccionar...', ''));
        items.forEach((item) => select.add(new Option(label(item), String(item.id))));
        if (items.some((item) => String(item.id) === previous)) select.value = previous;
    }

    function completarOpciones() {
        if (!state.catalogos) return;
        document.querySelectorAll('select[data-options]').forEach((select) => {
            const key = select.dataset.options;
            let items = key === 'espacios' ? state.espacios.filter((e) => e.disponible) : state.catalogos[key];
            if (key === 'servicios') items = items.filter((s) => s.activo);
            opciones(select, items || [], (item) => key === 'participantes'
                ? item.nombre + ' ' + item.apellido + ' · ' + item.cliente
                : item.nombre);
        });
        document.querySelectorAll('select[data-dependent]').forEach(actualizarDependiente);
    }

    function recursosPorTipo(tipo, formId) {
        if (tipo === 'ESPACIO') return state.espacios;
        if (tipo === 'CATERING' && formId === 'quote-form') return state.catalogos?.servicios || [];
        if (tipo === 'CATERING') return state.catering.map((s) => ({...s, nombre: s.servicio + ' · ' + s.fecha}));
        if (tipo === 'CURSO') return state.catalogos?.cursos || [];
        if (tipo === 'DIPLOMADO') return state.catalogos?.diplomados || [];
        if (tipo === 'ALQUILER') return state.allAlquileres.filter((a) => a.estado !== 'CANCELADO')
            .map((a) => ({...a, nombre: a.espacio + ' · ' + a.fecha + ' ' + a.horaInicio}));
        if (tipo === 'INSCRIPCION') return state.inscripciones.map((i) => ({...i, nombre: i.programa + ' · ' + i.participante}));
        if (tipo === 'COTIZACION') return state.cotizaciones.map((c) => ({...c, nombre: 'Cotización #' + c.id + ' · ' + c.cliente}));
        return [];
    }

    function actualizarDependiente(select) {
        const form = select.form;
        const target = form.elements.namedItem(select.dataset.dependent);
        if (!target) return;
        const cliente = form.elements.namedItem('clienteId');
        let items = recursosPorTipo(select.value, form.id);
        if (form.id === 'payment-form' && cliente?.value) {
            items = items.filter((item) => String(item.clienteId) === cliente.value);
        }
        opciones(target, items, (item) => item.nombre);
    }

    function fechaLocal() {
        const now = new Date();
        return [now.getFullYear(), String(now.getMonth() + 1).padStart(2, '0'), String(now.getDate()).padStart(2, '0')].join('-');
    }

    function minutos(time) {
        const [hour, minute] = time.split(':').map(Number);
        return hour * 60 + minute;
    }

    function pintarAgenda() {
        const schedule = document.getElementById('schedule');
        const active = state.alquileres.filter((a) => a.estado !== 'CANCELADO');
        document.getElementById('space-count').textContent = state.espacios.length;
        document.getElementById('available-count').textContent = state.espacios.filter((e) => e.disponible).length;
        document.getElementById('booking-count').textContent = active.length;
        const empty = document.getElementById('agenda-empty');
        empty.hidden = state.espacios.length > 0;
        empty.textContent = 'Aún no hay espacios. Registra uno para comenzar la agenda.';
        schedule.replaceChildren();
        if (!state.espacios.length) return;
        const start = Math.min(420, ...active.map((a) => Math.floor(minutos(a.horaInicio) / 60) * 60));
        const end = Math.max(1260, ...active.map((a) => Math.ceil(minutos(a.horaFin) / 60) * 60));
        const slots = Math.ceil((end - start) / 10);
        schedule.style.gridTemplateColumns = '56px repeat(' + state.espacios.length + ', minmax(165px, 1fr))';
        schedule.style.gridTemplateRows = '38px repeat(' + slots + ', 19px)';
        schedule.style.minWidth = 56 + state.espacios.length * 165 + 'px';
        const corner = texto('div', 'Hora', 'schedule-time schedule-header schedule-corner');
        corner.style.gridColumn = '1'; corner.style.gridRow = '1'; schedule.append(corner);
        state.espacios.forEach((space, index) => {
            const header = texto('div', space.nombre + (space.disponible ? '' : ' · Inactivo'), 'schedule-header');
            header.style.gridColumn = index + 2; header.style.gridRow = '1'; schedule.append(header);
        });
        for (let i = 0; i < slots; i++) {
            const value = start + i * 10;
            const time = texto('div', String(Math.floor(value / 60)).padStart(2, '0') + ':' +
                String(value % 60).padStart(2, '0'), 'schedule-time');
            time.style.gridColumn = '1'; time.style.gridRow = i + 2; schedule.append(time);
            state.espacios.forEach((space, index) => {
                const cell = document.createElement('div');
                cell.className = 'schedule-slot';
                cell.style.gridColumn = index + 2; cell.style.gridRow = i + 2;
                schedule.append(cell);
            });
        }
        active.forEach((booking) => {
            const column = state.espacios.findIndex((s) => s.id === booking.espacioId);
            if (column < 0) return;
            const event = document.createElement('div');
            event.className = 'schedule-booking';
            event.append(texto('strong', booking.cliente),
                texto('span', booking.horaInicio.slice(0, 5) + '–' + booking.horaFin.slice(0, 5)));
            event.title = booking.espacio + ' · ' + booking.cliente + ' · ' + booking.horaInicio + ' a ' + booking.horaFin;
            event.style.gridColumn = column + 2;
            event.style.gridRow = Math.floor((minutos(booking.horaInicio) - start) / 10) + 2 +
                ' / span ' + Math.max(1, Math.ceil((minutos(booking.horaFin) - minutos(booking.horaInicio)) / 10));
            schedule.append(event);
        });
        schedule.setAttribute('aria-label', 'Agenda del ' + document.getElementById('agenda-date').value +
            ': ' + active.length + ' reservas en ' + state.espacios.length + ' espacios. La lista siguiente ofrece el detalle.');
    }

    function pintarEspacios() {
        mostrarLista('spaces-list', state.espacios, (space) => {
            const button = texto('button', 'Editar', 'item-action');
            button.type = 'button';
            button.addEventListener('click', () => {
                const form = document.getElementById('space-form');
                for (const field of ['nombre', 'tipo', 'capacidad', 'precio', 'equipamiento']) {
                    form.elements.namedItem(field).value = space[field] ?? '';
                }
                form.elements.namedItem('disponible').checked = space.disponible;
                form.dataset.editingId = space.id;
                form.querySelector('button[type="submit"]').textContent = 'Guardar cambios';
                form.scrollIntoView({behavior: 'smooth', block: 'center'});
                form.elements.namedItem('nombre').focus();
            });
            return fila(space.nombre, space.tipo + ' · ' + space.capacidad + ' personas · $' + space.precio +
                (space.equipamiento ? ' · ' + space.equipamiento : ''), space.disponible ? 'Disponible' : 'Inactivo', button);
        }, 'Todavía no hay espacios registrados.');
    }

    function pintarReservas() {
        mostrarLista('bookings-list', state.alquileres, (booking) => {
            const button = booking.estado === 'CANCELADO' ? null : texto('button', 'Cancelar', 'item-action');
            if (button) {
                button.type = 'button';
                button.addEventListener('click', async () => {
                    if (!window.confirm('¿Cancelar la reserva de ' + booking.cliente + '?')) return;
                    button.disabled = true;
                    try { await request(api + '/alquileres/' + booking.id + '/cancelar', {method: 'PATCH'});
                        aviso('Reserva cancelada.'); await cargarAgenda();
                    } catch (error) { aviso(error.message, true); button.disabled = false; }
                });
            }
            return fila(booking.espacio + ' · ' + booking.cliente,
                booking.horaInicio.slice(0, 5) + '–' + booking.horaFin.slice(0, 5) +
                (booking.motivo ? ' · ' + booking.motivo : ''), booking.estado, button);
        }, 'No hay reservas para este día.');
    }

    async function cargarCatalogos() {
        state.catalogos = await request(api + '/catalogos');
        completarOpciones();
    }

    async function cargarAgenda() {
        const date = document.getElementById('agenda-date').value;
        [state.espacios, state.alquileres] = await Promise.all([
            request(api + '/espacios'), request(api + '/alquileres?fecha=' + encodeURIComponent(date))
        ]);
        completarOpciones();
        pintarAgenda(); pintarEspacios(); pintarReservas();
    }

    function pintarClientes() {
        mostrarLista('clients-list', state.catalogos.clientes,
            (client) => fila(client.nombre, client.duiNit ? 'DUI / NIT: ' + client.duiNit : 'Sin documento'),
            'Todavía no hay clientes registrados.');
    }

    function pintarParticipantes() {
        mostrarLista('participants-list', state.catalogos.participantes,
            (p) => fila(p.nombre + ' ' + p.apellido, 'Cliente: ' + p.cliente + (p.correo ? ' · ' + p.correo : '')),
            'Todavía no hay participantes registrados.');
    }

    async function cargarPanel(name) {
        if (name === 'agenda') { await cargarAgenda(); return; }
        if (name === 'clientes') { await cargarCatalogos(); pintarClientes(); return; }
        if (name === 'participantes') { await cargarCatalogos(); pintarParticipantes(); return; }
        if (name === 'inscripciones') {
            state.inscripciones = await request(api + '/inscripciones');
            mostrarLista('enrollments-list', state.inscripciones, (i) => fila(i.participante + ' · ' + i.programa,
                i.fecha + ' · Total $' + i.total, i.estado), 'No hay inscripciones registradas.');
        }
        if (name === 'catering') {
            [state.catering, state.catalogos.servicios] = await Promise.all([
                request(api + '/catering'), request(api + '/servicios-catering')]);
            mostrarLista('catering-list', state.catering, (s) => fila(s.servicio + ' · ' + s.cliente,
                s.fecha + ' ' + s.hora.slice(0, 5) + ' · ' + s.lugar + ' · ' + s.numeroAsistentes + ' asistentes · ' + s.menu,
                s.estado), 'No hay solicitudes de catering.');
            mostrarLista('services-list', state.catalogos.servicios, (s) => fila(s.nombre,
                s.tipoServicio + ' · Precio base $' + s.precioBase, s.activo ? 'Activo' : 'Inactivo'),
            'No hay servicios de catering registrados.');
        }
        if (name === 'cotizaciones') {
            state.cotizaciones = await request(api + '/cotizaciones');
            mostrarLista('quotes-list', state.cotizaciones, (q) => fila('Cotización #' + q.id + ' · ' + q.cliente,
                q.fecha + ' · Estimado $' + q.montoEstimado + (q.observaciones ? ' · ' + q.observaciones : ''),
                q.estado), 'No hay cotizaciones registradas.');
        }
        if (name === 'actividades') {
            [state.actividades, state.allAlquileres, state.catering] = await Promise.all([
                request(api + '/actividades'), request(api + '/alquileres'), request(api + '/catering')]);
            mostrarLista('activities-list', state.actividades, (a) => fila(a.titulo,
                a.fecha + ' · ' + a.horaInicio.slice(0, 5) + '–' + a.horaFin.slice(0, 5) +
                (a.cupo ? ' · Cupo ' + a.cupo : ''), a.tipo), 'No hay actividades registradas.');
        }
        if (name === 'pagos') {
            [state.pagos, state.inscripciones, state.allAlquileres, state.catering, state.cotizaciones] = await Promise.all([
                request(api + '/pagos'), request(api + '/inscripciones'), request(api + '/alquileres'),
                request(api + '/catering'), request(api + '/cotizaciones')]);
            mostrarLista('payments-list', state.pagos, (p) => {
                const button = p.estado === 'PENDIENTE' ? texto('button', 'Confirmar pago', 'item-action') : null;
                if (button) {
                    button.type = 'button';
                    button.addEventListener('click', async () => {
                        if (!window.confirm('¿Confirmar el pago de $' + p.monto + ' de ' + p.cliente + '?')) return;
                        button.disabled = true;
                        try { await request(api + '/pagos/' + p.id + '/confirmar', {method: 'PATCH'});
                            aviso('Pago confirmado.'); await cargarPanel('pagos');
                        } catch (error) { aviso(error.message, true); button.disabled = false; }
                    });
                }
                return fila(p.cliente + ' · $' + p.monto,
                    p.fecha + ' · ' + p.metodo + (p.referencia ? ' · ' + p.referencia : ''), p.estado, button);
            }, 'No hay pagos registrados.');
        }
        completarOpciones();
    }

    async function mostrarPanel(name) {
        if (!titles[name]) name = 'agenda';
        document.querySelectorAll('.panel').forEach((panel) => { panel.hidden = panel.id !== 'panel-' + name; });
        document.querySelectorAll('[data-panel]').forEach((button) => {
            const selected = button.dataset.panel === name;
            button.classList.toggle('is-current', selected);
            if (selected) button.setAttribute('aria-current', 'page');
            else button.removeAttribute('aria-current');
        });
        document.getElementById('panel-title').textContent = titles[name][0];
        document.getElementById('panel-description').textContent = titles[name][1];
        history.replaceState(null, '', '#'+name);
        try { await cargarPanel(name); } catch (error) { aviso(error.message, true); }
    }

    const formActions = [
        ['booking-form', '/alquileres', 'Reserva registrada.', ['clienteId', 'espacioId', 'fecha', 'horaInicio', 'horaFin', 'motivo']],
        ['space-form', '/espacios', 'Espacio guardado.', ['nombre', 'tipo', 'capacidad', 'precio', 'equipamiento', 'disponible']],
        ['client-form', '/clientes', 'Cliente registrado.', ['nombre', 'duiNit', 'empresa', 'correo', 'telefono', 'direccion']],
        ['participant-form', '/participantes', 'Participante registrado.', ['clienteId', 'nombre', 'apellido', 'correo', 'telefono']],
        ['enrollment-form', '/inscripciones', 'Inscripción creada.', ['participanteId', 'programaTipo', 'programaId']],
        ['catering-form', '/catering', 'Solicitud de catering registrada.', ['clienteId', 'servicioId', 'fecha', 'hora', 'numeroAsistentes', 'lugar', 'menu']],
        ['service-form', '/servicios-catering', 'Servicio registrado.', ['nombre', 'tipoServicio', 'precioBase', 'descripcion']],
        ['quote-form', '/cotizaciones', 'Cotización creada.', ['clienteId', 'conceptoTipo', 'conceptoId', 'cantidad', 'observaciones']],
        ['activity-form', '/actividades', 'Actividad programada.', ['titulo', 'fecha', 'horaInicio', 'horaFin', 'cupo', 'vinculacionTipo', 'vinculacionId']],
        ['payment-form', '/pagos', 'Pago pendiente registrado.', ['clienteId', 'conceptoTipo', 'conceptoId', 'monto', 'metodo', 'referencia']]
    ];

    for (const [id, path, success, fields] of formActions) {
        const form = document.getElementById(id);
        const feedback = texto('p', '', 'form-feedback');
        feedback.textContent = '';
        feedback.setAttribute('role', 'status');
        feedback.setAttribute('aria-live', 'polite');
        form.append(feedback);
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            if (!form.reportValidity()) return;
            feedback.textContent = '';
            feedback.classList.remove('is-error');
            const button = form.querySelector('button[type="submit"]');
            button.disabled = true;
            const original = button.textContent;
            button.textContent = 'Guardando...';
            const data = {};
            fields.forEach((field) => {
                const element = form.elements.namedItem(field);
                if (element.type === 'checkbox') data[field] = element.checked;
                else if (element.type === 'number') data[field] = element.value === '' ? null : Number(element.value);
                else if (field.endsWith('Id')) data[field] = element.value === '' ? null : Number(element.value);
                else data[field] = element.value.trim();
            });
            try {
                const editingId = form.dataset.editingId;
                await request(api + path + (editingId ? '/' + editingId : ''), {
                    method: editingId ? 'PUT' : 'POST', headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(data)
                });
                delete form.dataset.editingId;
                form.reset();
                button.textContent = original;
                if (id === 'space-form') {
                    form.elements.namedItem('disponible').checked = true;
                    button.textContent = 'Guardar espacio';
                }
                if (id === 'booking-form') {
                    document.getElementById('agenda-date').value = data.fecha;
                    form.elements.namedItem('fecha').value = data.fecha;
                }
                if (id === 'catering-form' || id === 'activity-form') {
                    form.elements.namedItem('fecha').value = fechaLocal();
                }
                aviso(success);
                feedback.textContent = success;
                await cargarCatalogos();
                await cargarPanel(document.querySelector('.nav-link.is-current').dataset.panel);
            } catch (error) {
                aviso(error.message, true);
                feedback.textContent = error.message;
                feedback.classList.add('is-error');
            }
            finally {
                button.disabled = false;
                if (id !== 'space-form' || form.dataset.editingId) button.textContent = original;
            }
        });
    }

    document.querySelectorAll('select[data-dependent]').forEach((select) => {
        select.addEventListener('change', () => actualizarDependiente(select));
    });
    document.querySelector('#payment-form [name="clienteId"]').addEventListener('change', () =>
        actualizarDependiente(document.querySelector('#payment-form [name="conceptoTipo"]')));
    document.getElementById('agenda-date').addEventListener('change', () => cargarAgenda().catch((e) => aviso(e.message, true)));
    const sidebar = document.getElementById('sidebar');
    const backdrop = document.getElementById('menu-backdrop');
    const menu = document.getElementById('menu-button');
    function cerrarMenu() {
        sidebar.classList.remove('is-open');
        backdrop.hidden = true;
        menu.setAttribute('aria-expanded', 'false');
    }
    document.querySelectorAll('[data-panel]').forEach((button) => button.addEventListener('click', () => {
        aviso(''); mostrarPanel(button.dataset.panel);
        cerrarMenu();
    }));
    menu.addEventListener('click', () => {
        const open = sidebar.classList.toggle('is-open');
        backdrop.hidden = !open;
        menu.setAttribute('aria-expanded', String(open));
        if (open) document.getElementById('menu-dismiss').focus();
    });
    document.getElementById('menu-dismiss').addEventListener('click', () => { cerrarMenu(); menu.focus(); });
    backdrop.addEventListener('click', () => { cerrarMenu(); menu.focus(); });
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && sidebar.classList.contains('is-open')) {
            cerrarMenu(); menu.focus();
        }
    });
    document.getElementById('logout-button').addEventListener('click', async () => {
        try { await request('/api/v1/auth/logout', {method: 'POST'}); }
        finally { window.location.assign('/'); }
    });

    (async () => {
        try {
            const user = await request('/api/v1/auth/me');
            if (user.rol !== 'RECEPCIONISTA') {
                window.location.replace(user.rol === 'ADMIN' ? '/admin/index.html' : '/cliente/index.html');
                return;
            }
            document.getElementById('user-name').textContent = user.nombre;
            const today = fechaLocal();
            document.getElementById('agenda-date').value = today;
            document.querySelector('#booking-form [name="fecha"]').value = today;
            document.querySelector('#catering-form [name="fecha"]').value = today;
            document.querySelector('#activity-form [name="fecha"]').value = today;
            await cargarCatalogos();
            await mostrarPanel(window.location.hash.slice(1));
        } catch (error) { aviso(error.message, true); }
    })();
})();
