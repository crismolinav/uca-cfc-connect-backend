-- Ejecutar una sola vez en uca_cfc_connect antes de iniciar esta versión.
ALTER TABLE cursos
    ADD COLUMN lo_que_aprenderas TEXT NULL,
    ADD COLUMN contenido_programa TEXT NULL,
    ADD COLUMN requisitos TEXT NULL,
    ADD COLUMN dirigido_a TEXT NULL,
    ADD COLUMN id_usuario_creador BIGINT NULL,
    ADD COLUMN fecha_creacion DATETIME NULL,
    ADD COLUMN fecha_actualizacion DATETIME NULL;

UPDATE cursos SET fecha_creacion = CURRENT_TIMESTAMP WHERE fecha_creacion IS NULL;
UPDATE cursos SET fecha_actualizacion = fecha_creacion WHERE fecha_actualizacion IS NULL;

ALTER TABLE cursos
    MODIFY COLUMN fecha_creacion DATETIME NOT NULL,
    MODIFY COLUMN fecha_actualizacion DATETIME NOT NULL,
    ADD CONSTRAINT fk_curso_usuario_creador FOREIGN KEY (id_usuario_creador) REFERENCES usuarios(id_usuario);

ALTER TABLE docentes
    ADD COLUMN descripcion TEXT NULL,
    ADD COLUMN foto_url VARCHAR(500) NULL,
    ADD COLUMN id_usuario BIGINT NULL,
    ADD CONSTRAINT uq_docente_usuario UNIQUE (id_usuario),
    ADD CONSTRAINT fk_docente_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario);

CREATE TABLE resenas_curso (
    id_resena BIGINT NOT NULL AUTO_INCREMENT,
    id_inscripcion BIGINT NOT NULL,
    id_docente BIGINT NULL,
    calificacion_curso INT NOT NULL,
    comentario_curso TEXT NOT NULL,
    calificacion_docente INT NULL,
    comentario_docente TEXT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_resena),
    CONSTRAINT uq_resena_inscripcion UNIQUE (id_inscripcion),
    CONSTRAINT fk_resena_inscripcion FOREIGN KEY (id_inscripcion) REFERENCES inscripciones(id_inscripcion),
    CONSTRAINT fk_resena_docente FOREIGN KEY (id_docente) REFERENCES docentes(id_docente),
    CONSTRAINT chk_resena_calificacion_curso CHECK (calificacion_curso BETWEEN 1 AND 5),
    CONSTRAINT chk_resena_calificacion_docente CHECK (calificacion_docente IS NULL OR calificacion_docente BETWEEN 1 AND 5)
);

INSERT INTO roles (nombre, descripcion)
SELECT 'DOCENTE', 'Docente asignado a cursos'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE UPPER(nombre) = 'DOCENTE');
