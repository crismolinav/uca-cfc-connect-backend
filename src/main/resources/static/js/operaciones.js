(function () {
    'use strict';
    const moduleName = document.body.dataset.module;
    const api = '/api/v1/' + moduleName;
    const plural = {espacios: 'espacios', alquileres: 'alquileres', cotizaciones: 'cotizaciones'}[moduleName];
    const singular = {espacios: 'espacio', alquileres: 'alquiler', cotizaciones: 'cotización'}[moduleName];
    const body = document.getElementById('items-body');
    const form = document.getElementById('item-form');
    const formDialog = document.getElementById('form-dialog');
    const confirmDialog = document.getElementById('confirm-dialog');
    const stateDialog = document.getElementById('state-dialog');
    let page = 0, size = 10, editingId = null, selected = null, csrfToken, loadNumber = 0;
    let clients = [], spaces = [], courses = [];

    async function csrf() {
        if (csrfToken) return csrfToken;
        const response = await fetch('/api/v1/auth/csrf', {credentials: 'same-origin'});
        if (!response.ok) throw new Error('No se pudo iniciar una solicitud segura.');
        csrfToken = (await response.json()).token;
        return csrfToken;
    }
    async function logout() {
        try { await fetch('/api/v1/auth/logout', {method: 'POST', credentials: 'same-origin', headers: {'X-CSRF-TOKEN': await csrf()}}); }
        finally { localStorage.removeItem('uca_cfc_token'); sessionStorage.removeItem('uca_cfc_token'); location.assign('/auth/login.html'); }
    }
    function errorMessage(data) {
        if (data?.mensaje || data?.message) return data.mensaje || data.message;
        return Object.values(data?.fieldErrors || data || {}).find(value => typeof value === 'string') || 'No se pudo completar la operación.';
    }
    async function request(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const headers = {...options.headers};
        if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) headers['X-CSRF-TOKEN'] = await csrf();
        const response = await fetch(url, {...options, credentials: 'same-origin', headers});
        if (response.status === 401) { await logout(); throw new Error('Tu sesión venció.'); }
        const data = response.status === 204 ? null : await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(errorMessage(data));
        return data;
    }
    const money = value => new Intl.NumberFormat('es-SV', {style: 'currency', currency: 'USD'}).format(value || 0);
    const date = value => value ? new Intl.DateTimeFormat('es-SV', {day: '2-digit', month: 'short', year: 'numeric'}).format(new Date(value + 'T00:00:00')) : '—';
    const stateLabel = value => ({EN_PROCESO: 'En proceso', PENDIENTE: 'Pendiente', APROBADA: 'Aprobada', RECHAZADA: 'Rechazada', CONFIRMADO: 'Confirmado', CANCELADO: 'Cancelado', FINALIZADO: 'Finalizado'}[value] || value);
    function badge(value) { const el = document.createElement('span'); el.className = 'state-pill state-' + String(value).toLowerCase().replace('_', '-'); el.textContent = stateLabel(value); return el; }
    function cell(row, value, className = '') { const td = document.createElement('td'); td.textContent = value ?? '—'; td.className = className; row.append(td); return td; }
    function button(label, handler, danger = false) { const b = document.createElement('button'); b.type = 'button'; b.textContent = label; if (danger) b.className = 'delete-row-button'; b.addEventListener('click', handler); return b; }
    function actions(item) {
        const wrap = document.createElement('span'); wrap.className = 'row-actions';
        wrap.append(button('Ver', () => openForm('view', item)), button('Editar', () => openForm('edit', item)));
        if (moduleName !== 'espacios') wrap.append(button('Estado', () => openState(item)));
        wrap.append(button('Eliminar', () => openDelete(item), true)); return wrap;
    }
    function row(item, number) {
        const tr = document.createElement('tr'); cell(tr, number);
        if (moduleName === 'espacios') {
            const main = cell(tr, item.nombre, 'operation-main-cell'); const small = document.createElement('small'); small.textContent = item.equipamiento || 'Sin equipamiento especificado'; main.append(small);
            cell(tr, item.tipo); cell(tr, item.capacidad + ' personas'); cell(tr, money(item.precio)); cell(tr, item.duracionMaximaHoras == null ? 'Sin definir' : Number(item.duracionMaximaHoras) + ' h'); const s = document.createElement('td'); s.append(badge(item.disponible ? 'DISPONIBLE' : 'NO_DISPONIBLE')); tr.append(s);
        } else if (moduleName === 'alquileres') {
            const main = cell(tr, item.nombreEspacio, 'operation-main-cell'); const small = document.createElement('small'); small.textContent = item.motivo || 'Sin motivo especificado'; main.append(small);
            cell(tr, item.nombreCliente); cell(tr, date(item.fecha) + ' · ' + item.horaInicio.slice(0, 5) + '–' + item.horaFin.slice(0, 5)); const s = document.createElement('td'); s.append(badge(item.estado)); tr.append(s);
        } else {
            const main = cell(tr, '#' + item.idCotizacion, 'operation-main-cell'); const small = document.createElement('small'); small.textContent = item.detalles.length + (item.detalles.length === 1 ? ' servicio' : ' servicios'); main.append(small);
            cell(tr, item.nombreCliente); cell(tr, date(item.fecha)); cell(tr, money(item.montoEstimado)); const s = document.createElement('td'); s.append(badge(item.estado)); tr.append(s);
        }
        const actionCell = document.createElement('td'); actionCell.append(actions(item)); tr.append(actionCell); return tr;
    }
    function buildParams() {
        const params = new URLSearchParams({pagina: page, tamano: size});
        const text = document.getElementById('search').value.trim(); if (text) params.set('texto', text);
        if (moduleName === 'espacios') {
            const available = document.getElementById('availability-filter').value, capacity = document.getElementById('capacity-filter').value;
            if (available) params.set('disponible', available); if (capacity) params.set('capacidadMinima', capacity); params.set('ordenarPor', 'nombre');
        } else if (moduleName === 'alquileres') {
            const status = document.getElementById('status-filter').value, space = document.getElementById('space-filter').value, requestedDate = document.getElementById('date-filter').value;
            if (status) params.set('estado', status); if (space) params.set('idEspacio', space); if (requestedDate) params.set('fecha', requestedDate); params.set('direccion', 'asc');
        } else { const status = document.getElementById('status-filter').value; if (status) params.set('estado', status); params.set('direccion', 'desc'); }
        return params;
    }
    async function load() {
        const sequence = ++loadNumber, status = document.getElementById('list-status'); status.classList.remove('visually-hidden'); status.textContent = 'Cargando ' + plural + '...'; body.replaceChildren();
        try {
            const data = await request(api + '?' + buildParams()); if (sequence !== loadNumber) return;
            if (!data.contenido.length && page > 0) { page--; return load(); }
            data.contenido.forEach((item, index) => body.append(row(item, page * size + index + 1)));
            status.textContent = data.contenido.length ? data.totalElementos + ' ' + plural + ' encontrados.' : 'No hay resultados para los filtros seleccionados.';
            status.classList.toggle('visually-hidden', data.contenido.length > 0);
            document.getElementById('pagination').hidden = data.totalElementos === 0; document.getElementById('previous-page').disabled = data.primera; document.getElementById('next-page').disabled = data.ultima;
            document.getElementById('page-indicator').textContent = 'Página ' + (data.paginaActual + 1) + ' de ' + data.totalPaginas; document.getElementById('total-count').textContent = data.totalElementos + ' ' + plural;
        } catch (error) { status.textContent = error.message; }
    }
    function populateSelect(select, values, idKey, labelKey, placeholder) {
        select.replaceChildren(new Option(placeholder, '')); values.forEach(item => select.add(new Option(item[labelKey], item[idKey])));
    }
    async function loadCatalogs() {
        if (moduleName !== 'espacios') {
            const [clientPage, spacePage] = await Promise.all([request('/api/v1/admin/clientes?page=0&size=100'), request('/api/v1/espacios?pagina=0&tamano=100&ordenarPor=nombre')]);
            clients = clientPage.content; spaces = spacePage.contenido;
            populateSelect(form.elements.idCliente, clients, 'id', 'nombre', 'Selecciona un cliente'); populateSelect(form.elements.idEspacio || document.createElement('select'), spaces, 'idEspacio', 'nombre', 'Selecciona un espacio');
            if (moduleName === 'alquileres') populateSelect(document.getElementById('space-filter'), spaces, 'idEspacio', 'nombre', 'Todos');
        }
        if (moduleName === 'cotizaciones') { const pageData = await request('/api/v1/cursos?pagina=0&tamano=100&activo=true'); courses = pageData.contenido; }
    }
    function setDisabled(disabled) { [...form.elements].forEach(el => { if (!['button', 'submit'].includes(el.type)) el.disabled = disabled; }); document.getElementById('dialog-save').hidden = disabled; }
    function openForm(mode, item = null) {
        form.reset(); editingId = mode === 'new' ? null : (item.idEspacio || item.idAlquiler || item.idCotizacion); selected = item; document.getElementById('dialog-error').textContent = '';
        document.getElementById('dialog-title').textContent = (mode === 'new' ? (moduleName === 'cotizaciones' ? 'Nueva ' : 'Nuevo ') : mode === 'view' ? 'Detalle de ' : 'Editar ') + singular;
        if (moduleName === 'espacios') ['nombre','tipo','capacidad','precio','equipamiento','duracionMaximaHoras'].forEach(k => form.elements[k].value = item?.[k] ?? '');
        if (moduleName === 'espacios') form.elements.disponible.checked = item?.disponible ?? true;
        if (moduleName === 'alquileres') { ['idCliente','idEspacio','fecha','horaInicio','horaFin','motivo'].forEach(k => form.elements[k].value = item?.[k] ?? ''); form.elements.fecha.min = new Date().toISOString().slice(0, 10); }
        if (moduleName === 'cotizaciones') { form.elements.idCliente.value = item?.idCliente ?? ''; form.elements.observaciones.value = item?.observaciones ?? ''; document.getElementById('quote-details').replaceChildren(); (item?.detalles || [null]).forEach(addDetail); }
        setDisabled(mode === 'view'); if (moduleName === 'cotizaciones') document.getElementById('add-detail').hidden = mode === 'view';
        formDialog.showModal();
    }
    function detailOptions(type, selectedId) {
        const values = type === 'CURSO' ? courses : spaces, idKey = type === 'CURSO' ? 'idCurso' : 'idEspacio', labelKey = type === 'CURSO' ? 'titulo' : 'nombre';
        return values.map(item => '<option value="' + item[idKey] + '" ' + (String(item[idKey]) === String(selectedId) ? 'selected' : '') + '>' + escapeHtml(item[labelKey]) + ' · ' + money(type === 'CURSO' ? item.costo : item.precio) + '</option>').join('');
    }
    function escapeHtml(value) { const span = document.createElement('span'); span.textContent = value || ''; return span.innerHTML; }
    function addDetail(detail = null) {
        const type = detail?.tipo || 'CURSO', div = document.createElement('div'); div.className = 'quote-detail';
        div.innerHTML = '<div class="field"><label>Tipo</label><select class="detail-type"><option value="CURSO">Curso</option><option value="ESPACIO">Espacio</option></select></div><div class="field detail-service"><label>Servicio</label><select class="detail-reference" required><option value="">Selecciona</option>' + detailOptions(type, detail?.idReferencia) + '</select></div><div class="field"><label>Cantidad</label><input class="detail-quantity" type="number" min="1" value="' + (detail?.cantidad || 1) + '" required></div><div class="field detail-description"><label>Descripción</label><input class="detail-text" maxlength="255" value="' + escapeHtml(detail?.descripcion || '') + '"></div><button class="detail-remove" type="button" aria-label="Quitar servicio">Quitar</button>';
        const typeSelect = div.querySelector('.detail-type'); typeSelect.value = type; typeSelect.addEventListener('change', () => { const select = div.querySelector('.detail-reference'); select.innerHTML = '<option value="">Selecciona</option>' + detailOptions(typeSelect.value); calculateTotal(); });
        div.querySelectorAll('select,input').forEach(el => el.addEventListener('input', calculateTotal)); div.querySelector('.detail-remove').addEventListener('click', () => { div.remove(); calculateTotal(); }); document.getElementById('quote-details').append(div); calculateTotal();
    }
    function calculateTotal() {
        if (moduleName !== 'cotizaciones') return; let total = 0;
        document.querySelectorAll('.quote-detail').forEach(row => { const type = row.querySelector('.detail-type').value, id = Number(row.querySelector('.detail-reference').value), quantity = Number(row.querySelector('.detail-quantity').value || 0); const item = (type === 'CURSO' ? courses : spaces).find(x => (type === 'CURSO' ? x.idCurso : x.idEspacio) === id); total += Number(type === 'CURSO' ? item?.costo : item?.precio) * quantity || 0; });
        document.getElementById('quote-total').textContent = 'Total estimado: ' + money(total);
    }
    function payload() {
        if (moduleName === 'espacios') return {nombre: form.elements.nombre.value.trim(), tipo: form.elements.tipo.value.trim(), capacidad: Number(form.elements.capacidad.value), precio: Number(form.elements.precio.value), equipamiento: form.elements.equipamiento.value.trim(), duracionMaximaHoras: Number(form.elements.duracionMaximaHoras.value), disponible: form.elements.disponible.checked};
        if (moduleName === 'alquileres') return {idCliente: Number(form.elements.idCliente.value), idEspacio: Number(form.elements.idEspacio.value), fecha: form.elements.fecha.value, horaInicio: form.elements.horaInicio.value, horaFin: form.elements.horaFin.value, motivo: form.elements.motivo.value.trim()};
        return {idCliente: Number(form.elements.idCliente.value), observaciones: form.elements.observaciones.value.trim(), detalles: [...document.querySelectorAll('.quote-detail')].map(row => { const type = row.querySelector('.detail-type').value, id = Number(row.querySelector('.detail-reference').value); return {idCurso: type === 'CURSO' ? id : null, idEspacio: type === 'ESPACIO' ? id : null, cantidad: Number(row.querySelector('.detail-quantity').value), descripcion: row.querySelector('.detail-text').value.trim()}; })};
    }
    async function save(event) {
        event.preventDefault(); if (!form.reportValidity()) return; if (moduleName === 'cotizaciones' && !document.querySelector('.quote-detail')) { document.getElementById('dialog-error').textContent = 'Agrega al menos un servicio.'; return; }
        const saveButton = document.getElementById('dialog-save'); saveButton.disabled = true; document.getElementById('dialog-error').textContent = '';
        try { await request(editingId ? api + '/' + editingId : api, {method: editingId ? 'PUT' : 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload())}); formDialog.close(); await load(); document.getElementById('action-status').textContent = 'El ' + singular + ' se guardó correctamente.'; }
        catch (error) { document.getElementById('dialog-error').textContent = error.message; } finally { saveButton.disabled = false; }
    }
    function openState(item) { selected = item; document.getElementById('new-state').value = item.estado; document.getElementById('state-description').textContent = 'Estado actual de ' + (moduleName === 'alquileres' ? item.nombreEspacio : 'la cotización #' + item.idCotizacion) + ': ' + stateLabel(item.estado) + '.'; document.getElementById('state-error').textContent = ''; stateDialog.showModal(); }
    async function saveState() { try { await request(api + '/' + (selected.idAlquiler || selected.idCotizacion) + '/estado', {method: 'PATCH', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({estado: document.getElementById('new-state').value})}); stateDialog.close(); await load(); } catch (error) { document.getElementById('state-error').textContent = error.message; } }
    function openDelete(item) { selected = item; document.getElementById('confirm-description').textContent = '¿Deseas eliminar ' + (moduleName === 'espacios' ? item.nombre : moduleName === 'alquileres' ? 'la reserva de ' + item.nombreEspacio : 'la cotización #' + item.idCotizacion) + '? Esta acción no se puede deshacer.'; document.getElementById('confirm-error').textContent = ''; confirmDialog.showModal(); }
    async function remove() { try { await request(api + '/' + (selected.idEspacio || selected.idAlquiler || selected.idCotizacion), {method: 'DELETE'}); confirmDialog.close(); await load(); } catch (error) { document.getElementById('confirm-error').textContent = error.message; } }
    function sidebar() { const panel = document.getElementById('sidebar'), menu = document.getElementById('menu-button'), main = document.getElementById('main-content'); const set = open => { panel.classList.toggle('is-open', open); main.inert = open; menu.setAttribute('aria-expanded', open); }; menu.addEventListener('click', () => set(!panel.classList.contains('is-open'))); document.getElementById('sidebar-close').addEventListener('click', () => set(false)); }
    async function init() {
        try { const user = await request('/api/v1/auth/me'); if (user.rol !== 'ADMIN') { location.replace('/cliente/index.html'); return; } document.getElementById('admin-name').textContent = user.nombre; await loadCatalogs(); await load(); }
        catch (error) { document.getElementById('list-status').textContent = error.message; }
    }
    document.getElementById('logout-button').addEventListener('click', logout); document.getElementById('new-button').addEventListener('click', () => openForm('new')); form.addEventListener('submit', save); document.getElementById('dialog-close').addEventListener('click', () => formDialog.close()); document.getElementById('dialog-cancel').addEventListener('click', () => formDialog.close());
    document.getElementById('confirm-cancel').addEventListener('click', () => confirmDialog.close()); document.getElementById('confirm-button').addEventListener('click', remove); if (stateDialog) { document.getElementById('state-cancel').addEventListener('click', () => stateDialog.close()); document.getElementById('state-save').addEventListener('click', saveState); }
    if (moduleName === 'cotizaciones') document.getElementById('add-detail').addEventListener('click', () => addDetail());
    document.getElementById('previous-page').addEventListener('click', () => { page--; load(); }); document.getElementById('next-page').addEventListener('click', () => { page++; load(); }); document.getElementById('page-size').addEventListener('change', event => { size = Number(event.target.value); page = 0; load(); });
    let timer; document.querySelectorAll('.filters-panel input,.filters-panel select').forEach(control => control.addEventListener(control.type === 'search' ? 'input' : 'change', () => { clearTimeout(timer); timer = setTimeout(() => { page = 0; load(); }, 250); })); document.getElementById('clear-filters').addEventListener('click', () => { document.querySelectorAll('.filters-panel input,.filters-panel select').forEach(control => control.value = ''); page = 0; load(); });
    sidebar(); init();
})();
