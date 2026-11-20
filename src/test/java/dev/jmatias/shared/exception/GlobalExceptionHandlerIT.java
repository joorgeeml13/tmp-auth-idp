package dev.jmatias.shared.exception;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GlobalExceptionHandlerIT {

    private static final String PROBLEM = "application/problem+json";

    @Test
    void customExceptionKeepsItsStatusAndKey() {
        given().when().get("/test-errors/custom")
                .then().statusCode(409).contentType(PROBLEM)
                .body("status", equalTo(409), "code", equalTo("test.conflict"),
                        "title", equalTo("Conflict"), "instance", equalTo("/test-errors/custom"),
                        "timestamp", notNullValue());
    }

    @Test
    void titleIsTranslatedFromAcceptLanguage() {
        given().header("Accept-Language", "es").when().get("/test-errors/custom")
                .then().statusCode(409).body("title", equalTo("Conflicto"));
    }

    @Test
    void validationErrorListsTheFailingField() {
        given().contentType("application/json").body("{\"email\":\"\"}")
                .when().post("/test-errors/validate")
                .then().statusCode(400).contentType(PROBLEM)
                .body("code", equalTo("error.validation"), "errors[0].field", equalTo("email"),
                        "errors[0].code", equalTo("NotBlank"));
    }

    @Test
    void webApplicationExceptionKeepsItsStatus() {
        given().when().get("/test-errors/web")
                .then().statusCode(403).contentType(PROBLEM).body("code", equalTo("error.status.403"));
    }

    @Test
    void unknownRouteStaysA404NotA500() {
        given().when().get("/nope").then().statusCode(404).contentType(PROBLEM);
    }

    @Test
    void unexpectedErrorIsA500WithoutLeakingInternals() {
        given().header("Accept-Language", "es").when().get("/test-errors/boom")
                .then().statusCode(500).contentType(PROBLEM)
                .body("title", equalTo("Error interno del servidor"), "code", equalTo("error.internal"))
                .body(not(containsString("secret")))
                .body(not(containsString("IllegalStateException")));
    }
}
