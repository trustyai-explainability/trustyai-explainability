package org.kie.trustyai.service.endpoints.service;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.profiles.flatfile.PVCTestProfile;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;


@QuarkusTest
@TestProfile(PVCTestProfile.class)
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

}