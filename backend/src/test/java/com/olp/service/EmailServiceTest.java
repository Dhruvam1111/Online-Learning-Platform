package com.olp.service;

import com.olp.entity.AssociationRequestStatus;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "senderEmail", "noreply@focotech.com");
    }

    private static String htmlOf(Part part) throws Exception {
        Object content = part.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                sb.append(htmlOf(multipart.getBodyPart(i)));
            }
            return sb.toString();
        }
        return "";
    }

    @Test
    void testSendOtpEmail_BlankSenderConfiguration_SkipsSendingSilently() {
        ReflectionTestUtils.setField(emailService, "senderEmail", "");

        assertDoesNotThrow(() -> emailService.sendOtpEmail("user@example.com", "Alice", "123456", "Login", 10));

        verifyNoInteractions(mailSender);
    }

    @Test
    void testSendOtpEmail_NullSenderConfiguration_SkipsSendingSilently() {
        ReflectionTestUtils.setField(emailService, "senderEmail", null);

        assertDoesNotThrow(() -> emailService.sendOtpEmail("user@example.com", "Alice", "123456", "Login", 10));

        verifyNoInteractions(mailSender);
    }

    @Test
    void testSendOtpEmail_Success_BuildsAndSendsMessageWithOtpSubjectAndRecipient() throws Exception {
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendOtpEmail("user@example.com", "Alice", "123456", "Password Reset", 10);

        verify(mailSender).send(message);
        assertEquals("Focotech Verification Code: 123456", message.getSubject());
        Address[] recipients = message.getAllRecipients();
        assertEquals(1, recipients.length);
        assertEquals("user@example.com", recipients[0].toString());
        assertTrue(message.getFrom()[0].toString().contains("noreply@focotech.com"));
    }

    @Test
    void testSendOtpEmail_HtmlBodyContainsOtpNamePurposeAndExpiry() throws Exception {
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendOtpEmail("user@example.com", "  Alice  ", "654321", "Password Reset", 15);

        String html = htmlOf(message);
        assertTrue(html.contains("654321"));
        assertTrue(html.contains(">Alice</strong>"));
        assertTrue(html.contains("Password Reset"));
        assertTrue(html.contains("Valid for 15 minutes"));
    }

    @Test
    void testSendOtpEmail_BlankName_GreetsWithEmailAddress() throws Exception {
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendOtpEmail("user@example.com", "   ", "111222", "Login", 10);

        assertTrue(htmlOf(message).contains(">user@example.com</strong>"));
    }

    @Test
    void testSendOtpEmail_NullName_GreetsWithEmailAddress() throws Exception {
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendOtpEmail("user@example.com", null, "111222", "Login", 10);

        assertTrue(htmlOf(message).contains(">user@example.com</strong>"));
    }

    @Test
    void testSendOtpEmail_SmtpRejection_IsReportedAsInvalidAddress() throws Exception {
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("550 mailbox unavailable")).when(mailSender).send(any(MimeMessage.class));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> emailService.sendOtpEmail("nobody@example.com", "Alice", "123456", "Login", 10));

        assertTrue(ex.getMessage().contains("rejected by the mail server"));
    }

    @Test
    void testSendOtpEmail_UnexpectedFailureWhileSending_IsWrappedWithOriginalMessage() throws Exception {
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new RuntimeException("connection reset")).when(mailSender).send(any(MimeMessage.class));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> emailService.sendOtpEmail("user@example.com", "Alice", "123456", "Login", 10));

        assertEquals("Email delivery failed: connection reset", ex.getMessage());
    }

    @Test
    void testSendOtpEmail_CreateMimeMessageFails_IsWrappedAsDeliveryFailure() {
        when(mailSender.createMimeMessage()).thenThrow(new IllegalStateException("mail session unavailable"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> emailService.sendOtpEmail("user@example.com", "Alice", "123456", "Login", 10));

        assertTrue(ex.getMessage().contains("Email delivery failed"));
        assertTrue(ex.getMessage().contains("mail session unavailable"));
    }

    @Test
    void testSendOtpEmail_MessagingExceptionWhileFormatting_IsReportedAsFormatFailure() throws Exception {
        MimeMessage brokenMessage = mock(MimeMessage.class);
        doThrow(new MessagingException("bad header")).when(brokenMessage).setFrom(any(Address.class));
        when(mailSender.createMimeMessage()).thenReturn(brokenMessage);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> emailService.sendOtpEmail("user@example.com", "Alice", "123456", "Login", 10));

        assertTrue(ex.getMessage().contains("Failed to format or send email to user@example.com"));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendCourseAssociationDecisionEmail_Approved_SendsNotification() throws Exception {
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendCourseAssociationDecisionEmail(
                "inst@olp.com", "Prof. James", "Databases 101",
                AssociationRequestStatus.APPROVED, "Welcome to the teaching team!");

        verify(mailSender).send(message);
        assertEquals("Course Association Request Approved: Databases 101", message.getSubject());
        String html = htmlOf(message);
        assertTrue(html.contains("Prof. James"));
        assertTrue(html.contains("Databases 101"));
        assertTrue(html.contains("Approved"));
        assertTrue(html.contains("Welcome to the teaching team!"));
    }

    @Test
    void testSendCourseAssociationDecisionEmail_BlankSender_SkipsSilently() {
        ReflectionTestUtils.setField(emailService, "senderEmail", "");

        assertDoesNotThrow(() -> emailService.sendCourseAssociationDecisionEmail(
                "inst@olp.com", "Prof. James", "Databases 101",
                AssociationRequestStatus.REJECTED, "Full team"));

        verifyNoInteractions(mailSender);
    }
}
