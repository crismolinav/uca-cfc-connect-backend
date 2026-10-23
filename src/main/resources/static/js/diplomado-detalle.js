(function () {
    'use strict';

    const api = '/api/v1/diplomados';
    const id = new URLSearchParams(window.location.search).get('id');
    const loading = document.getElementById('diploma-loading');
    const errorMessage = document.getElementById('diploma-error');
    const content = document.getElementById('diploma-content');
    const enrollButton = document.getElementById('diploma-enroll-button');
    let diploma;
    let csrfToken;

    async function request(url, options = {}) {
        const response = await fetch(url, {...options, credentials: 'same-origin'});
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.message || data.mensaje || 'No se pudo cargar la información.');
        return data;
    }

    async function secureRequest(url, options) {
        if (!csrfToken) {
            const response = await fetch('/api/v1/auth/csrf', {credentials: 'same-origin'});
            if (!response.ok) throw new Error('Inicia sesión para inscribirte.');
            csrfToken = (await response.json()).token;
        }
        return request(url, {...options, headers: {...options.headers, 'X-CSRF-TOKEN': csrfToken}});
    }

    function set(idElemento, value, fallback = 'Información pendiente.') {
        document.getElementById(idElemento).textContent = value == null || value === '' ? fallback : value;
    }

    function date(value, includeDay = false) {
        if (!value) return 'Por definir';
        return new Intl.DateTimeFormat('es-SV', {weekday: includeDay ? 'long' : undefined,
            day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC'})
            .format(new Date(value + 'T00:00:00Z'));
    }

    function money(value) {
        return new Intl.NumberFormat('es-SV', {style: 'currency', currency: 'USD'}).format(value || 0);
    }

    function hours(value) {
        return value + (Number(value) === 1 ? ' hora' : ' horas');
    }

    function renderSessions(sessions) {
        const list = document.getElementById('diploma-sessions');
        list.replaceChildren();
        sessions.forEach((session, index) => {
            const item = document.createElement('li');
            const title = document.createElement('strong');
            title.textContent = session.titulo || 'Sesión ' + (index + 1);
            const detail = document.createElement('p');
            const start = session.horaInicio ? session.horaInicio.slice(0, 5) : 'Hora por definir';
            const end = session.horaFin ? session.horaFin.slice(0, 5) : '';
            detail.textContent = date(session.fecha, true) + ' · ' + start + (end ? ' – ' + end : '')
                + (session.cupo ? ' · Cupo: ' + session.cupo : '');
            item.append(title, detail);
            list.append(item);
        });
    }

    function renderEnrollment(enrollments) {
        const enrollment = enrollments.find(item => String(item.idDiplomado) === String(diploma.idDiplomado));
        const status = document.getElementById('diploma-enroll-status');
        const learningLink = document.getElementById('diploma-learning-link');
        enrollButton.disabled = Boolean(enrollment);
        enrollButton.textContent = enrollment ? (enrollment.estado === 'CONFIRMADA' ? 'Inscripción confirmada' : 'Ya estás inscrito') : 'Inscribirme';
        status.textContent = enrollment
            ? enrollment.estado === 'CONFIRMADA'
                ? 'Tu inscripción está confirmada. Puedes revisar el estado desde Mi aprendizaje.'
                : 'Tu inscripción está pendiente. Desde Mi aprendizaje puedes pagar con transferencia, efectivo o tarjeta.'
            : 'La inscripción quedará pendiente de pago. Luego podrás pagar con transferencia, efectivo o tarjeta desde Mi aprendizaje.';
        learningLink.textContent = enrollment?.estado === 'PENDIENTE' ? 'Ir a Mi aprendizaje para pagar' : 'Ir a Mi aprendizaje';
    }

    async function cargar() {
        if (!id || !/^\d+$/.test(id)) {
            loading.hidden = true;
            errorMessage.textContent = 'No se indicó un diplomado válido.';
            return;
        }
        try {
            const [record, sessions, enrollments] = await Promise.all([
                request(api + '/' + encodeURIComponent(id)),
                request(api + '/' + encodeURIComponent(id) + '/actividades'),
                request('/api/v1/cliente/inscripciones').catch(() => [])
            ]);
            diploma = record;
            const fechas = date(record.fechaInicio) + ' – ' + date(record.fechaFin);
            const teachers = Array.isArray(record.docentes) && record.docentes.length
                ? record.docentes.map(teacher => teacher.nombre).join(', ') : 'Docentes por asignar';
            document.title = record.nombre + ' | UCA CFC Connect';
            set('diploma-breadcrumb-title', record.nombre);
            set('diploma-category', record.categoria);
            set('diploma-title', record.nombre);
            set('diploma-description', record.descripcion);
            set('diploma-summary', record.descripcion);
            set('diploma-modality', record.modalidad);
            set('diploma-teachers', teachers);
            set('diploma-teachers-summary', teachers);
            set('diploma-dates', fechas);
            set('diploma-quick-dates', fechas);
            set('diploma-cost', money(record.costo));
            set('diploma-duration', hours(record.duracionHoras));
            set('diploma-quick-modality', record.modalidad);
            set('diploma-quick-sessions', String(sessions.length));
            set('diploma-art-initial', (record.nombre || 'D').charAt(0).toLocaleUpperCase('es'));
            renderSessions(sessions);
            set('diploma-session-count', sessions.length + (sessions.length === 1 ? ' sesión' : ' sesiones'), '0 sesiones');
            set('diploma-sessions-status', sessions.length ? 'Consulta las fechas y horarios programados para este diplomado.' : 'No hay sesiones programadas.');
            renderEnrollment(Array.isArray(enrollments) ? enrollments : []);
            content.hidden = false;
            loading.hidden = true;
        } catch (error) {
            loading.hidden = true;
            errorMessage.textContent = error.message;
        }
    }

    enrollButton.addEventListener('click', async () => {
        if (!diploma) return;
        enrollButton.disabled = true;
        enrollButton.textContent = 'Procesando...';
        const status = document.getElementById('diploma-enroll-status');
        try {
            await secureRequest('/api/v1/cliente/inscripciones', {method: 'POST',
                headers: {'Content-Type': 'application/json'}, body: JSON.stringify({idDiplomado: diploma.idDiplomado})});
            enrollButton.textContent = 'Ya estás inscrito';
            document.getElementById('diploma-learning-link').textContent = 'Ir a Mi aprendizaje para pagar';
            status.textContent = 'Inscripción creada y pendiente de pago. Continúa en Mi aprendizaje para elegir tu método de pago.';
        } catch (error) {
            enrollButton.disabled = false;
            enrollButton.textContent = 'Inscribirme';
            status.textContent = error.message;
        }
    });

    cargar();
})();
