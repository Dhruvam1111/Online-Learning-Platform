package com.olp.service;

import com.olp.dto.CourseProgressResponse;
import com.olp.controller.StudentPerformanceController;
import org.springframework.security.access.prepost.PreAuthorize;
import com.olp.dto.StudentPerformanceDtos.*;
import com.olp.entity.*;
import com.olp.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentPerformanceServiceTest {
    @Mock StudentPerformanceReadRepository read;
    @Mock UserRepository users;
    @Mock CourseProgressService progress;
    @Mock QuizAttemptAnswerRepository quizAnswers;
    @Mock ExamAttemptAnswerRepository examAnswers;
    @Mock ExamQuestionRepository examQuestions;
    @InjectMocks StudentPerformanceService service;

    private User student() {
        User u = new User(); u.setId(5); u.setRole("STUDENT"); return u;
    }
    private Course course() {
        Course c = new Course(); c.setId(22); c.setTitle("Networks"); return c;
    }
    private QuizAttempt quizAttempt(boolean published) {
        Quiz q = new Quiz(); q.setId(12); q.setCourse(course()); q.setTitle("Quiz A");
        q.setTotalMarks(new BigDecimal("20")); q.setResultsPublished(published);
        QuizAttempt a = new QuizAttempt(); a.setId(55); a.setQuiz(q);
        a.setSubmittedAt(LocalDateTime.of(2026,11,1,10,0));
        a.setObtainedMarks(new BigDecimal("15")); a.setIsPublished(published);
        return a;
    }
    private ExamAttempt examAttempt(boolean published) {
        Exam e = new Exam(); e.setId(13); e.setCourse(course()); e.setTitle("Final");
        e.setMaxMarks(new BigDecimal("100")); e.setResultsPublished(published);
        ExamAttempt a = new ExamAttempt(); a.setId(56); a.setExam(e);
        a.setSubmittedAt(LocalDateTime.of(2026,11,2,10,0));
        a.setObtainedMarks(new BigDecimal("80")); a.setIsPublished(published);
        return a;
    }
    private void emptyHistory() {
        when(read.exams(5)).thenReturn(List.of());
        when(read.quizzes(5)).thenReturn(List.of());
        when(read.assignments(5)).thenReturn(List.of());
    }

    @Test void controllerUsesCorrectLowercaseStudentRole() {
        PreAuthorize annotation = StudentPerformanceController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertEquals("hasRole('student')", annotation.value());
    }

    @Test void rejectsNonStudent() {
        User u=student(); u.setRole("ADMIN");
        assertThrows(AccessDeniedException.class, () -> service.history(u,null));
    }
    @Test void onlyStudentMayAuthenticate() {
        User u=student();u.setRole("INSTRUCTOR");
        var auth=new UsernamePasswordAuthenticationToken(u,"",List.of());
        assertThrows(AccessDeniedException.class, () -> service.resolveStudent(auth));
    }
    @Test void omitsUnpublishedScores() {
        when(read.exams(5)).thenReturn(List.of(examAttempt(false)));
        when(read.quizzes(5)).thenReturn(List.of(quizAttempt(false)));
        when(read.assignments(5)).thenReturn(List.of());
        assertTrue(service.history(student(),null).isEmpty());
    }
    @Test void exposesPublishedExamAndQuizMarks() {
        when(read.exams(5)).thenReturn(List.of(examAttempt(true)));
        when(read.quizzes(5)).thenReturn(List.of(quizAttempt(true)));
        when(read.assignments(5)).thenReturn(List.of());
        List<Result> rows=service.history(student(),null);
        assertEquals(2,rows.size());
        assertEquals("EXAM",rows.get(0).type());
        assertEquals(80.0,rows.get(0).percentage());
        assertEquals(75.0,rows.get(1).percentage());
    }
    @Test void rejectsOtherCourseRequest() {
        when(read.existsByStudentIdAndCourseId(5,99)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.history(student(),99));
    }
    @Test void overviewUsesExistingLectureProgress() {
        Course c=course();Enrollment e=new Enrollment();e.setCourse(c);e.setStudent(student());
        when(read.enrollments(5)).thenReturn(List.of(e));
        emptyHistory();
        when(progress.getCourseProgress(5,22)).thenReturn(
                new CourseProgressResponse(22,3,4,75.0,List.of(1,2,3)));
        Overview o=service.overview(student());
        assertEquals(1,o.enrolledCourseCount());
        assertEquals(75.0,o.averageCourseCompletionPercentage());
        assertNull(o.averageAssessmentPercentage());
    }
    @Test void identifiesLowScoringTopicsFromPublishedQuizzes() {
        QuizAttempt a=quizAttempt(true);
        when(read.quizzes(5)).thenReturn(List.of(a));
        when(read.exams(5)).thenReturn(List.of());
        Question q = new Question(); q.setTopic("Routing");
        QuizAttemptAnswer ans=new QuizAttemptAnswer();ans.setQuestion(q);
        ans.setAwardedMarks(new BigDecimal("3"));ans.setMaxMarks(new BigDecimal("10"));
        when(quizAnswers.findByAttemptId(55)).thenReturn(List.of(ans));
        List<Topic> topics=service.practiceTopics(student(),null);
        assertEquals(1,topics.size());
        assertEquals("Routing",topics.get(0).topic());
        assertEquals(30.0,topics.get(0).percentage());
        assertTrue(topics.get(0).needsPractice());
    }
    @Test void trendIsChronologicalAndCalculatesRunningAverage() {
        when(read.exams(5)).thenReturn(List.of(examAttempt(true)));
        when(read.quizzes(5)).thenReturn(List.of(quizAttempt(true)));
        when(read.assignments(5)).thenReturn(List.of());
        List<TrendPoint> points=service.trend(student(),null);
        assertEquals(2,points.size());
        assertEquals(75.0,points.get(0).runningAveragePercentage());
        assertEquals(77.5,points.get(1).runningAveragePercentage());
    }
}
