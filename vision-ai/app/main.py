from fastapi import FastAPI, File, UploadFile

app = FastAPI(
    title="Farmacias Ahumada Vision AI",
    description="Microservicio de visión artificial para análisis de evidencias fotográficas.",
    version="1.0.0",
)


@app.get("/")
def inicio():
    return {
        "servicio": "Farmacias Ahumada Vision AI",
        "estado": "ACTIVO"
    }


@app.get("/health")
def health():
    return {
        "status": "UP",
        "service": "vision-ai"
    }


@app.post("/api/vision/analyze")
async def analizar_imagen(
    imagen: UploadFile = File(...)
):
    contenido = await imagen.read()

    return {
        "archivo": imagen.filename,
        "contentType": imagen.content_type,
        "tamanoBytes": len(contenido),
        "recibido": True,
        "estado": "PENDIENTE_ANALISIS"
    }