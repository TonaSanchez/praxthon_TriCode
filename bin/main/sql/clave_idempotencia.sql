CREATE TABLE clave_idempotencia (
    clave               VARCHAR(64) NOT NULL,
    operacion_id        BIGINT      NOT NULL,
    hash_cuerpo         CHAR(64)    NOT NULL,           -- SHA-256 del body original
    fecha_creacion      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    fecha_expiracion    DATETIME(3) NOT NULL,           -- +24h según §5.5

    PRIMARY KEY (clave),
    UNIQUE KEY uk_operacion (operacion_id),
    CONSTRAINT fk_idem_operacion
        FOREIGN KEY (operacion_id) REFERENCES operacion(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;