-- Ejecutar una sola vez sobre una copia respaldada de uca_cfc_connect (MySQL 8 / MariaDB).
-- Los nombres históricos se conservan sin adivinar apellidos. Recepción debe confirmarlos.
SET NAMES utf8mb4;
ALTER TABLE clientes
    CHANGE COLUMN nombre nombre_original VARCHAR(120) NULL,
    ADD COLUMN primer_nombre VARCHAR(60) NULL,
    ADD COLUMN segundo_nombre VARCHAR(60) NULL,
    ADD COLUMN primer_apellido VARCHAR(60) NULL,
    ADD COLUMN segundo_apellido VARCHAR(60) NULL,
    ADD COLUMN carnet VARCHAR(24) CHARACTER SET ascii COLLATE ascii_general_ci NULL,
    ADD COLUMN fecha_registro DATE NOT NULL DEFAULT (CURRENT_DATE),
    ADD CONSTRAINT uk_cliente_carnet UNIQUE (carnet);

-- Una ficha de cliente identifica a un estudiante. Detenerse y revisar si hay duplicados previos.
ALTER TABLE participantes
    MODIFY nombre VARCHAR(121) NOT NULL,
    MODIFY apellido VARCHAR(121) NOT NULL,
    ADD CONSTRAINT uk_participante_cliente UNIQUE (id_cliente);
ALTER TABLE usuarios MODIFY nombre VARCHAR(255) NOT NULL;

DELIMITER $$
CREATE TRIGGER clientes_carnet_inmutable BEFORE UPDATE ON clientes
FOR EACH ROW
BEGIN
    IF OLD.carnet IS NOT NULL AND NOT (BINARY OLD.carnet <=> BINARY NEW.carnet) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El carnet es permanente y no puede modificarse';
    END IF;
END$$
DELIMITER ;
