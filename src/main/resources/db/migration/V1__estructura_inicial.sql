create table campania(
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255),
    fecha_inicio DATE,
    fecha_fin DATE,
    estado VARCHAR(50)
);

create table producto(
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255),
    marca VARCHAR(255),
    palabras_clave VARCHAR(1000)
);

create table campania_productos(
    id BIGSERIAL PRIMARY KEY,
    campania_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,

    CONSTRAINT fk_campania_productos_campania
    FOREIGN KEY (campania_id)
    REFERENCES campania(id),

    CONSTRAINT fk_campania_productos_producto
    FOREIGN KEY (producto_id)
    REFERENCES producto(id),

    CONSTRAINT uk_campania_producto
    UNIQUE (campania_id, producto_id)
);

create table reemplazo(
    id BIGSERIAL PRIMARY KEY,
    campania_id BIGINT NOT NULL,
    producto_principal_id BIGINT NOT NULL,
    producto_reemplazo_id BIGINT NOT NULL,
    prioridad INTEGER NOT NULL CHECK (prioridad > 0),

    CONSTRAINT fk_reemplazo_campania
    FOREIGN KEY (campania_id)
    REFERENCES campania(id),

    CONSTRAINT fk_reemplazo_producto_principal
    FOREIGN KEY (producto_principal_id)
    REFERENCES producto(id),

    CONSTRAINT fk_reemplazo_producto_reemplazo
    FOREIGN KEY (producto_reemplazo_id)
    REFERENCES producto(id),

    CONSTRAINT uk_reemplazo_producto
    UNIQUE (campania_id,
    producto_principal_id,
    producto_reemplazo_id),

    CONSTRAINT uk_reemplazo_prioridad
    UNIQUE (
        campania_id,
        producto_principal_id,
        prioridad
    )


);