package org.kie.trustyai.service.scenarios.nodata;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.endpoints.service.ServiceMetadataEndpoint;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(BaseTestProfile.class)
@TestHTTPEndpoint(ServiceMetadataEndpoint.class)
class ServiceMetadataEndpointTest {

    @Inject
    Instance<MockMemoryStorage> storage;

    @BeforeEach
    void emptyStorage() {
        storage.get().emptyStorage();
    }

    @Test
    void get() {
        final ServiceMetadata serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(200)
                .extract()
                .body().as(ServiceMetadata.class);

        assertEquals(0, serviceMetadata.getMetrics().scheduledMetadata.dir);
        assertEquals(0, serviceMetadata.getMetrics().scheduledMetadata.spd);
        assertEquals(0, serviceMetadata.getData().getObservations());
        assertTrue(serviceMetadata.getData().getOutputSchema().isEmpty());
        assertTrue(serviceMetadata.getData().getInputSchema().isEmpty());
    }

}