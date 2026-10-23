(function () {
    'use strict';

    const api = '/api/v1/cursos';
    const id = new URLSearchParams(window.location.search).get('id');
    const loading = document.getElementById('course-loading');
    const errorMessage = document.getElementById('course-error');
    const content = document.getElementById('course-content');
    let curso;
    let inscripciones = [];
    let csrfToken;

    async function request(url) {
        const response = await fetch(url, {credentials: 'same-origin'});
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            const error = new Error(data.message || data.mensaje || 'No se pudo cargar la información.');
            error.status = response.status;
            throw error;
        }
        return data;
    }

    async function secureRequest(url, options = {}) {
        if (!csrfToken) {
            const response = await fetch('/api/v1/auth/csrf', {credentials: 'same-origin'});
            if (!response.ok) throw new Error('No se pudo iniciar una solicitud segura.');
            csrfToken = (await response.json()).token;
        }
        const response = await fetch(url, {...options, credentials: 'same-origin',
            headers: {...options.headers, 'X-CSRF-TOKEN': csrfToken}});
        const data = response.status === 204 ? null : await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.mensaje || data.message || 'No se pudo completar la solicitud.');
        return data;
    }

    function put(idElemento, value) {
        document.getElementById(idElemento).textContent = value == null || value === '' ? 'Información pendiente.' : value;
    }

    function fecha(value) {
        if (!value) return 'Por definir';
        return new Intl.DateTimeFormat('es-SV', {day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC'})
            .format(new Date(value + 'T00:00:00Z'));
    }

    function money(value) {
        return new Intl.NumberFormat('es-SV', {style: 'currency', currency: 'USD'}).format(value || 0);
    }

    function node(tag, className, text) {
        const item = document.createElement(tag);
        if (className) item.className = className;
        if (text !== undefined) item.textContent = text;
        return item;
    }

    function renderTextList(idElemento, value, ordered) {
        const list = document.getElementById(idElemento);
        list.replaceChildren();
        const entries = String(value || '').split(/\r?\n/).map(item => item.trim()).filter(Boolean);
        (entries.length ? entries : ['Información pendiente.']).forEach(item => list.append(node('li', '', item)));
        list.classList.toggle('is-ordered', Boolean(ordered));
    }

    function safeUrl(value) {
        try {
            const url = new URL(value, window.location.origin);
            if (url.protocol !== 'https:' && url.protocol !== 'http:') return null;
            if (url.origin === window.location.origin && !url.pathname.startsWith('/archivos/cursos/')) return null;
            return url.href;
        } catch (_) { return null; }
    }

    function iconoCategoria(category) {
        const value = String(category || '').toLocaleLowerCase('es');
        if (value.includes('tecnolog') || value.includes('inform')) return 'laptop';
        if (value.includes('administr') || value.includes('negocio') || value.includes('finanz')) return 'chart';
        if (value.includes('arte') || value.includes('diseñ') || value.includes('manga')) return 'palette';
        if (value.includes('idioma') || value.includes('inglés')) return 'globe';
        if (value.includes('salud')) return 'health';
        if (value.includes('educ')) return 'book';
        return 'sparkle';
    }

    function icon(name, className) {
        const paths = {
            laptop: ['M4 5h16v11H4z', 'M2 19h20l-2 2H4z'],
            chart: ['M4 19V10h4v9z', 'M10 19V5h4v14z', 'M16 19v-7h4v7z'],
            palette: ['M12 3a9 9 0 1 0 0 18h1a2 2 0 0 0 1.5-3.3c-.8-.9-.2-2.2 1-2.2H18a3 3 0 0 0 3-3c0-5.2-4-9.5-9-9.5z', 'M7.5 11h.01', 'M10 7.5h.01', 'M15 8h.01', 'M17 11.5h.01'],
            globe: ['M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z', 'M3 12h18', 'M12 3a15 15 0 0 1 0 18', 'M12 3a15 15 0 0 0 0 18'],
            health: ['M12 21s-8-4.5-8-11a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 10c0 6.5-8 11-8 11z', 'M12 8v7', 'M8.5 11.5h7'],
            book: ['M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21z', 'M4 5.5v15', 'M8 7h8', 'M8 11h8'],
            sparkle: ['m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z', 'm19 15 .8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z'],
            check: ['m5 12 4 4L19 6'], lock: ['M5 10h14v11H5z', 'M8 10V7a4 4 0 0 1 8 0v3'], play: ['m8 5 12 7-12 7z'],
            video: ['M3 6h13v12H3z', 'm16 10 5-3v10l-5-3z'], image: ['M3 4h18v16H3z', 'm4 17 5-5 4 4 3-3 4 4', 'M15 9h.01'], link: ['M10 13a5 5 0 0 0 7.1 0l2-2a5 5 0 0 0-7.1-7.1l-1.1 1.1', 'M14 11a5 5 0 0 0-7.1 0l-2 2a5 5 0 0 0 7.1 7.1l1.1-1.1']
        };
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('viewBox', '0 0 24 24'); svg.setAttribute('aria-hidden', 'true');
        svg.setAttribute('focusable', 'false'); svg.classList.add(className);
        (paths[name] || paths.sparkle).forEach((d) => {
            const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            path.setAttribute('d', d); svg.append(path);
        });
        return svg;
    }

    function renderProgram(value, enrollment) {
        const container = document.getElementById('course-program');
        container.replaceChildren();
        let modules;
        try { modules = JSON.parse(value || ''); } catch (_) { modules = null; }
        if (!Array.isArray(modules)) {
            modules = String(value || '').split(/\r?\n/).map(line => line.trim()).filter(Boolean)
                .map(titulo => ({titulo, descripcion: '', recursos: []}));
        }
        const progress = document.getElementById('course-progress');
        const progressMessage = document.getElementById('course-progress-message');
        const completion = document.getElementById('course-completion');
        completion.hidden = !enrollment?.finalizado;
        if (enrollment?.finalizado) {
            document.getElementById('course-certificate-link').href =
                `/api/v1/cliente/cursos/${encodeURIComponent(curso.idCurso)}/certificado`;
        }
        const completed = new Set(enrollment?.modulosCompletados || []);
        const total = modules.length;
        if (enrollment && enrollment.estado !== 'CONFIRMADA') {
            progress.hidden = true;
            progressMessage.hidden = false;
            progressMessage.classList.add('course-access-blocked');
            progressMessage.replaceChildren(icon('lock', 'interface-icon'),
                document.createTextNode('Tu acceso al curso está bloqueado. Podrás comenzar cuando recepción verifique y confirme tu pago.'));
            container.append(node('p', 'course-module-empty', 'El contenido se habilitará después de confirmar el pago.'));
            return;
        }
        progressMessage.classList.remove('course-access-blocked');
        if (enrollment && total) {
            const percent = enrollment.progresoPorcentaje || 0;
            progress.hidden = false;
            document.getElementById('course-progress-label').textContent = `${completed.size} de ${total} módulos · ${percent}%`;
            document.getElementById('course-progress-fill').style.width = percent + '%';
            progress.querySelector('[role="progressbar"]').setAttribute('aria-valuenow', String(percent));
            progressMessage.hidden = false;
            progressMessage.textContent = enrollment.finalizado
                ? '¡Completaste todo el contenido de este curso! Tu certificado ya está disponible.'
                : 'Completa cada módulo para desbloquear el siguiente. Tu avance también aparece en Mi aprendizaje.';
        } else {
            progress.hidden = true;
            progressMessage.hidden = true;
        }
        modules.forEach((module, index) => {
            const number = index + 1;
            const isCompleted = completed.has(number);
            const isUnlocked = number === 1 || completed.has(number - 1);
            const card = node('article', 'course-module-card' + (isUnlocked ? ' is-unlocked' : ' is-locked')
                + (isCompleted ? ' is-completed' : ''));
            const title = node('h3', '', module.titulo || 'Módulo ' + number);
            const badge = node('span', 'course-module-status');
            badge.append(icon(isCompleted ? 'check' : isUnlocked ? 'play' : 'lock', 'interface-icon'),
                document.createTextNode(isCompleted ? 'Completado' : isUnlocked ? 'Disponible' : 'Bloqueado'));
            card.append(node('span', 'course-module-number', isCompleted ? '✓' : String(number).padStart(2, '0')), title, badge);
            if (!isUnlocked) {
                const note = node('p', 'course-module-locked-note');
                note.append(icon('lock', 'interface-icon'), document.createTextNode(enrollment
                    ? 'Completa el módulo anterior para desbloquear este contenido.'
                    : 'Inscríbete en el curso y completa el módulo anterior para continuar.'));
                card.append(note);
                container.append(card);
                return;
            }
            if (module.descripcion) card.append(node('p', 'course-module-description', module.descripcion));
            (module.recursos || []).forEach(resource => {
                const url = safeUrl(resource.url);
                if (!url) return;
                const item = node('div', 'course-resource');
                const type = String(resource.tipo || '').toUpperCase();
                if (type === 'IMAGEN') {
                    const image = node('img', 'course-resource-image');
                    image.src = url; image.alt = resource.nombre || module.titulo || 'Imagen del curso'; image.loading = 'lazy';
                    item.append(image);
                } else if (type === 'VIDEO' && /\.(mp4|webm)(\?|$)/i.test(url)) {
                    const video = node('video', 'course-resource-video');
                    video.src = url; video.controls = true; video.preload = 'metadata';
                    item.append(video);
                }
                const resourceIcon = type === 'VIDEO' ? 'video' : type === 'IMAGEN' ? 'image' : 'link';
                const link = node('a', 'course-resource-link');
                link.append(icon(resourceIcon, 'interface-icon'), document.createTextNode(resource.nombre || 'Abrir recurso'));
                link.href = url; link.target = '_blank'; link.rel = 'noopener noreferrer';
                item.append(link);
                card.append(item);
            });
            if (!isCompleted && enrollment) {
                const complete = node('button', 'course-module-complete', 'Marcar como completado y desbloquear el siguiente →');
                complete.type = 'button';
                complete.addEventListener('click', async () => {
                    complete.disabled = true;
                    complete.textContent = 'Guardando tu avance…';
                    try {
                        const yaFinalizado = Boolean(enrollment.finalizado);
                        const actualizada = await secureRequest(`/api/v1/cliente/cursos/${curso.idCurso}/progreso`, {
                            method: 'POST', headers: {'Content-Type': 'application/json'},
                            body: JSON.stringify({numeroModulo: number})
                        });
                        Object.assign(enrollment, actualizada);
                        renderProgram(curso.contenidoPrograma, enrollment);
                        if (!yaFinalizado && actualizada.finalizado) {
                            progressMessage.textContent = 'Curso finalizado. Estamos descargando tu certificado PDF.';
                            window.location.assign(`/api/v1/cliente/cursos/${encodeURIComponent(curso.idCurso)}/certificado`);
                        }
                    } catch (error) {
                        complete.disabled = false;
                        complete.textContent = 'Marcar como completado y desbloquear el siguiente →';
                        progressMessage.hidden = false;
                        progressMessage.textContent = error.message;
                    }
                });
                card.append(complete);
            } else if (!enrollment && number === 1) {
                card.append(node('p', 'course-module-locked-note', 'Inscríbete para guardar tu avance y desbloquear el siguiente módulo.'));
            }
            container.append(card);
        });
        if (!modules.length) container.append(node('p', 'course-module-empty', 'El programa se publicará próximamente.'));
    }

    function activateStarPicker(containerId, inputId) {
        const container = document.getElementById(containerId);
        const input = document.getElementById(inputId);
        const buttons = [];
        for (let value = 1; value <= 5; value += 1) {
            const button = node('button', 'star-choice', '★');
            button.type = 'button';
            button.setAttribute('role', 'radio');
            button.setAttribute('aria-label', value + (value === 1 ? ' estrella' : ' estrellas'));
            button.setAttribute('aria-checked', 'false');
            button.addEventListener('click', () => {
                input.value = String(value);
                buttons.forEach((item, index) => {
                    item.classList.toggle('is-selected', index < value);
                    item.setAttribute('aria-checked', String(index + 1 === value));
                });
            });
            buttons.push(button);
            container.append(button);
        }
        container.addEventListener('keydown', event => {
            if (!['ArrowLeft', 'ArrowRight'].includes(event.key)) return;
            event.preventDefault();
            const current = Number(input.value) || 0;
            const next = Math.max(1, Math.min(5, current + (event.key === 'ArrowRight' ? 1 : -1)));
            buttons[next - 1].click();
            buttons[next - 1].focus();
        });
    }

    activateStarPicker('course-star-rating', 'course-review-stars');
    activateStarPicker('teacher-star-rating', 'teacher-review-stars');

    function renderCourse(course) {
        document.title = course.titulo + ' | UCA CFC Connect';
        put('course-breadcrumb-title', course.titulo);
        const category = document.getElementById('course-category');
        category.replaceChildren(icon(iconoCategoria(course.categoria), 'interface-icon'),
            document.createTextNode(course.categoria || 'Formación continua'));
        document.getElementById('course-art-initial').textContent = (course.categoria || course.titulo || 'C').trim().charAt(0).toUpperCase();
        const portada = document.getElementById('course-cover-image');
        const portadaUrl = course.imagenUrl ? safeUrl(course.imagenUrl) : null;
        portada.hidden = !portadaUrl;
        document.querySelector('.course-hero-art').classList.toggle('has-image', Boolean(portadaUrl));
        if (portadaUrl) portada.src = portadaUrl;
        put('course-title', course.titulo);
        put('course-description', course.descripcion);
        put('course-creator', course.creadoPor || 'Centro de Formación Continua');
        put('course-updated', course.fechaActualizacion
            ? new Intl.DateTimeFormat('es-SV', {dateStyle: 'medium'}).format(new Date(course.fechaActualizacion)) : 'Sin registro');
        put('course-rating', course.calificacionPromedio
            ? Number(course.calificacionPromedio).toFixed(1) + ' de 5' : 'Aún sin valoraciones');
        const ratingStars = document.querySelector('.course-stars');
        const filledStars = course.calificacionPromedio ? Math.max(1, Math.min(5, Math.round(Number(course.calificacionPromedio)))) : 0;
        ratingStars.textContent = '★'.repeat(filledStars) + '☆'.repeat(5 - filledStars);
        ratingStars.classList.toggle('has-rating', filledStars > 0);
        put('course-rating-summary', course.cantidadResenas
            ? course.cantidadResenas + (course.cantidadResenas === 1 ? ' valoración' : ' valoraciones') : 'Sin valoraciones');
        put('course-students', (course.estudiantesInscritos || 0) + ' estudiantes inscritos');
        put('course-cost', money(course.costo));
        put('course-modality', course.modalidad);
        put('course-duration', course.duracionHoras + (course.duracionHoras === 1 ? ' hora' : ' horas'));
        put('course-dates', fecha(course.fechaInicio) + ' – ' + fecha(course.fechaFin));
        put('course-schedule', course.horario || 'Por definir');
        put('course-capacity', course.cupoMaximo + ' personas');
        renderTextList('course-learning', course.loQueAprenderas, false);
        const enrollment = inscripciones.find(item => String(item.idCurso) === String(course.idCurso)
            && item.estado !== 'CANCELADA');
        renderProgram(course.contenidoPrograma, enrollment);
        put('course-requirements', course.requisitos || 'No se indicaron requisitos.');
        put('course-audience', course.dirigidoA || 'Información pendiente.');
        const teacherNames = course.docentes?.length ? course.docentes.map(teacher => teacher.nombre).join(', ') : 'Docentes por asignar';
        put('course-teachers', teacherNames);
        put('course-teachers-byline', teacherNames);
        const alreadyEnrolled = inscripciones.some(enrollment => String(enrollment.idCurso) === String(course.idCurso));
        const button = document.getElementById('course-enroll-button');
        button.disabled = alreadyEnrolled;
        button.textContent = alreadyEnrolled ? 'Ya estás inscrito' : 'Inscribirme';
        put('course-enroll-status', alreadyEnrolled
            ? enrollment?.estado === 'CONFIRMADA'
                ? 'Tu inscripción está confirmada. Ya puedes acceder al contenido del curso.'
                : 'Tu acceso está bloqueado hasta que recepción verifique y confirme tu pago.'
            : 'La inscripción quedará pendiente de pago. Podrás continuar desde Mi aprendizaje.');
        content.hidden = false;
        loading.hidden = true;
    }

    async function loadReviews() {
        const status = document.getElementById('course-review-status');
        const list = document.getElementById('course-review-list');
        const form = document.getElementById('course-review-form');
        status.textContent = 'Cargando valoraciones...';
        try {
            const reviews = await request(api + '/' + curso.idCurso + '/resenas');
            reviews.forEach(review => {
                const card = node('article', 'course-review-card');
                card.append(node('div', 'review-card-heading', '★ ' + review.calificacion + ' de 5'),
                    node('strong', '', review.autor), node('p', '', review.comentario || ''),
                    node('small', '', fecha(review.fecha)));
                list.append(card);
            });
            status.textContent = reviews.length ? '' : 'Este curso todavía no tiene valoraciones.';
        } catch (error) {
            status.textContent = error.message;
        }
        const enrollment = inscripciones.find(item => String(item.idCurso) === String(curso.idCurso)
            && item.estado === 'CONFIRMADA' && item.idInscripcion && !item.resenaEnviada);
        const yaValoro = Boolean(enrollment?.resenaEnviada);
        const puedeValorar = Boolean(enrollment && enrollment.estado === 'CONFIRMADA'
            && enrollment.idInscripcion && curso.fechaFin && curso.fechaFin < new Date().toISOString().slice(0, 10) && !yaValoro);
        form.hidden = !puedeValorar;
        const eligibility = document.getElementById('course-review-eligibility');
        eligibility.textContent = puedeValorar ? 'Selecciona de 1 a 5 estrellas para enviar tu valoración.'
            : yaValoro ? 'Ya enviaste tu valoración. Solo puedes valorar este curso una vez.'
                : enrollment ? 'Podrás valorar el curso cuando finalice y se confirme tu inscripción.'
                    : 'Inscríbete en el curso para poder valorarlo cuando finalice.';
        if (enrollment) form.dataset.inscripcion = enrollment.idInscripcion;
        const teacherChoice = document.getElementById('course-review-teacher');
        teacherChoice.replaceChildren(new Option('No valorar a un docente', ''));
        (curso.docentes || []).forEach(teacher => {
            const option = new Option(teacher.nombre, String(teacher.idDocente));
            teacherChoice.add(option);
        });
    }

    async function loadTeacherProfiles() {
        const list = document.getElementById('course-teacher-profiles');
        for (const teacher of curso.docentes || []) {
            const card = node('article', 'teacher-profile-card');
            try {
                const profile = await request('/api/v1/docentes/' + teacher.idDocente + '/perfil');
                if (profile.fotoUrl) {
                    const photo = node('img', 'teacher-profile-photo');
                    photo.src = profile.fotoUrl;
                    photo.alt = 'Foto de ' + profile.nombre;
                    card.append(photo);
                } else card.append(node('div', 'teacher-profile-initial', (profile.nombre || teacher.nombre).charAt(0).toUpperCase()));
                const details = node('div', 'teacher-profile-details');
                details.append(node('span', 'course-kicker', profile.especialidad || 'Docente'),
                    node('h3', '', profile.nombre), node('p', '', profile.descripcion || 'Docente asignado a este curso.'),
                    node('strong', 'teacher-profile-stats', (profile.calificacionPromedio ? Number(profile.calificacionPromedio).toFixed(1) + ' ★' : 'Sin valoraciones')
                        + ' · ' + (profile.estudiantesInscritos || 0) + ' estudiantes · ' + (profile.cantidadCursos || 0) + ' cursos'));
                (profile.resenas || []).slice(0, 2).forEach(review => details.append(node('blockquote', '', '“' + review.comentario + '”')));
                card.append(details);
            } catch (_) {
                card.append(node('div', 'teacher-profile-initial', teacher.nombre.charAt(0).toUpperCase()),
                    node('div', 'teacher-profile-details', teacher.nombre));
            }
            list.append(card);
        }
        if (!curso.docentes?.length) list.append(node('p', '', 'Los docentes de este curso se anunciarán próximamente.'));
    }

    document.getElementById('course-enroll-button').addEventListener('click', async event => {
        const button = event.currentTarget;
        const status = document.getElementById('course-enroll-status');
        button.disabled = true;
        status.textContent = 'Procesando tu inscripción...';
        try {
            const nuevaInscripcion = await secureRequest('/api/v1/cliente/inscripciones', {method: 'POST',
                headers: {'Content-Type': 'application/json'}, body: JSON.stringify({idCurso: curso.idCurso})});
            if (nuevaInscripcion) inscripciones.push(nuevaInscripcion);
            renderCourse(curso);
            status.textContent = 'Inscripción creada y pendiente de pago. Puedes continuar desde Mi aprendizaje.';
            document.getElementById('course-enroll-status').textContent = status.textContent;
        } catch (error) {
            status.textContent = error.message;
            button.disabled = error.message === 'Ya estás inscrito en este curso';
            if (button.disabled) button.textContent = 'Ya estás inscrito';
        }
    });

    document.getElementById('course-review-form').addEventListener('submit', async event => {
        event.preventDefault();
        const form = event.currentTarget;
        const status = document.getElementById('course-review-status');
        const teacherId = document.getElementById('course-review-teacher').value;
        const teacherStars = document.getElementById('teacher-review-stars').value;
        const teacherComment = document.getElementById('teacher-review-comment').value;
        try {
            if (!document.getElementById('course-review-stars').value) throw new Error('Selecciona una calificación de 1 a 5 estrellas.');
            if (teacherId && (!teacherStars || !teacherComment.trim())) throw new Error('Completa la calificación y el comentario del docente.');
            await secureRequest(api + '/' + curso.idCurso + '/resenas', {method: 'POST', headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({idInscripcion: Number(form.dataset.inscripcion),
                    calificacionCurso: Number(document.getElementById('course-review-stars').value),
                    comentarioCurso: document.getElementById('course-review-comment').value,
                    ...(teacherId ? {idDocente: Number(teacherId), calificacionDocente: Number(teacherStars), comentarioDocente: teacherComment} : {})})});
            const enrollment = inscripciones.find(item => String(item.idInscripcion) === form.dataset.inscripcion);
            if (enrollment) enrollment.resenaEnviada = true;
            status.textContent = 'Tu valoración quedó publicada.';
            form.hidden = true;
            document.getElementById('course-review-list').replaceChildren();
            curso = await request(api + '/' + curso.idCurso);
            renderCourse(curso);
            await loadReviews();
        } catch (error) { status.textContent = error.message; }
    });

    async function start() {
        if (!id || !/^\d+$/.test(id)) {
            loading.hidden = true;
            errorMessage.textContent = 'No se indicó un curso válido.';
            return;
        }
        try {
            try {
                const result = await request('/api/v1/cliente/inscripciones');
                if (Array.isArray(result)) inscripciones = result;
            } catch (error) {
                if (error.status !== 401 && error.status !== 403) throw error;
            }
            curso = await request(api + '/' + encodeURIComponent(id));
            renderCourse(curso);
            await Promise.all([loadReviews(), loadTeacherProfiles()]);
        } catch (error) {
            loading.hidden = true;
            errorMessage.textContent = error.message || 'No se pudo cargar este curso.';
        }
    }

    start();
})();
