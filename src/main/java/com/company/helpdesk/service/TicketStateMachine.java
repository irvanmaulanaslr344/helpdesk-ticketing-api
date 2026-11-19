package com.company.helpdesk.service;

import com.company.helpdesk.exception.ApiException;
import com.company.helpdesk.exception.ErrorCode;
import com.company.helpdesk.model.TicketStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
public class TicketStateMachine {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(TicketStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(TicketStatus.OPEN, Set.of(
                TicketStatus.IN_PROGRESS,
                TicketStatus.REJECTED
        ));
        ALLOWED_TRANSITIONS.put(TicketStatus.IN_PROGRESS, Set.of(
                TicketStatus.RESOLVED,
                TicketStatus.REOPENED,
                TicketStatus.REJECTED
        ));
        ALLOWED_TRANSITIONS.put(TicketStatus.RESOLVED, Set.of(
                TicketStatus.CLOSED,
                TicketStatus.REOPENED
        ));
        ALLOWED_TRANSITIONS.put(TicketStatus.CLOSED, Set.of(
                TicketStatus.REOPENED
        ));
        ALLOWED_TRANSITIONS.put(TicketStatus.REOPENED, Set.of(
                TicketStatus.IN_PROGRESS
        ));
        ALLOWED_TRANSITIONS.put(TicketStatus.REJECTED, Set.of());
    }

    public void validateTransition(TicketStatus currentStatus, TicketStatus targetStatus) {
        Set<TicketStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new ApiException(
                    ErrorCode.INVALID_TRANSITION,
                    "Transisi status dari " + currentStatus + " ke " + targetStatus + " tidak diizinkan"
            );
        }
    }
}