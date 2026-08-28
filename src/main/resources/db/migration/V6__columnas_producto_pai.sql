-- ============================================================
-- COLUMNAS DEL ARCHIVO PRODUCTOS_PAI
-- Detalle de Exhibición PAI
-- ============================================================

INSERT INTO archivo_definicion_columna (
    definicion_id,
    orden,
    nombre_columna,
    campo_destino,
    tipo_dato,
    obligatoria,
    es_clave_negocio,
    aliases,
    activo
)
SELECT
    id,
    1,
    'Sku',
    'sku',
    'STRING',
    TRUE,
    TRUE,
    '["SKU", "sku"]'::jsonb,
    TRUE
FROM archivo_definicion
WHERE codigo = 'PRODUCTOS_PAI';


INSERT INTO archivo_definicion_columna (
    definicion_id,
    orden,
    nombre_columna,
    campo_destino,
    tipo_dato,
    obligatoria,
    es_clave_negocio,
    aliases,
    activo
)
SELECT
    id,
    2,
    'Descriptor',
    'nombre',
    'STRING',
    TRUE,
    FALSE,
    '["Descripción", "Descripcion"]'::jsonb,
    TRUE
FROM archivo_definicion
WHERE codigo = 'PRODUCTOS_PAI';


INSERT INTO archivo_definicion_columna (
    definicion_id,
    orden,
    nombre_columna,
    campo_destino,
    tipo_dato,
    obligatoria,
    es_clave_negocio,
    aliases,
    activo
)
SELECT
    id,
    3,
    'Marca',
    'marca',
    'STRING',
    TRUE,
    FALSE,
    '[]'::jsonb,
    TRUE
FROM archivo_definicion
WHERE codigo = 'PRODUCTOS_PAI';


INSERT INTO archivo_definicion_columna (
    definicion_id,
    orden,
    nombre_columna,
    campo_destino,
    tipo_dato,
    obligatoria,
    es_clave_negocio,
    aliases,
    activo
)
SELECT
    id,
    4,
    'Ubicación',
    'ubicacion',
    'STRING',
    TRUE,
    FALSE,
    '["Ubicacion"]'::jsonb,
    TRUE
FROM archivo_definicion
WHERE codigo = 'PRODUCTOS_PAI';


INSERT INTO archivo_definicion_columna (
    definicion_id,
    orden,
    nombre_columna,
    campo_destino,
    tipo_dato,
    obligatoria,
    es_clave_negocio,
    aliases,
    activo
)
SELECT
    id,
    5,
    'Tipo Exhibición',
    'tipo_exhibicion',
    'STRING',
    TRUE,
    FALSE,
    '["Tipo Exhibicion"]'::jsonb,
    TRUE
FROM archivo_definicion
WHERE codigo = 'PRODUCTOS_PAI';


INSERT INTO archivo_definicion_columna (
    definicion_id,
    orden,
    nombre_columna,
    campo_destino,
    tipo_dato,
    obligatoria,
    es_clave_negocio,
    aliases,
    activo
)
SELECT
    id,
    6,
    'Ranking Exhibición',
    'ranking_exhibicion',
    'STRING',
    FALSE,
    FALSE,
    '["Ranking Exhibicion"]'::jsonb,
    TRUE
FROM archivo_definicion
WHERE codigo = 'PRODUCTOS_PAI';