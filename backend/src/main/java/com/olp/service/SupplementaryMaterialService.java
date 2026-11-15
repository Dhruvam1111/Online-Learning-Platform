package com.olp.service;

import com.olp.dto.ContentRemovalResponse;
import com.olp.dto.SupplementaryMaterialResponse;
import com.olp.entity.*;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service managing supplementary learning materials upload, reorder, replace, remove, and security validation
 * (FR-I14, FR-I16, FR-I17).
 */
@Service
@Transactional
public class SupplementaryMaterialService {

    private static final Logger logger = LoggerFactory.getLogger(SupplementaryMaterialService.class);

    private final LectureMaterialRepository materialRepository;
    private final LessonRepository lessonRepository;
    private final CourseModuleRepository moduleRepository;
    private final LectureRepository lectureRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private final MaterialStorageService materialStorageService;
    private final VirusScanner virusScanner;
    private final ContentDeclarationService contentDeclarationService;
    private final LectureProgressRepository lectureProgressRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseCompletionCriteriaRepository courseCompletionCriteriaRepository;

    @Autowired
    public SupplementaryMaterialService(
            LectureMaterialRepository materialRepository,
            LessonRepository lessonRepository,
            CourseModuleRepository moduleRepository,
            LectureRepository lectureRepository,
            CourseInstructorRepository courseInstructorRepository,
            UserRepository userRepository,
            MaterialStorageService materialStorageService,
            VirusScanner virusScanner,
            ContentDeclarationService contentDeclarationService,
            @Autowired(required = false) LectureProgressRepository lectureProgressRepository,
            @Autowired(required = false) LessonProgressRepository lessonProgressRepository,
            @Autowired(required = false) CourseCompletionCriteriaRepository courseCompletionCriteriaRepository) {
        this.materialRepository = materialRepository;
        this.lessonRepository = lessonRepository;
        this.moduleRepository = moduleRepository;
        this.lectureRepository = lectureRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
        this.materialStorageService = materialStorageService;
        this.virusScanner = virusScanner;
        this.contentDeclarationService = contentDeclarationService;
        this.lectureProgressRepository = lectureProgressRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.courseCompletionCriteriaRepository = courseCompletionCriteriaRepository;
    }

    public SupplementaryMaterialService(
            LectureMaterialRepository materialRepository,
            LessonRepository lessonRepository,
            CourseModuleRepository moduleRepository,
            LectureRepository lectureRepository,
            CourseInstructorRepository courseInstructorRepository,
            UserRepository userRepository,
            MaterialStorageService materialStorageService,
            VirusScanner virusScanner,
            ContentDeclarationService contentDeclarationService) {
        this(materialRepository, lessonRepository, moduleRepository, lectureRepository,
                courseInstructorRepository, userRepository, materialStorageService, virusScanner,
                contentDeclarationService, null, null, null);
    }

    /**
     * Resolves the authenticated User from the security context authentication.
     */
    public User resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Upload a supplementary material file with MIME, size, and virus scan validations (FR-I14).
     */
    public SupplementaryMaterialResponse uploadMaterial(
            User instructor,
            Integer lessonId,
            Integer moduleId,
            Integer lectureId,
            String title,
            MultipartFile file) {

        validateInstructorRole(instructor);

        if (lessonId == null && moduleId == null && lectureId == null) {
            throw new IllegalArgumentException("Material must be attached to a lesson, module, or lecture");
        }

        Course targetCourse = null;

        if (lessonId != null) {
            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));
            targetCourse = extractCourseFromLesson(lesson);
            // Gate: Content ownership declaration check (Step 14 / FR-I64)
            contentDeclarationService.assertDeclared(lessonId);
        } else if (moduleId != null) {
            CourseModule module = moduleRepository.findById(moduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));
            targetCourse = module.getCourse();
        } else {
            Lecture lecture = lectureRepository.findById(lectureId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with id: " + lectureId));
            targetCourse = lecture.getCourse();
        }

        assertInstructorAccess(targetCourse, instructor);

        // 1. File size and MIME type validation
        materialStorageService.validateMaterial(file);

        // 2. Pre-storage Virus and Malware Scan Hook
        try (InputStream inputStream = file.getInputStream()) {
            VirusScanner.ScanResult scanResult = virusScanner.scan(inputStream, file.getOriginalFilename());
            if (!scanResult.isClean()) {
                logger.warn("Virus/malware detected during upload of {}: {}", file.getOriginalFilename(), scanResult.getThreatName());
                throw new IllegalArgumentException("Malicious content detected in file: " + scanResult.getThreatName());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read file stream for virus scan", e);
        }

        // 3. Store file to disk
        String storedPath;
        try {
            storedPath = materialStorageService.storeMaterial(file);
        } catch (IOException e) {
            logger.error("Failed to store material file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Failed to persist material file to storage", e);
        }

        String finalTitle = (title != null && !title.isBlank()) ? title : file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileType = ext != null ? ext.toLowerCase(Locale.ROOT) : "file";

        // 4. Create material record - Defaults to view-only (isDownloadable = false)
        LectureMaterial material = new LectureMaterial(
                lessonId,
                moduleId,
                lectureId,
                instructor.getId(),
                finalTitle,
                file.getOriginalFilename(),
                storedPath,
                fileType,
                file.getContentType(),
                file.getSize(),
                false // default view-only
        );

        if (lessonId != null) {
            Integer nextOrder = materialRepository.findMaxOrderIndexByLessonId(lessonId) + 1;
            material.setOrderIndex(nextOrder);
        }

        LectureMaterial saved = materialRepository.save(material);
        return SupplementaryMaterialResponse.fromEntity(saved);
    }

    /**
     * Retrieve all active materials attached to a lesson ordered by order_index.
     */
    @Transactional(readOnly = true)
    public List<SupplementaryMaterialResponse> getMaterialsByLesson(User user, Integer lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));
        Course course = extractCourseFromLesson(lesson);
        assertInstructorAccess(course, user);

        return materialRepository.findByLessonId(lessonId)
                .stream()
                .filter(m -> m.getDeletedAt() == null)
                .sorted(Comparator.comparing(LectureMaterial::getOrderIndex, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LectureMaterial::getId, Comparator.nullsLast(Integer::compareTo)))
                .map(SupplementaryMaterialResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all active materials attached to a module.
     */
    @Transactional(readOnly = true)
    public List<SupplementaryMaterialResponse> getMaterialsByModule(User user, Integer moduleId) {
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));
        assertInstructorAccess(module.getCourse(), user);

        return materialRepository.findByModuleId(moduleId)
                .stream()
                .filter(m -> m.getDeletedAt() == null)
                .sorted(Comparator.comparing(LectureMaterial::getOrderIndex, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LectureMaterial::getId, Comparator.nullsLast(Integer::compareTo)))
                .map(SupplementaryMaterialResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve single active material by ID.
     */
    @Transactional(readOnly = true)
    public SupplementaryMaterialResponse getMaterialById(User user, Integer id) {
        LectureMaterial material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + id));

        if (material.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Material not found with id: " + id);
        }

        Course course = resolveCourseFromMaterial(material);
        assertInstructorAccess(course, user);

        return SupplementaryMaterialResponse.fromEntity(material);
    }

    /**
     * Toggles or updates the downloadable vs view-only status of a material (FR-I17, Step 16).
     */
    public SupplementaryMaterialResponse updateDownloadableStatus(User instructor, Integer materialId, boolean isDownloadable) {
        validateInstructorRole(instructor);

        LectureMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + materialId));

        if (material.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Material not found with id: " + materialId);
        }

        Course course = resolveCourseFromMaterial(material);
        assertInstructorAccess(course, instructor);

        material.setIsDownloadable(isDownloadable);
        LectureMaterial saved = materialRepository.save(material);
        logger.info("Updated material ID {} downloadable status to {} by user ID {}",
                materialId, isDownloadable, instructor.getId());

        return SupplementaryMaterialResponse.fromEntity(saved);
    }

    /**
     * Reorders materials inside a lesson according to orderedIds (FR-I16, Step 19).
     * Enforces contiguous 1-based order_index and rejects IDs that do not belong to the target lesson.
     */
    public List<SupplementaryMaterialResponse> reorderMaterialsInLesson(
            User instructor, Integer lessonId, List<Integer> orderedIds) {

        validateInstructorRole(instructor);

        if (lessonId == null) {
            throw new IllegalArgumentException("Lesson ID cannot be null");
        }
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));

        Course course = extractCourseFromLesson(lesson);
        assertInstructorAccess(course, instructor);

        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalArgumentException("Ordered IDs list cannot be empty");
        }

        // Validate no nulls and no duplicates
        Set<Integer> seen = new HashSet<>();
        for (Integer id : orderedIds) {
            if (id == null) {
                throw new IllegalArgumentException("Material ID in reorder list cannot be null");
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Duplicate material ID in reorder list: " + id);
            }
        }

        // Fetch active materials in the target lesson
        List<LectureMaterial> activeMaterials = materialRepository.findByLessonId(lessonId)
                .stream()
                .filter(m -> m.getDeletedAt() == null)
                .toList();

        Map<Integer, LectureMaterial> materialMap = activeMaterials.stream()
                .collect(Collectors.toMap(LectureMaterial::getId, Function.identity()));

        // Check that every ID in orderedIds belongs to this lesson
        for (Integer id : orderedIds) {
            if (!materialMap.containsKey(id)) {
                throw new IllegalArgumentException("Material ID " + id + " does not belong to lesson ID " + lessonId);
            }
        }

        // Update contiguous orderIndex (1, 2, 3...)
        for (int i = 0; i < orderedIds.size(); i++) {
            Integer id = orderedIds.get(i);
            LectureMaterial mat = materialMap.get(id);
            mat.setOrderIndex(i + 1);
            materialRepository.save(mat);
        }

        logger.info("Reordered {} materials in lesson {} by user {}", orderedIds.size(), lessonId, instructor.getId());

        return getMaterialsByLesson(instructor, lessonId);
    }

    /**
     * Replaces the file of a supplementary material item while preserving lesson, order, and progress references (FR-I16, Step 19).
     */
    public SupplementaryMaterialResponse replaceMaterialFile(
            User instructor, Integer materialId, MultipartFile file) {

        validateInstructorRole(instructor);

        if (materialId == null) {
            throw new IllegalArgumentException("Material ID cannot be null");
        }

        LectureMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + materialId));

        if (material.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Cannot replace file of deleted material with id: " + materialId);
        }

        Course course = resolveCourseFromMaterial(material);
        assertInstructorAccess(course, instructor);

        // 1. File size, non-empty, and MIME type validation
        materialStorageService.validateMaterial(file);

        // 2. Pre-storage virus and malware scan
        try (InputStream inputStream = file.getInputStream()) {
            VirusScanner.ScanResult scanResult = virusScanner.scan(inputStream, file.getOriginalFilename());
            if (!scanResult.isClean()) {
                logger.warn("Virus/malware detected during material replacement {}: {}", file.getOriginalFilename(), scanResult.getThreatName());
                throw new IllegalArgumentException("Malicious content detected in file: " + scanResult.getThreatName());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read file stream for virus scan", e);
        }

        // 3. Remove old physical file from disk
        materialStorageService.deleteMaterialFile(material.getFileUrl());

        // 4. Store replacement file
        String storedPath;
        try {
            storedPath = materialStorageService.storeMaterial(file);
        } catch (IOException e) {
            logger.error("Failed to store replacement material file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Failed to persist replacement material file to storage", e);
        }

        // 5. Update metadata while strictly preserving id, lessonId, moduleId, lectureId, createdAt, orderIndex, isDownloadable
        String originalFilename = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalFilename);
        String fileType = ext != null ? ext.toLowerCase(Locale.ROOT) : "file";

        material.setFileUrl(storedPath);
        material.setOriginalFilename(originalFilename);
        material.setFileType(fileType);
        material.setMimeType(file.getContentType());
        material.setFileSizeBytes(file.getSize());
        material.setScanStatus("CLEAN");
        material.setUpdatedAt(LocalDateTime.now());

        LectureMaterial saved = materialRepository.save(material);
        logger.info("Successfully replaced material file for ID {} with {} by user ID {}",
                materialId, originalFilename, instructor.getId());

        return SupplementaryMaterialResponse.fromEntity(saved);
    }

    /**
     * Removes content with confirmation (FR-I16, Step 19).
     * Soft-deletes when students already have progress, hard-deletes otherwise.
     * Evaluates course completion criteria and includes warning flag in the response.
     */
    public ContentRemovalResponse removeMaterial(User instructor, Integer materialId) {
        validateInstructorRole(instructor);

        if (materialId == null) {
            throw new IllegalArgumentException("Material ID cannot be null");
        }

        LectureMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + materialId));

        if (material.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Material already removed with id: " + materialId);
        }

        Course course = resolveCourseFromMaterial(material);
        assertInstructorAccess(course, instructor);

        boolean progressExists = hasStudentProgress(material);
        boolean criteriaWarning = checkCompletionCriteriaWarning(course.getId(), material.getLessonId());

        String warning = null;
        if (criteriaWarning) {
            warning = "Warning: This content item is part of the course completion criteria (mandatory lesson " + material.getLessonId() + ")";
        }

        if (progressExists) {
            // Soft-delete: retain database record and disk file for learner history
            material.setDeletedAt(LocalDateTime.now());
            materialRepository.save(material);

            String progressWarning = "Content item has active student progress and was soft-deleted to preserve learner records";
            String combinedWarning = (warning != null) ? warning + "; " + progressWarning : progressWarning;

            logger.info("Soft-deleted material ID {} due to active student progress by user ID {}", materialId, instructor.getId());
            return new ContentRemovalResponse(materialId, true, true, combinedWarning, combinedWarning, criteriaWarning);
        } else {
            // Hard-delete: delete physical file and delete DB entity
            materialStorageService.deleteMaterialFile(material.getFileUrl());
            materialRepository.delete(material);

            logger.info("Hard-deleted material ID {} by user ID {}", materialId, instructor.getId());
            return new ContentRemovalResponse(materialId, true, false, "Content item successfully deleted", warning, criteriaWarning);
        }
    }

    /**
     * Delete material delegate for backward compatibility.
     */
    public void deleteMaterial(User instructor, Integer id) {
        removeMaterial(instructor, id);
    }

    private boolean hasStudentProgress(LectureMaterial material) {
        if (material == null) {
            return false;
        }
        if (material.getLessonId() != null) {
            if (lessonProgressRepository != null && lessonProgressRepository.existsByLessonId(material.getLessonId())) {
                return true;
            }
            if (lectureProgressRepository != null && lectureProgressRepository.existsByLectureId(material.getLessonId())) {
                return true;
            }
        }
        if (material.getLectureId() != null) {
            if (lectureProgressRepository != null && lectureProgressRepository.existsByLectureId(material.getLectureId())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkCompletionCriteriaWarning(Integer courseId, Integer lessonId) {
        if (courseId == null || lessonId == null || courseCompletionCriteriaRepository == null) {
            return false;
        }
        try {
            return courseCompletionCriteriaRepository.findByCourseId(courseId)
                    .map(criteria -> criteria.getMandatoryLessonIdList() != null &&
                            criteria.getMandatoryLessonIdList().contains(lessonId))
                    .orElse(false);
        } catch (Exception e) {
            logger.warn("Could not check completion criteria for course {}: {}", courseId, e.getMessage());
            return false;
        }
    }

    private Course resolveCourseFromMaterial(LectureMaterial material) {
        if (material.getLessonId() != null) {
            Lesson lesson = lessonRepository.findById(material.getLessonId()).orElse(null);
            if (lesson != null) {
                return extractCourseFromLesson(lesson);
            }
        }
        if (material.getModuleId() != null) {
            CourseModule module = moduleRepository.findById(material.getModuleId()).orElse(null);
            if (module != null) {
                return module.getCourse();
            }
        }
        if (material.getLectureId() != null) {
            Lecture lecture = lectureRepository.findById(material.getLectureId()).orElse(null);
            if (lecture != null) {
                return lecture.getCourse();
            }
        }
        throw new ResourceNotFoundException("Could not associate material with course hierarchy");
    }

    private void validateInstructorRole(User user) {
        if (user == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        if (!"instructor".equalsIgnoreCase(user.getRole()) && !"admin".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only instructors or administrators can manage supplementary materials");
        }
    }

    private void assertInstructorAccess(Course course, User instructor) {
        if (course == null) {
            throw new ResourceNotFoundException("Associated course not found");
        }
        if (instructor == null) {
            throw new AccessDeniedException("Full authentication is required");
        }
        if ("admin".equalsIgnoreCase(instructor.getRole())) {
            return;
        }
        if (course.getInstructor() != null && Objects.equals(course.getInstructor().getId(), instructor.getId())) {
            return;
        }
        if (courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), instructor.getId())) {
            return;
        }
        throw new AccessDeniedException("You are not authorized to manage materials for course: " + course.getTitle());
    }

    private Course extractCourseFromLesson(Lesson lesson) {
        if (lesson.getSection() != null &&
                lesson.getSection().getModule() != null &&
                lesson.getSection().getModule().getCourse() != null) {
            return lesson.getSection().getModule().getCourse();
        }
        throw new ResourceNotFoundException("Lesson hierarchy is missing course association");
    }
}
