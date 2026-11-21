"""
Unit tests for Flask Microservice app.py.
"""

import unittest
from app import app


class TestFlaskAiApp(unittest.TestCase):

    def setUp(self):
        self.client = app.test_client()

    def test_health_check(self):
        response = self.client.get("/health")
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertEqual(data.get("status"), "UP")
        self.assertEqual(data.get("service"), "socrates-os-ai-synthesizer")

    def test_synthesize_endpoint(self):
        payload = {
            "course": {
                "course_id": 1,
                "title": "Computer Networks",
                "department_subject": "Computer Science",
                "difficulty_level": "Undergraduate",
                "description": "Networking basics",
                "learning_objectives": ["Understand TCP/IP"],
                "syllabus_outline": ["Transport Layer"]
            },
            "trajectory": {
                "current_module": "Module 3",
                "current_section": "Section 3.1",
                "lesson_id": 10,
                "lesson_title": "TCP Congestion Control",
                "lesson_index": 3,
                "total_lessons_in_course": 15,
                "estimated_duration_minutes": 45,
                "preceding_context": ["UDP Protocol"],
                "succeeding_boundary": ["Routing Algorithms"]
            },
            "transcript_text": "00:05:00 - AIMD additive increase multiplicative decrease",
            "extracted_notes_text": "# TCP Congestion\n- Slow start threshold",
            "question_count": 2,
            "include_flashcards": True
        }

        response = self.client.post("/synthesize", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertIn("executiveSummary", data)
        self.assertIn("fullStudyGuideMarkdown", data)
        self.assertEqual(len(data.get("quizQuestions", [])), 2)
        self.assertTrue(len(data.get("flashcards", [])) >= 1)

    def test_synthesize_missing_body(self):
        response = self.client.post("/synthesize", data="", content_type="application/json")
        self.assertEqual(response.status_code, 400)


if __name__ == "__main__":
    unittest.main()
