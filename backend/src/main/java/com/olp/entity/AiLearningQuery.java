package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity logging student queries and AI-assisted responses for course learning,
 * video summaries, and question explanations (FR-S71, FR-S72, FR-S73, FR-S74, FR-S75, FR-S76).
 */
@Entity
@Table(name = "ai_learning_queries")
public class AiLearningQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "query_type", length = 50, nullable = false)
    private String queryType;

    @Column(name = "context_type", length = 50)
    private String contextType;

    @Column(name = "context_id")
    private Integer contextId;

    @Column(name = "prompt_text", columnDefinition = "TEXT", nullable = false)
    private String promptText;

    @Column(name = "response_text", columnDefinition = "TEXT", nullable = false)
    private String responseText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AiLearningQuery() {}

    public AiLearningQuery(User student, Course course, String queryType, String contextType,
                           Integer contextId, String promptText, String responseText) {
        this.student = student;
        this.course = course;
        this.queryType = queryType;
        this.contextType = contextType;
        this.contextId = contextId;
        this.promptText = promptText;
        this.responseText = responseText;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public String getContextType() {
        return contextType;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }

    public Integer getContextId() {
        return contextId;
    }

    public void setContextId(Integer contextId) {
        this.contextId = contextId;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
