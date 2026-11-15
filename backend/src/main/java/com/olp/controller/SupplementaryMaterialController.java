package com.olp.controller;

import com.olp.dto.ContentRemovalResponse;
import com.olp.dto.ReorderStructureRequest;
import com.olp.dto.SupplementaryMaterialResponse;
import com.olp.dto.UpdateDownloadableRequest;
import com.olp.entity.User;
import com.olp.service.SupplementaryMaterialService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller for uploading, reordering, replacing, and managing supplementary course materials (FR-I14, FR-I16, FR-I17).
 */
@RestController
@RequestMapping({"/api/instructor/materials", "/api/instructors/materials"})
@PreAuthorize("hasAnyRole('instructor', 'admin')")
public class SupplementaryMaterialController {

    private final SupplementaryMaterialService materialService;

    @Autowired
    public SupplementaryMaterialController(SupplementaryMaterialService materialService) {
        this.materialService = materialService;
    }

    /**
     * POST /api/instructor/materials/upload
     * Upload a supplementary document, slide deck, or sheet attached to a lesson or module (FR-I14).
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SupplementaryMaterialResponse> uploadMaterial(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "lessonId", required = false) Integer lessonId,
            @RequestParam(value = "moduleId", required = false) Integer moduleId,
            @RequestParam(value = "lectureId", required = false) Integer lectureId,
            @RequestParam(value = "title", required = false) String title) {

        User user = materialService.resolveUser(authentication);
        SupplementaryMaterialResponse response = materialService.uploadMaterial(
                user, lessonId, moduleId, lectureId, title, file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/instructor/materials/lesson/{lessonId}
     * Retrieve all active materials attached to a specific lesson in order.
     */
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<List<SupplementaryMaterialResponse>> getMaterialsByLesson(
            @PathVariable Integer lessonId,
            Authentication authentication) {

        User user = materialService.resolveUser(authentication);
        List<SupplementaryMaterialResponse> materials = materialService.getMaterialsByLesson(user, lessonId);
        return ResponseEntity.ok(materials);
    }

    /**
     * GET /api/instructor/materials/module/{moduleId}
     * Retrieve all active materials attached to a specific course module.
     */
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<List<SupplementaryMaterialResponse>> getMaterialsByModule(
            @PathVariable Integer moduleId,
            Authentication authentication) {

        User user = materialService.resolveUser(authentication);
        List<SupplementaryMaterialResponse> materials = materialService.getMaterialsByModule(user, moduleId);
        return ResponseEntity.ok(materials);
    }

    /**
     * GET /api/instructor/materials/{id}
     * Retrieve metadata for a single material item.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SupplementaryMaterialResponse> getMaterialById(
            @PathVariable Integer id,
            Authentication authentication) {

        User user = materialService.resolveUser(authentication);
        SupplementaryMaterialResponse material = materialService.getMaterialById(user, id);
        return ResponseEntity.ok(material);
    }

    /**
     * PUT /api/instructor/materials/{id}/downloadable
     * PATCH /api/instructor/materials/{id}/downloadable
     * Update the downloadable vs view-only status of a material (FR-I17, Step 16).
     */
    @RequestMapping(value = "/{id}/downloadable", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<SupplementaryMaterialResponse> updateDownloadableStatus(
            @PathVariable Integer id,
            @RequestParam(value = "downloadable", required = false) Boolean downloadable,
            @RequestBody(required = false) UpdateDownloadableRequest request,
            Authentication authentication) {

        User user = materialService.resolveUser(authentication);
        boolean targetDownloadable = (request != null && request.resolveDownloadable() != null)
                ? request.resolveDownloadable()
                : (downloadable != null ? downloadable : true);

        SupplementaryMaterialResponse response = materialService.updateDownloadableStatus(user, id, targetDownloadable);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/instructor/materials/lesson/{lessonId}/reorder
     * Reorder content items within a lesson (FR-I16, Step 19).
     */
    @PutMapping("/lesson/{lessonId}/reorder")
    public ResponseEntity<List<SupplementaryMaterialResponse>> reorderMaterialsInLesson(
            @PathVariable Integer lessonId,
            @Valid @RequestBody ReorderStructureRequest request,
            Authentication authentication) {

        User user = materialService.resolveUser(authentication);
        List<SupplementaryMaterialResponse> response = materialService.reorderMaterialsInLesson(
                user, lessonId, request.getOrderedIds());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/instructor/materials/{id}/replace
     * Replace an existing material file while preserving lesson links, order index, and progress references (FR-I16, Step 19).
     */
    @PutMapping(value = "/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SupplementaryMaterialResponse> replaceMaterial(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        User user = materialService.resolveUser(authentication);
        SupplementaryMaterialResponse response = materialService.replaceMaterialFile(user, id, file);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/instructor/materials/{id}
     * Remove a supplementary material: soft-deletes when student progress exists, hard-deletes otherwise.
     * Evaluates course completion criteria and includes warning flags in response (FR-I16, Step 19).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ContentRemovalResponse> deleteMaterial(
            @PathVariable Integer id,
            Authentication authentication) {

        User user = materialService.resolveUser(authentication);
        ContentRemovalResponse response = materialService.removeMaterial(user, id);
        return ResponseEntity.ok(response);
    }
}
