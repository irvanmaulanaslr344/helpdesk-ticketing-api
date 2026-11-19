package com.company.helpdesk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@helpdesk.local}")
    private String fromAddress;

    @Async
    public void notifyAdminsNewTicket(List<String> adminEmails, Long ticketId, String judul, String pelapor) {
        String subject = "[Helpdesk] Tiket Baru #" + ticketId + " - " + judul;
        String body = """
                Halo Admin,

                Tiket baru telah dibuat oleh %s.
                ID Tiket : #%d
                Judul    : %s
                Status   : OPEN

                Silakan lakukan verifikasi dan penugasan pada sistem Helpdesk.
                """.formatted(pelapor, ticketId, judul);
        adminEmails.forEach(email -> sendSafely(email, subject, body));
    }

    @Async
    public void notifyTechnicianAssigned(String techEmail, String techName, Long ticketId, String judul) {
        String subject = "[Helpdesk] Penugasan Tiket #" + ticketId + " - " + judul;
        String body = """
                Halo %s,

                Anda telah ditugaskan untuk menangani tiket berikut:
                ID Tiket : #%d
                Judul    : %s

                Silakan periksa detail tiket di aplikasi Helpdesk.
                """.formatted(techName, ticketId, judul);
        sendSafely(techEmail, subject, body);
    }

    @Async
    public void notifyEmployeeTicketUpdated(
            String employeeEmail,
            String employeeName,
            Long ticketId,
            String judul,
            String statusBaru,
            String catatan
    ) {
        String subject = "[Helpdesk] Update Tiket #" + ticketId + " (" + statusBaru + ")";
        String noteText = (catatan == null || catatan.isBlank()) ? "-" : catatan;
        String body = """
                Halo %s,

                Tiket Anda telah diperbarui:
                ID Tiket    : #%d
                Judul       : %s
                Status Baru : %s
                Catatan     : %s

                Terima kasih,
                Tim Helpdesk
                """.formatted(employeeName, ticketId, judul, statusBaru, noteText);
        sendSafely(employeeEmail, subject, body);
    }

    private void sendSafely(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("[MAIL MOCK] To: {} | Subject: {} | Body: {}", to, subject, body.replace("\n", " "));
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Gagal mengirim email notifikasi ke {}: {}", to, ex.getMessage());
        }
    }
}