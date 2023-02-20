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
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(MetricsEndpointTestProfile.class)
@TestHTTPEndpoint(DisparateImpactRatioEndpoint.class)
class DisparateImpactRatioEndpointTest {

    @Test
    void dirGet() {
        when().get()
                .then()
                .statusCode(405)
                .body(is(""));
    }

    @Test
    void dirPostCorrect() {
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
        assertEquals("DIR", response.name);
        assertFalse(Double.isNaN(response.value));
    }

    @Test
    void dirPostIncorrectType() {
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
        assertEquals("DIR", response.name);
        assertTrue(Double.isNaN(response.value));
    }

    @Test
    void dirPostIncorrectInput() {
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