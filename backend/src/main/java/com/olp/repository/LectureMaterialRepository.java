package com.olp.repository;

import com.olp.entity.LectureMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LectureMaterialRepository extends JpaRepository<LectureMaterial, Integer> {
    List<LectureMaterial> findByLectureId(Integer lectureId);
}