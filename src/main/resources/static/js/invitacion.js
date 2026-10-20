(() => {
    'use strict';
    const form = document.getElementById('invitation-form');
    const status = document.getElementById('invitation-status');
    const token = new URLSearchParams(location.hash.slice(1)).get('token');
    history.replaceState(null, '', location.pathname);

    async function errorDe(response) {
        const data = await response.json().catch(() => ({}));
        return new Error(data.mensaje || 'No se pudo completar el registro.');
    }

    async function iniciar() {
        if (!token) throw new Error('No encontramos una invitación en este enlace. Solicita una nueva en recepción.');
        const response = await fetch('/api/v1/auth/invitacion?token=' + encodeURIComponent(token),
            {credentials: 'same-origin'});
        if (!response.ok) throw await errorDe(response);
        const data = await response.json();
        form.elements.namedItem('nombre').value = data.nombre;
        form.elements.namedItem('correo').value = data.correo;
        form.hidden = false;
        status.classList.add('is-success');
        status.textContent = 'Invitación válida. Completa los datos para crear tu cuenta.';
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!form.reportValidity()) return;
        const button = form.querySelector('button[type="submit"]');
        button.disabled = true;
        status.classList.add('is-success');
        status.textContent = 'Creando tu cuenta...';
        try {
            const csrfResponse = await fetch('/api/v1/auth/csrf', {credentials: 'same-origin'});
            if (!csrfResponse.ok) throw new Error('No se pudo iniciar una solicitud segura.');
            const csrf = await csrfResponse.json();
            const response = await fetch('/api/v1/auth/aceptar-invitacion', {
                method: 'POST', credentials: 'same-origin',
                headers: {'Content-Type': 'application/json', 'X-CSRF-TOKEN': csrf.token},
                body: JSON.stringify({token, dui: form.elements.namedItem('dui').value.trim(),
                    password: form.elements.namedItem('password').value,
                    aceptaTerminos: form.elements.namedItem('aceptaTerminos').checked})
            });
            if (!response.ok) throw await errorDe(response);
            location.replace('/cliente/index.html');
        } catch (error) {
            status.classList.remove('is-success');
            status.textContent = error.message;
            button.disabled = false;
        }
    });

    iniciar().catch((error) => { status.textContent = error.message; });
})();
