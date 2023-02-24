package org.kie.trustyai.service.scenarios.nodata;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.endpoints.service.ServiceMetadataEndpoint;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(NoDataTestProfile.class)
@TestHTTPEndpoint(ServiceMetadataEndpoint.class)
class ServiceMetadataEndpointTest {

    @Test
    void get() {
        final ServiceMetadata serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(200)
                .extract()
                .body().as(ServiceMetadata.class);

        assertEquals(0, serviceMetadata.metrics.scheduledMetadata.dir);
        assertEquals(0, serviceMetadata.metrics.scheduledMetadata.spd);
        assertEquals(0, serviceMetadata.data.observations);
        assertTrue(serviceMetadata.data.outputs.isEmpty());
        assertTrue(serviceMetadata.data.inputs.isEmpty());
    }

}