package dev.jmatias.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** Error body following RFC 9457 (Problem Details). Null/empty members are omitted from the JSON. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        int status,
        String code,
        String title,
        String detail,
        String instance,
        Instant timestamp,
        String traceId,
        List<FieldError> errors) {

    public ErrorResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public record FieldError(String field, String code, String message) {}
}
