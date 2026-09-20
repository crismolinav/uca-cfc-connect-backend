(function () {
    "use strict";

    let csrfToken;

    function limpiarAlmacenamientoAnterior() {
        localStorage.removeItem("uca_cfc_token");
        sessionStorage.removeItem("uca_cfc_token");
    }

    async function obtenerCsrf() {
        if (csrfToken) return csrfToken;
        const respuesta = await fetch("/api/v1/auth/csrf", {credentials: "same-origin"});
        if (!respuesta.ok) throw new Error("No pudimos iniciar una solicitud segura.");
        csrfToken = (await respuesta.json()).token;
        return csrfToken;
    }

    async function cerrarSesion() {
        try {
            const csrf = await obtenerCsrf();
            await fetch("/api/v1/auth/logout", {
                method: "POST",
                credentials: "same-origin",
                headers: {"X-CSRF-TOKEN": csrf}
            });
        } finally {
            limpiarAlmacenamientoAnterior();
            window.location.assign("/");
        }
    }

    function rutaPorRol(usuario) {
        if (usuario?.rol === "ADMIN") return "/admin/index.html";
        if (usuario?.rol === "CLIENTE") return "/cliente/index.html";
        return "/cuenta/perfil.html";
    }

    async function consultarUsuario() {
        const respuesta = await fetch("/api/v1/auth/me", {
            credentials: "same-origin"
        });
        if (!respuesta.ok) {
            const error = new Error("No pudimos verificar tu cuenta.");
            error.status = respuesta.status;
            throw error;
        }
        return respuesta.json();
    }

    function limpiarErrores(formulario) {
        formulario.querySelectorAll("[aria-invalid='true']").forEach((campo) => {
            campo.removeAttribute("aria-invalid");
        });
        formulario.querySelectorAll(".field-error").forEach((error) => {
            error.textContent = "";
        });
        const estado = document.getElementById("form-status");
        if (estado) estado.textContent = "";
    }

    function mostrarErrores(formulario, errores) {
        const campos = ["nombre", "dui", "correo", "password", "aceptaTerminos"];
        let primerCampo = null;

        campos.forEach((nombre) => {
            if (!errores[nombre]) return;
            const campo = formulario.elements.namedItem(nombre);
            const mensaje = document.getElementById(nombre + "-error");
            if (campo instanceof HTMLElement) {
                campo.setAttribute("aria-invalid", "true");
                if (!primerCampo) primerCampo = campo;
            }
            if (mensaje) mensaje.textContent = errores[nombre];
        });

        if (primerCampo) primerCampo.focus();
    }

    function cambiarCarga(boton, cargando) {
        boton.disabled = cargando;
        boton.textContent = cargando ? "Procesando..." : boton.dataset.idleLabel;
    }

    async function enviar(endpoint, datos) {
        const csrf = await obtenerCsrf();
        const headers = {"Content-Type": "application/json", "X-CSRF-TOKEN": csrf};
        const respuesta = await fetch(endpoint, {
            method: "POST",
            headers,
            credentials: "same-origin",
            body: JSON.stringify(datos)
        });
        let contenido = {};
        try {
            contenido = await respuesta.json();
        } catch (_error) {
            contenido = {};
        }
        return {respuesta, contenido};
    }

    function activarPasswordToggle() {
        document.querySelectorAll("[data-password-toggle]").forEach((boton) => {
            boton.addEventListener("click", () => {
                const id = boton.getAttribute("aria-controls");
                const campo = document.getElementById(id);
                const visible = campo.type === "text";
                campo.type = visible ? "password" : "text";
                boton.textContent = visible ? "Mostrar" : "Ocultar";
                boton.setAttribute("aria-label", visible ? "Mostrar contraseña" : "Ocultar contraseña");
            });
        });
    }

    function prepararLogin() {
        const formulario = document.getElementById("login-form");
        formulario.addEventListener("submit", async (evento) => {
            evento.preventDefault();
            limpiarErrores(formulario);

            if (!formulario.reportValidity()) return;
            const boton = formulario.querySelector("button[type='submit']");
            cambiarCarga(boton, true);

            try {
                const datos = {
                    correo: formulario.correo.value,
                    password: formulario.password.value,
                    recordar: formulario.recordar.checked
                };
                const {respuesta, contenido} = await enviar("/api/v1/auth/login", datos);
                if (!respuesta.ok) {
                    if (contenido.mensaje) {
                        document.getElementById("form-status").textContent = contenido.mensaje;
                    } else {
                        mostrarErrores(formulario, contenido);
                    }
                    return;
                }

                window.location.assign(rutaPorRol(contenido.usuario));
            } catch (_error) {
                document.getElementById("form-status").textContent =
                    "No pudimos conectar con el servidor. Intenta nuevamente.";
            } finally {
                cambiarCarga(boton, false);
            }
        });
    }

    function prepararRegistro() {
        const formulario = document.getElementById("registro-form");
        formulario.addEventListener("submit", async (evento) => {
            evento.preventDefault();
            limpiarErrores(formulario);

            if (!formulario.reportValidity()) return;
            const boton = formulario.querySelector("button[type='submit']");
            cambiarCarga(boton, true);

            try {
                const datos = {
                    nombre: formulario.nombre.value,
                    dui: formulario.dui.value,
                    correo: formulario.correo.value,
                    password: formulario.password.value,
                    aceptaTerminos: formulario.aceptaTerminos.checked
                };
                const {respuesta, contenido} = await enviar("/api/v1/auth/registro", datos);
                if (!respuesta.ok) {
                    if (contenido.mensaje) {
                        document.getElementById("form-status").textContent = contenido.mensaje;
                    } else {
                        mostrarErrores(formulario, contenido);
                    }
                    return;
                }

                window.location.assign(rutaPorRol(contenido.usuario));
            } catch (_error) {
                document.getElementById("form-status").textContent =
                    "No pudimos conectar con el servidor. Intenta nuevamente.";
            } finally {
                cambiarCarga(boton, false);
            }
        });
    }

    async function procesarCallback() {
        const parametros = new URLSearchParams(window.location.hash.slice(1));
        const error = parametros.get("error");
        const estado = document.getElementById("callback-status");
        const enlace = document.getElementById("callback-link");
        window.history.replaceState(null, document.title, window.location.pathname);

        if (!error) {
            try {
                const usuario = await consultarUsuario();
                window.location.replace(rutaPorRol(usuario));
            } catch (_error) {
                estado.textContent = "No pudimos verificar tu cuenta de Google. Vuelve a iniciar sesión.";
                estado.setAttribute("role", "alert");
                enlace.hidden = false;
            }
            return;
        }

        estado.textContent = error
            ? "Google no pudo completar el inicio de sesión. Intenta nuevamente."
            : "No recibimos un token válido. Vuelve a iniciar sesión.";
        estado.setAttribute("role", "alert");
        enlace.hidden = false;
    }

    async function cargarPerfil() {
        document.getElementById("logout-button").addEventListener("click", cerrarSesion);

        try {
            const usuario = await consultarUsuario();
            document.getElementById("profile-name").textContent = usuario.nombre;
            document.getElementById("profile-email").textContent = usuario.correo;
            document.getElementById("profile-role").textContent = usuario.rol;
            document.getElementById("admin-entry").hidden = usuario.rol !== "ADMIN";
            document.getElementById("profile-provider").textContent =
                usuario.proveedor === "google" ? "Google" : "Correo y contraseña";
            mostrarDuiEnPerfil(usuario);
            document.getElementById("profile-loading").hidden = true;
            document.getElementById("profile-content").hidden = false;
        } catch (error) {
            if (error.status === 401 || error.status === 403) {
                cerrarSesion();
                return;
            }
            document.getElementById("profile-loading").hidden = true;
            document.getElementById("profile-error").textContent =
                "No pudimos cargar tu cuenta. Actualiza la página para intentar nuevamente.";
        }
    }

    async function cargarPanelCliente() {
        document.getElementById("logout-button").addEventListener("click", cerrarSesion);
        try {
            const usuario = await consultarUsuario();
            if (usuario.rol !== "CLIENTE") {
                window.location.replace(rutaPorRol(usuario));
                return;
            }
            const nombre = usuario.nombre?.trim() || "estudiante";
            const firstName = document.getElementById("client-name");
            if (firstName) firstName.textContent = nombre.split(/\s+/)[0];
            const fullName = document.getElementById("client-full-name");
            if (fullName) fullName.textContent = nombre;
            document.getElementById("client-avatar").textContent = nombre.charAt(0).toUpperCase();
            const learningAvatar = document.getElementById("learning-avatar");
            if (learningAvatar) learningAvatar.textContent = nombre.charAt(0).toUpperCase();
            const email = document.getElementById("client-email");
            if (email) email.textContent = usuario.correo;
            const settingsName = document.getElementById("settings-name");
            if (settingsName) settingsName.value = nombre;
            const settingsEmail = document.getElementById("settings-email");
            if (settingsEmail) settingsEmail.value = usuario.correo || "";
            const settingsTimezone = document.getElementById("settings-timezone");
            if (settingsTimezone) settingsTimezone.options[0].textContent =
                Intl.DateTimeFormat().resolvedOptions().timeZone || "Zona local del navegador";
            const provider = document.getElementById("client-provider");
            if (provider) provider.textContent =
                usuario.proveedor === "google" ? "Google" : "Correo y contraseña";
            if (document.getElementById("profile-dui-row")) mostrarDuiEnPerfil(usuario);
            document.getElementById("client-loading").hidden = true;
            document.getElementById("client-content").hidden = false;
            if (document.body.dataset.page === "cliente" && (window.location.hash || new URLSearchParams(window.location.search).has("q"))) {
                const section = document.getElementById(window.location.hash.slice(1) || "explorar");
                if (section) requestAnimationFrame(() => section.scrollIntoView());
            }
        } catch (error) {
            if (error.status === 401 || error.status === 403) {
                cerrarSesion();
                return;
            }
            document.getElementById("client-loading").hidden = true;
            document.getElementById("client-error").textContent =
                "No pudimos cargar tu espacio. Actualiza la página para intentar nuevamente.";
        }
    }

    function mostrarDuiEnPerfil(usuario) {
        const fila = document.getElementById("profile-dui-row");
        const formulario = document.getElementById("complete-dui-form");
        fila.hidden = !usuario.dui;
        formulario.hidden = Boolean(usuario.dui) || usuario.rol !== "CLIENTE";
        if (usuario.dui) document.getElementById("profile-dui").textContent = usuario.dui;
    }

    function prepararCompletarDui() {
        const formulario = document.getElementById("complete-dui-form");
        formulario.addEventListener("submit", async (evento) => {
            evento.preventDefault();
            limpiarErrores(formulario);
            if (!formulario.reportValidity()) return;
            const boton = formulario.querySelector("button[type='submit']");
            cambiarCarga(boton, true);
            try {
                const {respuesta, contenido} = await enviar(
                    "/api/v1/auth/me/dui", {dui: formulario.dui.value}
                );
                if (!respuesta.ok) {
                    if (contenido.mensaje) {
                        document.getElementById("form-status").textContent = contenido.mensaje;
                    } else {
                        mostrarErrores(formulario, contenido);
                    }
                    return;
                }
                mostrarDuiEnPerfil(contenido);
            } catch (_error) {
                document.getElementById("form-status").textContent =
                    "No pudimos guardar tu DUI. Revisa tu conexión e intenta nuevamente.";
            } finally {
                cambiarCarga(boton, false);
            }
        });
    }

    limpiarAlmacenamientoAnterior();
    activarPasswordToggle();
    const pagina = document.body.dataset.page;
    if (pagina === "login") prepararLogin();
    if (pagina === "registro") prepararRegistro();
    if (pagina === "callback") procesarCallback();
    if (pagina === "perfil") {
        prepararCompletarDui();
        cargarPerfil();
    }
    if (pagina === "cliente") {
        prepararCompletarDui();
        cargarPanelCliente();
    }
    if (pagina === "aprendizaje" || pagina === "configuracion") cargarPanelCliente();
})();
