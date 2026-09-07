-- ============================================================
-- V18 - Ajuste del modelo fotográfico
--
-- Modelo definitivo:
--
-- REFERENCIA_OFICIAL
--        ├── EVIDENCIA_ZONAL
--        └── EVIDENCIA_FARMACIA
--
-- También se generaliza el módulo de análisis para poder
-- analizar tanto fotografías zonales como de farmacia.
-- ============================================================


-- ============================================================
-- 1. EVIDENCIA_FOTOGRAFICA
-- ============================================================

-- Eliminar CHECK anteriores relacionados con:
-- - tipo de evidencia
-- - relación con referencia zonal
-- - resultado
DO $$
DECLARE
    c RECORD;
BEGIN
    FOR c IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'public.evidencia_fotografica'::regclass
          AND contype = 'c'
          AND (
              pg_get_constraintdef(oid) ILIKE '%tipo_evidencia%'
              OR pg_get_constraintdef(oid) ILIKE '%referencia_zonal_id%'
              OR pg_get_constraintdef(oid) ILIKE '%resultado%'
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE public.evidencia_fotografica DROP CONSTRAINT %I',
            c.conname
        );
    END LOOP;
END $$;


-- La antigua referencia zonal realmente corresponde
-- a la imagen oficial del planograma.
UPDATE public.evidencia_fotografica
SET tipo_evidencia = 'REFERENCIA_OFICIAL'
WHERE tipo_evidencia = 'REFERENCIA_ZONAL';


-- Renombrar la relación.
ALTER TABLE public.evidencia_fotografica
    RENAME COLUMN referencia_zonal_id
    TO referencia_oficial_id;


-- Tipos de fotografías permitidos.
ALTER TABLE public.evidencia_fotografica
ADD CONSTRAINT chk_evidencia_tipo_v18
CHECK (
    tipo_evidencia IN (
        'REFERENCIA_OFICIAL',
        'EVIDENCIA_ZONAL',
        'EVIDENCIA_FARMACIA'
    )
);


-- Relación correcta según el tipo.
--
-- REFERENCIA_OFICIAL:
--   no pertenece a una farmacia
--   no referencia otra fotografía
--
-- EVIDENCIA_ZONAL / EVIDENCIA_FARMACIA:
--   pertenece a una farmacia
--   debe apuntar a la referencia oficial
ALTER TABLE public.evidencia_fotografica
ADD CONSTRAINT chk_evidencia_relacion_v18
CHECK (
    (
        tipo_evidencia = 'REFERENCIA_OFICIAL'
        AND farmacia_id IS NULL
        AND referencia_oficial_id IS NULL
    )
    OR
    (
        tipo_evidencia IN (
            'EVIDENCIA_ZONAL',
            'EVIDENCIA_FARMACIA'
        )
        AND farmacia_id IS NOT NULL
        AND referencia_oficial_id IS NOT NULL
        AND referencia_oficial_id <> id
    )
);


-- Resultados posibles.
ALTER TABLE public.evidencia_fotografica
ADD CONSTRAINT chk_evidencia_resultado_v18
CHECK (
    resultado IS NULL
    OR resultado IN (
        'CUMPLE',
        'NO_CUMPLE',
        'REQUIERE_REVISION',
        'REQUIERE_NUEVA_FOTO'
    )
);


CREATE INDEX IF NOT EXISTS idx_evidencia_referencia_oficial
    ON public.evidencia_fotografica(referencia_oficial_id);


-- ============================================================
-- 2. ANALISIS_EVIDENCIA
-- ============================================================

-- Ya no analizaremos exclusivamente una foto de farmacia.
-- También podremos analizar una evidencia zonal.
ALTER TABLE public.analisis_evidencia
    RENAME COLUMN evidencia_farmacia_id
    TO evidencia_evaluada_id;


-- La comparación es siempre contra la referencia oficial.
ALTER TABLE public.analisis_evidencia
    RENAME COLUMN referencia_zonal_id
    TO referencia_oficial_id;


-- Nombres OCR genéricos.
ALTER TABLE public.analisis_evidencia
    RENAME COLUMN texto_ocr_zonal
    TO texto_ocr_referencia;


ALTER TABLE public.analisis_evidencia
    RENAME COLUMN texto_ocr_farmacia
    TO texto_ocr_evidencia;


CREATE INDEX IF NOT EXISTS idx_analisis_evidencia_evaluada
    ON public.analisis_evidencia(evidencia_evaluada_id);


CREATE INDEX IF NOT EXISTS idx_analisis_referencia_oficial
    ON public.analisis_evidencia(referencia_oficial_id);


-- ============================================================
-- 3. ANALISIS_PRODUCTO
-- ============================================================

-- Eliminar CHECK antiguo de estado_comparacion
-- antes de cambiar los nombres de los estados.
DO $$
DECLARE
    c RECORD;
BEGIN
    FOR c IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'public.analisis_producto'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid)
              ILIKE '%estado_comparacion%'
    LOOP
        EXECUTE format(
            'ALTER TABLE public.analisis_producto DROP CONSTRAINT %I',
            c.conname
        );
    END LOOP;
END $$;


-- Renombrar columnas específicas de farmacia
-- a nombres genéricos para cualquier evidencia.
ALTER TABLE public.analisis_producto
    RENAME COLUMN presente_farmacia
    TO presente_evidencia;


ALTER TABLE public.analisis_producto
    RENAME COLUMN confianza_farmacia
    TO confianza_evidencia;


-- Convertir valores antiguos si existieran.
UPDATE public.analisis_producto
SET estado_comparacion = 'FALTANTE_EVIDENCIA'
WHERE estado_comparacion = 'FALTANTE_FARMACIA';


UPDATE public.analisis_producto
SET estado_comparacion = 'ADICIONAL_EVIDENCIA'
WHERE estado_comparacion = 'ADICIONAL_FARMACIA';


ALTER TABLE public.analisis_producto
ADD CONSTRAINT chk_analisis_producto_estado_v18
CHECK (
    estado_comparacion IS NULL
    OR estado_comparacion IN (
        'COINCIDE',
        'FALTANTE_EVIDENCIA',
        'ADICIONAL_EVIDENCIA',
        'NO_CONCLUYENTE'
    )
);