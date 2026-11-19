package com.company.helpdesk.repository;

import com.company.helpdesk.model.TicketHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

    @EntityGraph(attributePaths = {"diubahOleh"})
    List<TicketHistory> findByTicketIdOrderByTanggalAsc(Long ticketId);
}