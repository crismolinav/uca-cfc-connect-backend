-- Datos mínimos para utilizar el módulo de cursos.
-- El script es idempotente: puede ejecutarse más de una vez sin duplicar nombres.

USE uca_cfc_connect;

INSERT INTO categorias (nombre, descripcion)
SELECT 'Tecnología', 'Cursos de informática, datos y transformación digital'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Tecnología');

INSERT INTO categorias (nombre, descripcion)
SELECT 'Administración', 'Cursos de gestión, finanzas y desarrollo empresarial'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Administración');

INSERT INTO categorias (nombre, descripcion)
SELECT 'Idiomas', 'Cursos de idiomas para contextos profesionales'
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Idiomas');

INSERT INTO modalidades (nombre, descripcion)
SELECT 'Presencial', 'Sesiones impartidas en las instalaciones del centro'
WHERE NOT EXISTS (SELECT 1 FROM modalidades WHERE nombre = 'Presencial');

INSERT INTO modalidades (nombre, descripcion)
SELECT 'Virtual', 'Sesiones impartidas mediante una plataforma en línea'
WHERE NOT EXISTS (SELECT 1 FROM modalidades WHERE nombre = 'Virtual');

INSERT INTO modalidades (nombre, descripcion)
SELECT 'Híbrida', 'Combinación de sesiones presenciales y virtuales'
WHERE NOT EXISTS (SELECT 1 FROM modalidades WHERE nombre = 'Híbrida');
