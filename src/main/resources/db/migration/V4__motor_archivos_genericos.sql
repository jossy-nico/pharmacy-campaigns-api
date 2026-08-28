-- ============================================================
-- 1. DEFINICIÓN DE TIPOS DE ARCHIVO
-- Permite agregar nuevos tipos en el futuro sin modificar Java
-- ============================================================

CREATE TABLE archivo_definicion (
    id BIGSERIAL PRIMARY KEY,

    codigo VARCHAR(100) NOT NULL,
    nombre VARCHAR(255) NOT NULL,

    categoria VARCHAR(50) NOT NULL,

    formatos_permitidos JSONB NOT NULL DEFAULT '[]'::jsonb,

    fila_encabezado INTEGER,

    modo_sincronizacion VARCHAR(50) NOT NULL DEFAULT 'VERSIONADO',

    requiere_campania BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_farmacia BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_exhibidor BOOLEAN NOT NULL DEFAULT FALSE,

    conservar_columnas_desconocidas BOOLEAN NOT NULL DEFAULT TRUE,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_archivo_definicion_codigo
        UNIQUE (codigo),

    CONSTRAINT chk_archivo_definicion_fila
        CHECK (
            fila_encabezado IS NULL
            OR fila_encabezado >= 0
        )
);


-- ============================================================
-- 2. DEFINICIÓN DE COLUMNAS
-- Configura cómo leer cada Excel sin modificar el lector Java
-- ============================================================

CREATE TABLE archivo_definicion_columna (
    id BIGSERIAL PRIMARY KEY,

    definicion_id BIGINT NOT NULL,

    nombre_columna VARCHAR(255) NOT NULL,
    campo_destino VARCHAR(150) NOT NULL,

    tipo_dato VARCHAR(50) NOT NULL DEFAULT 'STRING',

    obligatoria BOOLEAN NOT NULL DEFAULT FALSE,

    es_clave_negocio BOOLEAN NOT NULL DEFAULT FALSE,

    aliases JSONB NOT NULL DEFAULT '[]'::jsonb,

    orden INTEGER,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_archivo_columna_definicion
        FOREIGN KEY (definicion_id)
        REFERENCES archivo_definicion(id),

    CONSTRAINT uk_archivo_columna_destino
        UNIQUE (
            definicion_id,
            campo_destino
        )
);


-- ============================================================
-- 3. ARCHIVO GENÉRICO
-- Excel, PDF, imágenes zonales, imágenes farmacia, etc.
-- ============================================================

CREATE TABLE archivo (
    id BIGSERIAL PRIMARY KEY,

    definicion_id BIGINT NOT NULL,

    grupo_version VARCHAR(500) NOT NULL,

    version INTEGER NOT NULL DEFAULT 1,

    estado_archivo VARCHAR(50) NOT NULL DEFAULT 'ACTIVO',

    estado_procesamiento VARCHAR(50)
        NOT NULL DEFAULT 'PENDIENTE',

    nombre_original VARCHAR(500) NOT NULL,

    nombre_almacenado VARCHAR(500) NOT NULL,

    mime_type VARCHAR(200),

    extension VARCHAR(30),

    tamano_bytes BIGINT NOT NULL,

    ruta_almacenamiento VARCHAR(1000) NOT NULL,

    hash_sha256 VARCHAR(64),

    -- Contexto opcional
    campania_id BIGINT,
    farmacia_id BIGINT,
    exhibidor_id BIGINT,

    -- Para imágenes
    vista VARCHAR(100),
    rol_imagen VARCHAR(100),

    -- Para futuras integraciones, por ejemplo Frogmi
    origen VARCHAR(100) NOT NULL DEFAULT 'CARGA_WEB',

    id_externo VARCHAR(255),

    fecha_captura TIMESTAMP,

    fecha_carga TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    fecha_modificacion TIMESTAMP,

    fecha_eliminacion TIMESTAMP,

    datos_adicionales JSONB NOT NULL DEFAULT '{}'::jsonb,

    datos_origen JSONB NOT NULL DEFAULT '{}'::jsonb,

    archivo_anterior_id BIGINT,

    CONSTRAINT fk_archivo_definicion
        FOREIGN KEY (definicion_id)
        REFERENCES archivo_definicion(id),

    CONSTRAINT fk_archivo_campania_generico
        FOREIGN KEY (campania_id)
        REFERENCES campania(id),

    CONSTRAINT fk_archivo_farmacia
        FOREIGN KEY (farmacia_id)
        REFERENCES farmacia(id),

    CONSTRAINT fk_archivo_exhibidor
        FOREIGN KEY (exhibidor_id)
        REFERENCES exhibidor(id),

    CONSTRAINT fk_archivo_anterior
        FOREIGN KEY (archivo_anterior_id)
        REFERENCES archivo(id),

    CONSTRAINT chk_archivo_version
        CHECK (version > 0),

    CONSTRAINT chk_archivo_tamano_generico
        CHECK (tamano_bytes >= 0),

    CONSTRAINT uk_archivo_grupo_version
        UNIQUE (
            grupo_version,
            version
        )
);


-- Solo puede existir una versión ACTIVA por grupo
CREATE UNIQUE INDEX uk_archivo_grupo_activo
ON archivo (grupo_version)
WHERE estado_archivo = 'ACTIVO';


-- Evita importar dos veces la misma evidencia externa
-- por ejemplo una misma foto proveniente de Frogmi
CREATE UNIQUE INDEX uk_archivo_origen_externo
ON archivo (origen, id_externo)
WHERE id_externo IS NOT NULL;


CREATE INDEX idx_archivo_definicion
ON archivo (definicion_id);

CREATE INDEX idx_archivo_campania
ON archivo (campania_id);

CREATE INDEX idx_archivo_farmacia
ON archivo (farmacia_id);

CREATE INDEX idx_archivo_exhibidor
ON archivo (exhibidor_id);


-- ============================================================
-- 4. SNAPSHOT DE CADA FILA DEL ARCHIVO
-- Conserva exactamente lo que se leyó en cada versión
-- ============================================================

CREATE TABLE archivo_registro (
    id BIGSERIAL PRIMARY KEY,

    archivo_id BIGINT NOT NULL,

    numero_fila INTEGER NOT NULL,

    clave_negocio VARCHAR(500),

    datos JSONB NOT NULL DEFAULT '{}'::jsonb,

    datos_adicionales JSONB NOT NULL DEFAULT '{}'::jsonb,

    valido BOOLEAN NOT NULL DEFAULT TRUE,

    errores JSONB NOT NULL DEFAULT '[]'::jsonb,

    CONSTRAINT fk_archivo_registro_archivo
        FOREIGN KEY (archivo_id)
        REFERENCES archivo(id),

    CONSTRAINT uk_archivo_numero_fila
        UNIQUE (
            archivo_id,
            numero_fila
        )
);


-- ============================================================
-- 5. ESTADO ACTUAL GENÉRICO
-- Permite sincronizar cualquier Excel sin crear tablas nuevas
-- ============================================================

CREATE TABLE registro_generico (
    id BIGSERIAL PRIMARY KEY,

    definicion_id BIGINT NOT NULL,

    clave_negocio VARCHAR(500) NOT NULL,

    datos JSONB NOT NULL DEFAULT '{}'::jsonb,

    datos_adicionales JSONB NOT NULL DEFAULT '{}'::jsonb,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    archivo_origen_id BIGINT,

    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_registro_generico_definicion
        FOREIGN KEY (definicion_id)
        REFERENCES archivo_definicion(id),

    CONSTRAINT fk_registro_generico_archivo
        FOREIGN KEY (archivo_origen_id)
        REFERENCES archivo(id),

    CONSTRAINT uk_registro_generico
        UNIQUE (
            definicion_id,
            clave_negocio
        )
);


-- ============================================================
-- 6. FARMACIAS
-- Si desaparece de un Excel actualizado, no se elimina:
-- queda inactiva
-- ============================================================

ALTER TABLE farmacia
ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;


-- ============================================================
-- 7. TIPOS INICIALES DE ARCHIVO
-- Nuevos tipos futuros podrán registrarse sin modificar Java
-- ============================================================

INSERT INTO archivo_definicion (
    codigo,
    nombre,
    categoria,
    formatos_permitidos,
    fila_encabezado,
    modo_sincronizacion,
    requiere_campania,
    requiere_farmacia,
    requiere_exhibidor
)
VALUES
(
    'FARMACIAS',
    'Listado maestro de farmacias',
    'TABULAR',
    '["xlsx", "xls"]'::jsonb,
    0,
    'UPSERT_INACTIVAR',
    FALSE,
    FALSE,
    FALSE
),
(
    'PRODUCTOS_PAI',
    'Detalle de exhibición PAI',
    'TABULAR',
    '["xlsx", "xls"]'::jsonb,
    0,
    'VERSIONADO',
    TRUE,
    FALSE,
    FALSE
),
(
    'REEMPLAZOS',
    'Productos de reemplazo',
    'TABULAR',
    '["xlsx", "xls"]'::jsonb,
    0,
    'VERSIONADO',
    TRUE,
    FALSE,
    FALSE
),
(
    'FOTO_ZONAL',
    'Fotografía referencia zonal',
    'IMAGEN',
    '["jpg", "jpeg", "png", "webp"]'::jsonb,
    NULL,
    'VERSIONADO',
    TRUE,
    FALSE,
    TRUE
),
(
    'FOTO_FARMACIA',
    'Fotografía evidencia farmacia',
    'IMAGEN',
    '["jpg", "jpeg", "png", "webp"]'::jsonb,
    NULL,
    'VERSIONADO',
    TRUE,
    TRUE,
    TRUE
),
(
    'DOCUMENTO_REFERENCIA',
    'Documento de referencia',
    'DOCUMENTO',
    '["pdf", "pptx", "xlsx"]'::jsonb,
    NULL,
    'VERSIONADO',
    TRUE,
    FALSE,
    FALSE
);


-- ============================================================
-- 8. ESQUEMA DEL ARCHIVO FARMACIAS
-- ============================================================

INSERT INTO archivo_definicion_columna (
    definicion_id,
    nombre_columna,
    campo_destino,
    tipo_dato,
    obligatoria,
    es_clave_negocio,
    aliases,
    orden
)
SELECT
    d.id,
    v.nombre_columna,
    v.campo_destino,
    v.tipo_dato,
    v.obligatoria,
    v.es_clave_negocio,
    v.aliases,
    v.orden
FROM archivo_definicion d
CROSS JOIN (
    VALUES

    (
        'Farmacia',
        'codigo_farmacia',
        'STRING',
        TRUE,
        TRUE,
        '["Código Farmacia", "Codigo Farmacia"]'::jsonb,
        1
    ),

    (
        'Dirección',
        'direccion',
        'STRING',
        TRUE,
        FALSE,
        '["Direccion"]'::jsonb,
        2
    ),

    (
        'Subgerente',
        'subgerente',
        'STRING',
        TRUE,
        FALSE,
        '[]'::jsonb,
        3
    ),

    (
        'Administrador Zonal',
        'administrador_zonal',
        'STRING',
        TRUE,
        FALSE,
        '[]'::jsonb,
        4
    ),

    (
        'Mercado',
        'mercado',
        'STRING',
        TRUE,
        FALSE,
        '[]'::jsonb,
        5
    ),

    (
        'Ciudad',
        'ciudad',
        'STRING',
        TRUE,
        FALSE,
        '[]'::jsonb,
        6
    ),

    (
        'Comuna',
        'comuna',
        'STRING',
        TRUE,
        FALSE,
        '[]'::jsonb,
        7
    ),

    (
        'Región',
        'region',
        'STRING',
        TRUE,
        FALSE,
        '["Region"]'::jsonb,
        8
    ),

    (
        'Formato Comercial',
        'formato_comercial',
        'STRING',
        TRUE,
        FALSE,
        '[]'::jsonb,
        9
    ),

    (
        'Clasificación',
        'clasificacion',
        'STRING',
        TRUE,
        FALSE,
        '["Clasificacion"]'::jsonb,
        10
    )

) AS v (
    nombre_columna,
    campo_destino,
    tipo_dato,
    obligatoria,
    es_clave_negocio,
    aliases,
    orden
)

WHERE d.codigo = 'FARMACIAS';