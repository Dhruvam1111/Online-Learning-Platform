package com.olp.service;

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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service managing supplementary learning materials upload and security validation (FR-I14, Step 15).
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
            ContentDeclarationService contentDeclarationService) {
        this.materialRepository = materialRepository;
        this.lessonRepository = lessonRepository;
        this.moduleRepository = moduleRepository;
        this.lectureRepository = lectureRepository;
        this.courseInstructorRepository = courseInstructorRepository;
        this.userRepository = userRepository;
        this.materialStorageService = materialStorageService;
        this.virusScanner = virusScanner;
        this.contentDeclarationService = contentDeclarationService;
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

        LectureMaterial saved = materialRepository.save(material);
        return SupplementaryMaterialResponse.fromEntity(saved);
    }

    /**
     * Retrieve all materials attached to a lesson.
     */
    @Transactional(readOnly = true)
    public List<SupplementaryMaterialResponse> getMaterialsByLesson(User user, Integer lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));
        Course course = extractCourseFromLesson(lesson);
        assertInstructorAccess(course, user);

        return materialRepository.findByLessonId(lessonId)
                .stream()
                .map(SupplementaryMaterialResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all materials attached to a module.
     */
    @Transactional(readOnly = true)
    public List<SupplementaryMaterialResponse> getMaterialsByModule(User user, Integer moduleId) {
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));
        assertInstructorAccess(module.getCourse(), user);

        return materialRepository.findByModuleId(moduleId)
                .stream()
                .map(SupplementaryMaterialResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve single material by ID.
     */
    @Transactional(readOnly = true)
    public SupplementaryMaterialResponse getMaterialById(User user, Integer id) {
        LectureMaterial material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + id));

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

        Course course = resolveCourseFromMaterial(material);
        assertInstructorAccess(course, instructor);

        material.setIsDownloadable(isDownloadable);
        LectureMaterial saved = materialRepository.save(material);
        logger.info("Updated material ID {} downloadable status to {} by user ID {}",
                materialId, isDownloadable, instructor.getId());

        return SupplementaryMaterialResponse.fromEntity(saved);
    }

    /**
     * Delete material and remove file from disk.
     */
    public void deleteMaterial(User instructor, Integer id) {
        validateInstructorRole(instructor);

        LectureMaterial material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found with id: " + id));

        Course course = resolveCourseFromMaterial(material);
        assertInstructorAccess(course, instructor);

        materialStorageService.deleteMaterialFile(material.getFileUrl());
        materialRepository.delete(material);
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

