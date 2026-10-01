CREATE TABLE IF NOT EXISTS operacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tipo_operacion VARCHAR(3) NOT NULL,
    estado_actual VARCHAR(20) NOT NULL,
    motivo_actual VARCHAR(10),
    emisor_nombre VARCHAR(40) NOT NULL,
    emisor_institucion VARCHAR(3) NOT NULL,
    emisor_cuenta VARCHAR(18),
    emisor_sucursal VARCHAR(20),
    emisor_documento VARCHAR(30),
    receptor_nombre VARCHAR(40) NOT NULL,
    receptor_institucion VARCHAR(3) NOT NULL,
    receptor_cuenta VARCHAR(18) NOT NULL,
    importe_valor DECIMAL(12,2) NOT NULL,
    importe_divisa VARCHAR(3) NOT NULL,
    concepto VARCHAR(40) NOT NULL,
    folio_numerico BIGINT NOT NULL,
    referencia_seguimiento VARCHAR(30) NOT NULL,
    clave_idempotencia VARCHAR(64),
    fecha_registro DATETIME(6) NOT NULL,
    fecha_actualizacion DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_operacion_referencia UNIQUE (referencia_seguimiento),
    CONSTRAINT uk_operacion_clave UNIQUE (clave_idempotencia),
    INDEX idx_operacion_fecha (fecha_registro, id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transicion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operacion_id BIGINT NOT NULL,
    estado_origen VARCHAR(20),
    estado_destino VARCHAR(20) NOT NULL,
    motivo VARCHAR(10),
    fecha DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_transicion_operacion (operacion_id, fecha, id),
    CONSTRAINT fk_transicion_operacion FOREIGN KEY (operacion_id) REFERENCES operacion (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS clave_idempotencia (
    clave VARCHAR(64) NOT NULL,
    operacion_id BIGINT NOT NULL,
    hash_cuerpo CHAR(64) NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_expiracion DATETIME(6) NOT NULL,
    PRIMARY KEY (clave),
    CONSTRAINT uk_clave_operacion UNIQUE (operacion_id),
    CONSTRAINT fk_clave_operacion FOREIGN KEY (operacion_id) REFERENCES operacion (id)
) ENGINE=InnoDB;