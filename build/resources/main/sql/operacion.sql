CREATE TABLE operacion (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    tipo_operacion      VARCHAR(3)   NOT NULL,         
    estado_actual       VARCHAR(20)  NOT NULL,         
    motivo_actual       VARCHAR(10)  NULL,              

    -- Emisor
    emisor_nombre           VARCHAR(40)  NOT NULL,
    emisor_institucion      VARCHAR(3)   NOT NULL,      
    emisor_cuenta           VARCHAR(18)  NULL,        
    emisor_sucursal         VARCHAR(20)  NULL,         
    emisor_documento        VARCHAR(30)  NULL,         

    -- Receptor
    receptor_nombre         VARCHAR(40)  NOT NULL,
    receptor_institucion    VARCHAR(3)   NOT NULL,
    receptor_cuenta         VARCHAR(18)  NOT NULL,

    -- Importe
    importe_valor           DECIMAL(12,2) NOT NULL,
    importe_divisa          VARCHAR(3)   NOT NULL DEFAULT 'MXN',

    -- Concepto / referencias
    concepto                VARCHAR(40)  NOT NULL,
    folio_numerico          BIGINT       NOT NULL,      
    referencia_seguimiento  VARCHAR(30)  NOT NULL,      

    -- Idempotencia
    clave_idempotencia      VARCHAR(64)  NULL,          

    -- Auditoría
    fecha_registro          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    fecha_actualizacion     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_referencia_seguimiento (referencia_seguimiento),
    UNIQUE KEY uk_clave_idempotencia (clave_idempotencia),
    KEY idx_estado (estado_actual),
    KEY idx_tipo (tipo_operacion),
    KEY idx_fecha (fecha_registro)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;