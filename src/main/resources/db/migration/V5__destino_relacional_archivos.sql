-- ============================================================
-- DESTINO RELACIONAL OPCIONAL PARA ARCHIVOS GENÉRICOS
--
-- Permite que una definición indique que, además de mantenerse
-- en registro_generico, sus datos deben sincronizarse con una
-- tabla de dominio existente.
-- ============================================================

ALTER TABLE archivo_definicion
ADD COLUMN tabla_destino VARCHAR(150);


-- FARMACIAS debe mantener sincronizada la tabla farmacia.
UPDATE archivo_definicion
SET tabla_destino = 'farmacia'
WHERE codigo = 'FARMACIAS';