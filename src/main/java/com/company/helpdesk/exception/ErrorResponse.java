package com.company.helpdesk.exception;

import java.util.List;

public record ErrorResponse(
        String error,
        String message,
        List<String> details
) {
    public static ErrorResponse of(ErrorCode code, String message, List<String> details) {
        return new ErrorResponse(code.name(), message, details == null ? List.of() : details);
    }
}