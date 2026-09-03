CREATE TABLE public.evidencia_fotografica (

    id BIGSERIAL PRIMARY KEY,

    campania_id BIGINT NOT NULL,

    /*
     * Para REFERENCIA_ZONAL será NULL.
     * Para EVIDENCIA_FARMACIA será obligatorio.
     */
    farmacia_id BIGINT,

    tipo_evidencia VARCHAR(30) NOT NULL,

    /*
     * Solo se utiliza en fotografías de farmacia.
     * Indica exactamente contra qué fotografía
     * zonal debe compararse.
     */
    referencia_zonal_id BIGINT,

    exhibidor VARCHAR(150) NOT NULL,

    vista VARCHAR(100) NOT NULL,

    nombre_original VARCHAR(500) NOT NULL,

    nombre_almacenado VARCHAR(500) NOT NULL,

    mime_type VARCHAR(150),

    extension VARCHAR(20),

    tamano_bytes BIGINT,

    ruta_almacenamiento VARCHAR(1000) NOT NULL,

    hash_sha256 VARCHAR(64),

    origen VARCHAR(50) NOT NULL DEFAULT 'CARGA_WEB',

    external_id VARCHAR(255),

    estado VARCHAR(40) NOT NULL DEFAULT 'CARGADA',

    resultado VARCHAR(40),

    observacion VARCHAR(2000),

    fecha_carga TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    fecha_modificacion TIMESTAMPTZ,

    usuario_carga VARCHAR(150),

    CONSTRAINT fk_evidencia_campania
        FOREIGN KEY (campania_id)
        REFERENCES public.campania(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_evidencia_farmacia
        FOREIGN KEY (farmacia_id)
        REFERENCES public.farmacia(id)
        ON DELETE RESTRICT,

    /*
     * Relación entre fotografía farmacia
     * y fotografía de referencia zonal.
     */
    CONSTRAINT fk_evidencia_referencia_zonal
        FOREIGN KEY (referencia_zonal_id)
        REFERENCES public.evidencia_fotografica(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_evidencia_tipo
        CHECK (
            tipo_evidencia IN (
                'REFERENCIA_ZONAL',
                'EVIDENCIA_FARMACIA'
            )
        ),

    /*
     * Una referencia zonal no pertenece
     * a una farmacia específica.
     *
     * Una evidencia de farmacia sí debe
     * indicar farmacia y fotografía zonal.
     */
    CONSTRAINT chk_evidencia_relacion_tipo
        CHECK (
            (
                tipo_evidencia = 'REFERENCIA_ZONAL'
                AND farmacia_id IS NULL
                AND referencia_zonal_id IS NULL
            )
            OR
            (
                tipo_evidencia = 'EVIDENCIA_FARMACIA'
                AND farmacia_id IS NOT NULL
                AND referencia_zonal_id IS NOT NULL
            )
        ),

    CONSTRAINT chk_evidencia_estado
        CHECK (
            estado IN (
                'CARGADA',
                'EN_ANALISIS',
                'ANALIZADA',
                'REQUIERE_NUEVA_FOTO',
                'REVISADA'
            )
        ),

    CONSTRAINT chk_evidencia_resultado
        CHECK (
            resultado IS NULL
            OR resultado IN (
                'CUMPLE',
                'NO_CUMPLE',
                'REQUIERE_NUEVA_FOTO'
            )
        )
);

CREATE INDEX idx_evidencia_campania
    ON public.evidencia_fotografica(campania_id);

CREATE INDEX idx_evidencia_farmacia
    ON public.evidencia_fotografica(farmacia_id);

CREATE INDEX idx_evidencia_campania_farmacia
    ON public.evidencia_fotografica(
        campania_id,
        farmacia_id
    );

CREATE INDEX idx_evidencia_referencia_zonal
    ON public.evidencia_fotografica(referencia_zonal_id);

CREATE INDEX idx_evidencia_tipo
    ON public.evidencia_fotografica(tipo_evidencia);

CREATE INDEX idx_evidencia_estado
    ON public.evidencia_fotografica(estado);