ALTER TABLE public.archivo_definicion
    ADD COLUMN politica_retencion VARCHAR(30)
        NOT NULL
        DEFAULT 'HISTORICO_COMPLETO',
    ADD COLUMN max_versiones_retenidas INTEGER;

ALTER TABLE public.archivo_definicion
    ADD CONSTRAINT chk_archivo_definicion_politica_retencion
    CHECK (
        politica_retencion IN (
            'HISTORICO_COMPLETO',
            'SOLO_ACTIVO',
            'ULTIMAS_N_VERSIONES'
        )
    );

ALTER TABLE public.archivo_definicion
    ADD CONSTRAINT chk_archivo_definicion_max_versiones
    CHECK (
        (
            politica_retencion = 'ULTIMAS_N_VERSIONES'
            AND max_versiones_retenidas IS NOT NULL
            AND max_versiones_retenidas > 0
        )
        OR
        (
            politica_retencion <> 'ULTIMAS_N_VERSIONES'
            AND max_versiones_retenidas IS NULL
        )
    );