# Pharmacy Campaigns API

Backend desarrollado en **Java + Spring Boot** para la gestión de campañas, archivos maestros, versionado de información y procesamiento dinámico de archivos.

El proyecto está diseñado con una arquitectura escalable que permite incorporar nuevos tipos de archivos sin crear un `Service` específico para cada uno.

## Tecnologías

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Flyway
- Apache POI
- Hibernate
- Maven
- Lombok

## Arquitectura de carga de archivos

El backend utiliza un motor genérico para procesar diferentes tipos de archivos.

```text
ArchivoController
        ↓
ArchivoService
        ↓
ArchivoSchemaRegistry
        ↓
LectorTabularGenerico
        ↓
ArchivoRegistro
        ↓
SincronizadorGenerico
        ↓
RegistroGenerico
        ↓
SincronizadorDestinoGenerico
        ↓
Tabla de dominio