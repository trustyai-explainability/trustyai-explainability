package org.kie.trustyai.service.endpoints.service;

import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(ServiceTestProfile.class)
@TestHTTPEndpoint(ServiceMetadataEndpoint.class)
class ServiceMetadataEndpointTest {

    @Inject
    Instance<ServiceDatasource> datasource;

    @Test
    void getTwoObservation() {
        datasource.get().generateRandomDataframe(2);
        final ServiceMetadata serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(200)
                .extract()
                .body().as(ServiceMetadata.class);

        assertEquals(0, serviceMetadata.metrics.scheduledMetadata.dir);
        assertEquals(0, serviceMetadata.metrics.scheduledMetadata.spd);
        assertEquals(2, serviceMetadata.data.observations);
        assertFalse(serviceMetadata.data.outputs.isEmpty());
        assertFalse(serviceMetadata.data.inputs.isEmpty());
        assertEquals(ServiceDatasource.inputNames, serviceMetadata.data.inputs.stream().map(schemaItem -> schemaItem.name).collect(Collectors.toList()));
        assertEquals(ServiceDatasource.outputNames, serviceMetadata.data.outputs.stream().map(schemaItem -> schemaItem.name).collect(Collectors.toList()));
    }

    @Test
    void getThousandObservation() {
        datasource.get().generateRandomDataframe(1000);
        final ServiceMetadata serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(200)
                .extract()
                .body().as(ServiceMetadata.class);

        assertEquals(0, serviceMetadata.metrics.scheduledMetadata.dir);
        assertEquals(0, serviceMetadata.metrics.scheduledMetadata.spd);
        assertEquals(1000, serviceMetadata.data.observations);
        assertFalse(serviceMetadata.data.outputs.isEmpty());
        assertFalse(serviceMetadata.data.inputs.isEmpty());
    }

    @Test
    void getTNoObservation() {
        datasource.get().setDataframe(null);
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