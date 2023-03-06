package org.kie.trustyai.service.endpoints.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.resteasy.reactive.RestResponse;
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
import io.restassured.common.mapper.TypeRef;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
@TestProfile(BaseTestProfile.class)
@TestHTTPEndpoint(ServiceMetadataEndpoint.class)
class ServiceMetadataEndpointTest {

    private static final String MODEL_ID = "example1";
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
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);

        final List<ServiceMetadata> serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(200)
                .extract()
                .body().as(new TypeRef<List<ServiceMetadata>>() {
                });

        assertEquals(1, serviceMetadata.size());
        assertEquals(0, serviceMetadata.get(0).getMetrics().scheduledMetadata.dir);
        assertEquals(0, serviceMetadata.get(0).getMetrics().scheduledMetadata.spd);
        assertEquals(2, serviceMetadata.get(0).getData().getObservations());
        assertFalse(serviceMetadata.get(0).getData().getOutputSchema().getItems().isEmpty());
        assertFalse(serviceMetadata.get(0).getData().getInputSchema().getItems().isEmpty());
        assertEquals(dataframe.getInputNames()
                .stream()
                .filter(name -> !name.equals(MetadataUtils.ID_FIELD))
                .filter(name -> !name.equals(MetadataUtils.TIMESTAMP_FIELD)).collect(Collectors.toList()),
                serviceMetadata.get(0).getData().getInputSchema().getItems().stream().map(SchemaItem::getName).collect(Collectors.toList()));
        assertEquals(dataframe.getOutputNames(), serviceMetadata.get(0).getData().getOutputSchema().getItems().stream().map(SchemaItem::getName).collect(Collectors.toList()));
    }

    @Test
    void getThousandObservations() throws JsonProcessingException {
        final Dataframe dataframe = datasource.get().generateRandomDataframe(1000);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        datasource.get().saveMetadata(datasource.get().createMetadata(dataframe), MODEL_ID);

        final List<ServiceMetadata> serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(200)
                .extract()
                .body().as(new TypeRef<List<ServiceMetadata>>() {
                });

        assertEquals(1, serviceMetadata.size());
        assertEquals(0, serviceMetadata.get(0).getMetrics().scheduledMetadata.dir);
        assertEquals(0, serviceMetadata.get(0).getMetrics().scheduledMetadata.spd);
        assertEquals(1000, serviceMetadata.get(0).getData().getObservations());
        assertFalse(serviceMetadata.get(0).getData().getOutputSchema().getItems().isEmpty());
        assertFalse(serviceMetadata.get(0).getData().getInputSchema().getItems().isEmpty());
    }

    @Test
    void getNoObservations() {
        datasource.get().empty();
        final List<ServiceMetadata> serviceMetadata = given()
                .when().get()
                .then()
                .statusCode(RestResponse.StatusCode.OK)
                .extract()
                .body().as(new TypeRef<List<ServiceMetadata>>() {
                });

        assertEquals(0, serviceMetadata.size());
    }

}