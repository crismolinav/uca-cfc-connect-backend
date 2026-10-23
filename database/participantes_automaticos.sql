-- Ejecutar después de normalizar_clientes_carnet.sql. Repetible: no duplica participantes.
-- Conserva los participantes existentes y no modifica inscripciones ni carnets.
SET NAMES utf8mb4;
START TRANSACTION;
INSERT INTO participantes (id_cliente, nombre, apellido, correo, telefono)
SELECT c.id_cliente,
       COALESCE(NULLIF(TRIM(CONCAT_WS(' ', c.primer_nombre, c.segundo_nombre)), ''), c.nombre_original),
       TRIM(CONCAT_WS(' ', c.primer_apellido, c.segundo_apellido)),
       c.correo, c.telefono
FROM clientes c
WHERE NOT EXISTS (SELECT 1 FROM participantes p WHERE p.id_cliente = c.id_cliente);
COMMIT;
