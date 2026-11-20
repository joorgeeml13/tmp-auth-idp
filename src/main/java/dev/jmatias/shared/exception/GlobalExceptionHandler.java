package dev.jmatias.shared.exception;

import dev.jmatias.shared.exception.ErrorResponse.FieldError;
import dev.jmatias.shared.i18n.Messages;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Turns every exception into an RFC 9457 problem body. The most specific mapper wins; Throwable is the catch-all. */
@ApplicationScoped
public class GlobalExceptionHandler {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_JSON = "application/problem+json";

    private final Messages messages;

    public GlobalExceptionHandler(Messages messages) {
        this.messages = messages;
    }

    @ServerExceptionMapper
    public Response custom(CustomException e, HttpHeaders headers, UriInfo uri) {
        if (e.getStatus() >= 500) {
            LOG.error("Business error " + e.getMessageKey(), e);
        } else {
            LOG.debugf(e, "Business error %s", e.getMessageKey());
        }
        var locale = locale(headers);
        return problem(e.getStatus(), e.getMessageKey(), statusTitle(e.getStatus(), locale),
                messages.get(e.getMessageKey(), locale, e.getArgs()), List.of(), uri);
    }

    @ServerExceptionMapper
    public Response validation(ConstraintViolationException e, HttpHeaders headers, UriInfo uri) {
        var errors = e.getConstraintViolations().stream()
                .map(v -> new FieldError(field(v), v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(), v.getMessage()))
                .toList();
        return problem(400, "error.validation", messages.get("error.validation", locale(headers)), null, errors, uri);
    }

    @ServerExceptionMapper
    public Response web(WebApplicationException e, HttpHeaders headers, UriInfo uri) {
        int status = e.getResponse().getStatus();
        if (status < 400) {
            return e.getResponse(); // redirects must stay untouched
        }
        if (status >= 500) {
            LOG.error("WebApplicationException " + status, e);
        }
        return problem(status, "error.status." + status, statusTitle(status, locale(headers)), null, List.of(), uri);
    }

    @ServerExceptionMapper
    public Response unexpected(Throwable e, HttpHeaders headers, UriInfo uri) {
        LOG.error("Unhandled exception", e); // stack trace only goes to the log, never to the client
        return problem(500, "error.internal", messages.get("error.internal", locale(headers)), null, List.of(), uri);
    }

    private Response problem(int status, String code, String title, String detail, List<FieldError> errors, UriInfo uri) {
        // traceId stays null until OpenTelemetry is wired in.
        var body = new ErrorResponse(status, code, title, detail, uri.getRequestUri().getPath(), Instant.now(), null, errors);
        return Response.status(status).type(PROBLEM_JSON).entity(body).build();
    }

    private String statusTitle(int status, Locale locale) {
        var key = "error.status." + status;
        var title = messages.get(key, locale);
        return title.equals(key) ? messages.get("error.status.other", locale) : title;
    }

    private Locale locale(HttpHeaders headers) {
        return headers.getAcceptableLanguages().stream().findFirst().orElse(Locale.ENGLISH);
    }

    private String field(ConstraintViolation<?> v) {
        String name = null;
        for (Path.Node node : v.getPropertyPath()) {
            name = node.getName(); // last node: "create.body.email" -> "email"
        }
        return name;
    }
}
