ALTER TABLE public.archivo_definicion
    ADD COLUMN permitir_columnas_adicionales BOOLEAN
        NOT NULL DEFAULT TRUE;


CREATE TABLE public.archivo_definicion_regla (

    id BIGSERIAL PRIMARY KEY,

    archivo_definicion_id BIGINT NOT NULL,

    nombre_campo VARCHAR(150) NOT NULL,

    columna_obligatoria BOOLEAN NOT NULL DEFAULT TRUE,

    valor_obligatorio BOOLEAN NOT NULL DEFAULT TRUE,

    rechazar_placeholders BOOLEAN NOT NULL DEFAULT TRUE,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_regla_archivo_definicion
        FOREIGN KEY (archivo_definicion_id)
        REFERENCES public.archivo_definicion(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_regla_nombre_campo
        CHECK (
            nombre_campo ~ '^[a-z][a-z0-9_]*$'
        )
);


CREATE UNIQUE INDEX uq_regla_dataset_campo
    ON public.archivo_definicion_regla (
        archivo_definicion_id,
        LOWER(nombre_campo)
    );