from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image, ImageStat, ImageFilter
import io


app = FastAPI(title="FarmChainX AI Grader")


app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8081"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def analyze_image(image_bytes: bytes):
    """
    Compute brightness, contrast, and sharpness to estimate quality.
    """
    img = Image.open(io.BytesIO(image_bytes)).convert("L") 

  
    stat = ImageStat.Stat(img)
    brightness = stat.mean[0] / 255.0
    contrast = stat.stddev[0] / 128.0

  
    edges = img.filter(ImageFilter.FIND_EDGES)
    sharpness = ImageStat.Stat(edges).mean[0] / 255.0

 
    score = (0.5 * brightness) + (0.3 * contrast) + (0.2 * sharpness)
    score = max(0, min(1, score))

    if score > 0.8:
        grade = "A"
    elif score > 0.6:
        grade = "B"
    elif score > 0.4:
        grade = "C"
    else:
        grade = "D"

    confidence = round(0.7 + 0.3 * abs(0.5 - score), 2)

    return {
        "grade": grade,
        "confidence": confidence
    }


@app.post("/predict")
async def predict(image: UploadFile = File(...)):
    """
    POST /predict
    Receives an image and returns grade + confidence.
    """
    contents = await image.read()
    result = analyze_image(contents)
    return JSONResponse(result)
