-- ============================================================
-- V7 - LIMPIEZA DEL MOTOR ANTIGUO
-- Deja el motor de archivos/datasets desacoplado del negocio.
-- ============================================================


-- ------------------------------------------------------------
-- 1. ARCHIVO
-- Archivo pasa a representar solamente información genérica
-- del archivo, su almacenamiento y versionado.
-- ------------------------------------------------------------

ALTER TABLE public.archivo
    DROP COLUMN IF EXISTS campania_id,
    DROP COLUMN IF EXISTS farmacia_id,
    DROP COLUMN IF EXISTS exhibidor_id,
    DROP COLUMN IF EXISTS vista,
    DROP COLUMN IF EXISTS rol_imagen;


-- ------------------------------------------------------------
-- 2. TABLAS DEL MOTOR ANTIGUO
-- Ya fueron reemplazadas por snapshots en datos_dinamicos.*
-- ------------------------------------------------------------

DROP TABLE IF EXISTS public.archivo_registro;

DROP TABLE IF EXISTS public.registro_generico;

DROP TABLE IF EXISTS public.archivo_definicion_columna;

DROP TABLE IF EXISTS public.archivo_campania;


-- ------------------------------------------------------------
-- 3. MODELO ESPECÍFICO ANTIGUO DE PRODUCTOS PAI
--
-- Productos, ubicación/exhibición, ranking y reemplazos
-- vienen ahora desde el dataset dinámico.
-- ------------------------------------------------------------

DROP TABLE IF EXISTS public.reemplazo;

DROP TABLE IF EXISTS public.campania_productos;

DROP TABLE IF EXISTS public.producto;

DROP TABLE IF EXISTS public.exhibidor;


-- ------------------------------------------------------------
-- 4. SIMPLIFICAR CATÁLOGO DE DATASETS
-- La estructura se detecta automáticamente.
-- ------------------------------------------------------------

ALTER TABLE public.archivo_definicion
    DROP COLUMN IF EXISTS fila_encabezado,
    DROP COLUMN IF EXISTS modo_sincronizacion,
    DROP COLUMN IF EXISTS tabla_destino,
    DROP COLUMN IF EXISTS requiere_campania,
    DROP COLUMN IF EXISTS requiere_farmacia,
    DROP COLUMN IF EXISTS requiere_exhibidor,
    DROP COLUMN IF EXISTS conservar_columnas_desconocidas;


-- ------------------------------------------------------------
-- 5. FARMACIA
-- Farmacia permanece como entidad de negocio.
-- Reemplazamos el antiguo boolean activo por un estado
-- extensible para el futuro CRUD.
-- ------------------------------------------------------------

ALTER TABLE public.farmacia
    ADD COLUMN IF NOT EXISTS estado VARCHAR(50);

UPDATE public.farmacia
SET estado =
    CASE
        WHEN activo = FALSE THEN 'INACTIVA'
        ELSE 'ACTIVA'
    END
WHERE estado IS NULL;

ALTER TABLE public.farmacia
    ALTER COLUMN estado SET DEFAULT 'ACTIVA';

ALTER TABLE public.farmacia
    ALTER COLUMN estado SET NOT NULL;

ALTER TABLE public.farmacia
    DROP COLUMN IF EXISTS activo;