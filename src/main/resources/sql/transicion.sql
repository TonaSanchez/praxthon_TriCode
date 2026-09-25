CREATE TABLE transicion (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    operacion_id    BIGINT      NOT NULL,
    estado_origen   VARCHAR(20) NULL,                  
    estado_destino  VARCHAR(20) NOT NULL,
    motivo          VARCHAR(10) NULL,                   
    fecha           DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    KEY idx_operacion (operacion_id),
    CONSTRAINT fk_transicion_operacion
        FOREIGN KEY (operacion_id) REFERENCES operacion(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;