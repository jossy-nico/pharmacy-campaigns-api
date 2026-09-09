/*
 * =========================================================
 * V19 - REFERENCIA OFICIAL OPCIONAL EN EVIDENCIAS
 *
 * Nuevo flujo:
 *
 * EVIDENCIA_ZONAL / EVIDENCIA_FARMACIA
 *        ↓
 * validación de calidad
 *        ↓
 * detección de productos
 *        ↓
 * PRODUCTOS_PAI
 *
 * La referencia oficial deja de ser obligatoria.
 *
 * Se conserva referencia_oficial_id como campo opcional
 * para compatibilidad, histórico o futuras validaciones.
 * =========================================================
 */


/*
 * Eliminar regla anterior de V18.
 */
ALTER TABLE evidencia_fotografica
DROP CONSTRAINT IF EXISTS chk_evidencia_relacion_v18;


/*
 * Nueva regla:
 *
 * REFERENCIA_OFICIAL
 * - no pertenece a farmacia
 * - no referencia otra evidencia
 *
 * EVIDENCIA_ZONAL / EVIDENCIA_FARMACIA
 * - debe pertenecer a una farmacia
 * - referencia_oficial_id puede ser NULL
 * - si existe, no puede apuntarse a sí misma
 */
ALTER TABLE evidencia_fotografica
ADD CONSTRAINT chk_evidencia_relacion_v19
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
        AND (
            referencia_oficial_id IS NULL
            OR referencia_oficial_id <> id
        )
    )
);