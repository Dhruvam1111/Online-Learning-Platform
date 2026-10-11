# PERSON 4 — Student Backend + Deployment (DevOps)

## What You Own

You own two areas:

**Area 1: Student Backend APIs**
All student-facing backend endpoints: course catalog, course detail, enrollment, and the lecture access (watch) endpoint.

**Area 2: Deployment**
Getting the entire application deployed to Render (backend) and the frontend deployed to Vercel/Netlify. You write the Dockerfile, configure environment variables, and make sure the live URL works.

**Nobody else writes student API code or touches deployment.**

---

## What You Depend On

| Dependency | Provider | What You Import/Use |
|---|---|---|
| `users` table | Pranamya (DB) | You don't query this directly for most things — the auth filter gives you the current user |
| `courses` table | Pranamya (DB) | You use Person 3's `Course` entity and `CourseRepository` — do NOT create your own |
| `lectures` table | Pranamya (DB) | You use Person 3's `Lecture` entity and `LectureRepository` — do NOT create your own |
| `enrollments` table | Pranamya (DB) | You create a JPA entity and repository for this table (you own it) |
| `User` entity | Person 2 (Auth) | `com.olp.entity.User` — import this, do NOT create your own |
| `UserRepository` | Person 2 (Auth) | `com.olp.repository.UserRepository` — import if you need instructor names |
| JWT auth filter | Person 2 (Auth) | Runs automatically. You use `Authentication` in controller methods |
| `@PreAuthorize` | Person 2 (Auth) | Enabled by Person 2's `SecurityConfig`. You add it to your controller methods |
| `Course` entity | Person 3 (Instructor) | `com.olp.entity.Course` — import this, do NOT create your own |
| `CourseRepository` | Person 3 (Instructor) | `com.olp.repository.CourseRepository` — import and use `findByStatusOrderByCreatedAtDesc("published")` |
| `Lecture` entity | Person 3 (Instructor) | `com.olp.entity.Lecture` — import this |
| `LectureRepository` | Person 3 (Instructor) | `com.olp.repository.LectureRepository` — import and use `findByCourseIdOrderByOrderIndexAsc(courseId)` |
| `LectureResponse` DTO | Person 3 (Instructor) | `com.olp.dto.LectureResponse` — reuse this for the watch page response |
| `YoutubeUtil` | Person 3 (Instructor) | `com.olp.util.YoutubeUtil` — use `buildEmbedUrl()` and `buildThumbnailUrl()` |

### Critical Rule: Do NOT Create Duplicate Entities or Repositories

Person 3 already created `Course.java`, `Lecture.java`, `CourseRepository.java`, and `LectureRepository.java`. If you create your own versions, Spring Boot will crash with duplicate bean errors. **Import theirs.**

You only create:
- `Enrollment.java` (entity) — Person 3 doesn't need this, so it's yours
- `EnrollmentRepository.java` — yours
- `StudentController.java` — yours
- `StudentService.java` — yours
- Student-specific DTOs — yours

### How to Get the Current User in Your Controllers

```java
@GetMapping("/some-endpoint")
public ResponseEntity<?> someMethod(Authentication authentication) {
    User currentUser = (User) authentication.getPrincipal();
    Integer studentId = currentUser.getId();
}
```

---

## Your Deliverables

### Area 1: Student Backend
1. `Enrollment` entity (JPA mapping to the `enrollments` table)
2. `EnrollmentRepository` (Spring Data JPA)
3. `StudentController` with all endpoints
4. `StudentService` with business logic
5. Student-specific DTOs

### Area 2: Deployment
6. `Dockerfile` in the project root
7. Render PostgreSQL instance setup
8. Render Web Service setup
9. Environment variable configuration
10. CORS update for the deployed frontend origin

---

## Project Structure

### Area 1 files (under `src/main/java/com/olp/`):

```
backend/src/main/java/com/olp/
├── entity/
│   ├── User.java                          ← EXISTS (Person 2) — DO NOT TOUCH
│   ├── Course.java                        ← EXISTS (Person 3) — DO NOT TOUCH
│   ├── Lecture.java                       ← EXISTS (Person 3) — DO NOT TOUCH
│   └── Enrollment.java                   ← YOU CREATE THIS
├── repository/
│   ├── UserRepository.java               ← EXISTS (Person 2) — import only
│   ├── CourseRepository.java             ← EXISTS (Person 3) — import only
│   ├── LectureRepository.java           ← EXISTS (Person 3) — import only
│   └── EnrollmentRepository.java        ← YOU CREATE THIS
├── dto/
│   ├── CatalogCourseResponse.java        ← YOU CREATE THIS
│   ├── CourseDetailStudentResponse.java   ← YOU CREATE THIS
│   ├── LectureResponse.java              ← EXISTS (Person 3) — reuse
│   └── ... (other DTOs from Person 2/3)
├── service/
│   └── StudentService.java               ← YOU CREATE THIS
├── controller/
│   └── StudentController.java            ← YOU CREATE THIS
└── util/
    └── YoutubeUtil.java                   ← EXISTS (Person 3) — import only
```

### Area 2 files (project root):

```
Online Learning Platform/
├── backend/
│   └── Dockerfile                         ← YOU CREATE THIS
├── docker-compose.yml                     ← EXISTS (Pranamya) — DO NOT TOUCH
└── ... (everything else)
```

---

# AREA 1: STUDENT BACKEND

---

## File 1: `Enrollment.java` (Entity)

```java
package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    @PrePersist
    protected void onCreate() {
        this.enrolledAt = LocalDateTime.now();
    }

    // ===== Constructors =====

    public Enrollment() {}

    public Enrollment(Integer studentId, Integer courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
    }

    // ===== Getters and Setters =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }
}
```

### Design Notes
| Decision | Reasoning |
|---|---|
| Plain `Integer studentId` instead of `@ManyToOne User` | Same pattern as Person 3. Avoids coupling to Person 2's `User` entity lifecycle. |
| Plain `Integer courseId` instead of `@ManyToOne Course` | Avoids coupling to Person 3's `Course` entity. You look up courses and enrollments separately. |
| `GenerationType.IDENTITY` | Matches Pranamya's `SERIAL` primary key. |
| No unique constraint annotation | The `UNIQUE(student_id, course_id)` constraint exists in Pranamya's SQL. JPA doesn't need to duplicate it — the database enforces it. You handle the resulting `DataIntegrityViolationException` in the service layer. |

---

## File 2: `EnrollmentRepository.java`

```java
package com.olp.repository;

import com.olp.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

    /**
     * Check if a student is enrolled in a specific course.
     * Used by: course detail page (show "Enroll" vs "Go to Lectures")
     * Used by: watch page (deny access if not enrolled)
     */
    boolean existsByStudentIdAndCourseId(Integer studentId, Integer courseId);

    /**
     * Get all enrollments for a student.
     * Used by: listing "My Enrolled Courses" (future sprint, but useful now for testing)
     */
    List<Enrollment> findByStudentId(Integer studentId);

    /**
     * Count how many students are enrolled in a course.
     * Used by: catalog page (show enrollment count, nice-to-have)
     */
    int countByCourseId(Integer courseId);
}
```

---

## File 3: Student-Specific DTOs

### `CatalogCourseResponse.java`
```java
package com.olp.dto;

import java.time.LocalDateTime;

public class CatalogCourseResponse {

    private Integer id;
    private String title;
    private String description;
    private String instructorName;
    private int lectureCount;
    private int enrollmentCount;
    private LocalDateTime createdAt;

    public CatalogCourseResponse(Integer id, String title, String description,
                                   String instructorName, int lectureCount,
                                   int enrollmentCount, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.instructorName = instructorName;
        this.lectureCount = lectureCount;
        this.enrollmentCount = enrollmentCount;
        this.createdAt = createdAt;
    }

    // ===== Getters =====

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getInstructorName() { return instructorName; }
    public int getLectureCount() { return lectureCount; }
    public int getEnrollmentCount() { return enrollmentCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

### `CourseDetailStudentResponse.java`
```java
package com.olp.dto;

import java.time.LocalDateTime;

public class CourseDetailStudentResponse {

    private Integer id;
    private String title;
    private String description;
    private String instructorName;
    private int lectureCount;
    private int enrollmentCount;
    private boolean isEnrolled;
    private LocalDateTime createdAt;

    public CourseDetailStudentResponse(Integer id, String title, String description,
                                        String instructorName, int lectureCount,
                                        int enrollmentCount, boolean isEnrolled,
                                        LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.instructorName = instructorName;
        this.lectureCount = lectureCount;
        this.enrollmentCount = enrollmentCount;
        this.isEnrolled = isEnrolled;
        this.createdAt = createdAt;
    }

    // ===== Getters =====

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getInstructorName() { return instructorName; }
    public int getLectureCount() { return lectureCount; }
    public int getEnrollmentCount() { return enrollmentCount; }
    public boolean getIsEnrolled() { return isEnrolled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

> **IMPORTANT:** The JSON key is `isEnrolled` (not `enrolled`). Jackson serializes the getter `getIsEnrolled()` as `isEnrolled`. Person 6 (frontend) will access `response.data.isEnrolled` in JavaScript. If you name the getter `isEnrolled()` (boolean style), Jackson serializes it as just `enrolled`, which would break the frontend. Use `getIsEnrolled()` to guarantee the JSON key is `isEnrolled`.

---

## File 4: `StudentService.java`

```java
package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.Course;
import com.olp.entity.Enrollment;
import com.olp.entity.Lecture;
import com.olp.entity.User;
import com.olp.repository.CourseRepository;
import com.olp.repository.EnrollmentRepository;
import com.olp.repository.LectureRepository;
import com.olp.repository.UserRepository;
import com.olp.util.YoutubeUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public StudentService(CourseRepository courseRepository,
                           LectureRepository lectureRepository,
                           EnrollmentRepository enrollmentRepository,
                           UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    // ===============================
    // CATALOG
    // ===============================

    /**
     * Get all published courses for the catalog page.
     * Returns courses newest-first, with instructor names and lecture counts.
     */
    public List<CatalogCourseResponse> getCatalog() {
        List<Course> courses = courseRepository.findByStatusOrderByCreatedAtDesc("published");

        return courses.stream().map(course -> {
            // Look up the instructor's name
            String instructorName = userRepository.findById(course.getInstructorId())
                    .map(User::getName)
                    .orElse("Unknown Instructor");

            int lectureCount = lectureRepository.countByCourseId(course.getId());
            int enrollmentCount = enrollmentRepository.countByCourseId(course.getId());

            return new CatalogCourseResponse(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    instructorName,
                    lectureCount,
                    enrollmentCount,
                    course.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }

    // ===============================
    // COURSE DETAIL
    // ===============================

    /**
     * Get a single course's details for the course detail page.
     * Includes whether the current student is enrolled.
     * Only returns published courses.
     */
    public CourseDetailStudentResponse getCourseDetail(Integer courseId, Integer studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Course not found"));

        // Only show published courses to students
        if (!"published".equals(course.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }

        // Look up instructor name
        String instructorName = userRepository.findById(course.getInstructorId())
                .map(User::getName)
                .orElse("Unknown Instructor");

        int lectureCount = lectureRepository.countByCourseId(courseId);
        int enrollmentCount = enrollmentRepository.countByCourseId(courseId);
        boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);

        return new CourseDetailStudentResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                instructorName,
                lectureCount,
                enrollmentCount,
                isEnrolled,
                course.getCreatedAt()
        );
    }

    // ===============================
    // ENROLLMENT
    // ===============================

    /**
     * Enroll the current student in a course.
     * Idempotent: if already enrolled, returns success without error.
     * Only allows enrollment in published courses.
     */
    public String enrollInCourse(Integer courseId, Integer studentId) {
        // 1. Verify the course exists and is published
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Course not found"));

        if (!"published".equals(course.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }

        // 2. Check if already enrolled
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            return "Already enrolled";
        }

        // 3. Create the enrollment
        try {
            Enrollment enrollment = new Enrollment(studentId, courseId);
            enrollmentRepository.save(enrollment);
            return "Enrolled successfully";
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request enrolled the student between
            // the check and the insert. This is fine — they're enrolled.
            return "Already enrolled";
        }
    }

    // ===============================
    // LECTURES (WATCH PAGE)
    // ===============================

    /**
     * Get the lecture list for a course.
     * ONLY returns lectures if the student is enrolled.
     * This is the core access control for FR-S11:
     * "Non-enrolled students are denied access."
     */
    public List<LectureResponse> getCourseLectures(Integer courseId, Integer studentId) {
        // 1. Verify the course exists
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Course not found"));

        // 2. Verify the student is enrolled
        if (!enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You must be enrolled in this course to view lectures");
        }

        // 3. Fetch and return lectures
        List<Lecture> lectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);

        return lectures.stream().map(lecture -> {
            String embedUrl = lecture.getVideoId() != null
                    ? YoutubeUtil.buildEmbedUrl(lecture.getVideoId())
                    : null;
            String thumbnailUrl = lecture.getVideoId() != null
                    ? YoutubeUtil.buildThumbnailUrl(lecture.getVideoId())
                    : null;

            return new LectureResponse(
                    lecture.getId(),
                    lecture.getTitle(),
                    lecture.getYoutubeUrl(),
                    lecture.getVideoId(),
                    embedUrl,
                    thumbnailUrl,
                    lecture.getOrderIndex()
            );
        }).collect(Collectors.toList());
    }
}
```

### Key Design Decisions

| Decision | Reasoning |
|---|---|
| Check-then-insert with `DataIntegrityViolationException` catch | The `existsByStudentIdAndCourseId` check handles 99% of cases. The `try/catch` handles the rare race condition where two enrollment requests arrive at the exact same time. Without it, the second request gets a 500 error from the unique constraint violation. |
| Draft courses return 404, not 403 | A student should not know that a draft course exists. Returning 404 for both "doesn't exist" and "not published" prevents information leakage. |
| `instructorName` fetched via separate query | Since Person 3 used `Integer instructorId` (not `@ManyToOne`), we look up the instructor name with a separate `userRepository.findById()`. For Sprint 1's small dataset, this is fine. In a later sprint, use a `@Query` with a JOIN for efficiency. |

---

## File 5: `StudentController.java`

```java
package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /**
     * GET /api/courses
     * Course catalog — list all published courses.
     * Any authenticated user can view (both students and instructors).
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCatalog() {
        List<CatalogCourseResponse> courses = studentService.getCatalog();
        return ResponseEntity.ok(Map.of("courses", courses));
    }

    /**
     * GET /api/courses/{id}
     * Course detail page — single course info + enrollment status.
     * Any authenticated user can view.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCourseDetail(
            @PathVariable Integer id,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        CourseDetailStudentResponse course = studentService.getCourseDetail(id, currentUser.getId());

        return ResponseEntity.ok(Map.of("course", course));
    }

    /**
     * POST /api/courses/{id}/enroll
     * Enroll the current student in a course.
     * Students only — instructors cannot enroll.
     */
    @PreAuthorize("hasRole('student')")
    @PostMapping("/{id}/enroll")
    public ResponseEntity<Map<String, Object>> enroll(
            @PathVariable Integer id,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        String message = studentService.enrollInCourse(id, currentUser.getId());

        HttpStatus status = message.equals("Enrolled successfully")
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    /**
     * GET /api/courses/{id}/lectures
     * Get the lecture list for a course (the watch page data).
     * Students only — must be enrolled.
     */
    @PreAuthorize("hasRole('student')")
    @GetMapping("/{id}/lectures")
    public ResponseEntity<Map<String, Object>> getLectures(
            @PathVariable Integer id,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();
        List<LectureResponse> lectures = studentService.getCourseLectures(id, currentUser.getId());

        return ResponseEntity.ok(Map.of("lectures", lectures));
    }
}
```

### Route Design Note

The student controller uses `/api/courses` (not `/api/student/courses`). This is intentional:
- The course catalog URL is clean and RESTful: `GET /api/courses`
- Person 3's instructor routes are under `/api/instructor/courses` — no conflict
- Person 6's frontend calls `GET /api/courses` for the catalog and `GET /api/courses/:id` for detail

There is **no route collision** with Person 3's controller because Person 3 uses `/api/instructor/courses`.

---

## API Contract Reference

This is the exact JSON shape Person 6 (frontend) will code against.

### GET `/api/courses` (Catalog)

**Request:**
```
GET /api/courses
Authorization: Bearer <any_user_token>
```

**Success (200):**
```json
{
  "courses": [
    {
      "id": 1,
      "title": "Web Development Fundamentals",
      "description": "Learn the core building blocks of the web: HTML for structure, CSS for styling, and JavaScript for interactivity. This course takes you from zero to building your first responsive website.",
      "instructorName": "Dr. Sarah Chen",
      "lectureCount": 3,
      "enrollmentCount": 2,
      "createdAt": "2026-09-25T10:00:00"
    },
    {
      "id": 2,
      "title": "Introduction to Databases",
      "description": "Understand how data is stored, queried, and managed...",
      "instructorName": "Dr. Sarah Chen",
      "lectureCount": 2,
      "enrollmentCount": 1,
      "createdAt": "2026-09-25T09:00:00"
    },
    {
      "id": 3,
      "title": "Python for Beginners",
      "description": "A gentle introduction to programming using Python...",
      "instructorName": "Prof. James Wilson",
      "lectureCount": 3,
      "enrollmentCount": 1,
      "createdAt": "2026-09-24T14:30:00"
    }
  ]
}
```

**Note:** Course 4 (draft) does NOT appear. Only 3 courses returned.

---

### GET `/api/courses/{id}` (Course Detail)

**Request:**
```
GET /api/courses/1
Authorization: Bearer <alice_token>
```

**Success (200) — Alice is enrolled in course 1:**
```json
{
  "course": {
    "id": 1,
    "title": "Web Development Fundamentals",
    "description": "Learn the core building blocks of the web...",
    "instructorName": "Dr. Sarah Chen",
    "lectureCount": 3,
    "enrollmentCount": 2,
    "isEnrolled": true,
    "createdAt": "2026-09-25T10:00:00"
  }
}
```

**Success (200) — Alice is NOT enrolled in course 2:**
```json
{
  "course": {
    "id": 2,
    "title": "Introduction to Databases",
    "description": "Understand how data is stored...",
    "instructorName": "Dr. Sarah Chen",
    "lectureCount": 2,
    "enrollmentCount": 1,
    "isEnrolled": false,
    "createdAt": "2026-09-25T09:00:00"
  }
}
```

**Draft course (404):**
```
GET /api/courses/4
```
```json
{
  "status": 404,
  "error": "Course not found",
  "timestamp": "2026-09-30T23:00:00"
}
```

---

### POST `/api/courses/{id}/enroll`

**Request:**
```
POST /api/courses/2/enroll
Authorization: Bearer <alice_token>
```

**First time (201):**
```json
{
  "message": "Enrolled successfully"
}
```

**Already enrolled (200):**
```json
{
  "message": "Already enrolled"
}
```

**Course not found (404):**
```json
{
  "status": 404,
  "error": "Course not found",
  "timestamp": "2026-09-30T23:00:00"
}
```

**Instructor tries to enroll (403):**
Spring Security returns 403 Forbidden automatically because of `@PreAuthorize("hasRole('student')")`.

---

### GET `/api/courses/{id}/lectures` (Watch Page)

**Request (enrolled):**
```
GET /api/courses/1/lectures
Authorization: Bearer <alice_token>
```

**Success (200) — Alice is enrolled in course 1:**
```json
{
  "lectures": [
    {
      "id": 1,
      "title": "What is the Web?",
      "youtubeUrl": "https://www.youtube.com/watch?v=hJHvdBlSxug",
      "videoId": "hJHvdBlSxug",
      "embedUrl": "https://www.youtube.com/embed/hJHvdBlSxug",
      "thumbnailUrl": "https://img.youtube.com/vi/hJHvdBlSxug/mqdefault.jpg",
      "orderIndex": 1
    },
    {
      "id": 2,
      "title": "HTML Crash Course",
      "youtubeUrl": "https://www.youtube.com/watch?v=UB1O30fR-EE",
      "videoId": "UB1O30fR-EE",
      "embedUrl": "https://www.youtube.com/embed/UB1O30fR-EE",
      "thumbnailUrl": "https://img.youtube.com/vi/UB1O30fR-EE/mqdefault.jpg",
      "orderIndex": 2
    },
    {
      "id": 3,
      "title": "CSS in 100 Seconds",
      "youtubeUrl": "https://www.youtube.com/watch?v=OEV8gMkCHXQ",
      "videoId": "OEV8gMkCHXQ",
      "embedUrl": "https://www.youtube.com/embed/OEV8gMkCHXQ",
      "thumbnailUrl": "https://img.youtube.com/vi/OEV8gMkCHXQ/mqdefault.jpg",
      "orderIndex": 3
    }
  ]
}
```

**Not enrolled (403):**
```
GET /api/courses/2/lectures
Authorization: Bearer <alice_token>
(Alice is not enrolled in course 2)
```
```json
{
  "status": 403,
  "error": "You must be enrolled in this course to view lectures",
  "timestamp": "2026-09-30T23:00:00"
}
```

---

## Testing with Postman

**Prerequisites:**
- Pranamya's database is running (`docker-compose up -d`)
- Person 2's auth module and Person 3's instructor module are in the codebase
- Login as `alice@student.com` / `password123` and save the token
- Login as `sarah@instructor.com` / `password123` and save that token too

### Test 1: Get the course catalog
```
GET http://localhost:8080/api/courses
Headers: Authorization: Bearer <alice_token>
Expected: 200, 3 courses (all published). Course 4 (draft) must NOT appear.
Verify: each course has instructorName, lectureCount, enrollmentCount
```

### Test 2: Get catalog as instructor (should also work)
```
GET http://localhost:8080/api/courses
Headers: Authorization: Bearer <sarah_token>
Expected: 200, same 3 courses. Catalog is visible to everyone.
```

### Test 3: Get detail for a course Alice is enrolled in
```
GET http://localhost:8080/api/courses/1
Headers: Authorization: Bearer <alice_token>
Expected: 200, isEnrolled = true
```

### Test 4: Get detail for a course Alice is NOT enrolled in
```
GET http://localhost:8080/api/courses/2
Headers: Authorization: Bearer <alice_token>
Expected: 200, isEnrolled = false
```

### Test 5: Get detail for a draft course
```
GET http://localhost:8080/api/courses/4
Headers: Authorization: Bearer <alice_token>
Expected: 404, "Course not found"
```

### Test 6: Get detail for a nonexistent course
```
GET http://localhost:8080/api/courses/999
Headers: Authorization: Bearer <alice_token>
Expected: 404, "Course not found"
```

### Test 7: Enroll Alice in course 2
```
POST http://localhost:8080/api/courses/2/enroll
Headers: Authorization: Bearer <alice_token>
Expected: 201, "Enrolled successfully"
```

### Test 8: Enroll Alice in course 2 again (idempotent)
```
POST http://localhost:8080/api/courses/2/enroll
Headers: Authorization: Bearer <alice_token>
Expected: 200, "Already enrolled" (not an error)
```

### Test 9: Verify enrollment reflected in detail
```
GET http://localhost:8080/api/courses/2
Headers: Authorization: Bearer <alice_token>
Expected: 200, isEnrolled = true (was false before Test 7)
```

### Test 10: Instructor tries to enroll (should fail)
```
POST http://localhost:8080/api/courses/1/enroll
Headers: Authorization: Bearer <sarah_token>
Expected: 403 Forbidden
```

### Test 11: Get lectures for enrolled course
```
GET http://localhost:8080/api/courses/1/lectures
Headers: Authorization: Bearer <alice_token>
Expected: 200, array of 3 lectures with embedUrl and thumbnailUrl
```

### Test 12: Get lectures for unenrolled course
```
GET http://localhost:8080/api/courses/3/lectures
Headers: Authorization: Bearer <bob_token>
(Bob is NOT enrolled in course 3)
Expected: 403, "You must be enrolled in this course to view lectures"
```

### Test 13: Enroll Bob in course 3 and retry
```
POST http://localhost:8080/api/courses/3/enroll
Headers: Authorization: Bearer <bob_token>
Expected: 201

GET http://localhost:8080/api/courses/3/lectures
Headers: Authorization: Bearer <bob_token>
Expected: 200, array of 3 Python lectures
```

### Test 14: Try to enroll in a draft course
```
POST http://localhost:8080/api/courses/4/enroll
Headers: Authorization: Bearer <alice_token>
Expected: 404, "Course not found"
```

---

# AREA 2: DEPLOYMENT

---

## File 6: `Dockerfile`

Place this in the `backend/` directory (or the project root if backend is the root).

```dockerfile
# ============================
# Stage 1: Build
# ============================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml first and download dependencies.
# This layer is cached — dependencies only re-download when pom.xml changes.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================
# Stage 2: Run
# ============================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# JVM memory flags to stay within Render's 512MB free tier
ENV JAVA_OPTS="-Xmx256m -Xss512k -XX:+UseSerialGC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Why These JVM Flags

| Flag | Purpose |
|---|---|
| `-Xmx256m` | Cap heap memory at 256MB. Render's free tier has 512MB total; the OS and JVM overhead take ~150MB, leaving ~350MB for the app. 256MB heap gives headroom. |
| `-Xss512k` | Reduce thread stack size from the default 1MB to 512KB. Saves memory when handling multiple requests. |
| `-XX:+UseSerialGC` | Use the simplest garbage collector. It uses less memory than G1GC (the default). Perfectly fine for a course project's load. |

---

## Render Setup: Step-by-Step

### Step 1: Create a Render Account
1. Go to https://render.com and sign up (free, no credit card)
2. Connect your GitHub account

### Step 2: Create the PostgreSQL Database
1. Dashboard → **New** → **PostgreSQL**
2. Settings:
   - **Name:** `olp-database`
   - **Database:** `online_learning`
   - **User:** leave as default (Render generates one)
   - **Region:** pick the one closest to your team
   - **Plan:** **Free**
3. Click **Create Database**
4. Once created, go to the database page and note these values:
   - **Internal Database URL** (starts with `postgres://`)
   - **Username** (e.g., `olp_database_user`)
   - **Password** (a long random string)
   - **Host** (e.g., `dpg-xxxxx-a.oregon-postgres.render.com`)
   - **Port** (`5432`)
   - **Database name** (`online_learning`)

### Step 3: Initialize the Remote Database
Render's free Postgres doesn't run init scripts automatically. You need to run Pranamya's SQL scripts manually:

```bash
# Using psql from your local machine:
# Replace the values with your Render database's EXTERNAL connection details
psql "postgres://<username>:<password>@<external-host>:5432/online_learning"

# Once connected, run the init scripts in order:
\i db/01-schema.sql
\i db/02-indexes.sql
\i db/03-seed.sql
```

Or use a GUI like DBeaver or pgAdmin if you prefer.

### Step 4: Create the Web Service
1. Dashboard → **New** → **Web Service**
2. Connect your GitHub repository
3. Settings:
   - **Name:** `olp-backend`
   - **Region:** same as the database
   - **Branch:** `main` (or your deployment branch)
   - **Runtime:** **Docker** (it will auto-detect the Dockerfile)
   - **Plan:** **Free**
4. **Environment Variables** (add all of these):

| Variable | Value | Notes |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://<internal-host>:5432/online_learning` | Use the INTERNAL host, not external. Internal is faster and free. Convert from `postgres://` to `jdbc:postgresql://` format. |
| `DATABASE_USERNAME` | `olp_database_user` | From Step 2 |
| `DATABASE_PASSWORD` | `<the-long-password>` | From Step 2 |
| `JWT_SECRET` | `<generate-a-64-char-random-string>` | Generate with: `openssl rand -base64 48` or use a password generator. Must be at least 32 characters. |
| `JWT_EXPIRATION_MS` | `86400000` | 24 hours in milliseconds |
| `PORT` | `8080` | Render expects the app to listen on this port |

5. Click **Create Web Service**

### Step 5: Convert the Database URL Format

Render gives you a URL like:
```
postgres://olp_database_user:somepassword@dpg-xxxxx-a.oregon-postgres.render.com:5432/online_learning
```

Spring Boot needs JDBC format:
```
jdbc:postgresql://dpg-xxxxx-a.oregon-postgres.render.com:5432/online_learning
```

The conversion:
- Replace `postgres://` with `jdbc:postgresql://`
- Remove the `username:password@` part (those go in separate env vars)
- For the `DATABASE_URL` env var, use the **internal** hostname (not the external one)

### Step 6: Verify Deployment
1. Wait for Render to build and deploy (first build takes 5-10 minutes)
2. Render gives you a URL like `https://olp-backend.onrender.com`
3. Test it:

```bash
# This should return 401 (unauthenticated) — that means the app is running
curl https://olp-backend.onrender.com/api/auth/me

# Register a test user
curl -X POST https://olp-backend.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@test.com","password":"password123","role":"student"}'

# Should return 201 with a token
```

### Step 7: Configure CORS for the Frontend

After Person 5 or Person 6 deploys the frontend (e.g., to `https://olp-frontend.vercel.app`), you need to add that origin to Person 2's `SecurityConfig.java`:

```java
config.setAllowedOrigins(List.of(
    "http://localhost:3000",
    "http://localhost:5173",
    "https://olp-frontend.vercel.app"  // ADD THIS
));
```

Then push the change and Render will auto-redeploy.

### Step 8: Handle the Free Tier Sleep Behavior

Render's free tier spins down the app after 15 minutes of inactivity. The next request takes 30-60 seconds to wake it up.

**For demo day:**
- Hit the backend URL 2-3 minutes before the demo starts to wake it up
- Or use a free "keep alive" service like UptimeRobot to ping the URL every 14 minutes (but this may violate Render's ToS for free tier — check first)
- Mention the cold start delay if it happens during a live demo

---

## Deployment Verification Checklist

- [ ] Render PostgreSQL instance is created and accessible
- [ ] Pranamya's schema, indexes, and seed data are loaded on the Render database
- [ ] Render Web Service is created and connected to the GitHub repo
- [ ] All 6 environment variables are set correctly
- [ ] `DATABASE_URL` uses JDBC format with the INTERNAL hostname
- [ ] `JWT_SECRET` is a long random string (not the dev default)
- [ ] The app builds successfully on Render (check the deploy logs)
- [ ] `curl https://<your-url>/api/auth/me` returns 401 (app is running)
- [ ] Can register and login via the deployed URL
- [ ] Seed data users (alice, sarah, etc.) can log in on the deployed instance
- [ ] CORS is configured to allow the deployed frontend origin
- [ ] Cold start (after sleep) works — just takes 30-60s

---

## Full Verification Checklist (Both Areas)

### Student Backend
- [ ] Catalog returns exactly 3 published courses (not 4)
- [ ] Each catalog entry includes `instructorName`
- [ ] Course detail returns correct `isEnrolled` value per student
- [ ] Draft courses return 404 to students
- [ ] Enrollment creates a row in the database
- [ ] Double-enrollment is handled gracefully (200, not error)
- [ ] Instructors get 403 on the enroll endpoint
- [ ] Lectures return 403 if the student is not enrolled
- [ ] Lectures return correctly after enrollment
- [ ] Lectures are ordered by `orderIndex` ascending
- [ ] All 14 Postman tests pass

### Deployment
- [ ] All 12 deployment checklist items pass (see above)

---

## Common Pitfalls

| Pitfall | How to Avoid |
|---|---|
| Creating duplicate `Course.java` or `CourseRepository.java` | Person 3 already created these. Two `@Entity` classes for the same table crashes Hibernate. Two `@Repository` interfaces for the same entity crashes Spring autowiring. Import Person 3's. |
| `getIsEnrolled()` vs `isEnrolled()` getter naming | If you use `boolean isEnrolled` with `isEnrolled()` getter, Jackson serializes it as `"enrolled"` (strips the "is" prefix). Person 6 expects `"isEnrolled"`. Use `getIsEnrolled()` explicitly. |
| `postgres://` vs `jdbc:postgresql://` in DATABASE_URL | Render gives you `postgres://user:pass@host:5432/db`. Spring Boot needs `jdbc:postgresql://host:5432/db`. These are different formats. If you paste the wrong one, Spring Boot fails to connect silently or throws a URL parsing error. |
| Using Render's EXTERNAL database host | Internal and external hosts are different. Internal is only accessible from Render services and is faster/free. External is for connecting from your local machine. The Web Service must use the internal host. |
| Free tier Postgres lifetime | Render's free Postgres instances have a limited lifetime (check current terms). Set a calendar reminder for the expiry date and back up your seed script. |
| 403 `ResponseStatusException` not returning JSON | Spring Boot's default error handling for `ResponseStatusException` may return HTML instead of JSON. Person 2's `GlobalExceptionHandler` should catch this, but verify the 403 response is JSON by testing with Postman. If it returns HTML, add a handler for `ResponseStatusException` to the `GlobalExceptionHandler`. |

---

## Git Branch

Work on branch: `feature/student-backend`

```bash
git checkout -b feature/student-backend
# ... do all your work ...
git add .
git commit -m "feat: add student module (catalog, enrollment, lectures) and Dockerfile"
git push origin feature/student-backend
# Then open a Pull Request to main
```
