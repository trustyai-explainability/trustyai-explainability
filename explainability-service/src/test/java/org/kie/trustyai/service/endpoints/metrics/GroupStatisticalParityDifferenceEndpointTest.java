package org.kie.trustyai.service.endpoints.metrics;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceResponse;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
@TestProfile(MetricsEndpointTestProfile.class)
@TestHTTPEndpoint(GroupStatisticalParityDifferenceEndpoint.class)
class GroupStatisticalParityDifferenceEndpointTest {

    @Test
    void spdGet() {
        when().get()
                .then()
                .statusCode(405)
                .body(is(""));
    }

    @Test
    void spdPostCorrect() {
        final Map<String, Object> payload = RequestPayloadGenerator.correct();

        final GroupStatisticalParityDifferenceResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(200)
                .extract()
                .body().as(GroupStatisticalParityDifferenceResponse.class);

        assertEquals("metric", response.type);
        assertEquals("SPD", response.name);
        assertFalse(Double.isNaN(response.value));
    }

    @Test
    void spdPostIncorrectType() {
        final Map<String, Object> payload = RequestPayloadGenerator.incorrectType();

        final GroupStatisticalParityDifferenceResponse response = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(200)
                .extract()
                .body().as(GroupStatisticalParityDifferenceResponse.class);

        assertEquals("metric", response.type);
        assertEquals("SPD", response.name);
        assertEquals(0.0, response.value);
    }

    @Test
    void spdPostIncorrectInput() {
        final Map<String, Object> payload = RequestPayloadGenerator.incorrectInput();

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post()
                .then()
                .statusCode(500)
                .body(is(""));

    }

}