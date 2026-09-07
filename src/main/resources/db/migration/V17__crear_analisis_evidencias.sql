CREATE TABLE public.analisis_evidencia (

    id BIGSERIAL PRIMARY KEY,

    evidencia_farmacia_id BIGINT NOT NULL,
    referencia_zonal_id BIGINT NOT NULL,

    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',

    resultado VARCHAR(30),

    resumen TEXT,

    texto_ocr_zonal TEXT,
    texto_ocr_farmacia TEXT,

    confianza NUMERIC(5,4),

    version_algoritmo VARCHAR(50),

    fecha_inicio TIMESTAMPTZ,
    fecha_fin TIMESTAMPTZ,

    error TEXT,

    CONSTRAINT fk_analisis_evidencia_farmacia
        FOREIGN KEY (evidencia_farmacia_id)
        REFERENCES public.evidencia_fotografica(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_analisis_referencia_zonal
        FOREIGN KEY (referencia_zonal_id)
        REFERENCES public.evidencia_fotografica(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_analisis_estado
        CHECK (
            estado IN (
                'PENDIENTE',
                'EN_PROCESO',
                'COMPLETADO',
                'ERROR'
            )
        ),

    CONSTRAINT chk_analisis_resultado
        CHECK (
            resultado IS NULL
            OR resultado IN (
                'CUMPLE',
                'NO_CUMPLE',
                'REQUIERE_REVISION',
                'REQUIERE_NUEVA_FOTO'
            )
        )
);

CREATE TABLE public.analisis_producto (

    id BIGSERIAL PRIMARY KEY,

    analisis_id BIGINT NOT NULL,

    sku VARCHAR(100),
    descriptor VARCHAR(500),
    marca VARCHAR(250),

    presente_referencia BOOLEAN NOT NULL DEFAULT FALSE,
    presente_farmacia BOOLEAN NOT NULL DEFAULT FALSE,

    confianza_referencia NUMERIC(5,4),
    confianza_farmacia NUMERIC(5,4),

    estado_comparacion VARCHAR(40) NOT NULL,

    CONSTRAINT fk_analisis_producto
        FOREIGN KEY (analisis_id)
        REFERENCES public.analisis_evidencia(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_estado_comparacion
        CHECK (
            estado_comparacion IN (
                'COINCIDE',
                'FALTANTE_FARMACIA',
                'ADICIONAL_FARMACIA',
                'NO_CONCLUYENTE'
            )
        )
);

CREATE INDEX idx_analisis_evidencia_farmacia
    ON public.analisis_evidencia(evidencia_farmacia_id);

CREATE INDEX idx_analisis_referencia_zonal
    ON public.analisis_evidencia(referencia_zonal_id);

CREATE INDEX idx_analisis_producto_analisis
    ON public.analisis_producto(analisis_id);