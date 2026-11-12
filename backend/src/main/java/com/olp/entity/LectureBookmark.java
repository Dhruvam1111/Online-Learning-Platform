package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_bookmarks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "lecture_id"})
})
public class LectureBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "lecture_id", nullable = false)
    private Integer lectureId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public LectureBookmark() {}

    public LectureBookmark(Integer studentId, Integer lectureId) {
        this.studentId = studentId;
        this.lectureId = lectureId;
        this.createdAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }

    public Integer getLectureId() { return lectureId; }
    public void setLectureId(Integer lectureId) { this.lectureId = lectureId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}