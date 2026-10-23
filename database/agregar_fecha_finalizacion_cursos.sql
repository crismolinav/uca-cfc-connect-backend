-- Ejecutar una sola vez en uca_cfc_connect antes de iniciar esta versión.
ALTER TABLE inscripciones
    ADD COLUMN fecha_finalizacion DATE NULL;
