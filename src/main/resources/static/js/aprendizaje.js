(function () {
    'use strict';

    const enrollmentList = document.getElementById('enrolled-courses');
    const paymentList = document.getElementById('client-payments');
    const status = document.getElementById('learning-status');
    const dialog = document.getElementById('client-payment-dialog');
    const form = document.getElementById('client-payment-form');
    const error = document.getElementById('client-payment-error');
    let csrfToken;
    let selectedEnrollment;

    async function request(url, options = {}) {
        if (options.method && !csrfToken) {
            const csrf = await fetch('/api/v1/auth/csrf', {credentials: 'same-origin'});
            if (!csrf.ok) throw new Error('Inicia sesión para continuar.');
            csrfToken = (await csrf.json()).token;
        }
        const response = await fetch(url, {
            ...options, credentials: 'same-origin',
            headers: {...options.headers, ...(options.method ? {'X-CSRF-TOKEN': csrfToken} : {})}
        });
        const body = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(body.mensaje || body.message || 'No se pudo completar la solicitud.');
        return body;
    }

    function node(tag, className, text) {
        const item = document.createElement(tag);
        if (className) item.className = className;
        if (text !== undefined) item.textContent = text;
        return item;
    }

    function money(value) {
        return new Intl.NumberFormat('es-SV', {style: 'currency', currency: 'USD'}).format(value);
    }

    function row(title, detail, state, action, imageUrl, courseUrl, progress) {
        const article = node('article', 'learning-item');
        if (imageUrl) {
            const cover = node(courseUrl ? 'a' : 'div', 'learning-item-cover');
            if (courseUrl) cover.href = courseUrl;
            const image = node('img', '', 'Portada de ' + title);
            image.src = imageUrl;
            image.alt = 'Portada de ' + title;
            image.loading = 'lazy';
            image.addEventListener('error', () => cover.remove());
            cover.append(image);
            article.append(cover);
        }
        const copy = node('div', 'learning-item-copy');
        copy.append(node('h3', '', title), node('p', '', detail));
        if (progress && progress.total > 0) {
            const meter = node('div', 'learning-progress');
            const heading = node('div', 'learning-progress-heading');
            heading.append(node('span', '', 'Progreso del curso'), node('strong', '', `${progress.completed} de ${progress.total} módulos · ${progress.percent}%`));
            const track = node('div', 'learning-progress-track');
            track.setAttribute('role', 'progressbar');
            track.setAttribute('aria-label', 'Progreso del curso');
            track.setAttribute('aria-valuemin', '0');
            track.setAttribute('aria-valuemax', '100');
            track.setAttribute('aria-valuenow', String(progress.percent));
            const fill = node('span', '');
            fill.style.width = `${progress.percent}%`;
            track.append(fill);
            meter.append(heading, track);
            copy.append(meter);
        }
        article.append(copy, node('span', 'learning-item-state', state));
        if (action) article.append(action);
        return article;
    }

    function render(enrollments, payments) {
        enrollmentList.replaceChildren();
        paymentList.replaceChildren();
        if (!enrollments.length) {
            enrollmentList.append(node('p', 'learning-note', 'Aún no tienes cursos inscritos. '));
            const link = node('a', '', 'Explorar cursos');
            link.href = '/cliente/index.html#explorar';
            enrollmentList.append(link);
        }
        const paymentsByEnrollment = new Map(payments.filter((p) => p.idInscripcion)
            .map((payment) => [payment.idInscripcion, payment]));
        document.getElementById('learning-course-count').textContent = String(enrollments.length);
        const coursesWithProgress = enrollments.filter(enrollment => enrollment.totalModulos > 0);
        const averageProgress = coursesWithProgress.length
            ? Math.round(coursesWithProgress.reduce((sum, enrollment) => sum + (enrollment.progresoPorcentaje || 0), 0) / coursesWithProgress.length)
            : 0;
        document.getElementById('learning-average-progress').textContent = averageProgress + '%';
        document.getElementById('learning-pending-count').textContent = String(enrollments.filter(enrollment =>
            enrollment.estado === 'PENDIENTE' || paymentsByEnrollment.get(enrollment.idInscripcion)?.estado === 'PENDIENTE').length);
        enrollments.forEach((enrollment) => {
            const payment = paymentsByEnrollment.get(enrollment.idInscripcion);
            const actions = node('div', 'learning-actions');
            if (enrollment.idCurso) {
                const detail = node('a', 'learning-course-link', 'Ver curso');
                detail.href = '/cliente/curso.html?id=' + encodeURIComponent(enrollment.idCurso);
                actions.append(detail);
            }
            if (enrollment.idCurso && enrollment.estado === 'PENDIENTE' && !payment) {
                const action = node('button', 'learning-action', 'Registrar pago');
                action.type = 'button';
                action.addEventListener('click', () => abrirPago(enrollment));
                actions.append(action);
            }
            const total = enrollment.total == null ? '' : money(enrollment.total) + ' · ';
            const courseUrl = enrollment.idCurso
                ? '/cliente/curso.html?id=' + encodeURIComponent(enrollment.idCurso) : null;
            enrollmentList.append(row(enrollment.curso, total + 'Inscripción ' + enrollment.fecha,
                payment?.estado === 'CONFIRMADO' ? 'Pagado' : payment?.estado === 'PENDIENTE' ? 'Pago pendiente' : enrollment.estado,
                actions.childElementCount ? actions : null, enrollment.imagenUrl, courseUrl,
                {completed: enrollment.modulosCompletados?.length || 0, total: enrollment.totalModulos || 0,
                    percent: enrollment.progresoPorcentaje || 0}));
        });
        if (!payments.length) paymentList.append(node('p', 'learning-note', 'Todavía no has registrado pagos.'));
        const enrollmentsById = new Map(enrollments.map(enrollment => [enrollment.idInscripcion, enrollment]));
        payments.forEach((payment) => {
            const enrollment = enrollmentsById.get(payment.idInscripcion);
            paymentList.append(row(payment.curso, money(payment.monto) + ' · ' + payment.fecha + ' · ' + payment.metodo,
                payment.estado + (payment.referencia ? ' · ' + payment.referencia : ''), null,
                enrollment?.imagenUrl, enrollment?.idCurso
                    ? '/cliente/curso.html?id=' + encodeURIComponent(enrollment.idCurso) : null));
        });
        status.textContent = 'Se actualizaron tus cursos y pagos.';
    }

    async function cargar() {
        status.textContent = 'Cargando tus cursos y pagos...';
        try {
            const [enrollments, payments] = await Promise.all([
                request('/api/v1/cliente/inscripciones'), request('/api/v1/cliente/pagos')
            ]);
            render(enrollments, payments);
        } catch (e) {
            status.textContent = e.message;
        }
    }

    function abrirPago(enrollment) {
        selectedEnrollment = enrollment;
        form.reset();
        error.textContent = '';
        document.getElementById('client-payment-course').textContent = enrollment.curso + ' · ' + money(enrollment.total);
        dialog.showModal();
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!selectedEnrollment || !form.reportValidity()) return;
        const button = form.querySelector('button[type="submit"]');
        button.disabled = true;
        error.textContent = '';
        try {
            await request('/api/v1/cliente/pagos', {method: 'POST', headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({idInscripcion: selectedEnrollment.idInscripcion,
                    metodo: document.getElementById('client-payment-method').value,
                    referencia: document.getElementById('client-payment-reference').value.trim() || null})});
            dialog.close();
            await cargar();
        } catch (e) {
            error.textContent = e.message;
        } finally {
            button.disabled = false;
        }
    });
    document.getElementById('client-payment-close').addEventListener('click', () => dialog.close());
    document.getElementById('client-payment-cancel').addEventListener('click', () => dialog.close());
    cargar();
})();
