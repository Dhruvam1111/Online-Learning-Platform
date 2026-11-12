package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lecture_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "lecture_id"})
})
public class LectureProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "lecture_id", nullable = false)
    private Integer lectureId;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt = LocalDateTime.now();

    @Column(name = "playback_position_seconds", nullable = false)
    private Integer playbackPositionSeconds = 0;

    public LectureProgress() {}

    public LectureProgress(Integer studentId, Integer lectureId) {
        this.studentId = studentId;
        this.lectureId = lectureId;
        this.completedAt = LocalDateTime.now();
        this.playbackPositionSeconds = 0;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }

    public Integer getLectureId() { return lectureId; }
    public void setLectureId(Integer lectureId) { this.lectureId = lectureId; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Integer getPlaybackPositionSeconds() { return playbackPositionSeconds; }
    public void setPlaybackPositionSeconds(Integer playbackPositionSeconds) { 
        this.playbackPositionSeconds = playbackPositionSeconds != null ? playbackPositionSeconds : 0; 
    }
}