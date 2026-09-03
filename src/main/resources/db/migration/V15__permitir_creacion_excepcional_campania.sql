ALTER TABLE public.campania_auditoria
    DROP CONSTRAINT IF EXISTS chk_campania_auditoria_accion;

ALTER TABLE public.campania_auditoria
    ADD CONSTRAINT chk_campania_auditoria_accion
        CHECK (
            accion IN (
                'CORRECCION',
                'EXTENSION',
                'CREACION_EXCEPCIONAL'
            )
        );