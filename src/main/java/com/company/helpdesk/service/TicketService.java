package com.company.helpdesk.service;

import com.company.helpdesk.config.RedisCacheConfig;
import com.company.helpdesk.dto.TicketDtos;
import com.company.helpdesk.exception.ApiException;
import com.company.helpdesk.exception.ErrorCode;
import com.company.helpdesk.model.*;
import com.company.helpdesk.repository.*;
import com.company.helpdesk.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TicketService {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "id", "id",
            "judul", "judul",
            "status", "status",
            "tanggal_dibuat", "tanggalDibuat",
            "tanggal_selesai", "tanggalSelesai"
    );

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TicketStateMachine stateMachine;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_KARYAWAN, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_ADMIN, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_SUPERVISOR, allEntries = true)
    })
    public TicketDtos.CreateTicketResponse createTicket(TicketDtos.CreateTicketRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Pelapor tidak ditemukan"));

        Category category = categoryRepository.findById(request.kategoriId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Kategori tidak ditemukan"));

        Ticket ticket = ticketRepository.save(Ticket.builder()
                .judul(request.judul().trim())
                .deskripsi(request.deskripsi().trim())
                .status(TicketStatus.OPEN)
                .kategori(category)
                .dibuatOleh(creator)
                .tanggalDibuat(LocalDateTime.now())
                .build());

        recordHistory(ticket, null, TicketStatus.OPEN, creator, "Tiket dibuat");

        List<String> adminEmails = userRepository.findByRoleAndAktifTrue(Role.ADMIN)
                .stream()
                .map(User::getEmail)
                .toList();
        emailNotificationService.notifyAdminsNewTicket(adminEmails, ticket.getId(), ticket.getJudul(), creator.getNama());

        return new TicketDtos.CreateTicketResponse(ticket.getId(), ticket.getStatus(), ticket.getTanggalDibuat());
    }

    @Transactional(readOnly = true)
    public TicketDtos.TicketListResponse listTickets(
            CustomUserDetails actor,
            int page,
            int limit,
            String sortBy,
            String order,
            TicketStatus status,
            Long kategoriId,
            LocalDate tanggalDari,
            LocalDate tanggalSampai,
            String search
    ) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.clamp(limit, 1, 100);
        String sortProperty = ALLOWED_SORT_FIELDS.getOrDefault(
                sortBy == null ? "tanggal_dibuat" : sortBy.toLowerCase(),
                "tanggalDibuat"
        );
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(safePage - 1, safeLimit, Sort.by(direction, sortProperty));

        Long ownerFilter = actor.getRole() == Role.KARYAWAN ? actor.getId() : null;
        var spec = TicketSpecification.filter(ownerFilter, status, kategoriId, tanggalDari, tanggalSampai, search);

        Page<Ticket> result = ticketRepository.findAll(spec, pageable);
        List<TicketDtos.TicketListItem> items = result.getContent()
                .stream()
                .map(t -> new TicketDtos.TicketListItem(
                        t.getId(),
                        t.getJudul(),
                        t.getStatus(),
                        t.getKategori().getNamaKategori(),
                        new TicketDtos.UserRef(t.getDibuatOleh().getId(), t.getDibuatOleh().getNama()),
                        t.getTanggalDibuat()
                ))
                .toList();

        return new TicketDtos.TicketListResponse(
                items,
                new TicketDtos.PageMeta(safePage, safeLimit, result.getTotalElements())
        );
    }

    @Transactional(readOnly = true)
    public TicketDtos.TicketDetailResponse getTicketDetail(Long ticketId, CustomUserDetails actor) {
        Ticket ticket = ticketRepository.findDetailById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Tiket tidak ditemukan"));

        if (actor.getRole() == Role.KARYAWAN && !ticket.getDibuatOleh().getId().equals(actor.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Anda tidak memiliki akses ke tiket karyawan lain");
        }

        List<TicketDtos.TicketHistoryItem> history = ticketHistoryRepository.findByTicketIdOrderByTanggalAsc(ticketId)
                .stream()
                .map(h -> new TicketDtos.TicketHistoryItem(
                        h.getStatusLama(),
                        h.getStatusBaru(),
                        h.getDiubahOleh().getNama(),
                        h.getCatatan(),
                        h.getTanggal()
                ))
                .toList();

        TicketDtos.UserRef assignee = ticket.getDitugaskanKe() == null
                ? null
                : new TicketDtos.UserRef(ticket.getDitugaskanKe().getId(), ticket.getDitugaskanKe().getNama());

        return new TicketDtos.TicketDetailResponse(
                ticket.getId(),
                ticket.getJudul(),
                ticket.getDeskripsi(),
                ticket.getStatus(),
                new TicketDtos.CategoryRef(ticket.getKategori().getId(), ticket.getKategori().getNamaKategori()),
                new TicketDtos.UserRef(ticket.getDibuatOleh().getId(), ticket.getDibuatOleh().getNama()),
                assignee,
                ticket.getTanggalDibuat(),
                ticket.getTanggalSelesai(),
                history
        );
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_KARYAWAN, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_ADMIN, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_SUPERVISOR, allEntries = true)
    })
    public TicketDtos.StatusResponse updateStatus(Long ticketId, TicketDtos.UpdateStatusRequest request, Long adminId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        User admin = findUserOrThrow(adminId);

        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = request.status();
        stateMachine.validateTransition(oldStatus, newStatus);

        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED) {
            if (ticket.getTanggalSelesai() == null) {
                ticket.setTanggalSelesai(LocalDateTime.now());
            }
        } else if (newStatus == TicketStatus.REOPENED || newStatus == TicketStatus.IN_PROGRESS) {
            ticket.setTanggalSelesai(null);
        }

        ticketRepository.save(ticket);
        recordHistory(ticket, oldStatus, newStatus, admin, request.catatan());

        emailNotificationService.notifyEmployeeTicketUpdated(
                ticket.getDibuatOleh().getEmail(),
                ticket.getDibuatOleh().getNama(),
                ticket.getId(),
                ticket.getJudul(),
                newStatus.name(),
                request.catatan()
        );

        return new TicketDtos.StatusResponse(ticket.getId(), ticket.getStatus());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_ADMIN, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_SUPERVISOR, allEntries = true)
    })
    public TicketDtos.AssignTicketResponse assignTicket(Long ticketId, TicketDtos.AssignTicketRequest request, Long adminId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        User admin = findUserOrThrow(adminId);
        User technician = userRepository.findById(request.ditugaskanKe())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Teknisi tidak ditemukan"));

        if (technician.getRole() != Role.ADMIN || !Boolean.TRUE.equals(technician.getAktif())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Tiket hanya dapat ditugaskan ke Admin/Teknisi aktif");
        }

        ticket.setDitugaskanKe(technician);
        ticketRepository.save(ticket);

        recordHistory(
                ticket,
                ticket.getStatus(),
                ticket.getStatus(),
                admin,
                "Tiket ditugaskan ke " + technician.getNama()
        );

        emailNotificationService.notifyTechnicianAssigned(
                technician.getEmail(),
                technician.getNama(),
                ticket.getId(),
                ticket.getJudul()
        );

        return new TicketDtos.AssignTicketResponse(ticket.getId(), technician.getId());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_ADMIN, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_SUPERVISOR, allEntries = true)
    })
    public TicketDtos.UpdateCategoryResponse updateCategory(Long ticketId, TicketDtos.UpdateCategoryRequest request, Long adminId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        User admin = findUserOrThrow(adminId);
        Category newCategory = categoryRepository.findById(request.kategoriId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Kategori tidak ditemukan"));

        String oldCategoryName = ticket.getKategori().getNamaKategori();
        ticket.setKategori(newCategory);
        ticketRepository.save(ticket);

        recordHistory(
                ticket,
                ticket.getStatus(),
                ticket.getStatus(),
                admin,
                "Kategori diubah dari " + oldCategoryName + " menjadi " + newCategory.getNamaKategori()
        );

        return new TicketDtos.UpdateCategoryResponse(ticket.getId(), newCategory.getId());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_KARYAWAN, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_ADMIN, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_SUPERVISOR, allEntries = true)
    })
    public TicketDtos.StatusResponse rejectTicket(Long ticketId, TicketDtos.NoteRequest request, Long adminId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        User admin = findUserOrThrow(adminId);

        TicketStatus oldStatus = ticket.getStatus();
        stateMachine.validateTransition(oldStatus, TicketStatus.REJECTED);

        ticket.setStatus(TicketStatus.REJECTED);
        ticket.setTanggalSelesai(LocalDateTime.now());
        ticketRepository.save(ticket);

        recordHistory(ticket, oldStatus, TicketStatus.REJECTED, admin, request.catatan().trim());

        emailNotificationService.notifyEmployeeTicketUpdated(
                ticket.getDibuatOleh().getEmail(),
                ticket.getDibuatOleh().getNama(),
                ticket.getId(),
                ticket.getJudul(),
                TicketStatus.REJECTED.name(),
                request.catatan().trim()
        );

        return new TicketDtos.StatusResponse(ticket.getId(), ticket.getStatus());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_KARYAWAN, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_ADMIN, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_SUPERVISOR, allEntries = true)
    })
    public TicketDtos.StatusResponse confirmTicket(Long ticketId, Long employeeId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        if (!ticket.getDibuatOleh().getId().equals(employeeId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Hanya pemilik tiket yang dapat mengonfirmasi penyelesaian");
        }

        TicketStatus oldStatus = ticket.getStatus();
        stateMachine.validateTransition(oldStatus, TicketStatus.CLOSED);

        User employee = findUserOrThrow(employeeId);
        ticket.setStatus(TicketStatus.CLOSED);
        if (ticket.getTanggalSelesai() == null) {
            ticket.setTanggalSelesai(LocalDateTime.now());
        }
        ticketRepository.save(ticket);

        recordHistory(ticket, oldStatus, TicketStatus.CLOSED, employee, "Penyelesaian dikonfirmasi oleh pelapor");
        return new TicketDtos.StatusResponse(ticket.getId(), ticket.getStatus());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_KARYAWAN, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_ADMIN, allEntries = true),
            @CacheEvict(value = RedisCacheConfig.CACHE_DASHBOARD_SUPERVISOR, allEntries = true)
    })
    public TicketDtos.StatusResponse reopenTicket(Long ticketId, TicketDtos.NoteRequest request, Long employeeId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        if (!ticket.getDibuatOleh().getId().equals(employeeId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Hanya pemilik tiket yang dapat membuka kembali tiket ini");
        }

        TicketStatus oldStatus = ticket.getStatus();
        stateMachine.validateTransition(oldStatus, TicketStatus.REOPENED);

        User employee = findUserOrThrow(employeeId);
        ticket.setStatus(TicketStatus.REOPENED);
        ticket.setTanggalSelesai(null);
        ticketRepository.save(ticket);

        recordHistory(ticket, oldStatus, TicketStatus.REOPENED, employee, request.catatan().trim());

        if (ticket.getDitugaskanKe() != null) {
            emailNotificationService.notifyEmployeeTicketUpdated(
                    ticket.getDitugaskanKe().getEmail(),
                    ticket.getDitugaskanKe().getNama(),
                    ticket.getId(),
                    ticket.getJudul(),
                    TicketStatus.REOPENED.name(),
                    request.catatan().trim()
            );
        }

        return new TicketDtos.StatusResponse(ticket.getId(), ticket.getStatus());
    }

    private void recordHistory(Ticket ticket, TicketStatus oldStatus, TicketStatus newStatus, User actor, String note) {
        ticketHistoryRepository.save(TicketHistory.builder()
                .ticket(ticket)
                .statusLama(oldStatus)
                .statusBaru(newStatus)
                .diubahOleh(actor)
                .catatan(note)
                .tanggal(LocalDateTime.now())
                .build());
    }

    private Ticket findTicketOrThrow(Long ticketId) {
        return ticketRepository.findDetailById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Tiket tidak ditemukan"));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User tidak ditemukan"));
    }
}