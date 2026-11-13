package com.olp.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
public class Course {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String category = "General";

    @Column(name = "learning_outcomes", columnDefinition = "TEXT")
    private String learningOutcomes;

    @Convert(converter = CourseStatusConverter.class)
    @Column(nullable = false, length = 20)
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "discount_start_date")
    private LocalDateTime discountStartDate;

    @Column(name = "discount_end_date")
    private LocalDateTime discountEndDate;

    @Column(name = "enrollment_limit")
    private Integer enrollmentLimit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoursePrerequisite> prerequisites = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseInstructor> instructors = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<CourseModule> modules = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = CourseStatus.DRAFT;
        }
        if (this.category == null || this.category.trim().isEmpty()) {
            this.category = "General";
        }
        if (this.price == null) {
            this.price = BigDecimal.ZERO;
        }
        if (this.discountValue == null) {
            this.discountValue = BigDecimal.ZERO;
        }
    }

    // Constructors

    public Course() {
    }

    public Course(User instructor, String title, String description,
            CourseStatus status) {
        this.instructor = instructor;
        this.title = title;
        this.description = description;
        this.status = status != null ? status : CourseStatus.DRAFT;
        this.category = "General";
    }

    public Course(User instructor, String title, String description,
            String status) {
        this(instructor, title, description, status != null ? CourseStatus.fromString(status) : CourseStatus.DRAFT);
    }

    public Course(User instructor, String title, String description) {
        this(instructor, title, description, CourseStatus.DRAFT);
    }

    public Course(User instructor, String title, String description,
            String category, CourseStatus status) {
        this(instructor, title, description, status);
        this.category = category != null && !category.trim().isEmpty() ? category.trim() : "General";
    }

    public Course(User instructor, String title, String description,
            CourseStatus status, String rejectionReason) {
        this(instructor, title, description, status);
        this.rejectionReason = rejectionReason;
    }

    public Course(User instructor, String title, String description,
            String status, String rejectionReason) {
        this(instructor, title, description, status != null ? CourseStatus.fromString(status) : CourseStatus.DRAFT,
                rejectionReason);
    }

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getInstructor() {
        return instructor;
    }

    public void setInstructor(User instructor) {
        this.instructor = instructor;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category != null && !category.trim().isEmpty() ? category.trim() : "General";
    }

    public String getLearningOutcomes() {
        return learningOutcomes;
    }

    public void setLearningOutcomes(String learningOutcomes) {
        this.learningOutcomes = learningOutcomes;
    }

    public List<String> getLearningOutcomesList() {
        if (learningOutcomes == null || learningOutcomes.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            if (learningOutcomes.trim().startsWith("[")) {
                return OBJECT_MAPPER.readValue(learningOutcomes, new TypeReference<List<String>>() {
                });
            }
            List<String> list = new ArrayList<>();
            for (String line : learningOutcomes.split("\r?\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
            return list;
        } catch (Exception e) {
            List<String> list = new ArrayList<>();
            for (String line : learningOutcomes.split("\r?\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
            return list;
        }
    }

    public void setLearningOutcomesList(List<String> outcomes) {
        if (outcomes == null || outcomes.isEmpty()) {
            this.learningOutcomes = null;
            return;
        }
        try {
            this.learningOutcomes = OBJECT_MAPPER.writeValueAsString(outcomes);
        } catch (Exception e) {
            this.learningOutcomes = String.join("\n", outcomes);
        }
    }

    public CourseStatus getStatus() {
        return status;
    }

    public void setStatus(CourseStatus status) {
        this.status = status;
    }

    public void setStatus(String status) {
        this.status = status != null ? CourseStatus.fromString(status) : null;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getReviewComments() {
        return reviewComments;
    }

    public void setReviewComments(String reviewComments) {
        this.reviewComments = reviewComments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<CoursePrerequisite> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(List<CoursePrerequisite> prerequisites) {
        this.prerequisites = prerequisites != null ? prerequisites : new ArrayList<>();
    }

    public List<CourseInstructor> getInstructors() {
        return instructors;
    }

    public void setInstructors(List<CourseInstructor> instructors) {
        this.instructors = instructors != null ? instructors : new ArrayList<>();
    }

    public List<CourseModule> getModules() {
        return modules;
    }

    public void setModules(List<CourseModule> modules) {
        this.modules = modules != null ? modules : new ArrayList<>();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price != null ? price : BigDecimal.ZERO;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue != null ? discountValue : BigDecimal.ZERO;
    }

    public LocalDateTime getDiscountStartDate() {
        return discountStartDate;
    }

    public void setDiscountStartDate(LocalDateTime discountStartDate) {
        this.discountStartDate = discountStartDate;
    }

    public LocalDateTime getDiscountEndDate() {
        return discountEndDate;
    }

    public void setDiscountEndDate(LocalDateTime discountEndDate) {
        this.discountEndDate = discountEndDate;
    }

    public Integer getEnrollmentLimit() {
        return enrollmentLimit;
    }

    public void setEnrollmentLimit(Integer enrollmentLimit) {
        this.enrollmentLimit = enrollmentLimit;
    }

    public boolean isFree() {
        return price == null || price.compareTo(BigDecimal.ZERO) == 0;
    }
}
