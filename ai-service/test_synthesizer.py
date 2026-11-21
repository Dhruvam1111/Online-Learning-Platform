"""
Unit tests for Python AI Lecture Notes & Transcript Synthesis Engine.
"""

import unittest
from context_pyramid import CourseContext, CurriculumTrajectory, ContextPyramid
from synthesizer import LectureSynthesizer, SynthesisResult


class TestLectureSynthesizer(unittest.TestCase):

    def setUp(self):
        self.course = CourseContext(
            course_id=101,
            title="CS-401: Distributed Systems & Consensus Algorithms",
            department_subject="Computer Science",
            difficulty_level="Advanced Undergraduate",
            description="Foundations of consensus, state machine replication, and distributed storage.",
            learning_objectives=[
                "Analyze safety and liveness invariants in distributed protocols",
                "Trace leader election dynamics under network partition"
            ],
            syllabus_outline=[
                "Module 1: RPC and Concurrency",
                "Module 2: Paxos Consensus",
                "Module 3: Raft Protocol"
            ]
        )

        self.trajectory = CurriculumTrajectory(
            current_module="Module 3: Raft Protocol",
            current_section="Section 3.2: Leader Election",
            lesson_id=14,
            lesson_title="Raft Leader Election & Heartbeat Dynamics",
            lesson_index=14,
            total_lessons_in_course=42,
            estimated_duration_minutes=48,
            preceding_context=["Paxos limitations", "State machine replication basics"],
            succeeding_boundary=["Log Replication", "Safety Invariants"]
        )

        self.transcript = """
        00:04:15 - Professor: Let us start with leader election in Raft. Think of it like an auction.
        00:16:30 - Professor: A node must receive votes from a strict majority quorum Q = floor(N/2) + 1. Remember this for your midterm!
        00:28:45 - Professor: Disk fsync latency is our primary production throughput bottleneck.
        """

        self.notes = """
        # Raft Leader Election
        - Nodes start in Follower state
        - Randomized election timeout: 150ms - 300ms
        - Quorum condition: Q = floor(N/2) + 1
        - Heartbeats sent periodically by Leader
        """

        self.pyramid = ContextPyramid(
            course=self.course,
            trajectory=self.trajectory,
            transcript_text=self.transcript,
            extracted_notes_text=self.notes,
            question_count=3,
            include_flashcards=True
        )

    def test_xml_payload_assembly(self):
        payload = self.pyramid.build_xml_payload()
        self.assertIn("<course_context>", payload)
        self.assertIn("<course_title>CS-401: Distributed Systems & Consensus Algorithms</course_title>", payload)
        self.assertIn("<curriculum_trajectory>", payload)
        self.assertIn("<current_module>Module 3: Raft Protocol</current_module>", payload)
        self.assertIn("<lecture_transcript format=\"time-indexed\">", payload)
        self.assertIn("<instructor_lecture_notes>", payload)
        self.assertIn("<generation_tasks>", payload)

    def test_synthesis_execution(self):
        synthesizer = LectureSynthesizer()
        result = synthesizer.synthesize(self.pyramid)

        self.assertIsInstance(result, SynthesisResult)
        self.assertTrue(len(result.executiveSummary) > 50)
        self.assertIn("Comprehensive Study Guide", result.fullStudyGuideMarkdown)
        self.assertIn("Raft Leader Election", result.fullStudyGuideMarkdown)

        # Verify deliverables
        self.assertTrue(len(result.glossary) >= 2)
        for g in result.glossary:
            self.assertTrue(bool(g.term))
            self.assertTrue(bool(g.formalDefinition))
            self.assertTrue(bool(g.intuition))

        self.assertTrue(len(result.spokenNuances) >= 2)
        for n in result.spokenNuances:
            self.assertTrue(bool(n.topic))
            self.assertTrue(bool(n.verbalInsight))
            self.assertTrue(bool(n.timestamp))

        self.assertEqual(len(result.quizQuestions), 3)
        for q in result.quizQuestions:
            self.assertTrue(bool(q.questionText))
            self.assertTrue(len(q.options) >= 1)
            self.assertTrue(len(q.correctAnswers) >= 1)
            self.assertTrue(bool(q.explanation))
            self.assertIn(q.bloomLevel, ["Remember", "Understand", "Apply", "Analyze", "Evaluate"])

        self.assertTrue(len(result.flashcards) >= 2)
        for f in result.flashcards:
            self.assertTrue(bool(f.front))
            self.assertTrue(bool(f.back))

    def test_json_serialization(self):
        synthesizer = LectureSynthesizer()
        result = synthesizer.synthesize(self.pyramid)
        json_str = result.model_dump_json()
        self.assertIn("executiveSummary", json_str)
        self.assertIn("quizQuestions", json_str)
        reconstructed = SynthesisResult.model_validate_json(json_str)
        self.assertEqual(len(reconstructed.quizQuestions), len(result.quizQuestions))


if __name__ == "__main__":
    unittest.main()
