package com.company.helpdesk.repository;

import com.company.helpdesk.model.Ticket;
import com.company.helpdesk.model.TicketStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class TicketSpecification {

    private TicketSpecification() {}

    public static Specification<Ticket> filter(
            Long ownerId,
            TicketStatus status,
            Long kategoriId,
            LocalDate tanggalDari,
            LocalDate tanggalSampai,
            String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (ownerId != null) {
                predicates.add(cb.equal(root.get("dibuatOleh").get("id"), ownerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (kategoriId != null) {
                predicates.add(cb.equal(root.get("kategori").get("id"), kategoriId));
            }
            if (tanggalDari != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("tanggalDibuat"), tanggalDari.atStartOfDay()));
            }
            if (tanggalSampai != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("tanggalDibuat"), tanggalSampai.atTime(LocalTime.MAX)));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("judul")), pattern),
                        cb.like(cb.lower(root.get("deskripsi")), pattern)
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}