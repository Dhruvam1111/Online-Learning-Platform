"""
Hierarchical 4-Tier Context Pyramid Builder for AI Lecture Synthesis.
Implements the architectural specification from AI_LECTURE_NOTES_AND_TRANSCRIPT_SYNTHESIS_DESIGN.md.
"""

from typing import List, Optional
from pydantic import BaseModel, Field


class CourseContext(BaseModel):
    course_id: int
    title: str
    department_subject: str = "Computer Science"
    difficulty_level: str = "Intermediate"
    description: str = ""
    learning_objectives: List[str] = Field(default_factory=list)
    syllabus_outline: List[str] = Field(default_factory=list)


class CurriculumTrajectory(BaseModel):
    current_module: str
    current_section: str
    lesson_id: int
    lesson_title: str
    lesson_index: int
    total_lessons_in_course: int
    estimated_duration_minutes: Optional[int] = 45
    preceding_context: List[str] = Field(default_factory=list)
    succeeding_boundary: List[str] = Field(default_factory=list)


class ContextPyramid(BaseModel):
    course: CourseContext
    trajectory: CurriculumTrajectory
    transcript_text: str = ""
    extracted_notes_text: str = ""
    custom_focus_instructions: Optional[str] = None
    question_count: int = 5
    include_flashcards: bool = True

    def build_xml_payload(self) -> str:
        """Assembles the delimited prompt payload for Socrates-OS."""
        clos_formatted = "\n    ".join(f"- {clo}" for clo in self.course.learning_objectives) if self.course.learning_objectives else "- General mastery of course curriculum"
        syllabus_formatted = "\n    ".join(f"{i+1}. {item}" for i, item in enumerate(self.course.syllabus_outline)) if self.course.syllabus_outline else "Standard modular syllabus"
        preceding_formatted = "\n    ".join(f"- {p}" for p in self.trajectory.preceding_context) if self.trajectory.preceding_context else "Foundational introduction"
        succeeding_formatted = "\n    ".join(f"- {s}" for s in self.trajectory.succeeding_boundary) if self.trajectory.succeeding_boundary else "Subsequent advanced topics"

        payload = f"""<course_context>
  <course_id>{self.course.course_id}</course_id>
  <course_title>{self.course.title}</course_title>
  <department_subject>{self.course.department_subject}</department_subject>
  <difficulty_level>{self.course.difficulty_level}</difficulty_level>
  <course_description>{self.course.description}</course_description>
  <course_learning_objectives>
    {clos_formatted}
  </course_learning_objectives>
  <syllabus_outline>
    {syllabus_formatted}
  </syllabus_outline>
</course_context>

<curriculum_trajectory>
  <current_module>{self.trajectory.current_module}</current_module>
  <current_section>{self.trajectory.current_section}</current_section>
  <current_lesson>
    <lesson_id>{self.trajectory.lesson_id}</lesson_id>
    <lesson_title>{self.trajectory.lesson_title}</lesson_title>
    <lesson_index>{self.trajectory.lesson_index}</lesson_index>
    <total_lessons_in_course>{self.trajectory.total_lessons_in_course}</total_lessons_in_course>
    <estimated_duration_minutes>{self.trajectory.estimated_duration_minutes}</estimated_duration_minutes>
  </current_lesson>
  <preceding_context>
    {preceding_formatted}
  </preceding_context>
  <succeeding_boundary>
    {succeeding_formatted}
  </succeeding_boundary>
</curriculum_trajectory>

<lecture_transcript format="time-indexed">
{self.transcript_text if self.transcript_text else "[No spoken audio transcript provided - synthesize from lecture notes and course trajectory]"}
</lecture_transcript>

<instructor_lecture_notes>
{self.extracted_notes_text if self.extracted_notes_text else "[No standalone notes uploaded - synthesize from spoken transcript and course syllabus]"}
</instructor_lecture_notes>

<generation_tasks>
  <task_1>Executive Summary & Core Pedagogical Thesis</task_1>
  <task_2>Comprehensive Lecture Study Guide (Thematic Modules with Transcript Timestamps)</task_2>
  <task_3>Technical Terminology & Conceptual Glossary</task_3>
  <task_4>Spoken Insights & Transcript-to-Notes Concordance Analysis</task_4>
  <task_5>Diagnostic Assessment Suite ({self.question_count} calibrated questions across Bloom's Taxonomy)</task_5>
  <task_6>Spaced-Repetition Active Recall Flashcards</task_6>
  {"<custom_focus>" + self.custom_focus_instructions + "</custom_focus>" if self.custom_focus_instructions else ""}
</generation_tasks>"""
        return payload
