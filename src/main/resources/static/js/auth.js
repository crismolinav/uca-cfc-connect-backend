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
        if (usuario?.rol === "DOCENTE") return "/docente/cursos.html";
        if (usuario?.rol === "RECEPCIONISTA") return "/recepcion/index.html";
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
        if (new URLSearchParams(window.location.search).get("cuenta") === "eliminada") {
            const estado = document.getElementById("form-status");
            estado.textContent = "Tu cuenta fue eliminada. Puedes crear una nueva si lo necesitas.";
            estado.classList.add("is-success");
            estado.setAttribute("role", "status");
        }
        formulario.addEventListener("submit", async (evento) => {
            evento.preventDefault();
            limpiarErrores(formulario);
            document.getElementById("form-status").classList.remove("is-success");
            document.getElementById("form-status").setAttribute("role", "alert");

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

    function prepararRecuperacion() {
        const formulario = document.getElementById("recovery-form");
        formulario.addEventListener("submit", async (evento) => {
            evento.preventDefault();
            limpiarErrores(formulario);
            document.getElementById("form-status").classList.remove("is-success");
            if (!formulario.reportValidity()) return;
            const boton = formulario.querySelector("button[type='submit']");
            cambiarCarga(boton, true);
            try {
                const {respuesta, contenido} = await enviar("/api/v1/auth/recuperar-password", {
                    correo: formulario.correo.value.trim()
                });
                if (!respuesta.ok) {
                    if (contenido.correo) mostrarErrores(formulario, contenido);
                    else document.getElementById("form-status").textContent = contenido.mensaje ||
                        "No se pudo enviar el enlace. Intenta más tarde.";
                    return;
                }
                const estado = document.getElementById("form-status");
                estado.classList.add("is-success");
                estado.textContent = "Si la cuenta existe, recibirás un correo con el enlace para cambiar la contraseña.";
                formulario.reset();
            } catch (_error) {
                document.getElementById("form-status").textContent =
                    "No se pudo conectar con el servidor. Intenta nuevamente.";
            } finally {
                cambiarCarga(boton, false);
            }
        });
    }

    function prepararRestablecimiento() {
        const formulario = document.getElementById("reset-form");
        const parametros = new URLSearchParams(window.location.hash.slice(1));
        const token = parametros.get("token");
        window.history.replaceState(null, document.title, window.location.pathname);
        if (!token) {
            document.getElementById("form-status").textContent =
                "Falta el enlace de recuperación. Solicita uno nuevo.";
            formulario.hidden = true;
            return;
        }
        formulario.addEventListener("submit", async (evento) => {
            evento.preventDefault();
            limpiarErrores(formulario);
            const estado = document.getElementById("form-status");
            estado.classList.remove("is-success");
            if (!formulario.reportValidity()) return;
            if (formulario.password.value !== formulario.confirmar.value) {
                formulario.confirmar.setAttribute("aria-invalid", "true");
                document.getElementById("confirmar-error").textContent = "Las contraseñas deben coincidir.";
                formulario.confirmar.focus();
                return;
            }
            const boton = formulario.querySelector("button[type='submit']");
            cambiarCarga(boton, true);
            try {
                const {respuesta, contenido} = await enviar("/api/v1/auth/restablecer-password", {
                    token, password: formulario.password.value
                });
                if (!respuesta.ok) {
                    if (contenido.password) mostrarErrores(formulario, contenido);
                    else estado.textContent = contenido.mensaje || "No se pudo cambiar la contraseña. Solicita un enlace nuevo.";
                    return;
                }
                formulario.hidden = true;
                estado.classList.add("is-success");
                estado.textContent = "Contraseña actualizada. Ya puedes iniciar sesión.";
            } catch (_error) {
                estado.textContent = "No se pudo conectar con el servidor. Intenta nuevamente.";
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
            document.getElementById("reception-entry").hidden = usuario.rol !== "RECEPCIONISTA";
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

    function prepararEliminacionCuenta() {
        const dialogo = document.getElementById("delete-account-dialog");
        const abrir = document.getElementById("delete-account-open");
        const cancelar = document.getElementById("delete-account-cancel");
        const formulario = document.getElementById("delete-account-form");
        const confirmacion = document.getElementById("delete-account-confirmation");
        const confirmar = document.getElementById("delete-account-confirm");
        const error = document.getElementById("delete-account-error");
        let enviando = false;

        abrir.addEventListener("click", () => dialogo.showModal());
        cancelar.addEventListener("click", () => dialogo.close());
        dialogo.addEventListener("cancel", (evento) => {
            if (enviando) evento.preventDefault();
        });
        dialogo.addEventListener("close", () => {
            formulario.reset();
            error.textContent = "";
            confirmacion.removeAttribute("aria-invalid");
            abrir.focus();
        });
        confirmacion.addEventListener("input", () => {
            error.textContent = "";
            confirmacion.removeAttribute("aria-invalid");
        });

        formulario.addEventListener("submit", async (evento) => {
            evento.preventDefault();
            const texto = confirmacion.value.trim().toUpperCase();
            if (texto !== "ELIMINAR") {
                error.textContent = "Escribe ELIMINAR para continuar.";
                confirmacion.setAttribute("aria-invalid", "true");
                confirmacion.focus();
                return;
            }

            enviando = true;
            confirmar.disabled = true;
            cancelar.disabled = true;
            try {
                const csrf = await obtenerCsrf();
                const respuesta = await fetch("/api/v1/auth/me", {
                    method: "DELETE",
                    credentials: "same-origin",
                    headers: {"Content-Type": "application/json", "X-CSRF-TOKEN": csrf},
                    body: JSON.stringify({confirmacion: texto})
                });
                if (respuesta.ok) {
                    limpiarAlmacenamientoAnterior();
                    window.location.replace("/auth/login.html?cuenta=eliminada");
                    return;
                }
                if (respuesta.status === 401 || respuesta.status === 403) {
                    window.location.replace("/auth/login.html");
                    return;
                }
                const contenido = await respuesta.json().catch(() => ({}));
                error.textContent = contenido.mensaje ||
                    "No pudimos eliminar la cuenta. Intenta de nuevo o contacta con soporte.";
            } catch (_error) {
                error.textContent = "No pudimos conectar con el servidor. Revisa tu conexión e intenta de nuevo.";
            } finally {
                enviando = false;
                confirmar.disabled = false;
                cancelar.disabled = false;
            }
        });
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
    if (pagina === "recuperar") prepararRecuperacion();
    if (pagina === "restablecer") prepararRestablecimiento();
    if (pagina === "callback") procesarCallback();
    if (pagina === "perfil") {
        prepararCompletarDui();
        cargarPerfil();
    }
    if (pagina === "cliente") {
        prepararCompletarDui();
        cargarPanelCliente();
    }
    if (pagina === "configuracion") prepararEliminacionCuenta();
    if (pagina === "aprendizaje" || pagina === "configuracion") cargarPanelCliente();
})();
