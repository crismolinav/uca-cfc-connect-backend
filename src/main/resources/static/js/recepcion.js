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

    function escaparHtml(value) {
        const element = document.createElement('span');
        element.textContent = value == null ? '' : String(value);
        return element.innerHTML;
    }

    function moneda(value) {
        return new Intl.NumberFormat('es-SV', {style: 'currency', currency: 'USD'}).format(Number(value) || 0);
    }

    function abrirConstancia(cotizacion) {
        const ventana = window.open('', '_blank', 'width=960,height=720');
        if (!ventana) {
            aviso('Permite las ventanas emergentes para abrir la constancia.', true);
            return;
        }
        const detalles = cotizacion.detalles || [];
        const filas = detalles.map((detalle) => '<tr><td>' + escaparHtml(detalle.tipo) + '</td><td>' +
            escaparHtml(detalle.nombre) + '</td><td>' + escaparHtml(detalle.descripcion || 'Sin programación especificada') +
            '</td><td>' + escaparHtml(detalle.cantidad) + '</td><td>' + moneda(detalle.precioUnitario) +
            '</td><td>' + moneda(detalle.subtotal) + '</td></tr>').join('');
        const emitida = new Intl.DateTimeFormat('es-SV', {dateStyle: 'long', timeStyle: 'short'}).format(new Date());
        ventana.document.open();
        ventana.document.write('<!doctype html><html lang="es"><head><meta charset="UTF-8"><title>Constancia de cotización #' +
            escaparHtml(cotizacion.id) + '</title><style>@page{size:A4;margin:18mm}*{box-sizing:border-box}body{margin:0;padding:30px;color:#24364b;font:14px Arial,sans-serif}header{display:flex;justify-content:space-between;align-items:start;padding-bottom:18px;border-bottom:3px solid #245fd6}h1{margin:4px 0 0;color:#17345d;font-size:26px}h2{margin:30px 0 12px;font-size:17px}.brand{color:#245fd6;font-weight:700;font-size:18px}.status{padding:7px 11px;border-radius:999px;background:#e9f2fc;color:#225a9d;font-weight:700}.meta{display:grid;grid-template-columns:1fr 1fr;gap:10px 28px;margin:26px 0}.meta div{padding:11px 0;border-bottom:1px solid #dfe7f0}.meta strong{display:block;margin-bottom:5px;color:#60738d;font-size:11px;text-transform:uppercase}table{width:100%;border-collapse:collapse;font-size:12px}th,td{padding:11px 9px;border:1px solid #dfe7f0;text-align:left;vertical-align:top}th{background:#eff4fb;color:#17345d}.total{margin-top:20px;text-align:right;color:#17345d;font-size:20px;font-weight:700}.observaciones{padding:15px;border-radius:7px;background:#f4f7fb;line-height:1.55}.actions{margin-top:30px;text-align:right}.actions button{padding:11px 17px;border:0;border-radius:7px;background:#245fd6;color:#fff;font-weight:700}@media print{body{padding:0}.actions{display:none}}</style></head><body><header><div><div class="brand">UCA CFC Connect</div><h1>Constancia de cotización</h1></div><span class="status">' +
            escaparHtml(cotizacion.estado) + '</span></header><section class="meta"><div><strong>Número</strong>#' +
            escaparHtml(cotizacion.id) + '</div><div><strong>Cliente</strong>' + escaparHtml(cotizacion.cliente) +
            '</div><div><strong>Fecha de cotización</strong>' + escaparHtml(cotizacion.fecha) +
            '</div><div><strong>Constancia emitida</strong>' + escaparHtml(emitida) +
            '</div></section><h2>Servicios cotizados</h2><table><thead><tr><th>Tipo</th><th>Servicio</th><th>Fecha y horario</th><th>Cantidad</th><th>Precio unitario</th><th>Subtotal</th></tr></thead><tbody>' +
            (filas || '<tr><td colspan="6">No hay servicios asociados.</td></tr>') +
            '</tbody></table><div class="total">Total estimado: ' + moneda(cotizacion.montoEstimado) +
            '</div><h2>Observaciones</h2><div class="observaciones">' + escaparHtml(cotizacion.observaciones || 'Sin observaciones') +
            '</div><div class="actions"><button id="print-button" type="button">Imprimir / Guardar como PDF</button></div></body></html>');
        ventana.document.close();
        ventana.document.getElementById('print-button').addEventListener('click', () => ventana.print());
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
            opciones(select, items || [], (item) => key === 'participantes' || key === 'participantesInscribibles'
                ? item.nombre + ' ' + item.apellido + ' · ' + item.cliente
                : key === 'espacios' && item.duracionMaximaHoras != null
                    ? item.nombre + ' · máximo ' + Number(item.duracionMaximaHoras) + ' h'
                : item.nombre);
        });
        document.querySelectorAll('select[data-dependent]').forEach(actualizarDependiente);
    }

    function recursosPorTipo(tipo, formId) {
        if (tipo === 'ESPACIO') return state.espacios.filter((espacio) => espacio.disponible);
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
        if (form.id === 'quote-form') actualizarProgramacionCotizacion();
    }

    function actualizarProgramacionCotizacion() {
        const form = document.getElementById('quote-form');
        const tipo = form.elements.namedItem('conceptoTipo').value;
        const academico = tipo === 'CURSO' || tipo === 'DIPLOMADO';
        document.querySelectorAll('[data-quote-schedule]').forEach((label) => { label.hidden = academico; });
        for (const name of ['fechaServicio', 'horaInicio', 'horaFin']) {
            const input = form.elements.namedItem(name);
            input.disabled = academico;
            input.required = !academico;
        }
        const summary = document.getElementById('quote-program');
        summary.hidden = !academico;
        if (!academico) return;
        const item = recursosPorTipo(tipo, form.id).find((value) =>
            String(value.id) === form.elements.namedItem('conceptoId').value);
        if (!item) {
            summary.textContent = 'Selecciona el ' + (tipo === 'CURSO' ? 'curso' : 'diplomado') +
                ' para consultar su programación registrada.';
            return;
        }
        if (tipo === 'CURSO') {
            summary.textContent = 'Programación registrada: ' + (item.fechaInicio || 'fecha por confirmar') +
                (item.fechaFin ? ' al ' + item.fechaFin : '') + ' · ' + (item.horario || 'horario por confirmar') +
                '. Recepción no puede modificarla desde la cotización.';
            return;
        }
        const sesiones = item.sesiones || [];
        summary.textContent = sesiones.length
            ? 'Sesiones registradas: ' + sesiones.map((session) => session.fecha + ' · ' +
                session.horaInicio.slice(0, 5) + '–' + session.horaFin.slice(0, 5)).join('; ') +
                '. Recepción no puede modificarlas desde la cotización.'
            : 'Programación registrada: ' + (item.fechaInicio || 'fecha por confirmar') +
                (item.fechaFin ? ' al ' + item.fechaFin : '') + ' · sesiones por confirmar.';
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
                (space.duracionMaximaHoras == null ? '' : ' · máximo ' + Number(space.duracionMaximaHoras) + ' h') +
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
        [state.catalogos, state.espacios] = await Promise.all([
            request(api + '/catalogos'), request(api + '/espacios')
        ]);
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
            (client) => {
                const button = !client.tieneCuenta && client.correo
                    ? texto('button', 'Enviar invitación', 'item-action') : null;
                if (button) {
                    button.type = 'button';
                    button.addEventListener('click', async () => {
                        button.disabled = true;
                        try {
                            const result = await request(api + '/clientes/' + client.id + '/invitacion', {method: 'POST'});
                            mostrarInvitacion(result);
                        } catch (error) { aviso(error.message, true); }
                        finally { button.disabled = false; }
                    });
                }
                return fila(client.nombre, (client.duiNit ? 'DUI / NIT: ' + client.duiNit : 'Sin documento') +
                    (client.correo ? ' · ' + client.correo : '') +
                    (client.tieneCuenta ? ' · Cuenta creada' : ' · Sin cuenta'), null, button);
            },
            'Todavía no hay clientes registrados.');
    }

    function mostrarInvitacion(result) {
        aviso(result.mensaje);
        const target = document.getElementById('invitation-result');
        target.replaceChildren(texto('span', result.mensaje));
        if (result.enlaceLocal) {
            const hint = texto('span', 'Copia el enlace y ábrelo en una ventana privada para no cambiar la sesión de recepción.');
            const input = document.createElement('input');
            input.className = 'invitation-link';
            input.type = 'text';
            input.readOnly = true;
            input.value = result.enlaceLocal;
            input.setAttribute('aria-label', 'Enlace de invitación local');
            const copy = texto('button', 'Copiar enlace', 'item-action');
            copy.type = 'button';
            copy.addEventListener('click', async () => {
                try { await navigator.clipboard.writeText(input.value); aviso('Enlace copiado. Ábrelo en una ventana privada.'); }
                catch { input.select(); aviso('Copia el enlace seleccionado y ábrelo en una ventana privada.'); }
            });
            target.append(hint, input, copy);
        }
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
            const summaries = await request(api + '/cotizaciones');
            state.cotizaciones = await Promise.all(summaries.map(async (quote) => {
                try { return await request(api + '/cotizaciones/' + quote.id); }
                catch { return quote; }
            }));
            mostrarLista('quotes-list', state.cotizaciones, (q) => {
                const button = q.estado === 'APROBADA' ? texto('button', 'Ver constancia', 'item-action') : null;
                if (button) {
                    button.type = 'button';
                    button.addEventListener('click', () => abrirConstancia(q));
                }
                return fila('Cotización #' + q.id + ' · ' + q.cliente,
                    q.fecha + ' · ' + (q.detalles || []).map((detalle) =>
                        detalle.tipo + ': ' + detalle.nombre + ' × ' + detalle.cantidad +
                        (detalle.descripcion ? ' · ' + detalle.descripcion : '')).join(' · ') +
                    ' · Estimado ' + moneda(q.montoEstimado) + (q.observaciones ? ' · ' + q.observaciones : ''),
                    q.estado, button);
            }, 'No hay cotizaciones registradas.');
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
        ['quote-form', '/cotizaciones', 'Cotización creada.', ['clienteId', 'conceptoTipo', 'conceptoId', 'cantidad', 'fechaServicio', 'horaInicio', 'horaFin', 'observaciones']],
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
                else if (element.disabled) data[field] = null;
                else if (element.type === 'number') data[field] = element.value === '' ? null : Number(element.value);
                else if (field.endsWith('Id')) data[field] = element.value === '' ? null : Number(element.value);
                else data[field] = element.value.trim();
            });
            if (id === 'booking-form') {
                const espacio = state.espacios.find((item) => item.id === data.espacioId);
                const minutosSolicitados = minutos(data.horaFin) - minutos(data.horaInicio);
                const minutosPermitidos = Number(espacio?.duracionMaximaHoras) * 60;
                if (espacio && Number.isFinite(minutosPermitidos) && minutosSolicitados > minutosPermitidos) {
                    const mensaje = 'La reserva no puede superar ' + Number(espacio.duracionMaximaHoras)
                        + ' horas para este espacio.';
                    feedback.textContent = mensaje;
                    feedback.classList.add('is-error');
                    aviso(mensaje, true);
                    button.disabled = false;
                    button.textContent = original;
                    return;
                }
            }
            if (id === 'quote-form' && data.horaInicio && data.horaFin &&
                    minutos(data.horaFin) <= minutos(data.horaInicio)) {
                const mensaje = 'La hora final del servicio debe ser posterior a la inicial.';
                feedback.textContent = mensaje;
                feedback.classList.add('is-error');
                aviso(mensaje, true);
                button.disabled = false;
                button.textContent = original;
                return;
            }
            try {
                const editingId = form.dataset.editingId;
                const saved = await request(api + path + (editingId ? '/' + editingId : ''), {
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
                if (id === 'quote-form') form.elements.namedItem('fechaServicio').value = fechaLocal();
                aviso(success);
                feedback.textContent = success;
                await cargarCatalogos();
                await cargarPanel(document.querySelector('.nav-link.is-current').dataset.panel);
                if (id === 'client-form' && !editingId && data.correo) {
                    try {
                        const invitation = await request(api + '/clientes/' + saved.id + '/invitacion', {method: 'POST'});
                        mostrarInvitacion(invitation);
                        feedback.textContent = 'Cliente guardado. ' + invitation.mensaje;
                    } catch (error) {
                        aviso('Cliente guardado, pero no se pudo enviar la invitación: ' + error.message, true);
                        feedback.textContent = 'Cliente guardado. Usa “Enviar invitación” para reintentar: ' + error.message;
                        feedback.classList.add('is-error');
                    }
                }
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
    document.querySelector('#quote-form [name="conceptoId"]').addEventListener('change', actualizarProgramacionCotizacion);
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
            const quoteDate = document.querySelector('#quote-form [name="fechaServicio"]');
            quoteDate.value = today;
            quoteDate.min = today;
            await cargarCatalogos();
            await mostrarPanel(window.location.hash.slice(1));
        } catch (error) { aviso(error.message, true); }
    })();
})();
