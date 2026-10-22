(() => {
    const token = new URLSearchParams(location.hash.slice(1)).get('token');
    const info = document.getElementById('invitacion-docente-info');
    const form = document.getElementById('invitacion-docente-form');
    const error = document.getElementById('invitacion-docente-error');
    let csrf;

    async function fetchData(url, options = {}) {
        const response = await fetch(url, {credentials: 'same-origin', ...options,
            headers: {'Content-Type': 'application/json', ...(options.headers || {})}});
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.message || data.mensaje || 'No se pudo verificar la invitación.');
        return data;
    }

    if (!token) { info.textContent = 'El enlace no contiene una invitación válida.'; }
    else fetchData(`/api/v1/auth/invitacion-docente?token=${encodeURIComponent(token)}`)
        .then(docente => { info.textContent = `Hola ${docente.nombre} (${docente.correo}). Elige una contraseña para ingresar.`; form.hidden = false; })
        .catch(failure => { info.textContent = failure.message; });

    form.addEventListener('submit', async event => {
        event.preventDefault(); error.textContent = '';
        const password = document.getElementById('docente-password').value;
        if (password !== document.getElementById('docente-password-confirm').value) {
            error.textContent = 'Las contraseñas no coinciden.'; return;
        }
        try {
            if (!csrf) csrf = (await fetchData('/api/v1/auth/csrf')).token;
            await fetchData('/api/v1/auth/aceptar-invitacion-docente', {method: 'POST',
                headers: {'X-CSRF-TOKEN': csrf}, body: JSON.stringify({token, password,
                    aceptaTerminos: document.getElementById('docente-terminos').checked})});
            location.replace('/docente/cursos.html');
        } catch (failure) { error.textContent = failure.message; }
    });
})();
