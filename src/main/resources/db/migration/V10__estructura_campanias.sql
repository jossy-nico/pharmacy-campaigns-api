ALTER TABLE public.campania
    ADD COLUMN codigo VARCHAR(100),
    ADD COLUMN anio INTEGER,
    ADD COLUMN mes INTEGER,
    ADD COLUMN fecha_limite_carga DATE,
    ADD COLUMN archivo_productos_id BIGINT;

ALTER TABLE public.campania
    ADD CONSTRAINT chk_campania_mes
        CHECK (mes IS NULL OR mes BETWEEN 1 AND 12);

ALTER TABLE public.campania
    ADD CONSTRAINT chk_campania_anio
        CHECK (anio IS NULL OR anio BETWEEN 2000 AND 2100);

ALTER TABLE public.campania
    ADD CONSTRAINT fk_campania_archivo_productos
        FOREIGN KEY (archivo_productos_id)
        REFERENCES public.archivo(id)
        ON DELETE RESTRICT;

CREATE UNIQUE INDEX uq_campania_codigo
    ON public.campania(codigo)
    WHERE codigo IS NOT NULL;

CREATE UNIQUE INDEX uq_campania_periodo
    ON public.campania(anio, mes)
    WHERE anio IS NOT NULL
      AND mes IS NOT NULL;

CREATE UNIQUE INDEX uq_campania_archivo_productos
    ON public.campania(archivo_productos_id)
    WHERE archivo_productos_id IS NOT NULL;


CREATE TABLE public.configuracion_sistema (

    id BIGSERIAL PRIMARY KEY,

    clave VARCHAR(100) NOT NULL UNIQUE,

    valor VARCHAR(500) NOT NULL,

    descripcion VARCHAR(500),

    fecha_modificacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);