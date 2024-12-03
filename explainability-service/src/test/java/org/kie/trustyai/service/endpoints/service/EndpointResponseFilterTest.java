package org.kie.trustyai.service.endpoints.service;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.payloads.data.download.DataRequestPayload;
import org.kie.trustyai.service.profiles.flatfile.DisabledEndpointsTestProfile;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(DisabledEndpointsTestProfile.class)
class EndpointResponseFilterTest {

    @Test
    void test404() {
        String endpoint = "/madeUpNonExistentEndpoint";
        given()
                .when().get(endpoint)
                .then()
                .statusCode(404)
                .body(is(String.format(EndpointResponseFilter.NOT_FOUND_MESSAGE_FMT, endpoint)));
    }

    @Test
    void test404Disabled() {
        String endpoint = "/data/download";
        given()
                .when()
                .contentType(ContentType.JSON)
                .body(new DataRequestPayload())
                .when()
                .post(endpoint).peek()
                .then()
                .statusCode(404)
                .body(is(String.format(EndpointResponseFilter.NOT_FOUND_MESSAGE_FMT, endpoint)));
    }

}
