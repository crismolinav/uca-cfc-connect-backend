-- Ejecutar una sola vez en uca_cfc_connect antes de iniciar esta versión.
ALTER TABLE cursos
    ADD COLUMN imagen_url VARCHAR(500) NULL;
