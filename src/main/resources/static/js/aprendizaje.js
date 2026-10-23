(function () {
    'use strict';

    const enrollmentList = document.getElementById('enrolled-courses');
    const paymentList = document.getElementById('client-payments');
    const status = document.getElementById('learning-status');
    const dialog = document.getElementById('client-payment-dialog');
    const form = document.getElementById('client-payment-form');
    const error = document.getElementById('client-payment-error');
    const pageSize = 3;
    let csrfToken;
    let selectedEnrollment;
    let enrollmentData = [];
    let paymentData = [];
    let enrollmentPage = 0;
    let paymentPage = 0;

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

    function updatePagination(id, page, itemCount, onPageChange) {
        const pagination = document.getElementById(id);
        const pageCount = Math.ceil(itemCount / pageSize);
        pagination.hidden = pageCount <= 1;
        const previous = pagination.querySelector('[data-page-direction="previous"]');
        const next = pagination.querySelector('[data-page-direction="next"]');
        previous.disabled = page <= 0;
        next.disabled = page >= pageCount - 1;
        pagination.querySelector('span').textContent = `Página ${page + 1} de ${pageCount}`;
        previous.onclick = () => { if (page > 0) onPageChange(page - 1); };
        next.onclick = () => { if (page < pageCount - 1) onPageChange(page + 1); };
    }

    function render() {
        const enrollments = enrollmentData;
        const payments = paymentData;
        enrollmentList.replaceChildren();
        paymentList.replaceChildren();
        if (!enrollments.length) {
            enrollmentList.append(node('p', 'learning-note', 'Aún no tienes cursos ni diplomados inscritos. '));
            const link = node('a', '', 'Explorar formación');
            link.href = '/cliente/index.html#explorar';
            enrollmentList.append(link);
        }
        const paymentsByEnrollment = new Map(payments.filter((p) => p.idInscripcion)
            .map((payment) => [payment.idInscripcion, payment]));
        document.getElementById('learning-course-count').textContent = String(enrollments.length);
        const coursesWithProgress = enrollments.filter(enrollment =>
            enrollment.estado === 'CONFIRMADA' && enrollment.totalModulos > 0);
        const averageProgress = coursesWithProgress.length
            ? Math.round(coursesWithProgress.reduce((sum, enrollment) => sum + (enrollment.progresoPorcentaje || 0), 0) / coursesWithProgress.length)
            : 0;
        document.getElementById('learning-average-progress').textContent = averageProgress + '%';
        document.getElementById('learning-pending-count').textContent = String(enrollments.filter(enrollment =>
            enrollment.estado === 'PENDIENTE' || paymentsByEnrollment.get(enrollment.idInscripcion)?.estado === 'PENDIENTE').length);
        const visibleEnrollments = enrollments.slice(enrollmentPage * pageSize, (enrollmentPage + 1) * pageSize);
        visibleEnrollments.forEach((enrollment) => {
            const payment = paymentsByEnrollment.get(enrollment.idInscripcion);
            const actions = node('div', 'learning-actions');
            if (enrollment.idCurso) {
                const detail = node('a', 'learning-course-link', 'Ver curso');
                detail.href = '/cliente/curso.html?id=' + encodeURIComponent(enrollment.idCurso);
                actions.append(detail);
            } else if (enrollment.idDiplomado) {
                const detail = node('a', 'learning-course-link', 'Ver diplomado');
                detail.href = '/cliente/diplomado.html?id=' + encodeURIComponent(enrollment.idDiplomado);
                actions.append(detail);
            }
            if (enrollment.idCurso && enrollment.finalizado) {
                const certificate = node('a', 'learning-course-link', 'Descargar certificado PDF');
                certificate.href = `/api/v1/cliente/cursos/${encodeURIComponent(enrollment.idCurso)}/certificado`;
                actions.append(certificate);
            }
            if (enrollment.estado === 'PENDIENTE' && (!payment || payment.estado === 'RECHAZADO')) {
                const action = node('button', 'learning-action', 'Pagar inscripción');
                action.type = 'button';
                action.addEventListener('click', () => abrirPago(enrollment));
                actions.append(action);
            }
            const total = enrollment.total == null ? '' : money(enrollment.total) + ' · ';
            const courseUrl = enrollment.idCurso
                ? '/cliente/curso.html?id=' + encodeURIComponent(enrollment.idCurso)
                : enrollment.idDiplomado ? '/cliente/diplomado.html?id=' + encodeURIComponent(enrollment.idDiplomado) : null;
            const tipoFormacion = enrollment.idDiplomado ? 'Diplomado' : 'Curso';
            const estadoVisible = enrollment.finalizado ? 'Curso finalizado'
                : enrollment.estado === 'CONFIRMADA' ? 'Acceso confirmado'
                : enrollment.estado === 'PENDIENTE' ? 'Inscripción pendiente de pago'
                    : enrollment.estado;
            enrollmentList.append(row(enrollment.curso, tipoFormacion + ' · ' + total + 'Inscripción ' + enrollment.fecha,
                estadoVisible,
                actions.childElementCount ? actions : null, enrollment.imagenUrl, courseUrl,
                enrollment.idCurso && enrollment.estado === 'CONFIRMADA' && enrollment.totalModulos > 0
                    ? {completed: enrollment.modulosCompletados?.length || 0, total: enrollment.totalModulos,
                        percent: enrollment.progresoPorcentaje || 0}
                    : null));
        });
        if (!payments.length) paymentList.append(node('p', 'learning-note', 'Todavía no has registrado pagos.'));
        const enrollmentsById = new Map(enrollments.map(enrollment => [enrollment.idInscripcion, enrollment]));
        const visiblePayments = payments.slice(paymentPage * pageSize, (paymentPage + 1) * pageSize);
        visiblePayments.forEach((payment) => {
            const enrollment = enrollmentsById.get(payment.idInscripcion);
            paymentList.append(row(payment.curso, money(payment.monto) + ' · ' + payment.fecha + ' · ' + payment.metodo,
                payment.estado + (payment.referencia ? ' · ' + payment.referencia : ''), null,
                enrollment?.imagenUrl, enrollment?.idCurso
                    ? '/cliente/curso.html?id=' + encodeURIComponent(enrollment.idCurso)
                    : enrollment?.idDiplomado ? '/cliente/diplomado.html?id=' + encodeURIComponent(enrollment.idDiplomado) : null));
        });
        updatePagination('enrollment-pagination', enrollmentPage, enrollments.length, (nextPage) => {
            enrollmentPage = nextPage;
            render();
        });
        updatePagination('payment-pagination', paymentPage, payments.length, (nextPage) => {
            paymentPage = nextPage;
            render();
        });
        status.textContent = 'Se actualizaron tus cursos, diplomados y pagos.';
    }

    async function cargar() {
        status.textContent = 'Cargando tus cursos, diplomados y pagos...';
        try {
            const [enrollments, payments] = await Promise.all([
                request('/api/v1/cliente/inscripciones'), request('/api/v1/cliente/pagos')
            ]);
            enrollmentData = enrollments;
            paymentData = payments;
            enrollmentPage = Math.min(enrollmentPage, Math.max(Math.ceil(enrollmentData.length / pageSize) - 1, 0));
            paymentPage = Math.min(paymentPage, Math.max(Math.ceil(paymentData.length / pageSize) - 1, 0));
            render();
        } catch (e) {
            status.textContent = e.message;
        }
    }

    function abrirPago(enrollment) {
        selectedEnrollment = enrollment;
        form.reset();
        error.textContent = '';
        document.getElementById('client-payment-course').textContent = (enrollment.idDiplomado ? 'Diplomado · ' : 'Curso · ')
            + enrollment.curso + ' · ' + money(enrollment.total);
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
    document.getElementById('client-pay-wompi').addEventListener('click', async (event) => {
        if (!selectedEnrollment) return;
        const button = event.currentTarget;
        button.disabled = true;
        error.textContent = '';
        try {
            const result = await request('/api/v1/cliente/pagos/wompi/iniciar', {
                method: 'POST', headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({idInscripcion: selectedEnrollment.idInscripcion})
            });
            window.location.assign(result.url);
        } catch (e) {
            error.textContent = e.message;
            button.disabled = false;
        }
    });
    document.getElementById('client-payment-close').addEventListener('click', () => dialog.close());
    document.getElementById('client-payment-cancel').addEventListener('click', () => dialog.close());
    const query = new URLSearchParams(window.location.search);
    let callbackMessage = '';
    if (query.get('pago') === 'wompi') {
        const messages = {
            confirmado: 'Pago aprobado por Wompi. Tu inscripción quedó confirmada y ya puedes iniciar el curso.',
            rechazado: 'Wompi no aprobó el pago. Puedes intentarlo nuevamente desde tu inscripción.',
            pendiente: 'No se pudo confirmar el pago todavía. Tu inscripción sigue bloqueada; actualiza esta página en un momento.'
        };
        callbackMessage = messages[query.get('estado')] || messages.pendiente;
        window.history.replaceState({}, '', window.location.pathname);
    }
    cargar().then(() => {
        if (callbackMessage) status.textContent = callbackMessage;
    });
})();
