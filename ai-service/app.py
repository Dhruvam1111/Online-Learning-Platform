"""
FastAPI Microservice for Socrates-OS AI Lecture Notes & Transcript Synthesis Engine.
Provides high-performance REST endpoints for Spring Boot backend integration.
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import uvicorn

from context_pyramid import ContextPyramid
from synthesizer import LectureSynthesizer, SynthesisResult

app = FastAPI(
    title="Socrates-OS AI Lecture Synthesis Engine",
    version="1.0.0",
    description="Multi-Tier Context Engineering & Academic Synthesis Service"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

synthesizer = LectureSynthesizer()


@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "socrates-os-ai-synthesizer",
        "engine": "python-fastapi",
        "version": "1.0.0"
    }


@app.post("/synthesize", response_model=SynthesisResult)
def run_synthesis(pyramid: ContextPyramid):
    try:
        result = synthesizer.synthesize(pyramid)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Synthesis failed: {str(e)}")


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
