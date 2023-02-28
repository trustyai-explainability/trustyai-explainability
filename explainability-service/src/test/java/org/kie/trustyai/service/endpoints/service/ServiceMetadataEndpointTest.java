package org.kie.trustyai.service.endpoints.service;

import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.data.utils.MetadataUtils;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.service.SchemaItem;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(BaseTestProfile.class)
@TestHTTPEndpoint(ServiceMetadataEndpoint.class)
class ServiceMetadataEndpointTest {

    @Inject
    Instance<MockDatasource> datasource;

    @Inject
    Instance<MockMemoryStorage> storage;

    @BeforeEach
    void clearStorage() {
        storage.get().emptyStorage();
    }

    @Test
    void getTwoObservations() throws JsonProcessingException {
        final Dataframe dataframe = datasource.get().generateRandomDataframe(2);
        datasource.get().saveDataframe(dataframe);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe));

        final ServiceMetadata serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(200)
                .extract()
                .body().as(ServiceMetadata.class);

        assertEquals(0, serviceMetadata.getMetrics().scheduledMetadata.dir);
        assertEquals(0, serviceMetadata.getMetrics().scheduledMetadata.spd);
        assertEquals(2, serviceMetadata.getData().getObservations());
        assertFalse(serviceMetadata.getData().getOutputSchema().isEmpty());
        assertFalse(serviceMetadata.getData().getInputSchema().isEmpty());
        assertEquals(dataframe.getInputNames()
                .stream()
                .filter(name -> !name.equals(MetadataUtils.ID_FIELD))
                .filter(name -> !name.equals(MetadataUtils.TIMESTAMP_FIELD)).collect(Collectors.toList()),
                serviceMetadata.getData().getInputSchema().stream().map(SchemaItem::getName).collect(Collectors.toList()));
        assertEquals(dataframe.getOutputNames(), serviceMetadata.getData().getOutputSchema().stream().map(SchemaItem::getName).collect(Collectors.toList()));
    }

    @Test
    void getThousandObservations() throws JsonProcessingException {
        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000);
        datasource.get().saveDataframe(dataframe);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe));

        final ServiceMetadata serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(200)
                .extract()
                .body().as(ServiceMetadata.class);

        assertEquals(0, serviceMetadata.getMetrics().scheduledMetadata.dir);
        assertEquals(0, serviceMetadata.getMetrics().scheduledMetadata.spd);
        assertEquals(1000, serviceMetadata.getData().getObservations());
        assertFalse(serviceMetadata.getData().getOutputSchema().isEmpty());
        assertFalse(serviceMetadata.getData().getInputSchema().isEmpty());
    }

    @Test
    void getNoObservations() {

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