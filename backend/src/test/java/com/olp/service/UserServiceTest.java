package com.olp.service;

import com.olp.dto.UpdateProfileRequest;
import com.olp.dto.UserResponse;
import com.olp.entity.User;
import com.olp.exception.DuplicateEmailException;
import com.olp.exception.InvalidCredentialsException;
import com.olp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User current;

    @BeforeEach
    void setUp() {
        current = new User("Sam", "sam@olp.com", "hash", "student", "active");
        current.setId(5);
    }

    private void stubFindAndSave() {
        when(userRepository.findById(5)).thenReturn(Optional.of(current));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    // =====================================================================
    // getProfile
    // =====================================================================

    @Test
    void testGetProfile_NullUser_ThrowsInvalidCredentials() {
        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> userService.getProfile(null));

        assertTrue(ex.getMessage().contains("not authenticated"));
    }

    @Test
    void testGetProfile_MapsFieldsWithoutTouchingRepository() {
        UserResponse response = userService.getProfile(current);

        assertEquals(5, response.getId());
        assertEquals("Sam", response.getName());
        assertEquals("sam@olp.com", response.getEmail());
        assertEquals("student", response.getRole());
        verifyNoInteractions(userRepository);
    }

    // =====================================================================
    // updateProfile
    // =====================================================================

    @Test
    void testUpdateProfile_NullUser_ThrowsInvalidCredentials() {
        assertThrows(InvalidCredentialsException.class,
                () -> userService.updateProfile(null, new UpdateProfileRequest("A", "a@olp.com")));

        verifyNoInteractions(userRepository);
    }

    @Test
    void testUpdateProfile_UserMissingFromDatabase_ThrowsInvalidCredentials() {
        when(userRepository.findById(5)).thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> userService.updateProfile(current, new UpdateProfileRequest("A", "a@olp.com")));

        assertEquals("User not found", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdateProfile_NameOnly_TrimsNameAndKeepsEmail() {
        stubFindAndSave();

        UserResponse response = userService.updateProfile(current, new UpdateProfileRequest("  Samuel  ", null));

        assertEquals("Samuel", response.getName());
        assertEquals("sam@olp.com", response.getEmail());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(current);
    }

    @Test
    void testUpdateProfile_BlankName_IsIgnored() {
        stubFindAndSave();

        UserResponse response = userService.updateProfile(current, new UpdateProfileRequest("   ", null));

        assertEquals("Sam", response.getName());
    }

    @Test
    void testUpdateProfile_SameEmailDifferentCase_SkipsDuplicateCheckAndKeepsStoredEmail() {
        stubFindAndSave();

        UserResponse response = userService.updateProfile(current, new UpdateProfileRequest(null, "  SAM@olp.com "));

        assertEquals("sam@olp.com", response.getEmail());
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void testUpdateProfile_EmailAlreadyUsedByAnotherAccount_ThrowsDuplicateEmail() {
        when(userRepository.findById(5)).thenReturn(Optional.of(current));
        when(userRepository.existsByEmail("taken@olp.com")).thenReturn(true);

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> userService.updateProfile(current, new UpdateProfileRequest("Sam", "taken@olp.com")));

        assertTrue(ex.getMessage().contains("already exists"));
        assertEquals("sam@olp.com", current.getEmail());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdateProfile_NewFreeEmail_IsNormalisedAndSaved() {
        stubFindAndSave();
        when(userRepository.existsByEmail("fresh@olp.com")).thenReturn(false);

        UserResponse response = userService.updateProfile(current, new UpdateProfileRequest(null, "  Fresh@OLP.com "));

        assertEquals("fresh@olp.com", response.getEmail());
        assertEquals("fresh@olp.com", current.getEmail());
    }

    @Test
    void testUpdateProfile_BlankEmail_IsIgnored() {
        stubFindAndSave();

        UserResponse response = userService.updateProfile(current, new UpdateProfileRequest(null, "   "));

        assertEquals("sam@olp.com", response.getEmail());
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void testUpdateProfile_NameAndEmail_BothUpdated() {
        stubFindAndSave();
        when(userRepository.existsByEmail("new@olp.com")).thenReturn(false);

        UserResponse response = userService.updateProfile(current, new UpdateProfileRequest("New Name", "new@olp.com"));

        assertEquals("New Name", response.getName());
        assertEquals("new@olp.com", response.getEmail());
        assertEquals(5, response.getId());
        assertEquals("student", response.getRole());
    }

    @Test
    void testUpdateProfile_EmptyRequest_StillSavesUnchangedUser() {
        stubFindAndSave();

        UserResponse response = userService.updateProfile(current, new UpdateProfileRequest(null, null));

        assertEquals("Sam", response.getName());
        assertEquals("sam@olp.com", response.getEmail());
        verify(userRepository, times(1)).save(current);
    }
}
