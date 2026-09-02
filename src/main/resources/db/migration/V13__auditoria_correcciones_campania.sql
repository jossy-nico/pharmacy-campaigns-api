CREATE TABLE public.campania_auditoria (

    id BIGSERIAL PRIMARY KEY,

    campania_id BIGINT NOT NULL,

    accion VARCHAR(50) NOT NULL,

    motivo VARCHAR(1000) NOT NULL,

    datos_anteriores JSONB NOT NULL,

    datos_nuevos JSONB NOT NULL,

    usuario VARCHAR(150),

    fecha_modificacion TIMESTAMPTZ
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_campania_auditoria_campania
        FOREIGN KEY (campania_id)
        REFERENCES public.campania(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_campania_auditoria_accion
        CHECK (
            accion IN (
                'CORRECCION'
            )
        )
);