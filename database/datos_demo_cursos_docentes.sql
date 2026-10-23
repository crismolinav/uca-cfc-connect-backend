-- Crea tres cursos para cada categoría existente y seis docentes de práctica.
-- Ejecutar después de datos_catalogo_cursos.sql y de las migraciones de cursos.
-- Se puede volver a ejecutar: omite docentes, cursos y asignaciones ya creados.

USE uca_cfc_connect;

INSERT INTO docentes (nombre, especialidad, correo, telefono, descripcion, foto_url)
SELECT nuevos.nombre, nuevos.especialidad, nuevos.correo, nuevos.telefono, nuevos.descripcion, NULL
FROM (
    SELECT 'Mariana López' AS nombre, 'Tecnología y desarrollo web' AS especialidad,
           'mariana.lopez@ejemplo.test' AS correo, '7000-1001' AS telefono,
           'Docente enfocada en programación práctica y desarrollo de aplicaciones.' AS descripcion
    UNION ALL SELECT 'Carlos Méndez', 'Administración y finanzas', 'carlos.mendez@ejemplo.test', '7000-1002',
           'Acompaña a equipos y emprendedores en gestión, presupuestos y toma de decisiones.'
    UNION ALL SELECT 'Ana Rivera', 'Idiomas y comunicación', 'ana.rivera@ejemplo.test', '7000-1003',
           'Imparte formación de idiomas con actividades enfocadas en situaciones cotidianas y laborales.'
    UNION ALL SELECT 'José Hernández', 'Diseño y creatividad', 'jose.hernandez@ejemplo.test', '7000-1004',
           'Comparte herramientas de diseño visual, ilustración y comunicación gráfica.'
    UNION ALL SELECT 'Gabriela Cruz', 'Educación y aprendizaje', 'gabriela.cruz@ejemplo.test', '7000-1005',
           'Diseña experiencias de aprendizaje claras, participativas y orientadas a la práctica.'
    UNION ALL SELECT 'Ricardo Flores', 'Emprendimiento y proyectos', 'ricardo.flores@ejemplo.test', '7000-1006',
           'Asesora proyectos desde la planificación hasta la presentación de resultados.'
) AS nuevos
WHERE NOT EXISTS (SELECT 1 FROM docentes d WHERE LOWER(d.correo) = LOWER(nuevos.correo));

DROP TEMPORARY TABLE IF EXISTS cursos_demo;
CREATE TEMPORARY TABLE cursos_demo AS
SELECT categoria.id_categoria,
       categoria.nombre AS categoria,
       plan.orden,
       CASE plan.orden
           WHEN 1 THEN CONCAT('Introducción a ', categoria.nombre)
           WHEN 2 THEN CONCAT('Herramientas prácticas de ', categoria.nombre)
           ELSE CONCAT('Proyecto aplicado de ', categoria.nombre)
       END AS titulo,
       ROW_NUMBER() OVER (ORDER BY categoria.id_categoria, plan.orden) AS orden_global
FROM categorias categoria
CROSS JOIN (SELECT 1 AS orden UNION ALL SELECT 2 UNION ALL SELECT 3) plan;

INSERT INTO cursos (
    titulo, descripcion, lo_que_aprenderas, contenido_programa, requisitos, dirigido_a,
    fecha_creacion, fecha_actualizacion, duracion_horas, cupo_maximo, costo,
    fecha_inicio, fecha_fin, horario, activo, id_categoria, id_modalidad
)
SELECT demo.titulo,
       CONCAT('Aprende fundamentos y herramientas prácticas de ', demo.categoria,
              ' mediante explicaciones claras, ejercicios guiados y un proyecto aplicado.'),
       CONCAT('Comprender conceptos básicos de ', demo.categoria, '.\n',
              'Aplicar herramientas y técnicas en ejercicios prácticos.\n',
              'Presentar un proyecto relacionado con ', demo.categoria, '.'),
       JSON_ARRAY(
           JSON_OBJECT('titulo', 'Conceptos esenciales', 'descripcion', CONCAT('Conoce los fundamentos de ', demo.categoria, ' y su uso en situaciones reales.'), 'recursos', JSON_ARRAY()),
           JSON_OBJECT('titulo', 'Práctica guiada', 'descripcion', CONCAT('Aplica herramientas de ', demo.categoria, ' mediante ejercicios paso a paso.'), 'recursos', JSON_ARRAY()),
           JSON_OBJECT('titulo', 'Proyecto final', 'descripcion', CONCAT('Desarrolla y presenta un proyecto breve relacionado con ', demo.categoria, '.'), 'recursos', JSON_ARRAY())
       ),
       'Interés por aprender. No se requiere experiencia previa.',
       CONCAT('Personas que desean iniciar o fortalecer sus conocimientos en ', demo.categoria, '.'),
       NOW(), NOW(), 12, 25, 30 + (demo.orden * 5),
       DATE_ADD(DATE_ADD(CURDATE(), INTERVAL MOD(7 - WEEKDAY(CURDATE()), 7) DAY),
                INTERVAL ((demo.orden - 1) * 7) DAY),
       DATE_ADD(DATE_ADD(CURDATE(), INTERVAL MOD(7 - WEEKDAY(CURDATE()), 7) DAY),
                INTERVAL (((demo.orden - 1) * 7) + 27) DAY),
       CASE demo.orden
           WHEN 1 THEN 'Lunes, Miércoles | 18:00-19:30'
           WHEN 2 THEN 'Martes, Jueves | 18:00-19:30'
           ELSE 'Sábado | 09:00-12:00'
       END,
       TRUE, demo.id_categoria,
       (SELECT id_modalidad FROM modalidades WHERE nombre = 'Virtual' LIMIT 1)
FROM cursos_demo demo
WHERE NOT EXISTS (
    SELECT 1 FROM cursos existente
    WHERE existente.id_categoria = demo.id_categoria AND existente.titulo = demo.titulo
);

INSERT INTO curso_docente (id_curso, id_docente)
SELECT curso.id_curso, docente.id_docente
FROM cursos_demo demo
JOIN cursos curso
  ON curso.id_categoria = demo.id_categoria AND curso.titulo = demo.titulo
JOIN docentes docente
  ON docente.correo = CASE MOD(demo.orden_global - 1, 6)
      WHEN 0 THEN 'mariana.lopez@ejemplo.test'
      WHEN 1 THEN 'carlos.mendez@ejemplo.test'
      WHEN 2 THEN 'ana.rivera@ejemplo.test'
      WHEN 3 THEN 'jose.hernandez@ejemplo.test'
      WHEN 4 THEN 'gabriela.cruz@ejemplo.test'
      ELSE 'ricardo.flores@ejemplo.test'
  END
WHERE NOT EXISTS (
    SELECT 1 FROM curso_docente asignacion
    WHERE asignacion.id_curso = curso.id_curso AND asignacion.id_docente = docente.id_docente
);

DROP TEMPORARY TABLE cursos_demo;
