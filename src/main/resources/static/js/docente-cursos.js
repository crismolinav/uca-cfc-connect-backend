(() => {
    const lista = document.getElementById('lista-cursos');
    const estado = document.getElementById('estado-cursos');
    const dialogo = document.getElementById('editar-contenido');
    const formulario = document.getElementById('form-contenido');
    const editorModulos = document.getElementById('contenido-modulos-editor');
    const portadaArchivo = document.getElementById('portada-curso');
    const portadaPreview = document.getElementById('portada-vista-previa');
    const buscarCurso = document.getElementById('buscar-curso-docente');
    const filtroCategoria = document.getElementById('filtrar-categoria-docente');
    const filtroModalidad = document.getElementById('filtrar-modalidad-docente');
    const filtroEstado = document.getElementById('filtrar-estado-docente');
    const ordenCursos = document.getElementById('ordenar-cursos-docente');
    const paginacion = document.getElementById('paginacion-cursos-docente');
    const paginaCursos = document.getElementById('pagina-cursos-docente');
    const cursosPorPagina = 6;
    let cursoActual;
    let tokenCsrf;
    let modulos = [];
    let portadaPendiente = null;
    let cursosAsignados = [];
    let paginaActual = 0;

    async function solicitar(url, opciones = {}) {
        const headers = {...(opciones.headers || {})};
        if (!(opciones.body instanceof FormData)) headers['Content-Type'] = 'application/json';
        const respuesta = await fetch(url, {credentials: 'same-origin', ...opciones,
            headers});
        if (!respuesta.ok) throw new Error(respuesta.status === 403 ? 'Esta cuenta no está vinculada a un docente.' : 'No se pudo completar la solicitud.');
        return respuesta.status === 204 ? null : respuesta.json();
    }

    async function salir() {
        try {
            if (!tokenCsrf) tokenCsrf = (await solicitar('/api/v1/auth/csrf')).token;
            await solicitar('/api/v1/auth/logout', {method: 'POST', headers: {'X-CSRF-TOKEN': tokenCsrf}});
        } finally { location.assign('/'); }
    }

    function mostrar(cursos) {
        cursosAsignados = Array.isArray(cursos) ? cursos : [];
        llenarOpciones(filtroCategoria, cursosAsignados.map(curso => curso.categoria));
        llenarOpciones(filtroModalidad, cursosAsignados.map(curso => curso.modalidad));
        filtrarCursos();
    }

    function llenarOpciones(control, valores) {
        const seleccionado = control.value;
        control.replaceChildren(new Option('Todas', ''));
        [...new Set(valores.filter(Boolean))].sort((a, b) => a.localeCompare(b, 'es'))
            .forEach(valor => control.add(new Option(valor, valor)));
        control.value = seleccionado;
    }

    function filtrarCursos() {
        const texto = buscarCurso.value.trim().toLocaleLowerCase('es');
        const categoria = filtroCategoria.value;
        const modalidad = filtroModalidad.value;
        const estadoSeleccionado = filtroEstado.value;
        const cursos = cursosAsignados.filter(curso => {
            const coincideTexto = !texto || [curso.titulo, curso.descripcion, curso.categoria]
                .some(valor => String(valor || '').toLocaleLowerCase('es').includes(texto));
            const coincideEstado = !estadoSeleccionado || Boolean(curso.activo) === (estadoSeleccionado === 'activo');
            return coincideTexto && (!categoria || curso.categoria === categoria)
                && (!modalidad || curso.modalidad === modalidad) && coincideEstado;
        });
        cursos.sort((a, b) => {
            if (ordenCursos.value === 'fecha') return String(a.fechaInicio || '').localeCompare(String(b.fechaInicio || ''));
            if (ordenCursos.value === 'estudiantes') return Number(b.estudiantesInscritos || 0) - Number(a.estudiantesInscritos || 0);
            return String(a.titulo || '').localeCompare(String(b.titulo || ''), 'es');
        });
        lista.replaceChildren();
        paginacion.hidden = true;
        if (!cursosAsignados.length) { estado.textContent = 'No tienes cursos asignados a tu cuenta.'; return; }
        if (!cursos.length) {
            estado.textContent = 'No encontramos cursos con esos filtros. Prueba con otros criterios.';
            paginacion.hidden = true;
            return;
        }
        const totalPaginas = Math.ceil(cursos.length / cursosPorPagina);
        paginaActual = Math.min(paginaActual, totalPaginas - 1);
        const visibles = cursos.slice(paginaActual * cursosPorPagina, (paginaActual + 1) * cursosPorPagina);
        estado.textContent = 'Mostrando ' + (paginaActual * cursosPorPagina + 1) + '–'
            + (paginaActual * cursosPorPagina + visibles.length) + ' de ' + cursos.length
            + (cursos.length === 1 ? ' curso asignado.' : ' cursos asignados.');
        paginacion.hidden = totalPaginas <= 1;
        paginaCursos.textContent = 'Página ' + (paginaActual + 1) + ' de ' + totalPaginas;
        document.getElementById('cursos-anterior-docente').disabled = paginaActual === 0;
        document.getElementById('cursos-siguiente-docente').disabled = paginaActual >= totalPaginas - 1;
        visibles.forEach(curso => {
            const tarjeta = document.createElement('article'); tarjeta.className = 'docente-course-card';
            const portada = document.createElement('div'); portada.className = 'docente-course-cover';
            const marca = document.createElement('span'); marca.textContent = curso.categoria || 'Formación continua'; portada.append(marca);
            if (curso.imagenUrl) {
                const imagen = document.createElement('img'); imagen.src = curso.imagenUrl;
                imagen.alt = 'Portada de ' + curso.titulo; imagen.loading = 'lazy';
                imagen.addEventListener('error', () => imagen.remove()); portada.prepend(imagen);
            }
            const contenido = document.createElement('div'); contenido.className = 'docente-course-card-body';
            const encabezado = document.createElement('div'); encabezado.className = 'docente-course-card-heading';
            const nombre = document.createElement('h2'); nombre.textContent = curso.titulo;
            const etiqueta = document.createElement('span'); etiqueta.className = 'docente-course-label';
            etiqueta.textContent = curso.activo ? 'Activo' : 'Inactivo';
            etiqueta.classList.toggle('is-inactive', !curso.activo);
            encabezado.append(nombre, etiqueta);
            const resumen = document.createElement('p'); resumen.className = 'docente-course-summary';
            resumen.textContent = curso.descripcion || 'Completa la descripción para presentar mejor este curso a tus estudiantes.';
            const datos = document.createElement('div'); datos.className = 'docente-course-facts';
            const fecha = document.createElement('div'); fecha.className = 'docente-course-fact';
            const fechaLabel = document.createElement('span'); fechaLabel.textContent = 'Fechas';
            const fechaValor = document.createElement('strong'); fechaValor.textContent = `${curso.fechaInicio || 'Por definir'} – ${curso.fechaFin || 'Por definir'}`;
            fecha.append(fechaLabel, fechaValor);
            const modalidad = document.createElement('div'); modalidad.className = 'docente-course-fact';
            const modalidadLabel = document.createElement('span'); modalidadLabel.textContent = 'Modalidad';
            const modalidadValor = document.createElement('strong'); modalidadValor.textContent = curso.modalidad || 'Por definir';
            modalidad.append(modalidadLabel, modalidadValor);
            const alumnos = document.createElement('div'); alumnos.className = 'docente-course-fact';
            const alumnosLabel = document.createElement('span'); alumnosLabel.textContent = 'Estudiantes';
            const alumnosValor = document.createElement('strong'); alumnosValor.textContent = String(curso.estudiantesInscritos || 0);
            alumnos.append(alumnosLabel, alumnosValor); datos.append(fecha, modalidad, alumnos);
            const acciones = document.createElement('div'); acciones.className = 'docente-course-card-actions';
            const boton = document.createElement('button'); boton.type = 'button'; boton.className = 'docente-edit-button'; boton.textContent = 'Editar contenido';
            boton.addEventListener('click', () => editar(curso));
            acciones.append(boton); contenido.append(encabezado, resumen, datos, acciones);
            tarjeta.append(portada, contenido); lista.append(tarjeta);
        });
    }

    [filtroCategoria, filtroModalidad, filtroEstado, ordenCursos].forEach(control => control.addEventListener('change', () => {
        paginaActual = 0;
        filtrarCursos();
    }));
    buscarCurso.addEventListener('input', () => { paginaActual = 0; filtrarCursos(); });
    document.getElementById('cursos-anterior-docente').addEventListener('click', () => {
        if (paginaActual > 0) { paginaActual -= 1; filtrarCursos(); }
    });
    document.getElementById('cursos-siguiente-docente').addEventListener('click', () => {
        paginaActual += 1;
        filtrarCursos();
    });
    document.getElementById('limpiar-filtros-docente').addEventListener('click', () => {
        buscarCurso.value = '';
        filtroCategoria.value = '';
        filtroModalidad.value = '';
        filtroEstado.value = '';
        ordenCursos.value = 'titulo';
        paginaActual = 0;
        filtrarCursos();
        buscarCurso.focus();
    });

    function editar(curso) {
        cursoActual = curso;
        document.getElementById('titulo-edicion').textContent = curso.titulo;
        for (const campo of formulario.elements) if (campo.name) campo.value = curso[campo.name] || '';
        try {
            const datos = JSON.parse(curso.contenidoPrograma || '');
            modulos = Array.isArray(datos) ? datos.map(modulo => ({titulo: modulo.titulo || '', descripcion: modulo.descripcion || '',
                recursos: Array.isArray(modulo.recursos) ? modulo.recursos.map(recurso => ({...recurso})) : []})) : [];
        } catch (_) {
            modulos = (curso.contenidoPrograma || '').split(/\r?\n/).map(linea => linea.trim()).filter(Boolean)
                .map(titulo => ({titulo, descripcion: '', recursos: []}));
        }
        portadaPendiente = null;
        portadaArchivo.value = '';
        portadaPreview.hidden = !curso.imagenUrl;
        if (curso.imagenUrl) portadaPreview.src = curso.imagenUrl;
        mostrarModulos();
        document.getElementById('estado-guardado').textContent = '';
        dialogo.showModal();
        dialogo.scrollTop = 0;
        document.querySelector('.docente-editor-body').scrollTop = 0;
    }

    function mostrarModulos() {
        editorModulos.replaceChildren();
        modulos.forEach((modulo, indice) => {
            const tarjeta = document.createElement('section'); tarjeta.className = 'course-module-editor';
            const encabezado = document.createElement('div'); encabezado.className = 'course-module-editor-head';
            const titulo = document.createElement('strong'); titulo.textContent = 'Módulo ' + (indice + 1);
            const quitar = document.createElement('button'); quitar.type = 'button'; quitar.className = 'remove-course-module'; quitar.textContent = 'Quitar módulo';
            quitar.addEventListener('click', () => { modulos.splice(indice, 1); mostrarModulos(); });
            encabezado.append(titulo, quitar); tarjeta.append(encabezado);
            const tituloLabel = document.createElement('label'); tituloLabel.textContent = 'Título del módulo';
            const tituloInput = document.createElement('input'); tituloInput.maxLength = 180; tituloInput.required = true; tituloInput.value = modulo.titulo;
            tituloInput.addEventListener('input', () => { modulo.titulo = tituloInput.value; });
            const tituloField = document.createElement('div'); tituloField.className = 'field'; tituloField.append(tituloLabel, tituloInput); tarjeta.append(tituloField);
            const detalleLabel = document.createElement('label'); detalleLabel.textContent = 'Descripción y actividades';
            const detalleInput = document.createElement('textarea'); detalleInput.rows = 3; detalleInput.maxLength = 4000; detalleInput.value = modulo.descripcion;
            detalleInput.addEventListener('input', () => { modulo.descripcion = detalleInput.value; });
            const detalleField = document.createElement('div'); detalleField.className = 'field'; detalleField.append(detalleLabel, detalleInput); tarjeta.append(detalleField);
            const recursos = document.createElement('div'); recursos.className = 'course-module-resources';
            modulo.recursos.forEach((recurso, ri) => {
                const fila = document.createElement('div'); fila.className = 'course-module-resource';
                const texto = document.createElement('span'); texto.textContent = (recurso.tipo || 'RECURSO') + ' · ' + (recurso.nombre || recurso.url || 'Archivo pendiente');
                const quitarRecurso = document.createElement('button'); quitarRecurso.type = 'button'; quitarRecurso.textContent = 'Quitar';
                quitarRecurso.addEventListener('click', () => { modulo.recursos.splice(ri, 1); mostrarModulos(); });
                fila.append(texto, quitarRecurso); recursos.append(fila);
            });
            tarjeta.append(recursos);
            const controles = document.createElement('div'); controles.className = 'course-module-add-resource';
            const enlace = document.createElement('input'); enlace.type = 'url'; enlace.placeholder = 'https:// enlace al recurso';
            const nombre = document.createElement('input'); nombre.maxLength = 180; nombre.placeholder = 'Nombre del recurso';
            const tipo = document.createElement('select');
            [['ENLACE','Enlace'],['VIDEO','Video'],['IMAGEN','Imagen'],['PRESENTACION','Presentación'],['LECTURA','Lectura']]
                .forEach(([valor, etiqueta]) => tipo.add(new Option(etiqueta, valor)));
            const agregar = document.createElement('button'); agregar.type = 'button'; agregar.textContent = 'Agregar enlace';
            agregar.addEventListener('click', () => {
                try { const destino = new URL(enlace.value.trim()); if (!['http:', 'https:'].includes(destino.protocol)) throw new Error(); }
                catch (_) { document.getElementById('estado-guardado').textContent = 'Escribe un enlace válido que comience con http:// o https://'; enlace.focus(); return; }
                modulo.recursos.push({nombre: nombre.value.trim() || enlace.value.trim(), tipo: tipo.value, url: enlace.value.trim()});
                mostrarModulos();
            });
            controles.append(enlace, tipo, nombre, agregar); tarjeta.append(controles);
            const archivo = document.createElement('input'); archivo.type = 'file'; archivo.accept = '.png,.jpg,.jpeg,.webp,.gif,.mp4,.webm,.pdf,.ppt,.pptx,.doc,.docx,.xls,.xlsx,.txt';
            const adjuntar = document.createElement('button'); adjuntar.type = 'button'; adjuntar.className = 'course-module-upload'; adjuntar.textContent = 'Adjuntar archivo';
            adjuntar.addEventListener('click', () => {
                const file = archivo.files?.[0];
                if (!file) { archivo.click(); return; }
                if (file.size > 50 * 1024 * 1024) { document.getElementById('estado-guardado').textContent = 'El archivo no puede superar los 50 MB.'; return; }
                const extension = file.name.split('.').pop().toLowerCase();
                const tipos = {png:'IMAGEN',jpg:'IMAGEN',jpeg:'IMAGEN',webp:'IMAGEN',gif:'IMAGEN',mp4:'VIDEO',webm:'VIDEO',pdf:'PRESENTACION',ppt:'PRESENTACION',pptx:'PRESENTACION',doc:'DOCUMENTO',docx:'DOCUMENTO',xls:'DOCUMENTO',xlsx:'DOCUMENTO',txt:'DOCUMENTO'};
                if (!tipos[extension]) { document.getElementById('estado-guardado').textContent = 'Ese formato no está permitido.'; return; }
                modulo.recursos.push({nombre: file.name, tipo: tipos[extension], archivo: file, url: null}); mostrarModulos();
            });
            const fileRow = document.createElement('div'); fileRow.className = 'course-module-file-upload'; fileRow.append(archivo, adjuntar); tarjeta.append(fileRow);
            editorModulos.append(tarjeta);
        });
    }

    function serializarModulos() {
        return JSON.stringify(modulos.map(modulo => ({titulo: modulo.titulo.trim(), descripcion: modulo.descripcion.trim(),
            recursos: modulo.recursos.filter(recurso => recurso.url).map(({nombre, tipo, url}) => ({nombre, tipo, url}))})));
    }

    async function subirArchivo(ruta, archivo) {
        const formData = new FormData(); formData.append('archivo', archivo);
        return solicitar(ruta, {method: 'POST', body: formData, headers: {'X-CSRF-TOKEN': tokenCsrf}});
    }

    formulario.addEventListener('submit', async evento => {
        evento.preventDefault();
        if (!formulario.reportValidity()) return;
        const datos = Object.fromEntries(new FormData(formulario));
        if (modulos.length < 3) {
            document.getElementById('estado-guardado').textContent = 'Cada curso debe incluir al menos 3 módulos.';
            return;
        }
        if (modulos.some(modulo => !modulo.titulo.trim())) {
            document.getElementById('estado-guardado').textContent = 'Completa el título de cada módulo.';
            return;
        }
        datos.contenidoPrograma = serializarModulos();
        try {
            if (!tokenCsrf) tokenCsrf = (await solicitar('/api/v1/auth/csrf')).token;
            if (portadaPendiente) {
                cursoActual = await subirArchivo(`/api/v1/cursos/${cursoActual.idCurso}/portada`, portadaPendiente);
                portadaPendiente = null;
            }
            for (const modulo of modulos) for (const recurso of modulo.recursos) {
                if (!recurso.archivo) continue;
                const subido = await subirArchivo(`/api/v1/cursos/${cursoActual.idCurso}/recursos`, recurso.archivo);
                recurso.url = subido.url; recurso.nombre = subido.nombre; recurso.tipo = subido.tipo;
                delete recurso.archivo;
            }
            const actualizado = await solicitar(`/api/v1/cursos/${cursoActual.idCurso}/contenido`, {
                method: 'PUT', body: JSON.stringify({...datos, contenidoPrograma: serializarModulos()}),
                headers: {'X-CSRF-TOKEN': tokenCsrf}});
            cursoActual = actualizado;
            document.getElementById('estado-guardado').textContent = 'Cambios guardados.';
            mostrar(await solicitar('/api/v1/docente/cursos'));
        } catch (error) { document.getElementById('estado-guardado').textContent = error.message; }
    });
    document.getElementById('agregar-modulo').addEventListener('click', () => {
        modulos.push({titulo: '', descripcion: '', recursos: []}); mostrarModulos();
        editorModulos.lastElementChild?.querySelector('input')?.focus();
    });
    portadaArchivo.addEventListener('change', () => {
        const archivo = portadaArchivo.files?.[0]; if (!archivo) return;
        if (!archivo.type.startsWith('image/') || archivo.size > 50 * 1024 * 1024) {
            document.getElementById('estado-guardado').textContent = 'Selecciona una imagen válida de hasta 50 MB.';
            portadaArchivo.value = ''; return;
        }
        portadaPendiente = archivo; portadaPreview.src = URL.createObjectURL(archivo); portadaPreview.hidden = false;
    });
    document.getElementById('cerrar-edicion').addEventListener('click', () => dialogo.close());
    document.getElementById('cancelar-edicion').addEventListener('click', () => dialogo.close());
    document.getElementById('logout-button').addEventListener('click', salir);
    const sidebar = document.getElementById('sidebar');
    const main = document.getElementById('main-content');
    const menu = document.getElementById('menu-button');
    function mostrarMenu(abierto, restaurar = true) {
        sidebar.classList.toggle('is-open', abierto);
        main.inert = abierto;
        menu.setAttribute('aria-expanded', String(abierto));
        if (abierto) document.getElementById('sidebar-close').focus();
        else if (restaurar) menu.focus();
    }
    menu.addEventListener('click', () => mostrarMenu(!sidebar.classList.contains('is-open')));
    document.getElementById('sidebar-close').addEventListener('click', () => mostrarMenu(false));
    document.addEventListener('keydown', evento => {
        if (evento.key === 'Escape' && sidebar.classList.contains('is-open')) mostrarMenu(false);
    });
    solicitar('/api/v1/auth/me').then(usuario => {
        if (usuario.rol !== 'DOCENTE') { location.replace('/'); return; }
        document.getElementById('docente-nombre').textContent = usuario.nombre;
    }).catch(() => { estado.textContent = 'Inicia sesión con tu cuenta docente para consultar tus cursos.'; });
    solicitar('/api/v1/docente/cursos').then(mostrar).catch(error => { estado.textContent = error.message; });
})();
