package com.olp.service;

import com.olp.dto.CreateLectureRequest;
import com.olp.dto.LectureResponse;
import com.olp.dto.ReorderLecturesRequest;
import com.olp.dto.UpdateLectureRequest;
import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.Lecture;
import com.olp.entity.User;
import com.olp.exception.InvalidCredentialsException;
import com.olp.exception.ResourceNotFoundException;
import com.olp.repository.CourseRepository;
import com.olp.repository.LectureRepository;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorLectureServiceTest {

    private static final String URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    private static final String VIDEO_ID = "dQw4w9WgXcQ";

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LectureRepository lectureRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InstructorLectureService service;

    private User instructor(Integer id) {
        User user = new User("Dr. Chen", "chen" + id + "@olp.com", "hash", "instructor");
        user.setId(id);
        return user;
    }

    private User student(Integer id) {
        User user = new User("Sam Student", "sam" + id + "@olp.com", "hash", "student");
        user.setId(id);
        return user;
    }

    private Course course(Integer id, User owner) {
        Course course = new Course(owner, "Course " + id, "Desc", CourseStatus.DRAFT);
        course.setId(id);
        return course;
    }

    private Lecture lecture(Integer id, Course course, int orderIndex) {
        Lecture lecture = new Lecture(course, "Lecture " + id, URL, VIDEO_ID, orderIndex);
        lecture.setId(id);
        return lecture;
    }

    private Course givenOwnedCourse(Integer courseId, User owner) {
        Course course = course(courseId, owner);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        return course;
    }

    private void stubSaveReturnsArgument() {
        when(lectureRepository.save(any(Lecture.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ===== resolveUser =====

    @Test
    void testResolveUser_NullAuthentication_Throws() {
        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(null));
    }

    @Test
    void testResolveUser_NullPrincipal_Throws() {
        Authentication auth = mock(Authentication.class);

        assertThrows(InvalidCredentialsException.class, () -> service.resolveUser(auth));
    }

    @Test
    void testResolveUser_UserPrincipal_ReturnedDirectly() {
        User user = instructor(1);

        assertSame(user, service.resolveUser(new UsernamePasswordAuthenticationToken(user, null)));
        verifyNoInteractions(userRepository);
    }

    @Test
    void testResolveUser_EmailPrincipal_FoundByEmail() {
        User user = instructor(1);
        when(userRepository.findByEmail("chen1@olp.com")).thenReturn(Optional.of(user));

        assertSame(user, service.resolveUser(new UsernamePasswordAuthenticationToken("chen1@olp.com", null)));
    }

    @Test
    void testResolveUser_EmailPrincipalNotFound_Throws() {
        when(userRepository.findByEmail("ghost@olp.com")).thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> service.resolveUser(new UsernamePasswordAuthenticationToken("ghost@olp.com", null)));

        assertTrue(ex.getMessage().contains("User not found"));
    }

    // ===== getCourseLectures =====

    @Test
    void testGetCourseLectures_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class, () -> service.getCourseLectures(null, 10));
    }

    @Test
    void testGetCourseLectures_NonInstructor_ThrowsAccessDenied() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.getCourseLectures(student(2), 10));

        assertTrue(ex.getMessage().contains("only instructors"));
        verifyNoInteractions(courseRepository, lectureRepository);
    }

    @Test
    void testGetCourseLectures_NullCourseId_Throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getCourseLectures(instructor(1), null));

        assertTrue(ex.getMessage().contains("Course ID cannot be null"));
    }

    @Test
    void testGetCourseLectures_CourseNotFound_Throws() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.getCourseLectures(instructor(1), 99));

        assertTrue(ex.getMessage().contains("Course not found with id: 99"));
    }

    @Test
    void testGetCourseLectures_NotOwner_ThrowsAccessDenied() {
        givenOwnedCourse(10, instructor(2));

        assertThrows(AccessDeniedException.class, () -> service.getCourseLectures(instructor(1), 10));
        verifyNoInteractions(lectureRepository);
    }

    @Test
    void testGetCourseLectures_CourseWithoutInstructor_ThrowsAccessDenied() {
        when(courseRepository.findById(10)).thenReturn(Optional.of(course(10, null)));

        assertThrows(AccessDeniedException.class, () -> service.getCourseLectures(instructor(1), 10));
    }

    @Test
    void testGetCourseLectures_ReturnsMappedLecturesInRepositoryOrder() {
        Course course = givenOwnedCourse(10, instructor(1));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(10))
                .thenReturn(List.of(lecture(2, course, 1), lecture(1, course, 2)));

        List<LectureResponse> result = service.getCourseLectures(instructor(1), 10);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getId());
        assertEquals(1, result.get(0).getOrderIndex());
        assertEquals(10, result.get(0).getCourseId());
        assertEquals(VIDEO_ID, result.get(0).getVideoId());
        assertEquals("https://www.youtube.com/embed/" + VIDEO_ID, result.get(0).getEmbedUrl());
        assertEquals(1, result.get(1).getId());
    }

    @Test
    void testGetCourseLectures_NoLectures_ReturnsEmptyList() {
        givenOwnedCourse(10, instructor(1));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(10)).thenReturn(List.of());

        assertTrue(service.getCourseLectures(instructor(1), 10).isEmpty());
    }

    // ===== reorderLectures =====

    @Test
    void testReorderLectures_NonInstructor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> service.reorderLectures(student(2), 10, new ReorderLecturesRequest(List.of(1))));
    }

    @Test
    void testReorderLectures_NotOwner_ThrowsAccessDenied() {
        givenOwnedCourse(10, instructor(2));

        assertThrows(AccessDeniedException.class,
                () -> service.reorderLectures(instructor(1), 10, new ReorderLecturesRequest(List.of(1))));
    }

    @Test
    void testReorderLectures_NullRequest_Throws() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.reorderLectures(instructor(1), 10, null));

        assertTrue(ex.getMessage().contains("cannot be empty"));
    }

    @Test
    void testReorderLectures_NullIdList_Throws() {
        givenOwnedCourse(10, instructor(1));

        assertThrows(IllegalArgumentException.class,
                () -> service.reorderLectures(instructor(1), 10, new ReorderLecturesRequest(null)));
    }

    @Test
    void testReorderLectures_EmptyIdList_Throws() {
        givenOwnedCourse(10, instructor(1));

        assertThrows(IllegalArgumentException.class,
                () -> service.reorderLectures(instructor(1), 10, new ReorderLecturesRequest(List.of())));
    }

    @Test
    void testReorderLectures_NullElement_Throws() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.reorderLectures(instructor(1), 10, new ReorderLecturesRequest(Arrays.asList(1, null))));

        assertTrue(ex.getMessage().contains("null elements"));
    }

    @Test
    void testReorderLectures_DuplicateIds_Throws() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.reorderLectures(instructor(1), 10, new ReorderLecturesRequest(List.of(1, 1))));

        assertTrue(ex.getMessage().contains("Duplicate lecture IDs"));
        verify(lectureRepository, never()).saveAll(any());
    }

    @Test
    void testReorderLectures_IdFromAnotherCourse_Throws() {
        Course course = givenOwnedCourse(10, instructor(1));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(10))
                .thenReturn(List.of(lecture(1, course, 1), lecture(2, course, 2)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.reorderLectures(instructor(1), 10, new ReorderLecturesRequest(List.of(1, 3))));

        assertTrue(ex.getMessage().contains("Lecture with ID 3 does not belong to course 10"));
        verify(lectureRepository, never()).saveAll(any());
    }

    @Test
    void testReorderLectures_PartialList_Throws() {
        Course course = givenOwnedCourse(10, instructor(1));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(10))
                .thenReturn(List.of(lecture(1, course, 1), lecture(2, course, 2), lecture(3, course, 3)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.reorderLectures(instructor(1), 10, new ReorderLecturesRequest(List.of(1, 2))));

        assertTrue(ex.getMessage().contains("must include all 3 lectures"));
        verify(lectureRepository, never()).saveAll(any());
    }

    @Test
    void testReorderLectures_Success_AssignsSequentialIndexesAndRefetches() {
        Course course = givenOwnedCourse(10, instructor(1));
        Lecture l1 = lecture(1, course, 1);
        Lecture l2 = lecture(2, course, 2);
        Lecture l3 = lecture(3, course, 3);
        List<Lecture> existing = List.of(l1, l2, l3);
        List<Lecture> refreshed = List.of(l3, l1, l2);
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(10)).thenReturn(existing, refreshed);

        List<LectureResponse> result = service.reorderLectures(instructor(1), 10,
                new ReorderLecturesRequest(List.of(3, 1, 2)));

        assertEquals(1, l3.getOrderIndex());
        assertEquals(2, l1.getOrderIndex());
        assertEquals(3, l2.getOrderIndex());
        verify(lectureRepository, times(1)).saveAll(existing);
        verify(lectureRepository, times(2)).findByCourseIdOrderByOrderIndexAsc(10);
        assertEquals(List.of(3, 1, 2), result.stream().map(LectureResponse::getId).toList());
        assertEquals(List.of(1, 2, 3), result.stream().map(LectureResponse::getOrderIndex).toList());
    }

    @Test
    void testReorderLectures_SameOrder_StillPersistsAllLectures() {
        Course course = givenOwnedCourse(10, instructor(1));
        Lecture l1 = lecture(1, course, 5);
        Lecture l2 = lecture(2, course, 9);
        List<Lecture> existing = List.of(l1, l2);
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(10)).thenReturn(existing);

        service.reorderLectures(instructor(1), 10, new ReorderLecturesRequest(List.of(1, 2)));

        assertEquals(1, l1.getOrderIndex());
        assertEquals(2, l2.getOrderIndex());
        verify(lectureRepository).saveAll(existing);
    }

    // ===== deleteLecture =====

    @Test
    void testDeleteLecture_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class, () -> service.deleteLecture(null, 10, 1));
    }

    @Test
    void testDeleteLecture_NonInstructor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> service.deleteLecture(student(2), 10, 1));
    }

    @Test
    void testDeleteLecture_NotOwner_ThrowsAccessDenied() {
        givenOwnedCourse(10, instructor(2));

        assertThrows(AccessDeniedException.class, () -> service.deleteLecture(instructor(1), 10, 1));
        verify(lectureRepository, never()).delete(any());
    }

    @Test
    void testDeleteLecture_NullLectureId_Throws() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.deleteLecture(instructor(1), 10, null));

        assertTrue(ex.getMessage().contains("Lecture ID cannot be null"));
    }

    @Test
    void testDeleteLecture_LectureNotFound_Throws() {
        givenOwnedCourse(10, instructor(1));
        when(lectureRepository.findById(5)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.deleteLecture(instructor(1), 10, 5));

        assertTrue(ex.getMessage().contains("Lecture not found with id: 5"));
    }

    @Test
    void testDeleteLecture_LectureBelongsToAnotherCourse_Throws() {
        givenOwnedCourse(10, instructor(1));
        when(lectureRepository.findById(5)).thenReturn(Optional.of(lecture(5, course(99, instructor(1)), 1)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.deleteLecture(instructor(1), 10, 5));

        assertTrue(ex.getMessage().contains("does not belong to course 10"));
        verify(lectureRepository, never()).delete(any());
    }

    @Test
    void testDeleteLecture_LectureWithoutCourse_Throws() {
        givenOwnedCourse(10, instructor(1));
        Lecture orphan = lecture(5, null, 1);
        when(lectureRepository.findById(5)).thenReturn(Optional.of(orphan));

        assertThrows(IllegalArgumentException.class, () -> service.deleteLecture(instructor(1), 10, 5));
        verify(lectureRepository, never()).delete(any());
    }

    @Test
    void testDeleteLecture_Success_DeletesThenRenumbersRemainingContiguously() {
        Course course = givenOwnedCourse(10, instructor(1));
        Lecture target = lecture(2, course, 2);
        Lecture first = lecture(1, course, 1);
        Lecture third = lecture(3, course, 3);
        List<Lecture> remaining = new ArrayList<>(List.of(first, third));
        when(lectureRepository.findById(2)).thenReturn(Optional.of(target));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(10)).thenReturn(remaining);

        service.deleteLecture(instructor(1), 10, 2);

        assertEquals(1, first.getOrderIndex());
        assertEquals(2, third.getOrderIndex());
        InOrder inOrder = inOrder(lectureRepository);
        inOrder.verify(lectureRepository).delete(target);
        inOrder.verify(lectureRepository).saveAll(remaining);
    }

    @Test
    void testDeleteLecture_LastLecture_SavesEmptyList() {
        Course course = givenOwnedCourse(10, instructor(1));
        Lecture only = lecture(1, course, 1);
        List<Lecture> remaining = new ArrayList<>();
        when(lectureRepository.findById(1)).thenReturn(Optional.of(only));
        when(lectureRepository.findByCourseIdOrderByOrderIndexAsc(10)).thenReturn(remaining);

        service.deleteLecture(instructor(1), 10, 1);

        verify(lectureRepository).delete(only);
        verify(lectureRepository).saveAll(remaining);
    }

    // ===== createLecture =====

    @Test
    void testCreateLecture_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.createLecture(null, 10, new CreateLectureRequest("T", URL)));
    }

    @Test
    void testCreateLecture_NonInstructor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> service.createLecture(student(2), 10, new CreateLectureRequest("T", URL)));
    }

    @Test
    void testCreateLecture_CourseNotFound_Throws() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createLecture(instructor(1), 99, new CreateLectureRequest("T", URL)));
    }

    @Test
    void testCreateLecture_NotOwner_ThrowsAccessDenied() {
        givenOwnedCourse(10, instructor(2));

        assertThrows(AccessDeniedException.class,
                () -> service.createLecture(instructor(1), 10, new CreateLectureRequest("T", URL)));
        verify(lectureRepository, never()).save(any());
    }

    @Test
    void testCreateLecture_NullRequest_Throws() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createLecture(instructor(1), 10, null));

        assertTrue(ex.getMessage().contains("Request cannot be null"));
    }

    @Test
    void testCreateLecture_BlankTitle_Throws() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createLecture(instructor(1), 10, new CreateLectureRequest("   ", URL)));

        assertTrue(ex.getMessage().contains("Title is required"));
    }

    @Test
    void testCreateLecture_NullTitle_Throws() {
        givenOwnedCourse(10, instructor(1));

        assertThrows(IllegalArgumentException.class,
                () -> service.createLecture(instructor(1), 10, new CreateLectureRequest(null, URL)));
    }

    @Test
    void testCreateLecture_MissingUrl_Throws() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createLecture(instructor(1), 10, new CreateLectureRequest("Intro", null)));

        assertTrue(ex.getMessage().contains("YouTube URL is required"));
    }

    @Test
    void testCreateLecture_BlankUrlAndNoAlias_Throws() {
        givenOwnedCourse(10, instructor(1));

        assertThrows(IllegalArgumentException.class,
                () -> service.createLecture(instructor(1), 10, new CreateLectureRequest("Intro", "   ")));
    }

    @Test
    void testCreateLecture_InvalidYoutubeUrl_ThrowsAndDoesNotSave() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createLecture(instructor(1), 10, new CreateLectureRequest("Intro", "https://example.com/video")));

        assertTrue(ex.getMessage().contains("Invalid YouTube URL"));
        verify(lectureRepository, never()).save(any());
    }

    @Test
    void testCreateLecture_ExplicitOrderIndex_IsUsedWithoutCounting() {
        Course course = givenOwnedCourse(10, instructor(1));
        stubSaveReturnsArgument();

        LectureResponse response = service.createLecture(instructor(1), 10, new CreateLectureRequest("  Intro  ", URL, 5));

        ArgumentCaptor<Lecture> captor = ArgumentCaptor.forClass(Lecture.class);
        verify(lectureRepository).save(captor.capture());
        Lecture saved = captor.getValue();
        assertEquals("Intro", saved.getTitle());
        assertEquals(URL, saved.getYoutubeUrl());
        assertEquals(VIDEO_ID, saved.getVideoId());
        assertEquals(5, saved.getOrderIndex());
        assertSame(course, saved.getCourse());
        verify(lectureRepository, never()).countByCourseId(any());
        assertEquals("Intro", response.getTitle());
        assertEquals(10, response.getCourseId());
        assertEquals("https://www.youtube.com/embed/" + VIDEO_ID, response.getEmbedUrl());
        assertEquals("https://img.youtube.com/vi/" + VIDEO_ID + "/mqdefault.jpg", response.getThumbnailUrl());
    }

    @Test
    void testCreateLecture_NoOrderIndex_AppendsAfterExistingLectures() {
        givenOwnedCourse(10, instructor(1));
        when(lectureRepository.countByCourseId(10)).thenReturn(3);
        stubSaveReturnsArgument();

        service.createLecture(instructor(1), 10, new CreateLectureRequest("Intro", URL));

        ArgumentCaptor<Lecture> captor = ArgumentCaptor.forClass(Lecture.class);
        verify(lectureRepository).save(captor.capture());
        assertEquals(4, captor.getValue().getOrderIndex());
    }

    @Test
    void testCreateLecture_ZeroOrderIndex_FallsBackToCountPlusOne() {
        givenOwnedCourse(10, instructor(1));
        when(lectureRepository.countByCourseId(10)).thenReturn(2);
        stubSaveReturnsArgument();

        service.createLecture(instructor(1), 10, new CreateLectureRequest("Intro", URL, 0));

        ArgumentCaptor<Lecture> captor = ArgumentCaptor.forClass(Lecture.class);
        verify(lectureRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getOrderIndex());
    }

    @Test
    void testCreateLecture_NegativeOrderIndex_FallsBackToCountPlusOne() {
        givenOwnedCourse(10, instructor(1));
        when(lectureRepository.countByCourseId(10)).thenReturn(0);
        stubSaveReturnsArgument();

        service.createLecture(instructor(1), 10, new CreateLectureRequest("Intro", URL, -4));

        ArgumentCaptor<Lecture> captor = ArgumentCaptor.forClass(Lecture.class);
        verify(lectureRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getOrderIndex());
    }

    @Test
    void testCreateLecture_PositionAlias_UsedWhenOrderIndexMissing() {
        givenOwnedCourse(10, instructor(1));
        stubSaveReturnsArgument();
        CreateLectureRequest request = new CreateLectureRequest("Intro", URL);
        request.setPosition(7);

        service.createLecture(instructor(1), 10, request);

        ArgumentCaptor<Lecture> captor = ArgumentCaptor.forClass(Lecture.class);
        verify(lectureRepository).save(captor.capture());
        assertEquals(7, captor.getValue().getOrderIndex());
    }

    @Test
    void testCreateLecture_VideoUrlAlias_UsedWhenYoutubeUrlMissing() {
        givenOwnedCourse(10, instructor(1));
        when(lectureRepository.countByCourseId(10)).thenReturn(0);
        stubSaveReturnsArgument();
        CreateLectureRequest request = new CreateLectureRequest();
        request.setTitle("Intro");
        request.setVideoUrl("https://youtu.be/" + VIDEO_ID);

        service.createLecture(instructor(1), 10, request);

        ArgumentCaptor<Lecture> captor = ArgumentCaptor.forClass(Lecture.class);
        verify(lectureRepository).save(captor.capture());
        assertEquals("https://youtu.be/" + VIDEO_ID, captor.getValue().getYoutubeUrl());
        assertEquals(VIDEO_ID, captor.getValue().getVideoId());
    }

    @Test
    void testCreateLecture_UrlWithSurroundingWhitespace_IsTrimmed() {
        givenOwnedCourse(10, instructor(1));
        when(lectureRepository.countByCourseId(10)).thenReturn(0);
        stubSaveReturnsArgument();

        service.createLecture(instructor(1), 10, new CreateLectureRequest("Intro", "   " + URL + "   "));

        ArgumentCaptor<Lecture> captor = ArgumentCaptor.forClass(Lecture.class);
        verify(lectureRepository).save(captor.capture());
        assertEquals(URL, captor.getValue().getYoutubeUrl());
    }

    // ===== updateLecture =====

    @Test
    void testUpdateLecture_NullUser_Throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.updateLecture(null, 10, 1, new UpdateLectureRequest("T", URL)));
    }

    @Test
    void testUpdateLecture_NonInstructor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> service.updateLecture(student(2), 10, 1, new UpdateLectureRequest("T", URL)));
    }

    @Test
    void testUpdateLecture_NotOwner_ThrowsAccessDenied() {
        givenOwnedCourse(10, instructor(2));

        assertThrows(AccessDeniedException.class,
                () -> service.updateLecture(instructor(1), 10, 1, new UpdateLectureRequest("T", URL)));
    }

    @Test
    void testUpdateLecture_NullLectureId_Throws() {
        givenOwnedCourse(10, instructor(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateLecture(instructor(1), 10, null, new UpdateLectureRequest("T", URL)));

        assertTrue(ex.getMessage().contains("Lecture ID cannot be null"));
    }

    @Test
    void testUpdateLecture_LectureNotFound_Throws() {
        givenOwnedCourse(10, instructor(1));
        when(lectureRepository.findById(5)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateLecture(instructor(1), 10, 5, new UpdateLectureRequest("T", URL)));
    }

    @Test
    void testUpdateLecture_LectureBelongsToAnotherCourse_Throws() {
        givenOwnedCourse(10, instructor(1));
        when(lectureRepository.findById(5)).thenReturn(Optional.of(lecture(5, course(99, instructor(1)), 1)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateLecture(instructor(1), 10, 5, new UpdateLectureRequest("T", URL)));

        assertTrue(ex.getMessage().contains("does not belong to course 10"));
        verify(lectureRepository, never()).save(any());
    }

    @Test
    void testUpdateLecture_LectureWithoutCourse_Throws() {
        givenOwnedCourse(10, instructor(1));
        when(lectureRepository.findById(5)).thenReturn(Optional.of(lecture(5, null, 1)));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateLecture(instructor(1), 10, 5, new UpdateLectureRequest("T", URL)));
    }

    @Test
    void testUpdateLecture_NullRequest_ChangesNothingButStillSaves() {
        Course course = givenOwnedCourse(10, instructor(1));
        Lecture existing = lecture(5, course, 2);
        when(lectureRepository.findById(5)).thenReturn(Optional.of(existing));
        stubSaveReturnsArgument();

        LectureResponse response = service.updateLecture(instructor(1), 10, 5, null);

        assertEquals("Lecture 5", existing.getTitle());
        assertEquals(URL, existing.getYoutubeUrl());
        assertEquals(2, existing.getOrderIndex());
        assertEquals("Lecture 5", response.getTitle());
        verify(lectureRepository).save(existing);
    }

    @Test
    void testUpdateLecture_BlankTitle_ThrowsAndDoesNotSave() {
        Course course = givenOwnedCourse(10, instructor(1));
        when(lectureRepository.findById(5)).thenReturn(Optional.of(lecture(5, course, 1)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateLecture(instructor(1), 10, 5, new UpdateLectureRequest("   ", null)));

        assertTrue(ex.getMessage().contains("Title cannot be blank"));
        verify(lectureRepository, never()).save(any());
    }

    @Test
    void testUpdateLecture_NullTitle_LeavesTitleUnchanged() {
        Course course = givenOwnedCourse(10, instructor(1));
        Lecture existing = lecture(5, course, 1);
        when(lectureRepository.findById(5)).thenReturn(Optional.of(existing));
        stubSaveReturnsArgument();

        service.updateLecture(instructor(1), 10, 5, new UpdateLectureRequest(null, null));

        assertEquals("Lecture 5", existing.getTitle());
    }

    @Test
    void testUpdateLecture_TitleIsTrimmed() {
        Course course = givenOwnedCourse(10, instructor(1));
        Lecture existing = lecture(5, course, 1);
        when(lectureRepository.findById(5)).thenReturn(Optional.of(existing));
        stubSaveReturnsArgument();

        service.updateLecture(instructor(1), 10, 5, new UpdateLectureRequest("  New Title  ", null));

        assertEquals("New Title", existing.getTitle());
    }

    @Test
    void testUpdateLecture_NewUrl_UpdatesUrlAndVideoId() {
        Course course = givenOwnedCourse(10, instructor(1));
        Lecture existing = lecture(5, course, 1);
        when(lectureRepository.findById(5)).thenReturn(Optional.of(existing));
        stubSaveReturnsArgument();

        LectureResponse response = service.updateLecture(instructor(1), 10, 5,
                new UpdateLectureRequest(null, "https://youtu.be/abcdefghijk"));

        assertEquals("https://youtu.be/abcdefghijk", existing.getYoutubeUrl());
        assertEquals("abcdefghijk", existing.getVideoId());
        assertEquals("abcdefghijk", response.getVideoId());
    }

    @Test
    void testUpdateLecture_VideoUrlAlias_UpdatesUrl() {
        Course course = givenOwnedCourse(10, instructor(1));
        Lecture existing = lecture(5, course, 1);
        when(lectureRepository.findById(5)).thenReturn(Optional.of(existing));
        stubSaveReturnsArgument();
        UpdateLectureRequest request = new UpdateLectureRequest();
        request.setVideoUrl("https://www.youtube.com/embed/abcdefghijk");

        service.updateLecture(instructor(1), 10, 5, request);

        assertEquals("abcdefghijk", existing.getVideoId());
    }

    @Test
    void testUpdateLecture_InvalidUrl_ThrowsAndDoesNotSave() {
        Course course = givenOwnedCourse(10, instructor(1));
        when(lectureRepository.findById(5)).thenReturn(Optional.of(lecture(5, course, 1)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateLecture(instructor(1), 10, 5, new UpdateLectureRequest(null, "https://example.com/video")));

        assertTrue(ex.getMessage().contains("Invalid YouTube URL"));
        verify(lectureRepository, never()).save(any());
    }

    @Test
    void testUpdateLecture_BlankUrl_IsIgnoredAndKeepsExistingUrl() {
        Course course = givenOwnedCourse(10, instructor(1));
        Lecture existing = lecture(5, course, 1);
        when(lectureRepository.findById(5)).thenReturn(Optional.of(existing));
        stubSaveReturnsArgument();

        service.updateLecture(instructor(1), 10, 5, new UpdateLectureRequest(null, "   "));

        assertEquals(URL, existing.getYoutubeUrl());
        assertEquals(VIDEO_ID, existing.getVideoId());
    }

    @Test
    void testUpdateLecture_PositiveOrderIndex_IsApplied() {
        Course course = givenOwnedCourse(10, instructor(1));
        Lecture existing = lecture(5, course, 1);
        when(lectureRepository.findById(5)).thenReturn(Optional.of(existing));
        stubSaveReturnsArgument();

        service.updateLecture(instructor(1), 10, 5, new UpdateLectureRequest(null, null, 6));

        assertEquals(6, existing.getOrderIndex());
    }

    @Test
    void testUpdateLecture_ZeroOrNegativeOrderIndex_IsIgnored() {
        Course course = givenOwnedCourse(10, instructor(1));
        Lecture existing = lecture(5, course, 3);
        when(lectureRepository.findById(5)).thenReturn(Optional.of(existing));
        stubSaveReturnsArgument();

        service.updateLecture(instructor(1), 10, 5, new UpdateLectureRequest(null, null, 0));
        service.updateLecture(instructor(1), 10, 5, new UpdateLectureRequest(null, null, -2));

        assertEquals(3, existing.getOrderIndex());
    }

    @Test
    void testUpdateLecture_PositionAlias_IsApplied() {
        Course course = givenOwnedCourse(10, instructor(1));
        Lecture existing = lecture(5, course, 1);
        when(lectureRepository.findById(5)).thenReturn(Optional.of(existing));
        stubSaveReturnsArgument();
        UpdateLectureRequest request = new UpdateLectureRequest();
        request.setPosition(8);

        service.updateLecture(instructor(1), 10, 5, request);

        assertEquals(8, existing.getOrderIndex());
    }
}
