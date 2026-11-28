"""
Flask Microservice for Socrates-OS AI Lecture Notes & Transcript Synthesis Engine.
Provides high-performance REST endpoints for Spring Boot backend integration.
"""

import os
from flask import Flask, request, jsonify
from context_pyramid import ContextPyramid
from synthesizer import LectureSynthesizer

app = Flask(__name__)
synthesizer = LectureSynthesizer()


@app.route("/health", methods=["GET"])
def health_check():
    return jsonify({
        "status": "UP",
        "service": "socrates-os-ai-synthesizer",
        "engine": "python-flask",
        "version": "1.0.0"
    }), 200


@app.route("/synthesize", methods=["POST"])
def run_synthesis():
    data = request.get_json(force=True, silent=True)
    if not data:
        return jsonify({"error": "Missing JSON request body"}), 400

    try:
        pyramid = ContextPyramid(**data)
        result = synthesizer.synthesize(pyramid)
        return jsonify(result.model_dump()), 200
    except Exception as e:
        return jsonify({"error": f"Synthesis failed: {str(e)}"}), 500


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    app.run(host="0.0.0.0", port=port)
