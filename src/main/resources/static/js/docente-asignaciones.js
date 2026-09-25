(function () {
    'use strict';

    window.crearGestorDocentes = function (tipo) {
        const dialog = document.getElementById('teacher-assignment-dialog');
        const options = document.getElementById('teacher-assignment-options');
        const empty = document.getElementById('teacher-assignment-empty');
        const error = document.getElementById('teacher-assignment-error');
        const save = document.getElementById('teacher-assignment-save');
        const singular = tipo === 'cursos' ? 'curso' : 'diplomado';
        let selectedItem;
        let trigger;
        let csrfToken;

        async function csrf() {
            if (csrfToken) return csrfToken;
            const response = await fetch('/api/v1/auth/csrf', {credentials: 'same-origin'});
            if (!response.ok) throw new Error('No se pudo iniciar una solicitud segura.');
            csrfToken = (await response.json()).token;
            return csrfToken;
        }

        function message(data) {
            if (data.message || data.mensaje) return data.message || data.mensaje;
            return Object.values(data.fieldErrors || data).find((value) => typeof value === 'string')
                || 'No se pudo completar la operación.';
        }

        async function request(url, requestOptions = {}) {
            const method = (requestOptions.method || 'GET').toUpperCase();
            const headers = {...requestOptions.headers};
            if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) headers['X-CSRF-TOKEN'] = await csrf();
            const response = await fetch(url, {...requestOptions, credentials: 'same-origin', headers});
            const data = response.status === 204 ? null : await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(message(data || {}));
            return data;
        }

        function addOption(teacher, assigned) {
            const label = document.createElement('label');
            label.className = 'teacher-option';
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.value = String(teacher.idDocente);
            checkbox.checked = assigned.has(teacher.idDocente);
            const text = document.createElement('span');
            const name = document.createElement('strong');
            name.textContent = teacher.nombre;
            const specialty = document.createElement('small');
            specialty.textContent = teacher.especialidad;
            text.append(name, specialty);
            label.append(checkbox, text);
            options.append(label);
        }

        async function open(item, source) {
            selectedItem = item;
            trigger = source;
            options.replaceChildren();
            empty.hidden = true;
            error.textContent = '';
            save.disabled = true;
            const name = tipo === 'cursos' ? item.titulo : item.nombre;
            document.getElementById('teacher-assignment-title').textContent = 'Docentes de ' + name;
            document.getElementById('teacher-assignment-description').textContent =
                'Selecciona quién impartirá este ' + singular + '. Se comprobarán los cruces de horario.';
            dialog.showModal();
            try {
                const id = tipo === 'cursos' ? item.idCurso : item.idDiplomado;
                const [catalog, assigned] = await Promise.all([
                    request('/api/v1/docentes?pagina=0&tamano=100&ordenarPor=nombre&direccion=asc'),
                    request('/api/v1/' + tipo + '/' + id + '/docentes')
                ]);
                const assignedIds = new Set(assigned.map((teacher) => teacher.idDocente));
                catalog.contenido.forEach((teacher) => addOption(teacher, assignedIds));
                empty.hidden = catalog.contenido.length > 0;
                save.disabled = catalog.contenido.length === 0;
            } catch (failure) {
                error.textContent = failure.message;
            }
        }

        async function persist() {
            if (!selectedItem) return;
            const id = tipo === 'cursos' ? selectedItem.idCurso : selectedItem.idDiplomado;
            const idsDocentes = [...options.querySelectorAll('input:checked')].map((input) => Number(input.value));
            error.textContent = '';
            save.disabled = true;
            try {
                await request('/api/v1/' + tipo + '/' + id + '/docentes', {
                    method: 'PUT',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({idsDocentes})
                });
                dialog.close();
            } catch (failure) {
                error.textContent = failure.message;
                save.disabled = false;
            }
        }

        document.getElementById('teacher-assignment-close').addEventListener('click', () => dialog.close());
        document.getElementById('teacher-assignment-cancel').addEventListener('click', () => dialog.close());
        save.addEventListener('click', persist);
        dialog.addEventListener('close', () => {
            if (trigger?.isConnected) trigger.focus();
            selectedItem = null;
            trigger = null;
        });

        return {open};
    };
})();
