CREATE TABLE archivo_campania (
    id BIGSERIAL PRIMARY KEY,

    campania_id BIGINT NOT NULL,

    nombre_original VARCHAR(500) NOT NULL,
    nombre_almacenado VARCHAR(500) NOT NULL,

    tipo_archivo VARCHAR(50) NOT NULL,

    mime_type VARCHAR(150),

    tamano_bytes BIGINT NOT NULL,

    ruta_almacenamiento VARCHAR(1000) NOT NULL,

    fecha_carga TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    estado_procesamiento VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE',

    datos_adicionales JSONB,

    hash_sha256 VARCHAR(64),

    CONSTRAINT fk_archivo_campania
        FOREIGN KEY (campania_id)
        REFERENCES campania(id),

    CONSTRAINT chk_archivo_tipo
        CHECK (
            tipo_archivo IN (
                'PRODUCTOS',
                'REEMPLAZOS'
            )
        ),

    CONSTRAINT chk_archivo_estado
        CHECK (
            estado_procesamiento IN (
                'PENDIENTE',
                'PROCESADO',
                'PROCESADO_CON_ADVERTENCIAS',
                'ERROR'
            )
        ),

    CONSTRAINT chk_archivo_tamano
        CHECK (tamano_bytes >= 0)
);