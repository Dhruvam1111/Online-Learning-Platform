# PERSON 3 — Instructor Backend Developer

## What You Own

You are the **sole owner** of all instructor-facing backend APIs. This includes creating courses, listing an instructor's own courses, viewing a single course with its lectures, and adding lectures to a course.

**Nobody else writes instructor API code.** Person 2 provides the auth middleware you use. Pranamya provides the database tables you query. Person 5 builds the frontend that calls your endpoints.

---

## What You Depend On

| Dependency | Provider | What You Import/Use |
|---|---|---|
| `users` table | Pranamya (DB) | You don't query this directly — the auth filter gives you the current user |
| `courses` table | Pranamya (DB) | You create JPA entities that map to this table |
| `lectures` table | Pranamya (DB) | You create JPA entities that map to this table |
| `User` entity | Person 2 (Auth) | `com.olp.entity.User` — you import this, do NOT create your own |
| `UserRepository` | Person 2 (Auth) | `com.olp.repository.UserRepository` — import if you need to look up instructor names |
| JWT auth filter | Person 2 (Auth) | Automatically runs before your endpoints. You just use `Authentication` in controller methods |
| `@PreAuthorize` | Person 2 (Auth) | Enabled by Person 2's `SecurityConfig`. You add it to your controller methods |
| `SecurityConfig` | Person 2 (Auth) | Already marks all non-auth endpoints as `authenticated()`. Your endpoints are protected by default |

### How to Get the Current User in Your Controllers

Person 2's `JwtAuthenticationFilter` puts the full `User` entity into the security context. Access it like this:

```java
@GetMapping("/some-endpoint")
public ResponseEntity<?> someMethod(Authentication authentication) {
    User currentUser = (User) authentication.getPrincipal();
    Integer instructorId = currentUser.getId();
    String role = currentUser.getRole();
}
```

### How to Restrict Endpoints to Instructors Only

Add this annotation to any controller method that only instructors should access:

```java
@PreAuthorize("hasRole('instructor')")
@PostMapping("/courses")
public ResponseEntity<?> createCourse(...) { ... }
```

Spring Security's `@PreAuthorize` checks the role set by Person 2's filter. If the user is a student, they get a 403 Forbidden automatically.

---

## Your Deliverables

1. `Course` entity (JPA mapping to the `courses` table)
2. `Lecture` entity (JPA mapping to the `lectures` table)
3. `CourseRepository` (Spring Data JPA)
4. `LectureRepository` (Spring Data JPA)
5. `InstructorController` with all endpoints
6. `InstructorService` with business logic
7. Request/Response DTOs
8. YouTube URL validation utility

---

## Project Structure

All your code goes under `src/main/java/com/olp/`. Create exactly these files:

```
backend/src/main/java/com/olp/
├── entity/
│   ├── User.java                          ← ALREADY EXISTS (Person 2 created this)
│   ├── Course.java                        ← YOU CREATE THIS
│   └── Lecture.java                       ← YOU CREATE THIS
├── repository/
│   ├── UserRepository.java                ← ALREADY EXISTS (Person 2)
│   ├── CourseRepository.java              ← YOU CREATE THIS
│   └── LectureRepository.java            ← YOU CREATE THIS
├── dto/
│   ├── CreateCourseRequest.java           ← YOU CREATE THIS
│   ├── AddLectureRequest.java             ← YOU CREATE THIS
│   ├── CourseListResponse.java            ← YOU CREATE THIS
│   ├── CourseDetailResponse.java          ← YOU CREATE THIS
│   ├── LectureResponse.java              ← YOU CREATE THIS
│   └── ... (Person 2's DTOs already here)
├── service/
│   ├── AuthService.java                   ← ALREADY EXISTS (Person 2)
│   └── InstructorService.java             ← YOU CREATE THIS
├── controller/
│   ├── AuthController.java                ← ALREADY EXISTS (Person 2)
│   └── InstructorController.java          ← YOU CREATE THIS
└── util/
    └── YoutubeUtil.java                   ← YOU CREATE THIS
```

> **IMPORTANT:** Do NOT create your own `User.java`, `UserRepository.java`, `SecurityConfig.java`, or any auth-related files. Person 2 owns those. You import them.

---

## File 1: `Course.java` (Entity)

```java
package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "instructor_id", nullable = false)
    private Integer instructorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "draft";
        }
    }

    // ===== Constructors =====

    public Course() {}

    public Course(Integer instructorId, String title, String description, String status) {
        this.instructorId = instructorId;
        this.title = title;
        this.description = description;
        this.status = status;
    }

    // ===== Getters and Setters =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
```

### Why `Integer instructorId` Instead of `@ManyToOne User instructor`?

Using a plain foreign key ID (`Integer instructorId`) instead of a JPA relationship (`@ManyToOne`) is a deliberate choice:
- Person 2 owns the `User` entity. If you use `@ManyToOne`, any change Person 2 makes to `User` could break your entity mapping.
- You don't need the full User object on every course query. You only need the instructor's name for display, and you can fetch that with a separate query when needed.
- It avoids lazy loading pitfalls (`LazyInitializationException`) that trip up many Spring Boot beginners.

If you need the instructor's name (for the catalog), do a join in the repository or fetch it separately.

---

## File 2: `Lecture.java` (Entity)

```java
package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lectures")
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "youtube_url", nullable = false, length = 500)
    private String youtubeUrl;

    @Column(name = "video_id", length = 20)
    private String videoId;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.orderIndex == null) {
            this.orderIndex = 0;
        }
    }

    // ===== Constructors =====

    public Lecture() {}

    public Lecture(Integer courseId, String title, String youtubeUrl, String videoId, Integer orderIndex) {
        this.courseId = courseId;
        this.title = title;
        this.youtubeUrl = youtubeUrl;
        this.videoId = videoId;
        this.orderIndex = orderIndex;
    }

    // ===== Getters and Setters =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
```

---

## File 3: `CourseRepository.java`

```java
package com.olp.repository;

import com.olp.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    /**
     * Find all courses owned by a specific instructor.
     * Used by: GET /api/instructor/courses
     */
    List<Course> findByInstructorIdOrderByCreatedAtDesc(Integer instructorId);

    /**
     * Find a specific course owned by a specific instructor.
     * Used by: GET /api/instructor/courses/:id
     * Returns empty if the course doesn't exist OR belongs to a different instructor.
     */
    Optional<Course> findByIdAndInstructorId(Integer id, Integer instructorId);

    /**
     * Find all published courses.
     * NOTE: Person 4 (Student Backend) also needs this method.
     * If Person 4 creates their own CourseRepository, this will conflict.
     * SOLUTION: You create this repository. Person 4 imports and uses it.
     */
    List<Course> findByStatusOrderByCreatedAtDesc(String status);
}
```

> **IMPORTANT for Person 4:** Person 3 owns `CourseRepository`. If you (Person 4) need to query courses, import `com.olp.repository.CourseRepository` and use its methods. Do NOT create a second repository for the same entity.

---

## File 4: `LectureRepository.java`

```java
package com.olp.repository;

import com.olp.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Integer> {

    /**
     * Get all lectures for a course, ordered by order_index.
     * Used by both instructor (manage course) and student (watch page).
     */
    List<Lecture> findByCourseIdOrderByOrderIndexAsc(Integer courseId);

    /**
     * Count lectures in a course.
     * Used to auto-calculate the next order_index when adding a new lecture.
     * Also used to show lecture count in course lists.
     */
    int countByCourseId(Integer courseId);
}
```

---

## File 5: `YoutubeUtil.java`

A utility to validate YouTube URLs and extract the video ID.

```java
package com.olp.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeUtil {

    // Matches these YouTube URL formats:
    // https://www.youtube.com/watch?v=dQw4w9WgXcQ
    // https://youtube.com/watch?v=dQw4w9WgXcQ
    // https://youtu.be/dQw4w9WgXcQ
    // https://www.youtube.com/embed/dQw4w9WgXcQ
    // https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=120
    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?(?:youtube\\.com/(?:watch\\?v=|embed/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    );

    /**
     * Extract the 11-character video ID from a YouTube URL.
     * Returns null if the URL is not a valid YouTube URL.
     *
     * Examples:
     *   "https://www.youtube.com/watch?v=dQw4w9WgXcQ" → "dQw4w9WgXcQ"
     *   "https://youtu.be/dQw4w9WgXcQ"                → "dQw4w9WgXcQ"
     *   "https://www.youtube.com/embed/dQw4w9WgXcQ"   → "dQw4w9WgXcQ"
     *   "https://www.google.com"                       → null
     *   ""                                              → null
     */
    public static String extractVideoId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Matcher matcher = YOUTUBE_PATTERN.matcher(url.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Check if a URL is a valid YouTube video URL.
     */
    public static boolean isValidYoutubeUrl(String url) {
        return extractVideoId(url) != null;
    }

    /**
     * Build the embeddable YouTube URL from a video ID.
     * This is what goes into the iframe src on the frontend.
     *
     * Example: "dQw4w9WgXcQ" → "https://www.youtube.com/embed/dQw4w9WgXcQ"
     */
    public static String buildEmbedUrl(String videoId) {
        return "https://www.youtube.com/embed/" + videoId;
    }

    /**
     * Build the thumbnail URL from a video ID.
     * YouTube provides free thumbnails at predictable URLs.
     *
     * Example: "dQw4w9WgXcQ" → "https://img.youtube.com/vi/dQw4w9WgXcQ/mqdefault.jpg"
     */
    public static String buildThumbnailUrl(String videoId) {
        return "https://img.youtube.com/vi/" + videoId + "/mqdefault.jpg";
    }

    // Private constructor — utility class, not instantiable
    private YoutubeUtil() {}
}
```

### YouTube Video ID Format
A YouTube video ID is always exactly **11 characters** and can contain:
- Letters (a-z, A-Z)
- Numbers (0-9)
- Hyphens (-)
- Underscores (_)

The regex `[a-zA-Z0-9_-]{11}` captures exactly this.

---

## File 6: DTOs

### `CreateCourseRequest.java`
```java
package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCourseRequest {

    @NotBlank(message = "Course title is required")
    @Size(max = 200, message = "Title must be 200 characters or fewer")
    private String title;

    @Size(max = 5000, message = "Description must be 5000 characters or fewer")
    private String description;

    // If null or not provided, defaults to "draft" in the entity
    private String status;

    // ===== Getters and Setters =====

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
```

### `AddLectureRequest.java`
```java
package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddLectureRequest {

    @NotBlank(message = "Lecture title is required")
    @Size(max = 200, message = "Title must be 200 characters or fewer")
    private String title;

    @NotBlank(message = "YouTube URL is required")
    private String youtubeUrl;

    // ===== Getters and Setters =====

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }
}
```

### `CourseListResponse.java`
```java
package com.olp.dto;

import java.time.LocalDateTime;

public class CourseListResponse {

    private Integer id;
    private String title;
    private String description;
    private String status;
    private int lectureCount;
    private LocalDateTime createdAt;

    public CourseListResponse(Integer id, String title, String description,
                               String status, int lectureCount, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.lectureCount = lectureCount;
        this.createdAt = createdAt;
    }

    // ===== Getters =====

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public int getLectureCount() { return lectureCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

### `CourseDetailResponse.java`
```java
package com.olp.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CourseDetailResponse {

    private Integer id;
    private String title;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private List<LectureResponse> lectures;

    public CourseDetailResponse(Integer id, String title, String description,
                                 String status, LocalDateTime createdAt,
                                 List<LectureResponse> lectures) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.lectures = lectures;
    }

    // ===== Getters =====

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<LectureResponse> getLectures() { return lectures; }
}
```

### `LectureResponse.java`
```java
package com.olp.dto;

public class LectureResponse {

    private Integer id;
    private String title;
    private String youtubeUrl;
    private String videoId;
    private String embedUrl;
    private String thumbnailUrl;
    private Integer orderIndex;

    public LectureResponse(Integer id, String title, String youtubeUrl,
                            String videoId, String embedUrl, String thumbnailUrl,
                            Integer orderIndex) {
        this.id = id;
        this.title = title;
        this.youtubeUrl = youtubeUrl;
        this.videoId = videoId;
        this.embedUrl = embedUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.orderIndex = orderIndex;
    }

    // ===== Getters =====

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public String getVideoId() { return videoId; }
    public String getEmbedUrl() { return embedUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public Integer getOrderIndex() { return orderIndex; }
}
```

---

## File 7: `InstructorService.java`

```java
package com.olp.service;

import com.olp.dto.*;
import com.olp.entity.Course;
import com.olp.entity.Lecture;
import com.olp.repository.CourseRepository;
import com.olp.repository.LectureRepository;
import com.olp.util.YoutubeUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InstructorService {

    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;

    public InstructorService(CourseRepository courseRepository,
                              LectureRepository lectureRepository) {
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
    }

    // ===============================
    // COURSES
    // ===============================

    /**
     * List all courses owned by the given instructor.
     * Returns them newest-first with lecture counts.
     */
    public List<CourseListResponse> getInstructorCourses(Integer instructorId) {
        List<Course> courses = courseRepository.findByInstructorIdOrderByCreatedAtDesc(instructorId);

        return courses.stream().map(course -> {
            int lectureCount = lectureRepository.countByCourseId(course.getId());
            return new CourseListResponse(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    course.getStatus(),
                    lectureCount,
                    course.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }

    /**
     * Create a new course for the given instructor.
     */
    public CourseDetailResponse createCourse(Integer instructorId, CreateCourseRequest request) {
        // Validate status if provided
        String status = request.getStatus();
        if (status == null || status.isBlank()) {
            status = "published"; // Default to published for Sprint 1 simplicity
        } else {
            status = status.toLowerCase().trim();
            if (!status.equals("draft") && !status.equals("published")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Status must be 'draft' or 'published'");
            }
        }

        Course course = new Course(
                instructorId,
                request.getTitle().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                status
        );
        course = courseRepository.save(course);

        return new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getStatus(),
                course.getCreatedAt(),
                List.of() // New course has no lectures yet
        );
    }

    /**
     * Get a single course with its lectures.
     * Only returns the course if it belongs to the given instructor.
     * Throws 404 if the course doesn't exist or belongs to someone else.
     */
    public CourseDetailResponse getCourseDetail(Integer courseId, Integer instructorId) {
        Course course = courseRepository.findByIdAndInstructorId(courseId, instructorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Course not found"));

        List<Lecture> lectures = lectureRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        List<LectureResponse> lectureResponses = lectures.stream()
                .map(this::toLectureResponse)
                .collect(Collectors.toList());

        return new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getStatus(),
                course.getCreatedAt(),
                lectureResponses
        );
    }

    // ===============================
    // LECTURES
    // ===============================

    /**
     * Add a lecture to a course.
     * Validates that:
     *   1. The course exists and belongs to this instructor
     *   2. The YouTube URL is valid
     * Auto-calculates the order_index.
     */
    public LectureResponse addLecture(Integer courseId, Integer instructorId,
                                       AddLectureRequest request) {
        // 1. Verify the course exists and belongs to this instructor
        Course course = courseRepository.findByIdAndInstructorId(courseId, instructorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Course not found"));

        // 2. Validate and extract the YouTube video ID
        String videoId = YoutubeUtil.extractVideoId(request.getYoutubeUrl());
        if (videoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid YouTube URL. Accepted formats: " +
                    "youtube.com/watch?v=..., youtu.be/..., youtube.com/embed/...");
        }

        // 3. Calculate the next order_index (append at the end)
        int nextOrder = lectureRepository.countByCourseId(courseId) + 1;

        // 4. Create and save the lecture
        Lecture lecture = new Lecture(
                courseId,
                request.getTitle().trim(),
                request.getYoutubeUrl().trim(),
                videoId,
                nextOrder
        );
        lecture = lectureRepository.save(lecture);

        return toLectureResponse(lecture);
    }

    // ===============================
    // HELPERS
    // ===============================

    /**
     * Convert a Lecture entity to a LectureResponse DTO.
     * Builds the embed URL and thumbnail URL from the stored video ID.
     */
    private LectureResponse toLectureResponse(Lecture lecture) {
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
    }
}
```

---

## File 8: `InstructorController.java`

```java
package com.olp.controller;

import com.olp.dto.*;
import com.olp.entity.User;
import com.olp.service.InstructorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instructor")
public class InstructorController {

    private final InstructorService instructorService;

    public InstructorController(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    /**
     * GET /api/instructor/courses
     * List all courses owned by the currently logged-in instructor.
     */
    @PreAuthorize("hasRole('instructor')")
    @GetMapping("/courses")
    public ResponseEntity<Map<String, Object>> listCourses(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        List<CourseListResponse> courses = instructorService.getInstructorCourses(currentUser.getId());

        return ResponseEntity.ok(Map.of("courses", courses));
    }

    /**
     * POST /api/instructor/courses
     * Create a new course.
     */
    @PreAuthorize("hasRole('instructor')")
    @PostMapping("/courses")
    public ResponseEntity<Map<String, Object>> createCourse(
            Authentication authentication,
            @Valid @RequestBody CreateCourseRequest request) {

        User currentUser = (User) authentication.getPrincipal();

        CourseDetailResponse course = instructorService.createCourse(currentUser.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Course created",
                        "course", course
                ));
    }

    /**
     * GET /api/instructor/courses/{id}
     * Get a single course with its lectures.
     * Only returns the course if it belongs to the logged-in instructor.
     */
    @PreAuthorize("hasRole('instructor')")
    @GetMapping("/courses/{id}")
    public ResponseEntity<Map<String, Object>> getCourse(
            Authentication authentication,
            @PathVariable Integer id) {

        User currentUser = (User) authentication.getPrincipal();

        CourseDetailResponse course = instructorService.getCourseDetail(id, currentUser.getId());

        return ResponseEntity.ok(Map.of("course", course));
    }

    /**
     * POST /api/instructor/courses/{id}/lectures
     * Add a lecture to a course.
     */
    @PreAuthorize("hasRole('instructor')")
    @PostMapping("/courses/{id}/lectures")
    public ResponseEntity<Map<String, Object>> addLecture(
            Authentication authentication,
            @PathVariable Integer id,
            @Valid @RequestBody AddLectureRequest request) {

        User currentUser = (User) authentication.getPrincipal();

        LectureResponse lecture = instructorService.addLecture(id, currentUser.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Lecture added",
                        "lecture", lecture
                ));
    }
}
```

---

## API Contract Reference

This is the exact JSON shape Person 5 (frontend) will code against. Do not change these without telling Person 5.

### GET `/api/instructor/courses`

**Request:**
```
GET /api/instructor/courses
Authorization: Bearer <instructor_token>
```

**Success (200):**
```json
{
  "courses": [
    {
      "id": 1,
      "title": "Web Development Fundamentals",
      "description": "Learn the core building blocks of the web...",
      "status": "published",
      "lectureCount": 3,
      "createdAt": "2026-09-25T10:00:00"
    },
    {
      "id": 2,
      "title": "Introduction to Databases",
      "description": "Understand how data is stored...",
      "status": "published",
      "lectureCount": 2,
      "createdAt": "2026-09-24T14:30:00"
    }
  ]
}
```

**403 (student tries to access):**
Spring Security returns 403 Forbidden automatically because of `@PreAuthorize("hasRole('instructor')")`.

---

### POST `/api/instructor/courses`

**Request:**
```
POST /api/instructor/courses
Authorization: Bearer <instructor_token>
Content-Type: application/json

{
  "title": "New Course Title",
  "description": "A description of the course",
  "status": "published"
}
```

**Success (201):**
```json
{
  "message": "Course created",
  "course": {
    "id": 5,
    "title": "New Course Title",
    "description": "A description of the course",
    "status": "published",
    "createdAt": "2026-09-30T23:00:00",
    "lectures": []
  }
}
```

**Validation Error (400):**
```json
{
  "status": 400,
  "error": "title: Course title is required",
  "timestamp": "2026-09-30T23:00:00"
}
```

---

### GET `/api/instructor/courses/{id}`

**Request:**
```
GET /api/instructor/courses/1
Authorization: Bearer <instructor_token>
```

**Success (200):**
```json
{
  "course": {
    "id": 1,
    "title": "Web Development Fundamentals",
    "description": "Learn the core building blocks of the web...",
    "status": "published",
    "createdAt": "2026-09-25T10:00:00",
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
      }
    ]
  }
}
```

**Not Found or Wrong Owner (404):**
```json
{
  "status": 404,
  "error": "Course not found",
  "timestamp": "2026-09-30T23:00:00"
}
```

---

### POST `/api/instructor/courses/{id}/lectures`

**Request:**
```
POST /api/instructor/courses/1/lectures
Authorization: Bearer <instructor_token>
Content-Type: application/json

{
  "title": "JavaScript Basics",
  "youtubeUrl": "https://www.youtube.com/watch?v=W6NZfCO5SIk"
}
```

**Success (201):**
```json
{
  "message": "Lecture added",
  "lecture": {
    "id": 9,
    "title": "JavaScript Basics",
    "youtubeUrl": "https://www.youtube.com/watch?v=W6NZfCO5SIk",
    "videoId": "W6NZfCO5SIk",
    "embedUrl": "https://www.youtube.com/embed/W6NZfCO5SIk",
    "thumbnailUrl": "https://img.youtube.com/vi/W6NZfCO5SIk/mqdefault.jpg",
    "orderIndex": 4
  }
}
```

**Invalid YouTube URL (400):**
```json
{
  "status": 400,
  "error": "Invalid YouTube URL. Accepted formats: youtube.com/watch?v=..., youtu.be/..., youtube.com/embed/...",
  "timestamp": "2026-09-30T23:00:00"
}
```

**Course Not Found or Wrong Owner (404):**
```json
{
  "status": 404,
  "error": "Course not found",
  "timestamp": "2026-09-30T23:00:00"
}
```

---

## Testing with Postman

**Prerequisites:** 
- Pranamya's database is running (`docker-compose up -d`)
- Person 2's auth module is deployed (or you run the whole app)
- Login as `sarah@instructor.com` / `password123` and save the token

### Test 1: Login as an instructor and save the token
```
POST http://localhost:8080/api/auth/login
Body: { "email": "sarah@instructor.com", "password": "password123" }
→ Save the "token" from the response
```

### Test 2: List instructor's courses
```
GET http://localhost:8080/api/instructor/courses
Headers: Authorization: Bearer <token>
Expected: 200, courses array with 2 courses (Sarah's courses from seed data)
```

### Test 3: List courses as a student (should be blocked)
```
Login as alice@student.com, get her token, then:
GET http://localhost:8080/api/instructor/courses
Headers: Authorization: Bearer <alice_token>
Expected: 403 Forbidden
```

### Test 4: Create a new course
```
POST http://localhost:8080/api/instructor/courses
Headers: Authorization: Bearer <sarah_token>
Body: {
  "title": "Advanced CSS Techniques",
  "description": "Master flexbox, grid, and animations",
  "status": "published"
}
Expected: 201, course object with id and empty lectures array
```

### Test 5: Create a course with blank title
```
POST http://localhost:8080/api/instructor/courses
Headers: Authorization: Bearer <sarah_token>
Body: { "title": "", "description": "No title" }
Expected: 400, validation error
```

### Test 6: View course detail with lectures
```
GET http://localhost:8080/api/instructor/courses/1
Headers: Authorization: Bearer <sarah_token>
Expected: 200, course object with lectures array containing 3 lectures
```

### Test 7: View another instructor's course
```
GET http://localhost:8080/api/instructor/courses/3
Headers: Authorization: Bearer <sarah_token>
(Course 3 belongs to James, not Sarah)
Expected: 404 "Course not found"
```

### Test 8: Add a lecture with a valid YouTube URL
```
POST http://localhost:8080/api/instructor/courses/1/lectures
Headers: Authorization: Bearer <sarah_token>
Body: {
  "title": "JavaScript Basics",
  "youtubeUrl": "https://www.youtube.com/watch?v=W6NZfCO5SIk"
}
Expected: 201, lecture object with extracted videoId and auto-calculated orderIndex (4)
```

### Test 9: Add a lecture with youtu.be short URL
```
POST http://localhost:8080/api/instructor/courses/1/lectures
Headers: Authorization: Bearer <sarah_token>
Body: {
  "title": "JS Functions",
  "youtubeUrl": "https://youtu.be/FOD408a0EzU"
}
Expected: 201, videoId should be "FOD408a0EzU", orderIndex should be 5
```

### Test 10: Add a lecture with an invalid URL
```
POST http://localhost:8080/api/instructor/courses/1/lectures
Headers: Authorization: Bearer <sarah_token>
Body: {
  "title": "Bad Lecture",
  "youtubeUrl": "https://www.google.com"
}
Expected: 400, "Invalid YouTube URL..."
```

### Test 11: Add a lecture to someone else's course
```
POST http://localhost:8080/api/instructor/courses/3/lectures
Headers: Authorization: Bearer <sarah_token>
Body: {
  "title": "Sneaky Lecture",
  "youtubeUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
}
Expected: 404 "Course not found" (not 403 — don't reveal the course exists)
```

### Test 12: Verify course detail now shows new lectures
```
GET http://localhost:8080/api/instructor/courses/1
Headers: Authorization: Bearer <sarah_token>
Expected: 200, lectures array now has 5 items (3 seed + 2 added in Tests 8-9)
```

---

## What Person 4 (Student Backend) Needs from You

Person 4 will import and use your repositories and entities. Confirm these to them:

| Item | Details |
|---|---|
| **`CourseRepository` location** | `com.olp.repository.CourseRepository` |
| **Method to get published courses** | `findByStatusOrderByCreatedAtDesc("published")` |
| **`LectureRepository` location** | `com.olp.repository.LectureRepository` |
| **Method to get lectures for a course** | `findByCourseIdOrderByOrderIndexAsc(courseId)` |
| **Method to count lectures** | `countByCourseId(courseId)` |
| **`Course` entity location** | `com.olp.entity.Course` |
| **`Lecture` entity location** | `com.olp.entity.Lecture` |
| **`LectureResponse` DTO location** | `com.olp.dto.LectureResponse` — Person 4 can reuse this for the student watch page |
| **`YoutubeUtil` location** | `com.olp.util.YoutubeUtil` — Person 4 can use `buildEmbedUrl()` if needed |

---

## What Person 5 (Instructor Frontend) Needs from You

Person 5 builds the UI that calls your endpoints. Confirm:

| Item | Details |
|---|---|
| **Base URL** | `http://localhost:8080/api/instructor` |
| **Auth header format** | `Authorization: Bearer <token>` on every request |
| **List courses** | `GET /api/instructor/courses` → `{ courses: [...] }` |
| **Create course** | `POST /api/instructor/courses` → `{ message, course }` |
| **Course detail** | `GET /api/instructor/courses/:id` → `{ course: { ..., lectures: [...] } }` |
| **Add lecture** | `POST /api/instructor/courses/:id/lectures` → `{ message, lecture }` |
| **Thumbnail URL** | Each lecture response includes `thumbnailUrl` — use it as the `src` for an `<img>` tag |
| **Embed URL** | Each lecture response includes `embedUrl` — use it as the `src` for an `<iframe>` tag |

---

## Verification Checklist

- [ ] Application starts without entity mapping errors
- [ ] `Course` entity maps correctly to Pranamya's `courses` table
- [ ] `Lecture` entity maps correctly to Pranamya's `lectures` table
- [ ] All 12 Postman tests pass
- [ ] Students get 403 on all `/api/instructor/*` endpoints
- [ ] Instructor A cannot see or modify Instructor B's courses (always 404, never 403)
- [ ] YouTube URL validation accepts `youtube.com/watch?v=`, `youtu.be/`, and `youtube.com/embed/` formats
- [ ] YouTube URL validation rejects non-YouTube URLs
- [ ] Video ID is correctly extracted and stored in the `video_id` column
- [ ] `order_index` auto-increments correctly (1, 2, 3, ...)
- [ ] `embedUrl` and `thumbnailUrl` are correctly constructed in every lecture response
- [ ] Creating a course with no status defaults to "published"
- [ ] Person 4 can import `CourseRepository`, `LectureRepository`, and `Course` entity without conflicts

---

## Common Pitfalls

| Pitfall | How to Avoid |
|---|---|
| Creating a duplicate `User` entity | Do NOT create your own `User.java`. Import Person 2's `com.olp.entity.User`. Two `@Entity` classes mapped to the same table causes Hibernate to crash. |
| Creating a duplicate `CourseRepository` | You own this. Tell Person 4 to import yours, not create their own. Two `@Repository` interfaces for the same entity cause Spring autowiring conflicts. |
| Using `@ManyToOne` for `instructorId` | Avoid it. Use a plain `Integer instructorId` column. See the explanation in the Course entity section above. |
| Returning 403 for "wrong owner" | Return 404 instead. If Instructor A queries Instructor B's course, saying "forbidden" reveals that the course exists. Saying "not found" reveals nothing. |
| Forgetting `@PreAuthorize` on a method | Every single method in `InstructorController` must have `@PreAuthorize("hasRole('instructor')")`. Missing it on even one endpoint lets students access it. |
| `GenerationType.AUTO` on entities | Use `GenerationType.IDENTITY`. Same reason as Person 2 — Pranamya's tables use `SERIAL`. |
| YouTube regex doesn't handle edge cases | The regex in `YoutubeUtil` handles the 4 most common formats. If you find a format it misses during testing, update the regex — but don't over-engineer it for Sprint 1. |

---

## Git Branch

Work on branch: `feature/instructor-backend`

```bash
git checkout -b feature/instructor-backend
# ... do all your work ...
git add .
git commit -m "feat: add instructor module (course CRUD, lecture CRUD, YouTube validation)"
git push origin feature/instructor-backend
# Then open a Pull Request to main
```
