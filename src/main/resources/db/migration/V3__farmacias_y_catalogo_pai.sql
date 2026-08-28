CREATE TABLE farmacia (
    id BIGSERIAL PRIMARY KEY,

    codigo_farmacia VARCHAR(20) NOT NULL,
    direccion VARCHAR(500) NOT NULL,
    subgerente VARCHAR(255) NOT NULL,
    administrador_zonal VARCHAR(255) NOT NULL,
    mercado VARCHAR(255) NOT NULL,
    ciudad VARCHAR(150) NOT NULL,
    comuna VARCHAR(150) NOT NULL,
    region VARCHAR(150) NOT NULL,
    formato_comercial VARCHAR(50) NOT NULL,
    clasificacion VARCHAR(50) NOT NULL,

    CONSTRAINT uk_farmacia_codigo
        UNIQUE (codigo_farmacia)
);


CREATE TABLE exhibidor (
    id BIGSERIAL PRIMARY KEY,

    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_exhibidor_codigo
        UNIQUE (codigo)
);


INSERT INTO exhibidor (codigo, nombre, tipo)
VALUES
    ('ACRILICO_1', 'Acrílico 1', 'ACRILICO'),
    ('ACRILICO_2', 'Acrílico 2', 'ACRILICO'),
    ('ACRILICO_3', 'Acrílico 3', 'ACRILICO'),
    ('ACRILICO_4', 'Acrílico 4', 'ACRILICO'),
    ('CILINDRO_SOLARES', 'Acrílico 5 - Cilindro DERMO', 'CILINDRO'),
    ('BAJO_MESON', 'Bajo Mesón ByCP', 'BAJO_MESON'),
    ('PAI_PEINETA', 'PAI Peineta', 'PEINETA'),
    ('EXHIBIDOR_LENTES', 'Exhibidor de Lentes', 'LENTES');


ALTER TABLE producto
    ADD COLUMN sku VARCHAR(50);

ALTER TABLE producto
    ADD CONSTRAINT uk_producto_sku
        UNIQUE (sku);


ALTER TABLE campania_productos
    ADD COLUMN exhibidor_id BIGINT;

ALTER TABLE campania_productos
    ADD COLUMN tipo_exhibicion VARCHAR(50);

ALTER TABLE campania_productos
    ADD COLUMN ranking_exhibicion INTEGER;


ALTER TABLE campania_productos
    ADD CONSTRAINT fk_campania_productos_exhibidor
        FOREIGN KEY (exhibidor_id)
        REFERENCES exhibidor(id);


ALTER TABLE campania_productos
    ADD CONSTRAINT chk_tipo_exhibicion
        CHECK (
            tipo_exhibicion IS NULL
            OR tipo_exhibicion IN (
                'REGULAR',
                'VARIEDAD',
                'SUPER PAI'
            )
        );


ALTER TABLE campania_productos
    ADD CONSTRAINT chk_ranking_exhibicion
        CHECK (
            ranking_exhibicion IS NULL
            OR ranking_exhibicion > 0
        );


ALTER TABLE campania_productos
    DROP CONSTRAINT uk_campania_producto;


ALTER TABLE campania_productos
    ADD CONSTRAINT uk_campania_producto_exhibidor
        UNIQUE (
            campania_id,
            producto_id,
            exhibidor_id
        );