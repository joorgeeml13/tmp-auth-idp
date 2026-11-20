package dev.jmatias.shared.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

/** Test-only endpoints that throw, to exercise GlobalExceptionHandler end to end. */
@Path("/test-errors")
public class TestErrorsResource {

    public record Body(@NotBlank String email) {}

    static class Conflict extends CustomException {
        Conflict() {
            super(409, "test.conflict");
        }
    }

    @GET
    @Path("/custom")
    public String custom() {
        throw new Conflict();
    }

    @GET
    @Path("/web")
    public String web() {
        throw new ForbiddenException();
    }

    @GET
    @Path("/boom")
    public String boom() {
        throw new IllegalStateException("secret: select * from account");
    }

    @POST
    @Path("/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    public String validate(@Valid Body body) {
        return "ok";
    }
}
